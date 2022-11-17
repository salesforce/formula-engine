package com.force.formula.commands;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
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
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.BaseLocalizer;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.DATE_TIME,isOffline=true)
public class FunctionDay extends FormulaCommandInfoImpl {
    public FunctionDay() {
        super("DAY", BigDecimal.class, new Class[] { Date.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionDayCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        return new SQLPair(String.format(getSqlHooks(context).sqlChronoUnit(ChronoUnit.DAYS, Date.class), args[0]), guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult("new " + context.getJsEngMod() + ".Decimal("+args[0].js+".getUTCDate())", args);
        }
        return JsValue.forNonNullResult(args[0].js+".getUTCDate()", args);
    }

}

class FunctionDayCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionDayCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        Date d = checkDateType(stack.pop());
        if (d == null)
            stack.push(null);
        else {
            Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.GMT);
            c.setTime(d);
            stack.push(new BigDecimal(c.get(Calendar.DAY_OF_MONTH)));
        }
    }
}
