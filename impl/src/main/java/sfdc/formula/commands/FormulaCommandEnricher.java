package sfdc.formula.commands;

import sfdc.formula.*;
import sfdc.formula.impl.FormulaAST;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
public interface FormulaCommandEnricher {
    FormulaAST enrich(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException;
}
