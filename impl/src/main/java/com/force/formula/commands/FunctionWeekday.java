/**
 * 
 */
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
 * Return the day of the week of the date, with Sunday = 1 and Saturday = 7.
 * @author stamm
 * @since 0.1.0
 */
@AllowedContext(section=SelectorSection.DATE_TIME,isOffline=true)
public class FunctionWeekday extends FormulaCommandInfoImpl {
    public FunctionWeekday() {
        super("WEEKDAY", BigDecimal.class, new Class[] { Date.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionWeekdayCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
    	String sql;
        if (getSqlHooks(context).isPostgresStyle()) {
        	sql = "1+EXTRACT (DOW FROM " + args[0] + ")::numeric";
        } else {
        	sql = "TO_NUMBER(TO_CHAR(" + args[0] + ",'d'))";
        }
        return new SQLPair(sql, guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult(args[0]+".getUTCDay()+1",args);
    }
    
    static class FunctionWeekdayCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;

    	public FunctionWeekdayCommand(FormulaCommandInfo formulaCommandInfo) {
            super(formulaCommandInfo);
        }

        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
            Date d = checkDateType(stack.pop());
            Object result;
            if (d == null) {
            	result = null;  // No NPEs if null
            } else {
                Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.GMT);
                c.setTime(d);
                result = new BigDecimal(c.get(Calendar.DAY_OF_WEEK));
            }
            stack.push(result);
        }
    }
}
