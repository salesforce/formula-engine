/**
 *
 */
package com.force.formula.impl;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.force.formula.ContextualFormulaFieldInfo;
import com.force.formula.Formula;
import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandVisitor;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaEngineHooks;
import com.force.formula.FormulaEvaluationException;
import com.force.formula.FormulaException;
import com.force.formula.FormulaFieldReferenceInfo;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.FormulaSchema;
import com.force.formula.FormulaTypeWithDomain;
import com.force.formula.InvalidFieldReferenceException;
import com.force.formula.UnsupportedTypeException;
import com.force.formula.commands.FieldReferenceCommand;
import com.force.formula.commands.FieldReferenceCommandInfo;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.impl.sql.FormulaDefaultSqlStyle;
import com.force.formula.sql.FormulaSqlStyle;
import com.force.formula.sql.FormulaWithSql;
import com.force.formula.sql.ITableAliasRegistry;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaFieldReferenceInfoImpl;
import com.force.formula.util.FormulaGeolocationService;
import com.force.i18n.BaseLocalizer;
import com.google.common.base.CharMatcher;

/**
 * The formula engine needs to dig into the specific details of the implementation
 * at times.  This interface is a central stop for all of these "hooks", but
 * is provided with a default.
 *
 * You want to override this and call FormulaEngine.setHooks()
 * @author stamm
 */
public interface FormulaValidationHooks extends FormulaEngineHooks {

    enum ParseOption {
        PARSE_USING_ANTLR2_ONLY,
        PARSE_USING_ANTLR4_ONLY,
        PARSE_USING_BOTH_BUT_RETURN_ANTLR2,
        PARSE_USING_BOTH_BUT_RETURN_ANTLR4
    }

    class ParsingMetrics {
        public ParseOption parseOption;

        //total is always set
        public Duration totalElapsed;

        //these 3 are only non-zero if we are running both ANTLR versions
        public Duration antlr2Elapsed;
        public Duration antlr4Elapsed;
        public Duration comparisonElapsed;

        public JvmMetrics before;
        public JvmMetrics after;

        public ParsingMetrics() {
            parseOption = ParseOption.PARSE_USING_ANTLR2_ONLY;

            totalElapsed = Duration.ZERO;

            antlr2Elapsed = Duration.ZERO;
            antlr4Elapsed = Duration.ZERO;
            comparisonElapsed = Duration.ZERO;

            before = new JvmMetrics();
            after = new JvmMetrics();
        }
    }

    static FormulaValidationHooks get() {
        return (FormulaValidationHooks)FormulaEngine.getHooks();
    }
    
	/**
	 * @return true, if the SQL generated should be in the postgres style, instead of the
	 * oracle style.  Mostly around casts
	 */
    @Override
	default FormulaSqlStyle getSqlStyle() {
		return FormulaDefaultSqlStyle.POSTGRES;
	}

    @Override
	default FormulaGeolocationService getFormulaGeolocationService() {
    	return GeolocationServiceImpl.getInstance();
	}

	/**
     * Validate that the given command isn't disallowed in the current formula context.
     *
     * Used often to prevent references to forbidden fields or global references
     * @param node the current parse node
     * @param command the command being parsed
     * @param context the current formula context
     * @throws FormulaException if an exception occurs during validation
     */
    default void parseHook_validateCommandInfoInContext(FormulaAST node, FormulaCommandInfo command, FormulaContext context) throws FormulaException {
    }

    /**
     * For contextual formulas, the reference may be a column inside a field, and we want to mark both.
     * That sort of details
     * TODO SLT FIXME: This should be handled in FieldSchema
     * @param fieldReferences the set of current fieldReferences to add to
     * @param referencedFieldInfo the current field that may be compound
     * @param context the formula context needed when creating a fieldInfoAdapter
     */
    default void parseHook_addExtraReferences(Set<ContextualFormulaFieldInfo> fieldReferences, ContextualFormulaFieldInfo referencedFieldInfo, FormulaContext context) {
    }

    /**
     * Check for cycles in field references, where the other referenced names might be formulae themselves
     * @param context the current context
     * @param fieldName the current "field" being evaluated
     * @param referencedNames the fields referenced in the current formula
     * @throws FormulaException if an exception occurs
     */
    default void parseHook_checkForCycles(FormulaContext context, String fieldName, Set<String> referencedNames) throws FormulaException {
    }

