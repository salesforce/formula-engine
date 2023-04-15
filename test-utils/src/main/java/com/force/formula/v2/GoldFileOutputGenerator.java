package com.force.formula.v2;

import com.force.formula.FormulaDataType;

/**
 * An interface that provides functionality to generate gold file output for a formula
 *
 * @param <T> testEntity for which the gold file output needs to be generated for
 */
public interface GoldFileOutputGenerator<T> {
    /**
     * A method that generates a gold file output for a formula
     *
     * @param formulaString formula code that needs to be executed
     * @param formulaDataType data type of the formula result
     * @param testEntity entity for which the formula needs to be evaluated
     * @return a string representation of the output value or error generated
     */
    String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType, T testEntity);
}
