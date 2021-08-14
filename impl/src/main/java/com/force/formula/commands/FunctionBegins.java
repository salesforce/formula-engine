package com.force.formula.commands;


import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.TEXT,isOffline=true)
public class FunctionBegins extends FormulaCommandInfoImpl {
    public FunctionBegins() {
        super("BEGINS", Boolean.class, new Class[] { String.class, String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionBeginsCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = "((" + args[1] + " IS NULL) OR (INSTR(" + args[0] + ", " + args[1] + ") = 1))";
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        String js =  "!"+args[1]+"||"+args[0]+".lastIndexOf("+jsNvl(args[1].js,"''")+",0)===0";
        return JsValue.forNonNullResult(js, args);
    }    
}

class FunctionBeginsCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionBeginsCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        String sub = checkStringType(stack.pop());
        String target = checkStringType(stack.pop());
        if ((sub == null) || (sub == ""))
            stack.push(true);
        else if ((target == null) || (target.equals("")))
            stack.push(null);
        else
            stack.push(target.startsWith(sub));
    }
}
