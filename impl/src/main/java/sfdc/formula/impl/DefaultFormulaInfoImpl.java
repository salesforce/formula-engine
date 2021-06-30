/**
 * 
 */
package sfdc.formula.impl;

import sfdc.formula.*;

/**
 * A defualt formula info.  You shouldn't use this and instead extends BaseFormulaInfoImpl yourself
 * @author stamm
 * @since 0.0.1
 */
public final class DefaultFormulaInfoImpl extends BaseFormulaInfoImpl implements RuntimeSqlFormulaInfo {

	public DefaultFormulaInfoImpl(FormulaContext context, String source, FormulaProperties properties)
			throws FormulaException {
		super(context, source, properties);
	}

	public DefaultFormulaInfoImpl(FormulaContext context, String originalSource, FormulaProperties properties,
			boolean existingFormula, boolean forceDisabled, boolean isCreateOrEditFormula) throws FormulaException {
		super(context, originalSource, properties, existingFormula, forceDisabled, isCreateOrEditFormula);
	}

	@Override
	public boolean referenceEncryptedFields() {
		return false;
	}

	@Override
	public boolean hasFormatCurrencyCommand() {
		return false;
	}

	@Override
	public FormulaWithSql getFormula() throws FormulaException {
		return (FormulaWithSql) super.getFormula();
	}

}
