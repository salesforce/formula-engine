package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * Perform a RPAD function which takes a string and truncates it
 * to the given size, or pads it out with spaces or an optional
 * string to the right of the string
 *
 * @author stamm
 * @since 150
 */
@AllowedContext(section=SelectorSection.TEXT)
public class FunctionRpad extends FormulaCommandInfoImpl {
    public FunctionRpad() {
        super("RPAD", String.class, new Class[] { });
    }


    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionRpadCommand(this, node.getNumberOfChildren() == 3);
    
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = node.getNumberOfChildren() == 3 ? "RPAD(" + args[0] + ", GREATEST(" + args[1] + ", 0), " + args[2]+ ")"
            : "RPAD(" + args[0] + ", GREATEST(" + args[1] + ", 0))";
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public Class<?>[] getArgumentTypes(FormulaAST node, FormulaContext context) {
        return (node.getNumberOfChildren() == 3) ? new Class[] { String.class, BigDecimal.class, String.class }
            : new Class[] { String.class, BigDecimal.class };
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        String padValue = (node.getNumberOfChildren() == 3) ? args[2].js : "' '";
        return JsValue.forNonNullResult("("+args[0].js+"+Array("+FunctionLpad.MAXPAD+").join("+padValue+")).substring(0,"+jsToNum(context, args[1].js)+")", args);
    }
}

class FunctionRpadCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;
	final boolean hasPadStr;
    public FunctionRpadCommand(FormulaCommandInfo info, boolean hasPadStr) {
        super(info);
        this.hasPadStr = hasPadStr;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        String padding = hasPadStr ? checkStringType(stack.pop()) : " ";
        BigDecimal bdCount = checkNumberType(stack.pop());
        String target = checkStringType(stack.pop());
        if (bdCount == null) {
            stack.push(null);
            return;
        }
        int count = bdCount.intValue();
        if ((target == null) || (target.equals("")))
            stack.push(null);
        // Length is < 0, therefore truncate to 0
        else if (count <= 0)
            stack.push(null);
        // Length is < string size, so no padding, but oracle truncates, so we shall as well
        else if (count <= target.length())
            stack.push(target.substring(0,count));
        else {
            // Now we pad.
            StringBuilder sb = new StringBuilder(count);
            sb.append(target);
            // Append the padding to the right side
            while (sb.length() < count) {
                sb.append(padding);
            }
            // Truncate the string if necessary
            if (hasPadStr) sb.setLength(count);
            stack.push(sb.toString());
        }
    }
}