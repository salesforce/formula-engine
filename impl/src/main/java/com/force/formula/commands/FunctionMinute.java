package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Calendar;

import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.i18n.BaseLocalizer;

/**
 * Describe your class here.
 *
 * @author pjain
 * @since 208
 */
@AllowedContext(section=SelectorSection.DATE_TIME, nonFlowOnly=true, isOffline=true)
public class FunctionMinute extends FormulaCommandInfoImpl {
    public FunctionMinute() {
        super("MINUTE", BigDecimal.class, new Class[] { FormulaTime.class});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionMinuteCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        // convert muillisecs since midnight to minutes portion of time  trunc((args[0] -trunc(args[0]/3600000) * 3600000)/60000)
        String sql = getMinuteExpr(args[0]);
        return new SQLPair(sql, guards[0]);
    }
    
    public static String getMinuteExpr(String arg)  {
        return "TRUNC((" + arg + "-TRUNC(" + arg + "/" + FormulaDateUtil.HOUR_IN_MILLIS+ ") * " + FormulaDateUtil.HOUR_IN_MILLIS + ")/" + FormulaDateUtil.MINUTE_IN_MILLIS + ")";
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult("new $F.Decimal("+args[0].js+".getUTCMinutes())", args);
        }
        return JsValue.forNonNullResult(jsToDec(context, args[0]+".getUTCMinutes()"), args);
    }
}

class FunctionMinuteCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionMinuteCommand(FormulaCommandInfo formulaCommandInfo) {
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
            stack.push(new BigDecimal(c.get(Calendar.MINUTE)));
        }
    }
}
