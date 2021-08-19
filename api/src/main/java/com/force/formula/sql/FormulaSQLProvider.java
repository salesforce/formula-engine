package com.force.formula.sql;

import com.force.formula.FormulaException;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 144
 */
public interface FormulaSQLProvider {
    SQLPair getSQL(ITableAliasRegistry registry) throws FormulaException;
}
