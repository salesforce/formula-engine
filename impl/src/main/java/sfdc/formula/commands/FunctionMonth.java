package sfdc.formula.commands;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import com.force.i18n.BaseLocalizer;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.*;

import java.util.Deque;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.DATE_TIME,isOffline=true)
public class FunctionMonth extends FormulaCommandInfoImpl {
    public FunctionMonth() {
        super("MONTH", BigDecimal.class, new Class[] { Date.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionMonthCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = "EXTRACT (MONTH FROM " + args[0] + ")";
        if (FormulaCommandInfoImpl.shouldGeneratePsql(context)) {
            sql = sql + "::numeric";
        }
        return new SQLPair(sql, guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult("new $F.Decimal("+args[0].js+".getUTCMonth()+1)", args);
        }
        return JsValue.forNonNullResult(jsToDec(context, args[0]+".getUTCMonth()+1"),args);
    }
}

class FunctionMonthCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionMonthCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        Date d = checkDateType(stack.pop());
        if (d == null)
            stack.push(null);
        else {
            Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.GMT);
            c.setTime(d);
            stack.push(new BigDecimal(c.get(Calendar.MONTH) + 1));
        }
    }
}
