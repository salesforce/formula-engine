package sfdc.formula.commands;

import java.math.BigDecimal;
import java.util.Calendar;

import java.util.Deque;
import com.force.i18n.BaseLocalizer;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.*;

/**
 * Describe your class here.
 *
 * @author pjain
 * @since 208
 */
@AllowedContext(section=SelectorSection.DATE_TIME, nonFlowOnly=true, isOffline=true)
public class FunctionHour extends FormulaCommandInfoImpl {
    public FunctionHour() {
        super("HOUR", BigDecimal.class, new Class[] { FormulaTime.class});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionHourCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = getHourExpr(args[0]);
        return new SQLPair(sql, guards[0]);
    }
    
    public static String getHourExpr(String arg)  {
        return "TRUNC(" + arg + "/" + FormulaDateUtil.HOUR_IN_MILLIS + ")";
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult("new $F.Decimal("+args[0].js+".getUTCHours())", args);
        }
        return JsValue.forNonNullResult(jsToDec(context, args[0]+".getUTCHours()"), args);
    }
}

class FunctionHourCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionHourCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
       // Date d = checkDateType(stack.pop());
        FormulaTime d = (FormulaTime)stack.pop();  // to da validate
        if (d == null)
            stack.push(null);
        else {
            Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.GMT);
            c.setTimeInMillis(d.getTimeInMillis());
            stack.push(new BigDecimal(c.get(Calendar.HOUR_OF_DAY)));
        }
    }
}