    /**
     * @return whether in the current context, we should ignore the sql text length limit (like when installing package
     */
    default boolean parseHook_ignoreSqlTextLengthLimit() {
        return false;
    }

    /**
     * Because it has to generate a Thunk to delay evaluation of vlookupto the end,
     * we need to know about it when parsing or else performance is bad
     *
     * TODO SLT FIXME make the parser better at this
     * @param commandInfo the VLookup command being parsed
     * @param vlookupCommands the set of commands inside the vlookup
     * @return Generate a function VLookup.
     * @throws FormulaException if an exception occurs during the VLookup creation
     */
    default FormulaCommand parseHook_generateFunctionVLookup(FormulaCommandInfo commandInfo,
            List<FormulaCommand>  vlookupCommands) throws FormulaException {
        return null;
    }

    /**
     * We need to create a special predict command, similar to VLOOKUP that has knowledge of
     * it's parameters. This is used for bulk evaluation
     * @param commandInfo the command being parsed
     * @param predictCommands the parameters to the command (i.e. the predictions)
     * @return the Prediction function (SFDC specific)
     */
    default FormulaCommand parseHook_generateFunctionPredict(FormulaCommandInfo commandInfo,
            List<FormulaCommand> predictCommands) {
        return null;
    }

    /**
     * Validate the current ast to make sure there isn't illegal use of javascript in the formula output.
     * Since this is highly variable based on the use case and other "allowed" exception in the app, it's
     * in a property hook
     * @param ast the current formula node
     * @param name the name of the node
     * @param command the command for the node
     * @param attributes the current attributes of the formula to set based on use case (GETS_SESSION_ID means you can't use image)
     * @param formulaProperties the formula properties
     * @throws FormulaException if an exception occurs during validation
     */
    default void parseHook_validateJavascriptInCommands(FormulaAST ast, String name, FormulaCommand command, BitSet attributes, FormulaProperties formulaProperties) throws FormulaException {
        // default is no validation
    }

    /**
     * Hook for allowing the engine to test for an If statement that is to nested and fail or log it
     * @param astRoot the root of the IF statement
     * @param source the original source of the formula
     * @param sqlPair the SQLPair associated with the ifs
     * @throws FormulaException if an exception occurs during validation
     */
    default void parseHook_validateNestedIf(FormulaAST astRoot, String source, SQLPair sqlPair) throws FormulaException {
    }
    
    /**
     * @return True, if nested-IFs in the FormulaAST should be converted to the equivalent IFS function subtree.
     */
    default boolean parseHook_shouldOptimizeNestedIfs() {
        return false; //TODO(arman): default is false for now since we have not implemented FunctionIfs.java class yet
    }

    /**
     * Log the fact that a javascript formula was generated.  Use only under duress.
     * @param runTimeInitial the runtime of the formula
     * @param size the Javascript size
     * @param completed whether it was completed
     * @param formula the formula that was evaluated (which should be logged tokenized)
     */
    default void parseHook_logOfflineFormula(long runTimeInitial, int size, boolean completed, String formula) {
        // Don't log by default because it's super expensive and may contain customer data
    }

    /**
     * Log the fact that a parsed formula couldn't be determined to be deterministic or not.  This usually means there's
     * a programming error, but it's minor enough to be dealt with asynchronously.
     * @param x the exception that occurred
     * @param fieldName the name of the field
     */
    default void parseHook_logDeterminismFailure(FormulaException x, String fieldName) {
        //Gack.sendGack(GackLevel.SEVERE, GackID.DEFAULT, "Non-Blocking Exception", "Formula exception on " + fieldName, x);
    }

    /**
     * Log the fact that ANTLR4 threw an unexpected exception
     * @param formula the formula value 
     * @param properties the properties for parsing
     * @param e the error that wasn't expected
     */
    default void parseHook_logANTLR4Failure(String formula, FormulaProperties properties, Throwable e) {

    }

    /**
     * Log parsing-related metrics.
     * The goal is to find formulas that take abnormally longer to parse, so we can optimize the parser for them.
     * @param formula the code of the formula
     * @param properties the properties to use while parsing
     * @param parsingMetrics the parsing metrics
     */
    default void parseHook_logParsingMetrics(String formula, FormulaProperties properties, ParsingMetrics parsingMetrics) {

    }

