package sfdc.formula.commands;

import java.math.BigDecimal;
import java.util.Calendar;
import com.force.i18n.BaseLocalizer;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.*;

import java.util.Deque;

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
        String sql = getMillisecondExpr(args[0]); 
        return new SQLPair(sql, guards[0]);
    }
    
    public static String getMillisecondExpr(String arg)  {
        return "TRUNC((" + arg + " -TRUNC(" + arg + "/1000) * 1000)) ";
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
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
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
