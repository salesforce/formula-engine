/**
 * 
 */
package com.force.formula;

/**
 * @author stamm
 *
 */
public enum MockFormulaType implements FormulaTypeSpec {
	DEFAULT,
	NULLASNULL,
	JAVASCRIPT,
	JAVASCRIPT_NULLASNULL,
	TEMPLATE,
	TEMPLATE_PARSE,
	DYNAMIC;

	@Override
	public int getMaxLength() {
        return FormulaInfo.MAX_FORMULA_LENGTH;
	}

	@Override
	public FormulaProperties getDefaultProperties() {
		FormulaProperties result =  new FormulaProperties();
		if (this == JAVASCRIPT || this == JAVASCRIPT_NULLASNULL) result.setGenerateJavascript(true);
		if (this == NULLASNULL || this == JAVASCRIPT_NULLASNULL) {
		    result.setTreatNullNumberAsZero(false);
		}
		if (this == TEMPLATE) {
		    result.setGenerateSQL(false);
	    	result.setPolymorphicReturnType(true);
		}
		if (this == TEMPLATE_PARSE) {
		    result.setGenerateSQL(false);
	    	result.setPolymorphicReturnType(true);
	    	result.setParseAsTemplate(true);
		}
		if (this == DYNAMIC) {
		    result.setGenerateSQL(false);
		    result.setAllowSubscripts(true);
	    	result.setPolymorphicReturnType(true);
		}
		return result;
	}

	@Override
	public boolean allowPicklistTextConversion() {
		return this != DYNAMIC;  // To test where picklist text conversion is disallowed
	}

	@Override
	public boolean isTemplate() {
		return this == TEMPLATE || this == TEMPLATE_PARSE;
	}

	@Override
	public int getMaxTreeDepth() {
        return 10;
	}

	@Override
	public String getDisplay() {
		return name();
	}
}

