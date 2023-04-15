package com.force.formula.v2;

import com.force.formula.FormulaDataType;

import java.util.Map;

/**
 * An interface that provides functionality to execute a formula
 *
 * @param <T> testEntity for which the formula needs to be executed for
 * @param <U> any additional tester that can be used for execution can be plugged here
 */
public interface IFormulaExecutor<T,U> {
    /**
     * A method that executes a formula and returns its result
     *
     * @param formulaString formula code that needs to be executed
     * @param formulaDataType data type of the formula result
     * @param testInput values for each of the reference fields on which the formula depends
     * @param testEntity entity for which the formula needs to be evaluated
     * @param additionalTester any additional tester that can be used for execution can be plugged here like DBTester for
     *                         SQL execution
     * @return a string representation of the output value or error generated
     */
    String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, T testEntity, U additionalTester);
}
