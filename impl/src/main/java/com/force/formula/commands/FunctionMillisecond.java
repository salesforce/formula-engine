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
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.BaseLocalizer;

/**
 * Describe your class here.
 *
 * @author pjain
 * @since 208
 */
@AllowedContext(section=SelectorSection.DATE_TIME, nonFlowOnly=true)
public class FunctionMillisecond extends FormulaCommandInfoImpl {
    public FunctionMillisecond() {
        super("MILLISECOND", BigDecimal.class, new Class[] { FormulaTime.class});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionMillisecondCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        // convert muillisecs since midnight to millseconds portion of time  trunc((args[0] -trunc(args[0]/1000) * 1000))
        String sql = getMillisecondExpr(args[0], context); 
        return new SQLPair(sql, guards[0]);
    }
    
    public static String getMillisecondExpr(String arg, FormulaContext context)  {
    	FormulaSqlHooks hooks = getSqlHooks(context);
    	if (hooks.isTransactSqlStyle()) {
    		return "DATEPART(MILLISECOND,"+arg+")";
        } else if (context.getSqlStyle().isPrestoStyle()) {
            return "MILLISECOND("+arg+")";            
    	} else if (context.getSqlStyle().isMysqlStyle()) {
    		return "1000*MICROSECOND("+arg+")";
    	}
    	
    	String trunc = hooks.sqlTrunc(arg + "/1000");
        return hooks.sqlTrunc("(" + arg + " -"+trunc+" * 1000)");
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult(args[0]+".getMilliseconds()", args);
    }
}

class FunctionMillisecondCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionMillisecondCommand(FormulaCommandInfo formulaCommandInfo) {
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
            stack.push(new BigDecimal(c.get(Calendar.MILLISECOND)));
        }
    }
}
