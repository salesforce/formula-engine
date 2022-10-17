package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Deque;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaException;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.FormulaTime;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.sql.FormulaSqlStyle;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaDateUtil;
import com.force.formula.util.FormulaI18nUtils;
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
        String sql = getMinuteExpr(args[0], context);
        return new SQLPair(sql, guards[0]);
    }
    
    public static String getMinuteExpr(String arg, FormulaContext context)  {
        FormulaSqlStyle style = context.getSqlStyle();
        if (style.isMysqlStyle() || style.isPrestoStyle()) {
     		return "MINUTE(" + arg + ")";
    	} else if (style.isTransactSqlStyle()) {
    		return "DATEPART(minute,"+arg+")";
    	}
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
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
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
