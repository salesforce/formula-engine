package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.TEXT,isOffline=true)
public class FunctionRight extends FormulaCommandInfoImpl {
    public FunctionRight() {
        super("RIGHT", String.class, new Class[] { String.class, BigDecimal.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionRightCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        // Oracle allows {n,m} where m < 0 and treats it as {0,0} but Sayonara will throw an error, and
        // this actually comes up in formula tests. The Sayonara sql would work on both, but it makes the sql longer
        // so that is not allowed for Oracle
        String sql;
        if (FormulaCommandInfoImpl.shouldGeneratePsql(context)) {
            sql = "REGEXP_SUBSTR("+args[0]+",'.{0,'||NVL2("+args[1]+",CASE WHEN "+args[1]+"<0 THEN 0 ELSE "+args[1]+" END,0)||'}$',1,1,'n')"; 
        } else {
            sql = "REGEXP_SUBSTR("+args[0]+",'.{0,'||NVL("+args[1]+",0)||'}$',1,1,'n')";
        }
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        // Per Java convention, return null if the padding length is <= 0
        return JsValue.forNonNullResult("((" + args[1] + "<=0)?'':"+args[0] +").slice(-("+args[1]+"))", args); 
    }

}

class FunctionRightCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionRightCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        BigDecimal count = checkNumberType(stack.pop());
        String target = checkStringType(stack.pop());

        if (count == null) {
            stack.push(null);
            return;
        }
        int compare = count.compareTo(BigDecimal.ZERO);
        if (compare <= 0)
            stack.push(null);
        else if ((target == null) || (target.equals("")))
            stack.push(null);
        else if (count.intValue() > target.length())
            stack.push(target);
        else
            stack.push(target.substring(target.length() - count.intValue(), target.length()));
    }
}
