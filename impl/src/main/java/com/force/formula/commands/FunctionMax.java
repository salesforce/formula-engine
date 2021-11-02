package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.google.common.base.Joiner;

/**
 *
 * @author djacobs
 * @since 144
 */
@AllowedContext(section=SelectorSection.MATH,isOffline=true)
public class FunctionMax extends FormulaCommandInfoImpl {

    private static final int MAX_NUMBER_OF_ARGS = 255;

    public FunctionMax() {
        super("MAX", BigDecimal.class, new Class[] {});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionMaxCommand(this, node.getNumberOfChildren());
    }

    @Override
    public Class<?>[] getArgumentTypes(FormulaAST node, FormulaContext context) throws WrongNumberOfArgumentsException {
        int numberOfChildren = node.getNumberOfChildren();
        if (numberOfChildren > MAX_NUMBER_OF_ARGS) {
            throw new WrongNumberOfArgumentsException(node.getText(), MAX_NUMBER_OF_ARGS, node);
        }

        Class<?>[] argumentTypes = new Class[numberOfChildren];
        Arrays.fill(argumentTypes, BigDecimal.class);
        return argumentTypes;
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        StringBuilder sql = new StringBuilder(64);
        sql.append("GREATEST(").append(args[0]);
        for (int i = 1; i < args.length; i++)
            sql.append(", ").append(args[i]);
        sql.append(")");
        return new SQLPair(sql.toString(), SQLPair.generateGuard(guards, null));
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        // TODO: doesn't work with nulls with Decimal
        /*if (context.useHighPrecisionJs()) {
            return "Decimal.max.apply(Decimal,[" + TextUtil.collectionToString(Arrays.asList(args), ",") + "].filter(Number))";
        }*/
        // Nulls work with math
        return JsValue.forNonNullResult(jsMathPkg(context)+".max(" + Joiner.on(',').join(args) + ")", args);
    }
}

class FunctionMaxCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	FunctionMaxCommand(FormulaCommandInfo info, int numArgs) {
        super(info);
        this.numArgs = numArgs;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        BigDecimal result = checkNumberType(stack.pop());
        if (result == null) {
            stack.push(null);
            return;
        }
        for (int i = 1; i < numArgs; i++) {
            BigDecimal next = checkNumberType(stack.pop());
            if (next == null) {
                stack.push(null);
                return;
            }
            if (next.compareTo(result) > 0) {
                result = next;
            }
        }
        stack.push(result);
    }

    private final int numArgs;
}
