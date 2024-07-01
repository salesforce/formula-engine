package com.force.formula.commands;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
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
public class FunctionSecond extends FormulaCommandInfoImpl {
    public FunctionSecond() {
        super("SECOND", BigDecimal.class, new Class[] { FormulaTime.class});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionSecondCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String str = getSqlHooks(context).sqlCastNull(args[0], FormulaTime.class);
        return new SQLPair(String.format(getSqlHooks(context).sqlChronoUnit(ChronoUnit.SECONDS, FormulaTime.class), str), guards[0]);
    }

    public static String getSecondExpr(String arg, FormulaContext context)  {
        FormulaSqlStyle style = context.getSqlStyle();
        if (style.isMysqlStyle() || style.isPrestoStyle()) {
            return "SECOND(" + arg + ")";
        } else if (style.isTransactSqlStyle()) {
            return "DATEPART(second,"+arg+")";
        }
        return "TRUNC((" + arg + "-TRUNC(" + arg + "/" + FormulaDateUtil.MINUTE_IN_MILLIS+ ") * " + FormulaDateUtil.MINUTE_IN_MILLIS + ")/1000)";
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult(jsToDec(context, args[0].js+".getSeconds()"), args);
    }
}

class FunctionSecondCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

    public FunctionSecondCommand(FormulaCommandInfo formulaCommandInfo) {
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
            stack.push(new BigDecimal(c.get(Calendar.SECOND)));
        }
    }
}
