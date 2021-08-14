package com.force.formula.impl;

import com.force.formula.FormulaException;
import com.force.formula.SQLPair;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 144
 */
public interface FormulaSQLProvider {
    SQLPair getSQL(TableAliasRegistry registry) throws FormulaException;
}
