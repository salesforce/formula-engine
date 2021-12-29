package com.force.formula.commands;

import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaDateUtil;
import com.force.formula.util.FormulaI18nUtils;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.DATE_TIME, isOffline=true)
public class FunctionToday extends FormulaCommandInfoImpl {

    public FunctionToday() {
        super("TODAY", Date.class, new Class[] {});
        FormulaCommandInfoRegistry.addBindingObserver(todayBindingObserver);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionTodayCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        // Return placeholder value that will be replaced with the current day during bulk evaluation
        return new SQLPair(TODAY_MARKER, null);
    }

    private static final BindingObserver todayBindingObserver = new BindingObserver() {
        @Override
        public String bind(String value) {
            // Instead of using user's locale, we use ENGLISH so that the year representation is inline with 
            // what the DB expects. th_TH returns 2554 instead of 2011, which causes incorrect query
            Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(Locale.ENGLISH);

            if (value.contains(FunctionToday.TODAY_MARKER)) {
            	FormulaSqlHooks hooks = (FormulaSqlHooks) FormulaEngine.getHooks().getSqlStyle();
                FormulaEngine.getHooks().adjustCalendarForTestEnvironment(c); // This allows tools like the report hammer to evaluate formulas as of the db cut date
                value = value.replaceAll(FunctionToday.TODAY_MARKER, hooks.getDateLiteralFromCalendar(c));
            }

            return value;
        }
    };

    private static final String TODAY_MARKER = "__TODAY__";

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return new JsValue("new Date(new Date().setUTCHours(0,0,0,0))", null, false);
    }

}

class FunctionTodayCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionTodayCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        stack.push(FormulaDateUtil.todayGmt());
    }

    @Override
    public boolean isDeterministic(FormulaContext formulaContext) {
        return false;
    }
}
