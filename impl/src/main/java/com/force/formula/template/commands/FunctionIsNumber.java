package com.force.formula.template.commands;

import java.lang.reflect.Type;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.commands.*;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.BigDecimalHelper;

@AllowedContext(section=SelectorSection.LOGICAL, isOffline=true)
public class FunctionIsNumber extends FormulaCommandInfoImpl implements FormulaCommandValidator {
    public FunctionIsNumber() {
        super("ISNUMBER");
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties)
        throws FormulaException {
        if (node.getNumberOfChildren() != 1) {
            throw new WrongNumberOfArgumentsException(node.getText(), 1, node);
        }
        Type dataType = ((FormulaAST)node.getFirstChild()).getDataType();
        if (dataType != String.class && dataType != RuntimeType.class) {
            throw new IllegalArgumentTypeException(node.getText());
        }

        return dataType == RuntimeType.class ? dataType : Boolean.class;
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws FormulaException {
        return new FunctionIsNumberCommand(this);
    }

    /*
     * Make the matching as efficient as possible (regex's are sloooowww).
     * 1. condense sequences of digits to one 0
     * 2. then list all valid cases
     * This is _much_ faster than doing the matching in one regex as it limits backtracking to at most one character.
     */
    public static String getSQL(FormulaContext context, String arg) {
        return String.format(getSqlHooks(context).sqlIsNumber(), arg);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
        throws FormulaException {
        return new SQLPair(getSQL(context, args[0]), guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return new JsValue((args[0].guard!=null?"!"+args[0].guard:"")+"!isNaN(" + args[0].js + ")", null, false);
    }


    //@edu.umd.cs.findbugs.annotations.SuppressWarnings({ "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "RV_RETURN_VALUE_IGNORED"})
    private static class FunctionIsNumberCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;

		public FunctionIsNumberCommand(FormulaCommandInfo formulaCommandInfo) {
            super(formulaCommandInfo);
        }

        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
            Object input = stack.pop();
            if (input != null && input != ConstantString.NullString) {
                stack.push(BigDecimalHelper.functionIsNumber(checkStringType(input)));
            } else {
                stack.push(Boolean.FALSE);
            }
        }
    }
}