    /**
     * If running in PARSE_USING_BOTH_BUT_RETURN_ANTLR2 parsing mode, this hook will determine whether
     * ANTLR4 should be skipped (e.g. ANTLR2 took too long and we don't want to spend more time parsing).
     * @param formula the code of the formula
     * @param properties the properties to use while parsing
     * @param antlr2Duration the time it took to parse with antlr2.
     * @return whether anltr4 parsing should be skipped
     */
    default boolean parseHook_shouldSkipANTLR4(String formula, FormulaProperties properties, Duration antlr2Duration) {
        return false;
    }

    /**
     * Log that there was a difference between ANTLR2 and ANTLR4 results.
     * @param message the message to log
     * @param formula the formula source
     * @param properties the properties to use while parsing
     */
    default void parseHook_antlr2vs4Failure(String message, String formula, FormulaProperties properties) {
        //Gack.sendGack(GackLevel.LOGGING_ONLY, GackID.DEFAULT, "ANTLR4", message, null);
    }

    /**
     * @param formula the formula source
     * @param properties the properties used to parse.
     * @return which ANTLR version to use to parse or use both
     */
    default ParseOption parseHook_getParseOption(String formula, FormulaProperties properties) {
        return ParseOption.PARSE_USING_ANTLR4_ONLY;
    }

    /**
     * @return JVM metrics for current thread
     */
    default JvmMetrics parseHook_getJvmMetrics() {
        return new JvmMetrics();
    }

    /**
     * @return Whether the given formulaAst has encrypted fields
     * @param context the current context
     * @param ast the formula being parsed
     * @param properties current properties
     * @throws FormulaException whether an exception occured during processing
     */
    default boolean parseHook_hasEncryptedDataReferences(FormulaContext context, FormulaAST ast, FormulaProperties properties) throws FormulaException {
        return false;
    }

    /**
     * @return whether or not to generate javascript, even if the current context says not to.
     * Used mostly in testing.
     */
    default boolean parseHook_generateJavascript() {
        return false;
    }

    /**
     * @return the exception to use when a formula is disabled.  FormulaException isn't runtime and this
     * gets thrown in a non-checked exception case.
     */
    default RuntimeException parseHook_getFormulaDisabledException() {
        return new RuntimeException("Formula Disabled");
        // return new FormulaDisabledException("Formula Disabled"))
    }

    /**
     * Construct a concrete FieldReference command for the current environment
     * @param commandName the comma
     * @param fieldName the durable name of the field reference
     * @param useUnderlyingType should you use the underlying type or the template type
     * @param isRoot whether this is the root of the template, meaning you can just return the value directly
     * @param isDynamicReferenceBase see FormulaFieldReference.isDynamicBase
     * @return a concrete FieldReferenceCommand for the given fieldName
     */
    default FieldReferenceCommand parseHook_constructFieldReferenceCommand(String commandName, String fieldName, boolean useUnderlyingType, boolean isRoot, boolean isDynamicReferenceBase) {
        return new FieldReferenceCommand(commandName, fieldName, useUnderlyingType, isRoot, isDynamicReferenceBase);
    }

    
    /**
     * Get field path of references for the given formulaFieldInfo to get from the original context all way way to the formulafieldInfo.  
     * This involves the need to construct a FormulaFieldReferenceInfo which is specific to the implementation
     * @param formulaFieldInfo the field to look
     * @param handlePersonContact - Whether or not the path should include additional links through person contacts. (Salesforce specific, sorry)
     * @return the field path (in order) to get to formulaFieldInfo
     */
    default List<FormulaFieldReferenceInfo> getFieldPath(ContextualFormulaFieldInfo formulaFieldInfo, boolean handlePersonContact) {
        return null;

    }
    
    /**
     * Certain Sql references for fields and columns may not be valid, and this allows validating those references.  This is a salesforce core-specific hack in all likelihood
     * @param entity the entity of the formula to example
     * @param reference the field being references
     * @param context the current formula context
     * @throws FormulaException if an error occured while evaluating the sql field reference
     */
    default void parseHook_validateSqlFieldReference(FormulaSchema.Entity entity, FormulaSchema.FieldOrColumn reference, FormulaContext context) throws FormulaException {
    }
    
