package sfdc.formula.impl;

import sfdc.formula.FormulaException;
import sfdc.formula.SQLPair;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 144
 */
public interface FormulaSQLProvider {
    SQLPair getSQL(TableAliasRegistry registry) throws FormulaException;
}
