package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.BaseLocalizer;

/**
 * Represents the day of the year (1-366)
 *
 * @author stamm
 * @since 140
 */
@AllowedContext(section=SelectorSection.DATE_TIME,isOffline=true)
public class FunctionDayOfYear extends FormulaCommandInfoImpl {
    public FunctionDayOfYear() {
        super("DAYOFYEAR", BigDecimal.class, new Class[] { Date.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionDayOfYearCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        return new SQLPair(String.format(getSqlHooks(context).sqlGetDayOfYear(), args[0]), guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        String js =  jsToDec(context, "$F.dayofyear(" + args[0] + ")");
        return JsValue.generate(js, args, true, args[0]);
    }

    static class FunctionDayOfYearCommand extends AbstractFormulaCommand {
	    private static final long serialVersionUID = 1L;
	
		public FunctionDayOfYearCommand(FormulaCommandInfo formulaCommandInfo) {
	        super(formulaCommandInfo);
	    }
	
	    @Override
	    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
	        Date d = checkDateType(stack.pop());
	        if (d == null)
	            stack.push(null);
	        else {
	            Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.GMT);
	            c.setTime(d);
	            stack.push(new BigDecimal(c.get(Calendar.DAY_OF_YEAR)));
	        }
	    }
    }
}
