package com.force.formula.commands;

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

/**
 * REVERSE(Text) returns the reverse of the text provided
 *
 * @author stamm
 * @since 0.2.21
 */
@AllowedContext(section=SelectorSection.TEXT,isOffline=true)
public class FunctionReverse extends FormulaCommandInfoImpl {
    public FunctionReverse() {
        super("REVERSE", String.class, new Class[] { String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionReverseCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        return new SQLPair("REVERSE(" + args[0] + ")", guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult(args[0] + ".split(\"\").reverse().join(\"\")", args); 
    }
    
    static class FunctionReverseCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;
    
    	public FunctionReverseCommand(FormulaCommandInfo formulaCommandInfo) {
            super(formulaCommandInfo);
        }
    
        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
            String arg = checkStringType(stack.pop());
            if ((arg == null) || (arg.equals(""))) {
                stack.push(arg);
            } else {
                stack.push(new StringBuffer(arg).reverse().toString());
            }
        }
    }
}
