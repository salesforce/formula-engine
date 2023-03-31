package com.force.formula.v2;

import com.force.formula.FormulaDataType;

import java.util.Map;

public interface IFormulaExecutor<T,U> {
    String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, T testEntity, U additionalTester);
}
