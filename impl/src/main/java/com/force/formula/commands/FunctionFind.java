package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 144
 */
@AllowedContext(section=SelectorSection.TEXT,isOffline=true)
public class FunctionFind extends FormulaCommandInfoImpl {
    public FunctionFind() {
        super("FIND", BigDecimal.class, new Class[] {});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionFindCommand(this, node.getNumberOfChildren() == 3);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        StringBuilder sql = new StringBuilder();

        FormulaSqlHooks hooks = getSqlHooks(context); 
        if (args.length == 3) {
        	// With postgres, you need to do a bunch of stuff to get around the lack of a built-in for the offset.
            String startPosition = hooks.sqlNvl() + "(" + args[2] + ", 1)";
	        String instr = hooks.sqlInstr3(args[1], args[0], hooks.sqlGreatest(startPosition, "1"));
	        sql.append(String.format(hooks.sqlNvl() + "(" + instr + ", 0)", args[1], args[0], startPosition));
        } else {
        	// Use regular instr if there isn't a position offset
	        sql.append(String.format(hooks.sqlNvl() + "(" + hooks.sqlInstr2(args[1], args[0]) + ", 0)", args[1], args[0]));
        }

        String guard = SQLPair.generateGuard(guards, null);

        return new SQLPair(sql.toString(), guard);
    }

    @Override
    public Class<?>[] getArgumentTypes(FormulaAST node, FormulaContext context) {
        return (node.getNumberOfChildren() == 3) ? new Class[] { String.class, String.class, BigDecimal.class }
            : new Class[] { String.class, String.class };
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        String arg0 = args[0].guard!=null? "(" + args[0].guard + "?(" + args[0].js + "):null)" : args[0].js;
        String offset = "";
        
        // Find can't return null, always returns a number
        if (node.getNumberOfChildren() == 3) {
            String arg2 = args[2].guard!=null? "(" + args[2].guard + "?(" + args[2].js + "):0)" : args[2].js;
            offset = ","+arg2+"-1";
        }
        if (args[1].couldBeNull || args[1].guard != null) {
            String guard = args[1].guard != null ? args[1].guard + "&&" + args[1].js : args[1].js;
            return JsValue.generate(jsToDec(context,"(("+guard+")?("+args[1].js+".indexOf("+arg0+offset+")+1):0)"), new JsValue[0], false);  // Yup.  Off by 1.  Awesome.  
        }
        return JsValue.generate(jsToDec(context,args[1].js+".indexOf("+arg0+offset+")+1"), new JsValue[0], false);  // Yup.  Off by 1.  Awesome.  
    }
}

class FunctionFindCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionFindCommand(FormulaCommandInfo  info, boolean hasStartPosition) {
        super(info);
        this.hasStartPosition = hasStartPosition;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        BigDecimal startPosition = (hasStartPosition) ? checkNumberType(stack.pop()) : BigDecimal.ZERO;
        String value = checkStringType(stack.pop());
        String search = checkStringType(stack.pop());

        int location = ((value != null) && (search != null)) ? value.indexOf(search, startPosition.intValue() - 1) + 1 : 0;
        stack.push(BigDecimal.valueOf(location));
    }

    private final boolean hasStartPosition;
}
