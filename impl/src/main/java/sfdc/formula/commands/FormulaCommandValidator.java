package sfdc.formula.commands;

import java.lang.reflect.Type;

import sfdc.formula.*;
import sfdc.formula.impl.FormulaAST;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public interface FormulaCommandValidator {
    Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException;
}
