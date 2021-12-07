package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Deque;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaException;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.sql.SQLPair;

/**
 * Return the DateTime corresponding to a unix time (i.e. number of seconds since Jan 1, 1970
 * FROMUNIXTIME(Number) returns a DateTime
 *
 * @author stamm
 * @since 0.1.2
 */
@AllowedContext(section=SelectorSection.DATE_TIME, isOffline=true)
public class FunctionFromUnixTime extends FormulaCommandInfoImpl  {
    public FunctionFromUnixTime() {
        super("FROMUNIXTIME", FormulaDateTime.class, new Class[] { BigDecimal.class});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionFromUnixTimeCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        return new SQLPair(String.format(getSqlHooks(context).getDateFromUnixTime(), args[0]), guards[0]);
    }
  
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult("new Date(("+jsToNum(context, args[0].js)+")*1000)", args);
    }
   
    static class FunctionFromUnixTimeCommand extends AbstractFormulaCommand {
	    private static final long serialVersionUID = 1L;
	
		public FunctionFromUnixTimeCommand(FormulaCommandInfo formulaCommandInfo) {
	        super(formulaCommandInfo);
	    }
	
	    @Override
	    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
	    	BigDecimal d = checkNumberType(stack.pop());
	        if (d == null)
	            stack.push(null);
	        else {
	        	stack.push(new FormulaDateTime(new Date(d.movePointRight(3).longValue())));
	    	}
	     }
    }
}
