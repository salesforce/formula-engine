package com.force.formula.v2;

import com.force.formula.v2.exception.FormulaFileParseException;

import java.util.List;

/**
 * An interface to parse test xml files into formula test definition objects
 *
 * @param <T> formula test definition object
 */
public interface IFormulaTestDefinitionParser<T> {
    /**
     * A method to parse test xml files into formula test definition objects
     *
     * @param testDefinitionFilePaths a list of test xml file paths
     * @return a list of formula test definitions
     * @throws FormulaFileParseException if there is an issue reading or parsing the file paths
     */
    List<T> parse(List<String> testDefinitionFilePaths) throws FormulaFileParseException;
}
