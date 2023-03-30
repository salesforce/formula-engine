package com.force.formula.v2;

import com.force.formula.v2.exception.FormulaFileParseException;

import java.util.List;

public interface IFormulaTestDefinitionParser<T> {
    List<T> parse(List<String> testDefinitionFilePaths) throws FormulaFileParseException;
}
