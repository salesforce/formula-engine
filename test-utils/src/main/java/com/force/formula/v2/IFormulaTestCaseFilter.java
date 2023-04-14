package com.force.formula.v2;

import java.util.List;

/**
 * An interface to filter formula test definitions
 *
 * @param <T> formula test definition object
 */
public interface IFormulaTestCaseFilter<T> {
    /**
     * A method that filters formula test definitions
     * @param formulaTestCaseInfo a list of unfiltered formula test definitions
     * @return a list of filtered formula test definitions
     */
    List<T> filter(List<T> formulaTestCaseInfo);
}
