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
public class FunctionLen extends FormulaCommandInfoImpl {
    public FunctionLen() {
        super("LEN", BigDecimal.class, new Class[] { String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionLenCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = getSqlHooks(context).sqlNvl() + "(LENGTH(" + args[0] + "),0)";
        // Make the sql be numeric if using postgres
    	sql = getSqlHooks(context).sqlMakeDecimal(sql);
        String guard = guards[0];
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        // Yeah, this is absurd, but whaddayagonna do.  Swallow the guard and always return 0
        String js = jsToDec(context, jsNvl2WithGuard(args[0], "("+args[0]+").length", jsToDec(context,"0"), true));  // Yeah, zero.  Weird.
        return new JsValue(js, null, false);
    }
}

class FunctionLenCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionLenCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        String arg = checkStringType(stack.pop());
        if ((arg == null) || (arg.equals("")))
            stack.push(BigDecimal.ZERO);
        else
            stack.push(new BigDecimal(arg.length()));
    }
}
