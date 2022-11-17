package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaException;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
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
    	FormulaSqlHooks hooks = getSqlHooks(context);
    	String len = hooks.isTransactSqlStyle() ? "LEN" : "LENGTH";
        String sql = hooks.sqlNvl() + "(" + len + "(" + args[0] + "),0)";
        // Make the sql be numeric if using postgres
        sql = hooks.sqlMakeDecimal(sql);
        String guard = guards[0];
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        // Yeah, this is absurd, but whaddayagonna do.  Swallow the guard and always return 0
        String js = jsToDec(context, jsNvl2WithGuard(context, args[0], "("+args[0]+").length", jsToDec(context,"0"), true));  // Yeah, zero.  Weird.
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
