package com.force.formula.template.commands;

import java.lang.reflect.Type;
import java.util.Deque;
import java.util.LinkedHashMap;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaException;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.commands.AbstractFormulaCommand;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.commands.FormulaCommandInfoImpl;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.sql.SQLPair;

/**
 * @author dchasman
 * @since 144
 */
@AllowedContext(section=SelectorSection.ADVANCED,isSql=false, displayOnly=true,isJavascript=false)
public class FunctionMap extends FormulaCommandInfoImpl {

    public static class StringToStringMap extends LinkedHashMap<String, String> {

		private static final long serialVersionUID = 1L;
    }

    public FunctionMap() {
        super("MAP", StringToStringMap.class, new Class[] {});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionMapCommand(this, node.getNumberOfChildren());
    }

    @Override
    public Type[] getArgumentTypes(FormulaAST node, FormulaContext context) {
        // This "function" requires an even number of arguments.  We allocate an argument array here that is an even number size, rounded up
        // from actual number.  Then, the validation code that calls this will throw an error "number of arguments is invalid", because the actual number does
        // not match the size of this array.
        int numberOfArguments = (node.getNumberOfChildren() + 1)/ 2 *2;   // round up
        Type[] argumentTypes = new Type[numberOfArguments];

        // The argument array must contain pairs, where the first of each pair is a string key.  Second can apparently be any type and gets
        // converted to string when this is used.
        // Note the framework validates the first of each pair actually is a string.
        int n = 0;
        FormulaAST child = (FormulaAST)node.getFirstChild();
        while (child != null) {
            if (n%2 == 0) {
                argumentTypes[n++] = String.class;
            } else {
                argumentTypes[n++] = child.getDataType();
            }
            child = (FormulaAST)child.getNextSibling();
        }

        return argumentTypes;
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
        throws FormulaException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        throw new UnsupportedOperationException();
    }

}

class FunctionMapCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	FunctionMapCommand(FormulaCommandInfo info, int numArgs) {
        super(info);
        this.numArgs = numArgs;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        FunctionMap.StringToStringMap result = new FunctionMap.StringToStringMap();

        // Temporary arrays hold the popped elements so we can push them into the result map
        // in the original order.   Not strictly necessary (we don't guarantee order), but helps
        // to avoid unnecessary randomness
        int numPair = numArgs/2;
        String [] values = new String[numPair];
        String [] keys = new String[numPair];

        for (int i = 0; i < numPair; ++i) {
            Object value = stack.pop();
            values[i] = value != null ? value.toString() : null;
            keys[i] = checkStringType(stack.pop());
        }
        for (int i = numPair - 1; i >= 0; --i) {
            result.put(keys[i], values[i]);
        }

        stack.push(result);
    }

    private final int numArgs;
}