    /**
     * Generate a javascript field reference to a record on the current context.
     * @param foci the field or column to reference
     * @param path the path from the current record
     * @param includeContextPrefix whether or not the field reference should include "context." before the path.
     * @return a javascript expression that will resolve for a reference to the given field
     */
    default String parseHook_generateJsFieldReference(FormulaSchema.FieldOrColumn foci, StringBuilder path, boolean includeContextPrefix) {
        return (includeContextPrefix ? "context." : "") + "record." + path + foci.getName(); 
    }
        
    /**
     * Visit the composite formula for the given visitor.  
     * This hook allows you to migrate contexts if the formulas come from different 
     * @param formula the formula to visit
     * @param visitor the visitor
     * @param fci the field that defined the embedded formula
     */
    default void parseHook_visitCompositeFormulaCommand(Formula formula, FormulaCommandVisitor visitor, FormulaSchema.FieldOrColumn fci) {
        ((FormulaWithSql)formula).visitFormulaCommands(visitor);
    }
    
    /**
     * Allow the sql generator to override field reference sql based on types and other things like currency
     * @param embeddedFormula the formula we're evaluating
     * @param sql the sql generated for the field reference automatically
     * @param fieldPath the field path to the formula
     * @param formulaFieldInfo the formula field info
     * @param registry the table registry
     * @return sql to evaluate this formula
     */
    default String overrideFieldReferenceSql(FormulaWithSql embeddedFormula, String sql, List<FormulaFieldReferenceInfo> fieldPath, ContextualFormulaFieldInfo formulaFieldInfo, TableAliasRegistry registry) {
        return sql;
    }
    
    default List<FormulaFieldReferenceInfo> parseHook_getFieldReferenceInfos(String fieldName, boolean isDynamicBase, FormulaDataType formulaResultDataType,
            FormulaContext formulaContext, ITableAliasRegistry reg) throws UnsupportedTypeException, InvalidFieldReferenceException {
        ContextualFormulaFieldInfo formulaFieldInfo = formulaContext.lookup(fieldName, isDynamicBase);
        FormulaSchema.FieldOrColumn referencedField = formulaFieldInfo == null ? null : formulaFieldInfo.getFieldOrColumnInfo();
        if (referencedField != null) {
            List<FormulaFieldReferenceInfo> result = new ArrayList<>();
            List<FormulaFieldReferenceInfo> fkPath = FieldReferenceCommandInfo.getFieldPath(formulaFieldInfo);
            // If this is a direct reference to a field in the same entity, then the path will be null.
            // Otherwise it will be the series of FK links that must be followed to reach the target entity
            if (fkPath != null) {
                result.addAll(fkPath);
            }
            result.add(new FormulaFieldReferenceInfoImpl(referencedField));
            return result;
        }  
        return null;
    }

    /**
     * Allow the FieldReference to override the return type of the field based on the column type.
     * Extend this if you have extended the formula field types
     * @param columnType the column type expected for the field reference
     * @param node the AST node of the field reference
     * @param context the formula context being evaluates
     * @param domain the domain associated with the FieldInfo of this field reference
     * @return null, or the Type that should be overridden.
     */
    default Type parseHook_getFieldReturnTypeOverride(FormulaDataType columnType, FormulaAST node, FormulaContext context, FormulaSchema.Entity[] domain) {
        return null;
    }
    
