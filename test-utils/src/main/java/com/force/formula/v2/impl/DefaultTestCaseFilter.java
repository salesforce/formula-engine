package com.force.formula.v2.impl;

import com.force.formula.v2.IFormulaTestCaseFilter;
import com.force.formula.v2.data.FormulaTestDefinition;

import java.util.List;

/**
 * This filter is a default filter which is a pass through filter.
 */
public class DefaultTestCaseFilter implements IFormulaTestCaseFilter<FormulaTestDefinition> {

    /**
     * Implementation of a pass through filter
     * @param formulaTestCaseInfo a list of FormulaTestDefinition that needs to be filtered
     * @return a list of FormulaTestDefinition after applying the filter
     */
    @Override
    public List<FormulaTestDefinition> filter(List<FormulaTestDefinition> formulaTestCaseInfo) {
        return formulaTestCaseInfo;
    }
}
