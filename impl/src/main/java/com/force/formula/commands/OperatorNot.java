package com.force.formula.commands;

import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.LOGICAL, isOffline = true)
public class OperatorNot extends FormulaCommandInfoImpl {
    public OperatorNot() {
        super("NOT", Boolean.class, new Class[] { Boolean.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new OperatorNotFormulaCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        // Not(null) is still null.   Pull out constant case here to avoid invalid expressions later
        String result = "NULL".equals(args[0]) ? "NULL" : "(NOT " + args[0] + ")";
        return new SQLPair(result,  guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        if (args[0] == null || "null".equals(args[0].js)) {
            return new JsValue("null", "true", true);  // null = null.  sigh
        } else {
            return JsValue.forNonNullResult("!("+args[0]+")", args);  // If we do not, null -> true or false.
        }
    }
}

class OperatorNotFormulaCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public OperatorNotFormulaCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        Boolean arg = checkBooleanType(stack.pop());
        if (arg == null)
            stack.push(null);
        else
            stack.push(Boolean.valueOf(!arg.booleanValue()));
    }
}
