package com.force.formula.v2;

import java.util.List;

public interface IFormulaTestCaseFilter<T> {
    List<T> filter(List<T> formulaTestCaseInfo);
}
