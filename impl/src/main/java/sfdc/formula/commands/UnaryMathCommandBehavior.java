package sfdc.formula.commands;

import java.io.Serializable;

import sfdc.formula.*;
import sfdc.formula.impl.FormulaAST;
import sfdc.formula.impl.JsValue;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public abstract class UnaryMathCommandBehavior implements Serializable {

    private static final long serialVersionUID = 1L;

	UnaryMathCommandBehavior() {
    }
    
    public abstract UnaryMathCommand getCommand(FormulaCommandInfo info);

    public abstract SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards);

    public abstract JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args);
}
