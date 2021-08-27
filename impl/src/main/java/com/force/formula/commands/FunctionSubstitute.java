package com.force.formula.commands;

import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaTextUtil;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 144
 */
@AllowedContext(section=SelectorSection.TEXT)
public class FunctionSubstitute extends FormulaCommandInfoImpl {
    public FunctionSubstitute() {
        super("SUBSTITUTE", String.class, new Class[] { String.class, String.class, String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionSubstituteCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = String.format("REPLACE(%s, %s, %s)", args[0], args[1], args[2]);
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.generate(args[0] + ".replace(" + args[1] + "," + args[2] + ")", args, false, args[0]); // TODO: Ignore other guards?
    }

}

class FunctionSubstituteCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionSubstituteCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        String newText = checkStringType(stack.pop());
        String oldText = checkStringType(stack.pop());
        String sourceText = checkStringType(stack.pop());

        if ((sourceText == null) || (oldText == null)) {
            stack.push(null);
            return;
        }
        if (sourceText.length() == 0) {
            stack.push(sourceText);
            return;
        }
        
        if (newText == null) {
            newText = "";
        }

        stack.push(FormulaTextUtil.replaceSimple(sourceText, oldText, newText));
    }
}
