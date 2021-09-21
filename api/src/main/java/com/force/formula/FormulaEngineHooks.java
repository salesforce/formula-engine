package com.force.formula;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

import com.force.formula.sql.FormulaSqlStyle;
import com.force.formula.util.FormulaI18nUtils;
import com.force.formula.util.FormulaTextUtil;
import com.force.i18n.BaseLocalizer;
import com.force.i18n.LocalizerFactory;
import com.google.common.collect.Lists;

/**
 * The formula engine needs to dig into the specific details of the api at times.
 * This interface is a central stop for all of these "hooks", but is provided with a default.
 *
 * NOTE: You most definitely want to extend FormulaValidationHooks
 * @author stamm
 *
 */
public interface FormulaEngineHooks {
	/**
	 * @return true, if the SQL generated should be in the postgres style, instead of the
	 * oracle style.  Mostly around casts.  The implementation has the default behavior
	 */
	default FormulaSqlStyle getSqlStyle() {
		return null;
	}

	/**
	 * When testing for nullability, there are some complex objects that may "appear"
	 * null but won't actually be a null reference
	 * @param value the value to check for null,
	 * @return <tt>null</tt> if value should be treated as a null value for comparisons
	 */
	default Object hook_unwrapForNullable(Object value) {
		if (value instanceof FormulaDataValue && ((FormulaDataValue)value).isEmpty()) {
			return null;
		}
		return value;
	}

	/**
	 * @return whter a test calendar should be used for the current time in SQL.
	 */
	default boolean useTestCalendarForNow() {
		return false;
	}

	/**
	 * Adjust the calendar for a test environment so that the SQL can be generated in a constant way.
	 * @param cal
	 */
    default void adjustCalendarForTestEnvironment(Calendar cal) {
    }

    /**
     * @return the current time to use when unit testing (since you want the time to be constant)
     */
    default FormulaTime testHook_getCurrentTime() {
    	return null;
    }

    /**
     * @return false if you don't care if the true and false paths of IF match (which is how templates work)
     */
    default boolean functionHook_validateIfFunctionTypeEquality() {
    	return true;
    }

    /**
     * Convert the checked exception for FormulaDateException to a user-manageable Date Exception
     * @param ex
     */
	default void handleFormulaDateException(FormulaDateException ex) {
		throw new RuntimeException(ex);
	}

	/**
     * Convert the checked exception for FormulaTimeException to a user-manageable Time Exception
     * @param ex
     */
    default void handleFormulaTimeException(FormulaDateException ex) {
        throw new RuntimeException(ex);
    }

	/**
	 * @return the locale for the user (even if the locale has been overridden for the current context)
	 */
	default Locale getCurrentUserLocale() {
		return FormulaI18nUtils.getLocalizer().getLocale();
	}

	/**
	 * @return the type name to use for the value in the error message when there is a type mismatch.
	 * If the value has an "interesting" type, this allows the error message to be overridden.
	 */
	default String getTypeNameForErrorMessage(Object value) {
		return null;
	}

	/**
	 * @return <tt>true</tt> if the container is currently compiling this formula, which means some
	 * actual values might be "fake".  So this allows the validation to be more forgiving.
	 */
	default boolean isFormulaContainerCompiling() {
		return false;
	}

	/**
	 * @return whether the current context should block Javascript formula execution.
	 */
	default boolean shouldBlockJsFormulaExecution() {
		return true;
	}

	/**
	 * Should this object be treated as null when type checking?
	 * @param obj the possibly null object
	 * @return whether the object should be treated as null *and* it is not null
	 */
	default boolean treatObjectAsNull(Object obj) {
		return false;
	}


    /**
     * Determine if we should be allowing subscripts in the given context.
     */
    default boolean formulaAllowSubscripts() {
    	return false;
    }

    /**
     * @return whether to return a boolean result or a null result when comparing two null values.
     * Use true if you want java/groovy semantics, false for oracle style null semantics
     */
    default boolean shouldReturnBoolResult(FormulaRuntimeContext context) {
    	return false;
    }

    /**
     * Construct a Geolocation object
     */
    default FormulaGeolocation constructGeolocation(Number latitude, Number longitude) {
    	return null;
    }
    
    /**
     * Construct a Time object
     */
    default FormulaTime constructTime(Number millis) {
    	//private static final long DAY_TO_MS = 86400000L;
    	return new FormulaTime.TimeWrapper(LocalTime.ofNanoOfDay((millis.longValue() % 86400000L)  * 1000000L));
    }

    /**
     * @return the type for picklists to use
     */
    default Class<?> getPicklistType() {
        return null;
    }

