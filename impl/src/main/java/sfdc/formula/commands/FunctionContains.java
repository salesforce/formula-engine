package sfdc.formula.commands;


import java.util.Deque;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.*;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section = SelectorSection.TEXT, isOffline = true)
public class FunctionContains extends FormulaCommandInfoImpl {
    public FunctionContains() {
        super("CONTAINS", Boolean.class, new Class[] { String.class, String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionContainsCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        boolean filter = context.getProperty(FormulaContext.FORMULA_FILTER) != null;
        String sql = "((" + args[1] + " IS NULL) OR (INSTR(" + (filter ? "NVL(" : "") + args[0] + (filter ? ",' ')" : "")+ ", " + args[1] + ") >= 1))";
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        String text = jsNvl(args[0].js, "''");  // Guard against text being null.  Note that FILTER above.
        return JsValue.generate("(!"+args[1]+"||(("+text+").indexOf("+args[1]+")>=0))", args, false);
    }
}

class FunctionContainsCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionContainsCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        String sub = checkStringType(stack.pop());
        String target = checkStringType(stack.pop());
        if ((sub == null) || (sub == ""))
            stack.push(true);
        else if ((target == null) || (target.equals("")))
            stack.push(context.getProperty(FormulaContext.FORMULA_FILTER) != null ? Boolean.FALSE : null);
        else
            stack.push(target.contains(sub));
    }
}
