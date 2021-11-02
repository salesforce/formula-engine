package com.force.formula.commands;


import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
@AllowedContext(section = SelectorSection.TEXT,isOffline=true)
public class OperatorConcat extends FormulaCommandInfoImpl {
    public OperatorConcat() {
        super("&", String.class, new Class[] { String.class, String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new OperatorConcatCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = String.format(getSqlHooks(context).sqlConcat(true), args[0], args[1]);
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        // TODO, nulls turn into "".  Maybe very wrong.
        return JsValue.generate(jsNvl(args[0].js,"''") + "+" + jsNvl(args[1].js,"''"), args, false);
    }


}

class OperatorConcatCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public OperatorConcatCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        String rhs = checkStringType(stack.pop());
        String lhs = checkStringType(stack.pop());
        if (lhs == null)
            stack.push(rhs);
        else if (rhs == null)
            stack.push(lhs);
        else
            stack.push(lhs + rhs);
    }
}
