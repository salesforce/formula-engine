package com.force.formula.impl;

import com.force.formula.*;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public interface FormulaProvider {
    Formula getFormula() throws FormulaException;
    
    FormulaTypeSpec getFormulaType();

    String getSource() throws FormulaException;
}
