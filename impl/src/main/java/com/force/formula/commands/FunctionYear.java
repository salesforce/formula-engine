package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
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
public class FunctionYear extends FormulaCommandInfoImpl {
    public FunctionYear() {
        super("YEAR", BigDecimal.class, new Class[] { Date.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionYearCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = "EXTRACT (YEAR FROM " + args[0] + ")";
        if (getSqlHooks(context).isPostgresStyle()) {
            sql = sql + "::numeric";
        }
        return new SQLPair(sql,guards[0]);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult("new $F.Decimal("+args[0].js+".getUTCFullYear())", args);
        }
        return JsValue.forNonNullResult(args[0]+".getUTCFullYear()", args);
    }
}

class FunctionYearCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionYearCommand(FormulaCommandInfo formulaCommandInfo) {
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
            stack.push(new BigDecimal(c.get(Calendar.YEAR)));
        }
    }
}
