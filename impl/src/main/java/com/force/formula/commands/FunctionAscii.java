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
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.sql.SQLPair;

/**
 * ASCII(text) returns the first characters codepoint in UTF-8.  Called ascii for historical reasons
 *
 * @author stamm
 * @since 0.1.0
 */
@AllowedContext(section=SelectorSection.TEXT,isOffline=true)
public class FunctionAscii extends FormulaCommandInfoImpl {
    public FunctionAscii() {
        super("ASCII", BigDecimal.class, new Class[] { String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionAsciiCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        return new SQLPair(String.format(getSqlHooks(context).sqlAscii(), args[0]), guards[0]);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult(jsToDec(context, args[0] + ".codePointAt(0)"), args); 
    }
    
    static class FunctionAsciiCommand extends AbstractFormulaCommand {
	    private static final long serialVersionUID = 1L;
	
		public FunctionAsciiCommand(FormulaCommandInfo formulaCommandInfo) {
	        super(formulaCommandInfo);
	    }
	
	    @Override
	    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
	        String arg = checkStringType(stack.pop());
	        if (arg == null || arg.length() == 0)
	            stack.push(null);
	        else {
	        	stack.push(new BigDecimal(arg.codePointAt(0)));
	      	}
	    }
    }
}