    /**
     * Create an error message for an unsupported type with a possible help link
     * @param name the field name with the exception
     * @param suffix the suffix for the key to FormulaFieldExceptionMessages.UnsupportedTypeException
     * @return an err
     */
    default String createErrorMessageForPicklists(String name, String suffix){
        return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "UnsupportedTypeException" +
                suffix, FormulaTextUtil.escapeToHtml(name), "#");
    	/*
        StringBuilder sb = new StringBuilder();
        // Escape the name of the field for XSS security
        sb.append(I18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", UnsupportedTypeException.class.getName() +
            suffix, TextUtil.escapeToHtml(name), getHelpLink()));
        return sb.toString();
        */
    }

    /**
     * Log information about a formula during runtime (when evaluated or parsed).
     */
    default void logFormulaRuntime(long runTimeInitial, int formulaSize, String commands, String globalVariables,
            int sqlSize, int jsSize, String context, boolean evaluate, Exception exception) {
        // nothing to do
    }

    /**
     * Log information about a formula during runtime (when evaluated or parsed) with fieldName
     * Currently used for Failure/Error use-cases
     */
    default void logFormulaRuntime(long runTimeInitial, int formulaSize, String commands, String globalVariables,
                                   int sqlSize, int jsSize, String context, boolean evaluate, String fieldName, Exception exception) {
        // nothing to do
    }

    /**
     * Log information about a formula during design time when it is being parsed
     */
    default void logFormulaDesignTime(long runTimeInitial, int formulaSize, String commands, String globalVariables,
            Boolean polymorphicFields, String returnType, String context, Exception exception) {
        // nothing to do
    }

    /**
     * Whether or not we should log during runtime. This is to enable sampling in the core app.
     */
    default boolean shouldLogRuntime() {
        return false;
    }
    
    /**
     * For the given field reference and target type, return the underlying value of that reference in the given context
     * @param dataType the target data type
     * @param context
     * @param fieldReference
     * @param useUnderlyingType
     * @return
     */
    default Object getFieldReferenceValue(ContextualFormulaFieldInfo fieldInfo, FormulaDataType dataType, FormulaRuntimeContext context, FormulaFieldReference fieldReference, boolean useUnderlyingType) throws FormulaException  {
        // The caller may need to override this to handle custom data types that are context dependent
        return getAndConvertFieldReferenceValue(dataType, context, fieldReference, useUnderlyingType, false);
    }
    
    /**
     * Get the value from the given context and then convert the type to the given.
     * Caller should override this to deal with custom data types that are *not* context dependent.  
     * @param dt the target data type
     * @param context the formula context to use to evaluate the reference
     * @param fieldReference the field reference
     * @param useUnderlyingType whether to use the templateType or "real" type of the field reference
     * @param escapeStringForSQLGeneration whether to replace single quotes with doubled-single quoted (' -> '')
     * @return the converted value
     */
    default Object getAndConvertFieldReferenceValue(FormulaDataType dt, FormulaRuntimeContext context, FormulaFieldReference fieldReference, boolean useUnderlyingType, boolean escapeStringForSQLGeneration)
                throws FormulaException {
        Object value = null;

        if (dt.isPercent()) {
            BigDecimal percent = context.getNumber(fieldReference);
            if (percent != null) {
                if (Boolean.TRUE.equals(context.getProperty(FormulaContext.SKIP_PERCENT_VALUE_ADJUST))) {
                    value = percent;
                } else {
                    value = percent.movePointLeft(2).stripTrailingZeros();
                }
            }
        } else if (dt.isLocation()) {
            value = context.getLocation(fieldReference);
        } else if (dt.isCurrency()) {
            value = context.getNumber(fieldReference);
            FormulaCurrencyData currency = context.getCurrency(fieldReference);
            if (currency != null) {
                String sourceIsoCode = currency.getIsoCode();
                String targetIsoCode = context.getCurrencyIsoCode();
                if (sourceIsoCode != null && targetIsoCode != null && !sourceIsoCode.equals(targetIsoCode)) {
                    value = convertCurrency((BigDecimal)value, targetIsoCode, false);
                    value = convertCurrency((BigDecimal)value, sourceIsoCode, true);
                }
            }
        } else if (dt.isNumber()) {
            value = context.getNumber(fieldReference);
        } else if (dt.isSimpleTextOrClob() || dt.isId() || dt.isTextEnum() || dt.isAnyPerson()) {
            value = context.getString(fieldReference, !useUnderlyingType);
            if (escapeStringForSQLGeneration) {
                value = FormulaTextUtil.replaceSimple((String)value, "'", "''");
            }
        } else if (dt.isDateOnly()) {
            value = context.getDate(fieldReference);
        } else if (dt.isDate()) {
            value = context.getDateTime(fieldReference);
        } else if (dt.isTimeOnly()) {
            value = context.getTime(fieldReference);
        } else if (dt.isBoolean()) {
            value = context.getBoolean(fieldReference);
        } else if (dt.isPickval() || dt.isMultiEnum()) {
            value = context.getString(fieldReference, !useUnderlyingType);
        } else {
            throw new UnsupportedTypeException(fieldReference.toString(), dt);
        }

        return value;
    }

    /**
     * Convert the currency value with the given isoCode to the "default" currency for the company.
     * @param value the value of the currency
     * @param isoCode the iso code of the currency
     * @param toCanonical whether to convert to the "canonical" currency
     * 
     * Usually this is called in pairs, with (value, targetIsoCode, false) then (value, sourceIsoCode, true)
     * @return the value converted to the 
     */
    default BigDecimal convertCurrency(BigDecimal value, String isoCode, boolean toCanonical) {
        return value;
    }
       
    /**
     * Provide a hook for lookup of a field reference when you want to check for compilation (when you want to turn off security)
     * @param context
     * @param fieldReference
     * @return
     * @throws InvalidFieldReferenceException
     * @throws UnsupportedTypeException
     */
    default ContextualFormulaFieldInfo lookupFieldReferenceForCompile(FormulaContext context, FormulaFieldReference fieldReference) throws InvalidFieldReferenceException, UnsupportedTypeException {
        return context.lookup(fieldReference);
    }

    /**
     * @return the FormulaInformationContext provider when switching between formula contexts across entities
     */
    default FormulaInformationContext.Provider getInformationContextProvider() {
        return null;  // SfdcCtx.formula()
    }
    
    /**
     * @param field the field to check
     * @return whether the field is currently readable for the user.  Often, the admin has to compile formulas for fields they can't read.
     */
    default boolean isFieldReadable(FormulaSchema.Field field) {
        return true;
    }

    /**
     * For certain contexts, like SystemFormulaContext, date time needs to be generated by default, but doing it on every
     * instantiation is expensive.  This allows it to be generated by hand
     * @return the data type 
     */ 
    default FormulaDataType getDataTypeByName(String name) {
        return null; 
    }
    
    /**
     * Allow the indexability for a text formula to be overridable 
     * @param context the formula context for the text function
     * @param picklistFieldName the name of the picklist field.
     * @return whether the text funciton is in
     */
    default boolean isTextFunctionIndexable(FormulaContext context, String picklistFieldName, boolean isDeterministic) {
        return false;
    }
    
    /**
     * Convert the stored value for a picklist to the "canonical" value to use in display.  An example would 
     * be if you store a picklist value as a number, but you should evaulate the value as a string, this does that
     * conversion. 
     * @param toConvert the value specified in the encoded formula for a picklist.  In general, it'll be a String
     * @param picklistFieldInfo the type of picklist to lookup
     * @return the canonical value to use for a picklist
     */
    default Object getTextForPicklist(Object toConvert, FormulaFieldInfo picklistFieldInfo) {
        return toConvert;
    }

    
    /**
     * Convert the field value for which underlying values will match it the underlying engine
     * @param formulaFieldInfo the formula field being evaluated
     * @param fieldValue the value of the field
     * @param context the current context
     * @param forSql if this is for SQL evaluation, instead of a template
     * @param forJs if this is for javascript evaluation offline.
     * @return the list of values that match the fieldValue
     * @throws InvalidFieldReferenceException
     * @throws UnsupportedTypeException
     */
    default List<String> getUnderlyingValuesForPicklist(FormulaFieldInfo formulaFieldInfo, String fieldValue, FormulaContext context, boolean forSql, boolean forJs)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        return Lists.newArrayList("");
    }

    /**
     * Push full access rights for field reference DB lookups in the formula engine
     */
	default void hook_pushFullAccessRights() {}

    /**
     * Pop full access rights after field reference DB lookups in the formula engine
     */
	default void hook_popAccessRights() {}
	
	/**
     * Push given component namespace on the stack. 
     * This is useful mainly for Extension package. Extension package namespace is on the top of the stack, however in order to resolve field
     * we need to push field's namespace.
     */
    default void hook_pushComponentNamespace(String namespace) {}

    /**
     * Pop component namespace from the stack.
     */
    default void hook_popComponentNamespace() {}

    default String getDbTimeZoneID(TimeZone tz) {
    	return tz.getID();
    }
    
    /**
     * @return the salesforce style id converted to 18 characters, if it looks like an Id.  The implementation is 
     * @param id the possible id to convert
     */
    default String convertIdTo18Digits(String id) {
    	return id;
    }
    
    /**
     * For values that are decorated strings (like IDs that have a domain set), convert
     * them to a String for use in string operations.
     * @param obj the object to convert
     * @return the object, or it converted to a String if appropriate.
     */
    default Object convertToString(Object obj) {
    	return obj;
    }

    /**
     * @return the localizer to use for determining the current locale, format, timezone, and label set.
     */
    default BaseLocalizer getLocalizer() {
    	return LocalizerFactory.get().getDefaultLocalizer();
    }
}
