package sfdc.formula.impl;

import sfdc.formula.FormulaException;
import sfdc.formula.RuntimeFormulaInfo;

/**
 * Convenience method to return a FormulaWithSql for getFormula for callers that interact with SQL. 
 * @author stamm
 *
 */
public interface RuntimeSqlFormulaInfo extends RuntimeFormulaInfo {
    /**
     * Return the runtime representation of this object
     * @return Formula object
     */
    @Override
    FormulaWithSql getFormula() throws FormulaException;
}
