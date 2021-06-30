/**
 *
 */
package sfdc.formula.impl;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;

import com.google.common.base.CharMatcher;

import sfdc.formula.*;
import sfdc.formula.commands.*;

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
     * Validate that the given command isn't disallowed in the current formula context.
     *
     * Used often to prevent references to forbidden fields or global references
     * @param node the current parse node
     * @param command the command being parsed
     * @param context the current formula context
     * @throws FormulaException
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
     * @throws FormulaException
     */
    default FormulaCommand parseHook_generateFunctionVLookup(FormulaCommandInfo commandInfo,
            List<FormulaCommand>  vlookupCommands) throws FormulaException {
        return null;
    }

    /**
     * We need to create a special predict command, similar to VLOOKUP that has knowledge of
     * it's parameters. This is used for bulk evaluation
     * @param 
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
     * @param properties the current properties of the formula to set based on use case (GETS_SESSION_ID means you can't use image)
     * @param formulaProperties the formula properties
     */
    default void parseHook_validateJavascriptInCommands(FormulaAST ast, String name, FormulaCommand command, BitSet properties, FormulaProperties formulaProperties) throws FormulaException {
        // default is no validation
    }

    /**
     * Hook for allowing the engine to test for an If statement that is to nested and fail or log it
     * @param astRoot
     * @param source
     * @param sqlPair
     * @throws FormulaException
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
     */
    default void parseHook_logOfflineFormula(long runTimeInitial, int size, boolean completed, String formula) {
        // Don't log by default because it's super expensive and may contain customer data
    }

    /**
     * Log the fact that a parsed formula couldn't be determined to be deterministic or not.  This usually means there's
     * a programming error, but it's minor enough to be dealt with asynchronously.
     */
    default void parseHook_logDeterminismFailure(FormulaException x, String fieldName) {
        //Gack.sendGack(GackLevel.SEVERE, GackID.DEFAULT, "Non-Blocking Exception", "Formula exception on " + fieldName, x);
    }

    /**
     * Log the fact that ANTLR4 threw an unexpected exception
     */
    default void parseHook_logANTLR4Failure(String formula, FormulaProperties properties, Throwable e) {

    }

    /**
     * Log parsing-related metrics.
     * The goal is to find formulas that take abnormally longer to parse, so we can optimize the parser for them.
     */
    default void parseHook_logParsingMetrics(String formula, FormulaProperties properties, ParsingMetrics parsingMetrics) {

    }

    /**
     * If running in PARSE_USING_BOTH_BUT_RETURN_ANTLR2 parsing mode, this hook will determine whether
     * ANTLR4 should be skipped (e.g. ANTLR2 took too long and we don't want to spend more time parsing).
     */
    default boolean parseHook_shouldSkipANTLR4(String formula, FormulaProperties properties, Duration antlr2Duration) {
        return false;
    }

    /**
     * Log that there was a difference between ANTLR2 and ANTLR4 results.
     */
    default void parseHook_antlr2vs4Failure(String message, String formula, FormulaProperties properties) {
        //Gack.sendGack(GackLevel.LOGGING_ONLY, GackID.DEFAULT, "ANTLR4", message, null);
    }

    /**
     * @return which ANTLR version to use to parse or use both
     */
    default ParseOption parseHook_getParseOption(String formula, FormulaProperties properties) {
        return ParseOption.PARSE_USING_ANTLR2_ONLY;
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
        return new FieldReferenceCommand(commandName, fieldName, isRoot, isDynamicReferenceBase);
    }

    
    /**
     * Get field path of references for the given formulaFieldInfo to get from the original context all way way to the formulafieldInfo.  
     * This involves the need to construct a FormulaFieldReferenceInfo which is specific to the implementation
     * @param formulaFieldInfo the field to look
     * @param handlePersonContact - Whether or not the path should include additional links through person contacts. (Core specific crap, sorry)
     */
    default List<FormulaFieldReferenceInfo> getFieldPath(ContextualFormulaFieldInfo formulaFieldInfo, boolean handlePersonContact) {
        return null;

    }
    
    /**
     * Certain Sql references for fields and columns may not be valid, and this allows validating those references.  This is a sfdc core-specific hack in all likelihood
     * @param entity the entity of the formula to example
     * @param reference the field being references
     * @param context the current formula context
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
     * @param columnType
     * @param node
     * @param context
     * @param domain
     * @return
     */
    default Type parseHook_getFieldReturnTypeOverride(FormulaDataType columnType, FormulaAST node, FormulaContext context, FormulaSchema.Entity[] domain) {
        return null;
    }
    
    /**
     * Return the SQL Pair for evaluating picklist values in the DB.  This is... rather difficult
     * @param node the node for the FunctionText
     * @param context the context in which is 
     * @param args
     * @param guards
     * @param registry
     * @return
     */

    default SQLPair getPicklistSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) throws FormulaException {
        return null;
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
     */
    default ShortCircuitBehavior parseHook_caseShortCircuit(String argumentName) {
        if (argumentName == null) return ShortCircuitBehavior.EVAL_ALL;
        switch (argumentName) {
        case "IF":
            // To make CASE conditional, add it to this branch
            return ShortCircuitBehavior.THUNK_REST;
        case "AND":
        case "OR":
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
     * Construct an IdType with the given target domain.
     * @param domain
     * @return
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
     * Oracle has a different notion of upper case vs java, especially around Szet and
     * sigma and the turkish I.
     * @param string the value to convert to upper case
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

}
