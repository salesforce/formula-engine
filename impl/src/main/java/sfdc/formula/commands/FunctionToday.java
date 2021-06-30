package sfdc.formula.commands;

import java.util.*;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.*;

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
                FormulaEngine.getHooks().adjustCalendarForTestEnvironment(c); // This allows tools like the report hammer to evaluate formulas as of the db cut date
                value = value.replaceAll(FunctionToday.TODAY_MARKER, "DATE '" + c.get(Calendar.YEAR) + "-"
                    + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DATE) + "'");
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
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        stack.push(FormulaDateUtil.todayGmt());
    }

    @Override
    public boolean isDeterministic(FormulaContext formulaContext) {
        return false;
    }
}
