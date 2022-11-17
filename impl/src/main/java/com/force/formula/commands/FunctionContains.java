package com.force.formula.commands;


import java.util.Deque;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaException;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.sql.SQLPair;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section = SelectorSection.TEXT, isOffline = true)
public class FunctionContains extends FormulaCommandInfoImpl {
    public FunctionContains() {
        super("CONTAINS", Boolean.class, new Class[] { String.class, String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionContainsCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        boolean filter = context.getProperty(FormulaContext.FORMULA_FILTER) != null;
        String filteredArg0 = (filter ? (getSqlHooks(context).sqlNvl() + "(") : "") + args[0] + (filter ? ",' ')" : "");
        String sql = "((" + args[1] + " IS NULL) OR (" + getSqlHooks(context).sqlInstr2(filteredArg0, args[1]) + " >= 1))";
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        String text = jsNvl(context, args[0].js, "''");  // Guard against text being null.  Note that FILTER above.
        return JsValue.generate("(!"+args[1]+"||(("+text+").indexOf("+args[1]+")>=0))", args, false);
    }
}

class FunctionContainsCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionContainsCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        String sub = checkStringType(stack.pop());
        String target = checkStringType(stack.pop());
        if ((sub == null) || (sub == ""))
            stack.push(true);
        else if ((target == null) || (target.equals("")))
            stack.push(context.getProperty(FormulaContext.FORMULA_FILTER) != null ? Boolean.FALSE : null);
        else
            stack.push(target.contains(sub));
    }
}
