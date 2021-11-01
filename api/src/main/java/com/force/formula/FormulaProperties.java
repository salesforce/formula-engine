package com.force.formula;

/**
 *
 * @author djacobs
 * @since 140
 */
public class FormulaProperties implements Cloneable {

    @Override
    protected FormulaProperties clone() throws CloneNotSupportedException {
        return (FormulaProperties)super.clone();
    }

    public void setGenerateSQL(boolean generateSQL) {
        this.generateSQL = generateSQL;
    }

    public boolean getGenerateSQL() {
        return generateSQL;
    }
    
    public void setGenerateJavascript(boolean generateJavascript) {
        this.generateJavascript = generateJavascript;
    }

    public boolean getGenerateJavascript() {
        return generateJavascript;
    }

    public void setThrowOnUnavailableField(boolean newValue) {
        this.throwOnUnavailableField = newValue;
    }

    public boolean getThrowOnUnavailableField() {
        return throwOnUnavailableField;
    }

    public void setTreatNullNumberAsZero(boolean treatNullNumberAsZero) {
        this.treatNullNumberAsZero = treatNullNumberAsZero;
    }

    public boolean getTreatNullNumberAsZero() {
        return treatNullNumberAsZero;
    }

    public boolean isDisabled() {
        return this.isDisabled;
    }

    public void setDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
    }

    public void setAllowCycles(boolean allowCycles) {
        this.allowCycles = allowCycles;
    }

    /**
     * @return return whether cycles are allowed or not in this formula (i.e. is this formula self-referencable, kl)
     */
    public boolean getAllowCycles() {
        return this.allowCycles;
    }

    public boolean isPolymorphicReturnType() {
        return this.polymorphicReturnType;
    }

    public void setPolymorphicReturnType(boolean polymorphicReturnType) {
        this.polymorphicReturnType = polymorphicReturnType;
    }

    public boolean getParseAsTemplate() {
        return parseAsTemplate;
    }

    public void setParseAsTemplate(boolean parseAsTemplate) {
        this.parseAsTemplate = parseAsTemplate;
    }

    public boolean getFailOnEmbeddedFormulaExceptions() {
        return failOnEmbeddedFormulaParserExceptions;
    }

    public void setFailOnEmbeddedFormulaExceptions(boolean failOnEmbeddedFormulaParserExceptions) {
        this.failOnEmbeddedFormulaParserExceptions = failOnEmbeddedFormulaParserExceptions;
    }

    public boolean getIgnoreFailOnEmbeddedFormulaParserExceptionsForParsing() {
        return ignoreFailOnEmbeddedFormulaParserExceptionsForParsing;
    }

    public void setIgnoreFailOnEmbeddedFormulaParserExceptionsForParsing(boolean ignoreFailOnEmbeddedFormulaParserExceptionsForParsing) {
        this.ignoreFailOnEmbeddedFormulaParserExceptionsForParsing = ignoreFailOnEmbeddedFormulaParserExceptionsForParsing;
    }

    public boolean getAllowBadRelatedFieldReferences() {
        return allowBadRelatedFieldReferences;
    }

    public void setAllowBadRelatedFieldReferences(boolean allowBadRelatedFieldReferences) {
        this.allowBadRelatedFieldReferences = allowBadRelatedFieldReferences;
    }


    public boolean getCheckDecodedSize() {
        return checkDecodedSize;
    }

    public void setCheckDecodedSize(boolean checkDecodedSize) {
        this.checkDecodedSize = checkDecodedSize;
    }

    public boolean getAllowSubscripts() {
        return allowSubscripts;
    }
    
    public void setAllowSubscripts(boolean allow) {
        this.allowSubscripts = allow;   
    }

    public void setParseAsReference(boolean parseAsReference) {
        this.parseAsReference = parseAsReference;
    } 
    
    public boolean getParseAsReference() {
        return parseAsReference;
    }
    
    public int getMaxSqlSize() {
        return this.maxSqlSize;
    }

    /**
     * The max size has an upper bound. Think about performance impact if 
     * a large value is needed. 
     * @param maxSqlSize the maximum sql size for the formula
     */
    public void setMaxSqlSize(int maxSqlSize) {
        if (maxSqlSize > FormulaInfo.MAX_SQL_SIZE_LIMIT) {
            throw new IllegalArgumentException("value too large");
        }
        this.maxSqlSize = maxSqlSize;
    }
    
    
    // This value being true means formula is being parsed for its execution on a given record.
    public boolean isExistingFormula() {
        return this.existingFormula;
    }

    public void setExistingFormula(boolean existingFormula) {
        this.existingFormula = existingFormula;
    }

    private boolean throwOnUnavailableField = true;
    private boolean generateSQL = true;
    private boolean generateJavascript = false;
    private boolean treatNullNumberAsZero = true;
    private boolean allowCycles;
    private boolean polymorphicReturnType;
    private boolean parseAsTemplate;

    /**
     * name of this flag is misleading: it's for ignoring parsing errors as well as invalid/inaccessible fields
     */
    private boolean failOnEmbeddedFormulaParserExceptions = true;

    /**
     * this flag is for ignoring failOnEmbeddedFormulaParserExceptions flag when it comes to parsing
     * (i.e. fail on parsing exceptions but continue to ignore invalid/inaccessible fields)
     */
    private boolean ignoreFailOnEmbeddedFormulaParserExceptionsForParsing = false;

    private boolean isDisabled;
    private boolean allowBadRelatedFieldReferences = true;
    private boolean checkDecodedSize = true;
    private boolean allowSubscripts = false;
    private boolean parseAsReference = false;
    private int maxSqlSize = FormulaInfo.MAX_SQL_SIZE_NEW_FORMULA;
    private boolean existingFormula = false;
}