    /**
     * @return the SQL Pair for evaluating picklist values in the DB for TEXT(...).  This is... rather difficult
     * @param node the node for the FunctionText.  Use this to lookup what is happening with the pciklist
     * @param context the context for the evaluation
     * @param args the arguments for the TEXT function
     * @param guards the sql guards for the TEXT function
     * @param registry the table registry for subsitution table names
     * @throws FormulaException if an error occured while getting the picklist values
     */
    default SQLPair getPicklistSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) throws FormulaException {
        return null;
    }

    /**
     * If the number of columns for the coordinates isn't 2 (i.e. lat,long), this handles 
     * @param coordinates the SQL strings for the coordinates
     * @param treatNullAsZero whether null should be treated as zero for coordinates
     * @return the SQL to use for coordinates based on a list of columns
     */
    default 
    String[] getDistanceSql(List<String> coordinates, boolean treatNullAsZero) {
        int coordinateCount = coordinates.size();
        if (coordinateCount == 3) {  //Lat, long, XYZ
	        // Compound Geolocation field with XYZ column
	        String xyzColumnValue = coordinates.get(2);
		    String[] xyzStrings = getFormulaGeolocationService().getXyzStrings(xyzColumnValue);
		    if(treatNullAsZero) {
		        String[] origin = getFormulaGeolocationService().getXyzStrings(0, 0);
		        assert xyzStrings.length == 3;
		        for(int i=0; i < xyzStrings.length; i++) {
		        	if (getSqlStyle().isOracleStyle()) {
		        		xyzStrings[i] = "NVL2(" + xyzColumnValue + "," + xyzStrings[i] + "," + origin[i] + ")";
		        	} else {
		        		xyzStrings[i] = "CASE WHEN " + xyzColumnValue + " IS NOT NULL THEN " + xyzStrings[i] + " ELSE " + origin[i] + "END";
		        	}
		        }
		    }
		    return xyzStrings;
        } else {	    
	        throw new IllegalArgumentException("Incorrect number of columns as arguments to distance function. Got: " 
	                + String.join(", ", coordinates));
        }
    }
    
    /**
     * Returns true if the org can use the DATEVALUE() function fixed for Daylight Saving Time. This method depends on
     * the DatevalueFixForDSTEnabled preference. There is a chance that some orgs developed custom workarounds for the
     * older version of the DATEVALUE() that would often return wrong results for timezones observing DST. The
     * preference should give these orgs a grace period to adjust for the fixed version. The preference should be
     * deprecated and this method should be removed in 224. (W-869341 / W-6053217)
     *
     * @return true if the org should use DATEVALUE() fixed for Daylight Saving Time.
     */
    default boolean canUseDatevalueFixedForDST() {
        return true;
    }
    
    /**
     * Defines the short circuit behavior of a command 
     */
    enum ShortCircuitBehavior {
        EVAL_ALL,   // Evaluate 
        THUNK_ALL,  // Thunk all arguments
        THUNK_REST,  // Thunk arguments past the first
        THUNK_VLOOKUP,  // Special case for VLOOKUP
        THUNK_PREDICT,  // Special case for PREDICT
    }
    
    /**
     * @return whether the CASE statement should be considered 
     * @param argumentName the command name of the function
     */
    default ShortCircuitBehavior parseHook_caseShortCircuit(String argumentName) {
        if (argumentName == null) return ShortCircuitBehavior.EVAL_ALL;
        switch (argumentName) {
        case "IF": 
        case "IFS":
            // To make CASE conditional, add it to this branch
            return ShortCircuitBehavior.THUNK_REST;
        case "AND":
        case "OR":
        case "IFERROR":
        case "TEMPLATE":
            // PrevGroupVal and ParentGroupVal arguments are used at parse time only and somewhat costly to evaluate
            // Delay all of the arguments
        case "PREVGROUPVAL":  
        case "PARENTGROUPVAL":
            return ShortCircuitBehavior.THUNK_ALL;
        case "VLOOKUP":
            return ShortCircuitBehavior.THUNK_VLOOKUP;
        case "PREDICT":
            return ShortCircuitBehavior.THUNK_PREDICT;
        default:
            // Fall through
        }
        return ShortCircuitBehavior.EVAL_ALL;
    }
    
    /**
     * @return an IdType with the given target domain.
     * @param domain the domains for the idtype
     */
    default FormulaTypeWithDomain.IdType constructIdType(FormulaSchema.Entity[] domain) {
    	return null;
    }

    /**
     * Construct an IdType with the given target field (where it'll obtain the set of domains from the Field) 
     * @param field the field or column from which to get the domains 
     * @param isSobjectRow if this is a reference to an entire sobject row, instead of a string id.
     * @return an IdType corresponding to that field
     */
    default FormulaTypeWithDomain.IdType constructIdType(FormulaSchema.FieldOrColumn field, boolean isSobjectRow) {
    	return constructIdType(field.getFieldInfo().getFormulaForeignKeyDomains());
    }
    
    /**
     * @return a map of the scales based on currency ISO code for the current system.
     * Defaults to the set of isoCodes in the JDK
     * 
     * Note: the value '2" will be the default used in FunctionFormatCurrency
     */
    default Map<String,Integer> getCurrencyScaleByIsoCode() {
    	return Currency.getAvailableCurrencies().stream().collect(
    			Collectors.toMap(cur->cur.getCurrencyCode(), cur->cur.getDefaultFractionDigits()));
    }
    
    /**
     * @param isoCode the currency ISO code 
     * @return the scale to use for the given isocode
     * Defaults to the JDK currency scale
     * 
     * Note: the value '2" will be the default used for invalid isocodes
     */
    default int getCurrencyScaleForIsoCode(String isoCode) {
    	if (isoCode == null) return 2;
    	try {
	    	Currency curr = Currency.getInstance(isoCode);
	    	return curr != null ? curr.getDefaultFractionDigits() : 2;
    	} catch (IllegalArgumentException ex) {  // If you pass in a random string
    		return 2; 
    	}
    }

    /**
     * Method used by FunctionFormat for validating values passing into MessageFormat.  
     * @param pattern the pattern passed into the function
     * @param args the arguments being passed in to messageformat
     * @return the pattern to use to evaluate the arguments.  
     * @throws FormulaEvaluationException if the pattern is invalid
     */
    default String validateMessageFormat(String pattern, Object[] args) throws FormulaEvaluationException {
        return pattern;
    }

    /**
     * Get the correct time format, where the symbols are converted properly to
     * the users language. This is non-trivial to implement so 
     * @param localizer the current localizer
     * @return the "correct" type format
     * @throws FormulaEvaluationException if the pattern is invalid
     */
    default DateFormat getCorrectShortTimeFormat(BaseLocalizer localizer) throws FormulaEvaluationException {
        return localizer.getTimeFormat();
    }

    
    /**
     * Oracle has a different notion of upper case vs java, especially around Szet and
     * sigma and the turkish I.
     * @param value the value to convert to upper case
     * @param locale the locale to use for the conversion
     * @return string converted to upper case for the given locale
     */
    default String toOracleUpperCase(String value, Locale locale) {
    	// return I18nUtils.toOracleUpperCase(string, locale);
    	/*
        if (locale != null && "tr".equals(locale.getLanguage())) {
            return OracleUpperTable.TURKISH.toUpperCase(value);
        }
        return OracleUpperTable.XWEST_EUROPEAN.toUpperCase(value);
    	*/
    	if (value == null) return null;
    	if (locale == null) {
    		return value.toUpperCase();
    	} else {
    		return value.toUpperCase(locale);
    	}
    }

    /**
     * Oracle has a different notion of upper case vs java, especially around Szet and
     * sigma and the turkish I.
     * @param value the string to convert to upper case
     * @param locale the locale to use for the conversion
     * @return string converted to lower case for the given locale
     */
    default String toOracleLowerCase(String value, Locale locale) {
    	//return I18nUtils.toOracleLowerCase(string, locale);
    	
        String modValue = value;
        if (value != null && value.indexOf('\u0130') != -1) {
            //replace capital dotted I with normal lowercase i
            modValue = CharMatcher.is('\u0130').replaceFrom(value, "\u0069");
        }

        if (modValue == null) {
            return null;
        } else if (locale == null) {
            return modValue.toLowerCase();
        } else {
            return modValue.toLowerCase(locale);
        }
    }
    
    /**
     * @param context the current formula context, if you have the URL encoding in a property
     * @return the URL encoding to use when rendering static markup from a template.
     */
    default String getTemplateUrlEncoding(FormulaRuntimeContext context) {
        return null;
    }
    
    /**
     * @return the URL encoding to use when calling URLEncoding.  Defaults to UTF_8
     */
    default String getUrlEncoding() {
        return null;
    }
    
    
    /**
     * @return whether java and javascript should treat null months in ADDMONTHS() as zero, 
     * and return the passed in date if true.  Defaults to false, which returns null.
     */
    default boolean functionHook_addNullMonthsAsZero() {
        return false;
    }
    
    /**
     * Encode the given string as a URL for use in embedding in a template.  If you do not want to do URL encoding, 
     * override to just return string.
     * @param string the string to encode
     * @param urlEncoding the encoding to use, derived from getUrlEncoding, will possibly be null
     * @return the function encoded
     * @throws UnsupportedEncodingException if the urlEncoding cannot be used to encode string
     */
    default String templateUrlEncodeString(String string, String urlEncoding) throws UnsupportedEncodingException {
        // retrun OptimizedURLEncoder.encode(TextUtil.replaceIncompatibleCharacters(entry.toString(), urlEncoding), urlEncoding);
        return URLEncoder.encode(string, urlEncoding);
    }

    /**
     * When TRUE, CaseSafeId will use sql function to convert an ID, otherwise it will use inline sql to convert an ID.
     *
     * Default false
     */
    default boolean shouldOptimizeCaseSafeIdFunc() {
        return false;
    }
}
