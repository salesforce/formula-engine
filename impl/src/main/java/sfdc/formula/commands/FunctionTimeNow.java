package sfdc.formula.commands;

import java.util.Calendar;
import java.util.GregorianCalendar;

import java.util.Deque;
import com.force.i18n.BaseLocalizer;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.*;

/**
 * Describe your class here.
 *
 * @author p
 * @since 140
 */
@AllowedContext(section=SelectorSection.DATE_TIME, nonFlowOnly=true)
public class FunctionTimeNow extends FormulaCommandInfoImpl {

    public FunctionTimeNow() {
        super("TIMENOW", FormulaTime.class, new Class[] {});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionTimeNowCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
    	FormulaTime tt = FormulaEngine.getHooks().testHook_getCurrentTime();
        if (tt != null) {
            return new SQLPair(""+tt.getTimeInMillis(), null);
        } 
        else {
            return FormulaCommandInfoImpl.shouldGeneratePsql(context) ? 
                    new SQLPair("EXTRACT(EPOCH FROM AGE(SFDC_TIMESTAMP(), DATE_TRUNC('day', SFDC_TIMESTAMP())))::BIGINT::NUMERIC", null)
                    : new SQLPair("TO_NUMBER(TO_CHAR(SYSTIMESTAMP, 'SSSSS.ff3'))*1000", null);
        }
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return new JsValue("new Date()", null, false);
    }

}

class FunctionTimeNowCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionTimeNowCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    private static final String FUNCTIONTIMENOW_VALUE = "common.formula.commands.FunctionTimeNow.value";

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        FormulaTime now = (FormulaTime)context.getProperty(FUNCTIONTIMENOW_VALUE);
        if (now == null) {
            Calendar cal = new GregorianCalendar(BaseLocalizer.GMT_TZ);
            now = FormulaEngine.getHooks().constructTime(FormulaDateUtil.millisecondOfDay(cal));
            context.setProperty(FUNCTIONTIMENOW_VALUE, now);
        }

        stack.push(now);
    }

    @Override
    public boolean isDeterministic(FormulaContext formulaContext) {
        return false;
    }
}
