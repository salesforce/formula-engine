/**
 * 
 */
package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
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
    	FormulaSqlHooks hooks = getSqlHooks(context);
        if (hooks.isPostgresStyle()) {
        	sql = "1+EXTRACT (DOW FROM " + args[0] + ")::numeric";
        } else if (hooks.isMysqlStyle()) {
        	sql = "DAYOFWEEK(" + args[0] + ")";
        } else if (hooks.isPrestoStyle()) {
            sql = "1+DAY_OF_WEEK(" + args[0] + ")";
        } else if (hooks.isTransactSqlStyle()) {
        	sql = "DATEPART(weekday," + args[0] + ")";
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
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
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
