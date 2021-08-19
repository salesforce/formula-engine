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
	JAVASCRIPT_NULLASNULL;

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
		return result;
	}

	@Override
	public boolean isTemplate() {
		return false;
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

