/*
 * Created on Jan 26, 2005
 *
 */
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
public abstract class BinaryMathCommandBehavior implements Serializable {
    private static final long serialVersionUID = 1L;

	public abstract FormulaCommand getCommand(FormulaCommandInfo info);

    public abstract SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards);
    
    public abstract JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args);
}
