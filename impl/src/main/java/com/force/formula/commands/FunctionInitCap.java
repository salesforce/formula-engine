package com.force.formula.commands;


import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaTextUtil;

/**
 * INITCAP() converts the first letter of each word to upper case and the remaining to lower case.
 * TODO: Support a locale parameter
 *
 * @author stamm
 * @since 0.1.0
 */
@AllowedContext(section=SelectorSection.TEXT,isOffline=true)
public class FunctionInitCap extends FormulaCommandInfoImpl {
    public FunctionInitCap() {
        super("INITCAP", String.class, new Class[] { String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionTrimCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        return new SQLPair(String.format(getSqlHooks(context).sqlInitCap(false), args[0]), guards[0]);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult("$F.initcap("+args[0] +")",args);
    }
    
    static class FunctionTrimCommand extends AbstractFormulaCommand {
	    private static final long serialVersionUID = 1L;
	
		public FunctionTrimCommand(FormulaCommandInfo formulaCommandInfo) {
	        super(formulaCommandInfo);
	    }
	
	    @Override
	    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
	        String arg = checkStringType(stack.pop());
	        if ((arg == null) || (arg.equals("")))
	            stack.push(null);
	        else {
	            stack.push(FormulaTextUtil.formulaInitCap(arg));
	        }
	    }
    }
}
