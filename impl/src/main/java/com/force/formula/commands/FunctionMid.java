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
public class FunctionMid extends FormulaCommandInfoImpl {
    public FunctionMid() {
        super("MID", String.class, new Class[] { String.class, BigDecimal.class, BigDecimal.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionMidCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
    	FormulaSqlHooks hooks = getSqlHooks(context);
        String sql = hooks.getSubstringFunction() + "(" + args[0] + ", " + hooks.sqlGreatest(args[1], "1") + ", " + hooks.sqlEnsurePositive(args[2]) + ")";
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        String sql = args[0]+".substr(Math.max("+args[1]+"-1,0),Math.max("+args[2]+",0))";
        return JsValue.forNonNullResult(sql, args);
    }

}

class FunctionMidCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionMidCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        BigDecimal count = checkNumberType(stack.pop());
        BigDecimal start = checkNumberType(stack.pop());
        String target = checkStringType(stack.pop());
        if ((count == null) || (start == null)) {
            stack.push(null);
            return;
        }
        if (count.compareTo(BigDecimal.ZERO) < 0)
            count = BigDecimal.ZERO;
        if (start.compareTo(BigDecimal.ONE) < 0)
            start = BigDecimal.ONE;
        if ((target == null) || (target.equals("")) || (count.compareTo(BigDecimal.ZERO) == 0)
            || (start.compareTo(new BigDecimal(target.length())) > 0))
            stack.push(null);
        else if (start.intValue() + count.intValue() > target.length())
            stack.push(target.substring(start.intValue() - 1, target.length()));
        else
            stack.push(target.substring(start.intValue() - 1, start.intValue() - 1 + count.intValue()));
    }
}
