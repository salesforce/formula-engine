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
 * CHR(number) converts a UTF-8 character point (number to a string
 *
 * @author stamm
 * @since 0.1.0
 */
@AllowedContext(section=SelectorSection.TEXT,isOffline=true)
public class FunctionChr extends FormulaCommandInfoImpl {
    public FunctionChr() {
        super("CHR", String.class, new Class[] { BigDecimal.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionChrCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String guard = SQLPair.generateGuard(guards, args[0] + "<1");
        return new SQLPair(String.format(getSqlHooks(context).sqlChr(), args[0]), guard);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
    	String guard = args[0].couldBeNull ? args[0] + "&&" : "";
        return JsValue.generate("(" + guard +jsToNum(context, args[0].js)+">0?String.fromCodePoint(Math.trunc("+jsToNum(context, args[0].js) +")):null)",args,true);
    }
    
    static class FunctionChrCommand extends AbstractFormulaCommand {
	    private static final long serialVersionUID = 1L;
	
		public FunctionChrCommand(FormulaCommandInfo formulaCommandInfo) {
	        super(formulaCommandInfo);
	    }
	
	    @Override
	    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
	        BigDecimal arg = checkNumberType(stack.pop());
	        if ((arg == null) || (arg.equals(BigDecimal.ZERO)))
	            stack.push(null);
	        else {
	        	int c = arg.intValue();
	        	try {
	        		stack.push(Character.toString(c));
	        	} catch (IllegalArgumentException x) {
	        		stack.push(null); // Use null for an invalid character
	        	}
	      	}
	    }
    }
}
