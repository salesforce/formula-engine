package sfdc.formula.commands;

import sfdc.formula.*;
import sfdc.formula.impl.FormulaAST;

public interface FormulaCommandOptimizer {
    // optimize is allowed to modify the parsetree *and* the ast node
    FormulaAST optimize(FormulaAST node, FormulaContext context) throws FormulaException;
}
