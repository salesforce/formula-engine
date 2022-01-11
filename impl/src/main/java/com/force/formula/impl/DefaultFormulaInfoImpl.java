/**
 * 
 */
package com.force.formula.impl;

import com.force.formula.*;
import com.force.formula.sql.FormulaWithSql;
import com.force.formula.sql.RuntimeSqlFormulaInfo;

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
	public FormulaWithSql getFormula() throws FormulaException {
		return (FormulaWithSql) super.getFormula();
	}

}
