package com.force.formula.v2.impl;

import com.force.formula.v2.IFormulaTestCaseFilter;
import com.force.formula.v2.data.FormulaTestDefinition;

import java.util.List;

/**
 * This filter is a default filter which is a pass through filter.
 */
public class DefaultTestCaseFilter implements IFormulaTestCaseFilter<FormulaTestDefinition> {
    @Override
    public List<FormulaTestDefinition> filter(List<FormulaTestDefinition> formulaTestCaseInfo) {
        return formulaTestCaseInfo;
    }
}
