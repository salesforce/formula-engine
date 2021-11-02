package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.TEXT,isOffline=true)
public class FunctionLeft extends FormulaCommandInfoImpl {
    public FunctionLeft() {
        super("LEFT", String.class, new Class[] { String.class, BigDecimal.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionLeftCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = "SUBSTR(" + args[0] + ", 1, GREATEST(" + args[1] + ", 0))";
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult(args[0] + ".substring(0,"+args[1]+")", args);
    }
}

class FunctionLeftCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionLeftCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        BigDecimal count = checkNumberType(stack.pop());
        String target = checkStringType(stack.pop());
        if (count == null) {
            stack.push(null);
            return;
        }
        int compare = count.compareTo(BigDecimal.ZERO);
        if (compare <= 0)
            stack.push(null);
        else if ((target == null) || (target.equals("")))
            stack.push(null);
        else if (count.intValue() > target.length())
            stack.push(target);
        else
            stack.push(target.substring(0, count.intValue()));
    }
}
