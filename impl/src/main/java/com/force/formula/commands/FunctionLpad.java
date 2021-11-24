package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * Perform a LPAD function which takes a string and truncates it
 * to the given size, or pads it out with spaces or an optional
 * string to the left of the string
 *
 * @author stamm
 * @since 150
 */
@AllowedContext(section=SelectorSection.TEXT)
public class FunctionLpad extends FormulaCommandInfoImpl {
    static final int MAXPAD = 256;  // FIXME SLT: MAGIC NUMBER
    
    public FunctionLpad() {
        super("LPAD", String.class, new Class[] { });
    }


    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionLpadCommand(this, node.getNumberOfChildren() == 3);
    
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        FormulaSqlHooks hooks = getSqlHooks(context);
        String amount = hooks.sqlEnsurePositive(hooks.sqlRoundScaleArg(args[1]));  // prevent negative numbers
        String sql = hooks.sqlLpad(args[0], amount, node.getNumberOfChildren() == 3 ? args[2] : null);
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
        // Per SQL convention, LPAD should return null if
        // 1) the original value is null
        // 2) the end length is null or < 1
        String padValue = (node.getNumberOfChildren() == 3) ? jsNvl(args[2].js, "' '") : "' '";
        // The pad value can be null, we guard against it above
        return JsValue.generate("$F.lpad("+args[0]+","+jsToNum(context, args[1].js)+","+padValue+")", args, args[0].couldBeNull || args[1].couldBeNull, args[0], args[1]);
        // NOTE: This is incorrect.  It needs to check for the length being < args[1] and if so return a substring
        //return "(!("+args[0]+")||!("+args[1]+")||"+args[1]+"<1)?null:"+args[1]+"<"+(Array("+MAXPAD+").join("+padValue+")+"+args[0]+").slice(-"+args[1]+")";
    }

}

class FunctionLpadCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;
	boolean hasPadStr;
    public FunctionLpadCommand(FormulaCommandInfo info, boolean hasPadStr) {
        super(info);
        this.hasPadStr = hasPadStr;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
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
            // Append the padding to the left side before adding the stuff
            while (sb.length() < (count - target.length())) {
                sb.append(padding);
            }
            // Truncate the string if necessary
            if (hasPadStr) sb.setLength(count - target.length());
            sb.append(target);
            stack.push(sb.toString());
        }
    }
}
