/*
 * Created on Dec 8, 2004
 */
package com.force.formula;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter interface to provide for multiple sources of pointwise data access
 *
 * @author dchasman
 * @since 140
 */
public interface FormulaContext extends Tokenizer {
    public static final String DO_NOT_TREAT_NULLS_AS_STRING = "com.force.formula.DO_NOT_TREAT_NULLS_AS_STRING";
    public static final String ALLOW_NULL_MAP_KEY = "com.force.formula.ALLOW_NULL_MAP_KEY";
    public static final String FILTER_ENCRYPTED_FIELDS = "filterEncryptedFields";
    public static final String DO_NOT_USE_DB_VALUE_FOR_PICKLIST_EVALUATION = "com.force.formula.DO_NOT_USE_DB_VALUE_FOR_PICKLIST_EVALUATION";
    public static final String SKIP_PERCENT_VALUE_ADJUST = "com.force.formula.SKIP_PERCENT_VALUE_ADJUST";
    /** When generating javascript or determining references, should formula fields be expanded recursively? If not set, defaults to no. **/
    public static final String EXPAND_FORMULA_REFERENCES = "com.force.formula.EXPAND_FORMULA_REFERENCES";
    public static final String FORMULA_FILTER = "common.config.field.FORMULA_FILTER";
    public static final String CHECK_SQL_LENGTH_LIMIT = "com.force.formula.impl.CHECK_SQL_LENGTH_LIMIT";
    public static final String HIGHPRECISION_JS = "com.force.formula.impl.HIGHPRECISION_JS";
    /** See {@link #jsDatesAreStrings} */
    public static final String JS_DATES_ARE_STRINGS = "com.force.formula.impl.JS_DATES_ARE_STRINGS";

    public static final String IS_CREATE_OR_EDIT_FORMULA = "com.force.formula.impl.IS_CREATE_OR_EDIT_FORMULA";

    String getName();

    /**
     * For spanning formulas we want to be able to generate the full context name for the entire context path.  This will
     * traverse up the parent contexts and prepend their names.
     * @param useDurableName
     * @param relativeToContext is to generate the full name relative to this FormulaContext
     */
    String getFullName(boolean useDurableName, FormulaContext relativeToContext);

    FormulaReturnType getFormulaReturnType();

    ContextualFormulaFieldInfo lookup(FormulaFieldReference field) throws InvalidFieldReferenceException, UnsupportedTypeException;
    ContextualFormulaFieldInfo lookup(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;
    ContextualFormulaFieldInfo lookup(String fieldName, boolean isDynamicRefBase) throws InvalidFieldReferenceException, UnsupportedTypeException;

    DisplayField[] getDisplayFields(FormulaSchema.Entity entityInfo);
    DisplayField getDisplayField(FormulaSchema.ApiElement aei);

    boolean isFunctionSupported(FormulaCommandType command);

    /**
     * NOTE: This method is meant to be used in conjunction with isFunctionSupported (i.e. the default implementation
     * doesn't call isFunctionSupported).
     *
     * @param command the function to check
     * @return true if the given function is supported to run on the client in this context; false otherwise
     */
    default boolean isFunctionSupportedOffline(FormulaCommandType command) {
        return isFunctionSupportedOffline(command, 0);
    }

    /** Allows a context to distinguish support for a function by the number of arguments. e.g. "4 - 5" vs "-5" */
    default boolean isFunctionSupportedOffline(FormulaCommandType command, int numberOfArguments) {
        FormulaCommandType.AllowedContext context = command.getAllowedContext();
        return !context.isInternalSfdcOnly()
                && (context.isOffline() || ("-".equals(command.getName()) && numberOfArguments == 1));
    }

    /**
     * NOTE: Ensure this method is correct (or overridden) for your context before using.
     *
     * @param variableType the type of the global variable, including $ (e.g. $Label)
     * @return true if the given global variable type is supported to run on the client in this context; false otherwise
     */
    default boolean isGlobalVariableSupportedOffline(String variableType) { return false; }

    FormulaContext[] getAdditionalContexts() throws InvalidFieldReferenceException;

    FormulaContext getParentContext();

    GlobalFormulaProperties getGlobalProperties();

    <T> T getProperty(String name);

    void setProperty(String name, Object value);

    /**
     * This method can be overriden by implementing classes to indicate whether or not this context should be included for display
     * in the UI. An example of this is $Recordtype, we still support it internally but don't display it in the UI, this is to avoid breaking already existing formulas.
     *
     * @return true if this context should not be displayed on the UI.
     */
    boolean isUIDeprecated();

    /**
     * Used for internal debugging purposes
     *
     * @return a Map&lt;String, String&gt; of key-value pairs representing information about this context that would be
     *         useful to include while throwing an exception, for example.
     */
    default Map<String, String> getMetaInformation() {
        Map<String, String> metaInfo = new LinkedHashMap<>();
        metaInfo.put("FormulaContext_ContextName", getClass().getSimpleName());
        metaInfo.put("FormulaContext_ReturnType", getFormulaReturnType().getDataType().getName());
        return metaInfo;
    }

    default boolean isCheckingSqlLengthLimit() {
        Boolean isSqlLimitCheck = getProperty(FormulaContext.CHECK_SQL_LENGTH_LIMIT);
        return isSqlLimitCheck == null ? false : isSqlLimitCheck;
    }

    /**
     * @return <tt>true</tt> if javascript should use high precision, instead of native numbers.
     * Currently, this defaults to using the "decimal.js" library, 
     */
    default boolean useHighPrecisionJs() {
        Boolean isHPJs = getProperty(FormulaContext.HIGHPRECISION_JS);
        return isHPJs == null ? false : isHPJs;
    }

    /**
     * @return <tt>true</tt> if, when evaluating JS any date, time, or date/time field will be
     * represented as a string in the fomrat <tt>YYYY-MM-DD HH:MM:SS.mmm</tt>
     */
    default boolean jsDatesAreStrings() {
        Boolean val = getProperty(JS_DATES_ARE_STRINGS);
        return val == null ? true : val;
    }
    
    /**
     * Javascript is case sensitive when naming, so this returns the "canonical" name for the field,
     * but not the durable one (which may be an internal Id in case of renaming)
     * @return the name to use when referencing the field in javascript.
     * @throws InvalidFieldReferenceException 
     */
    default String toJavascriptName(String fieldName) throws InvalidFieldReferenceException {
        return fieldName;
    }

    /**
     * @return how you get to this formula context from javascript.  Used for global references
     * to see if they can be in "context." or should be global.  Defaults to getName().
     */
    default String getJavascriptReference() {
        return getName();
    }
    
    /**
     * @return <tt>true</tt> if postgres syntax should be used for SQL expressions
     */
    boolean isSqlPostgresStyle(); 

    
    /**
     * Implement this marker interface if you have the ability to access all the formula field references
     * locally from javascript on the "Context" variable
     * @author stamm
     * @since 0.2
     * TODO SLT: Make sure PageContextProvider implements it
     */
    interface WithLocalJsContext extends FormulaContext{
        
    }
}
