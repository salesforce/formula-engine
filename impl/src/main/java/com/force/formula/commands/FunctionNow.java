package com.force.formula.commands;

import java.util.Calendar;
import java.util.Date;

import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaDateUtil;
import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.BaseLocalizer;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.DATE_TIME, isOffline=true)
public class FunctionNow extends FormulaCommandInfoImpl {
    private static final Supplier<Boolean> USE_FILTER_AS_OF_DATE =
    		Suppliers.memoize(()->FormulaEngine.getHooks().useTestCalendarForNow());

    public FunctionNow() {
        super("NOW", FormulaDateTime.class, new Class[] {});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionNowCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
    	
        if (USE_FILTER_AS_OF_DATE.get()) {
            // If the server is configured with a filter as of date (as for the report hammer or related system testing tools, then use a fixed date instead of today)
            Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.LOCAL);
            FormulaEngine.getHooks().adjustCalendarForTestEnvironment(c); // This allows tools like the report hammer to evaluate formulas as of the db cut date
            return new SQLPair(FormulaDateUtil.dateToSqlToDateString(c.getTime()), null);
        } else {
            return new SQLPair(getSqlHooks(context).sqlNow(), null);
        }
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        if (USE_FILTER_AS_OF_DATE.get()) {
            // If the server is configured with a filter as of date (as for the report hammer or related system testing tools, then use a fixed date instead of today)
            Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.LOCAL);
            FormulaEngine.getHooks().adjustCalendarForTestEnvironment(c); // This allows tools like the report hammer to evaluate formulas as of the db cut date
            return new JsValue("new Date("+c.getTimeInMillis()+")", null, false);
        } else {
            return new JsValue("new Date()", null, false);
        }
    }

}

class FunctionNowCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionNowCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    private static final String FUNCTIONNOW_VALUE = "common.formula.commands.FunctionNow.value";

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        FormulaDateTime now = (FormulaDateTime)context.getProperty(FUNCTIONNOW_VALUE);
        if (now == null) {
            now = new FormulaDateTime(new Date());
            context.setProperty(FUNCTIONNOW_VALUE, now);
        }

        stack.push(now);
    }

    @Override
    public boolean isDeterministic(FormulaContext formulaContext) {
        return false;
    }
}
