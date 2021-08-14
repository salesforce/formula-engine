package com.force.formula.commands;

import java.lang.reflect.Type;

import com.force.formula.*;
import com.force.formula.impl.FormulaAST;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public interface FormulaCommandValidator {
    Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException;
}
