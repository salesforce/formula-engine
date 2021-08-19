package com.force.formula.template.commands;

import java.lang.reflect.Type;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.commands.*;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 *
 * A base class for all text encoding commands
 *
 * @author emoses
 * @since 160
 */
public abstract class EncodingFunctionBase extends FormulaCommandInfoImpl implements FormulaCommandValidator {

    public EncodingFunctionBase(String name) {
        super(name);
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties)
        throws FormulaException {
        if (node.getNumberOfChildren() != 1) {
            throw new WrongNumberOfArgumentsException(node.getText(), 1, node);
        }

        FormulaAST arg = (FormulaAST)node.getFirstChild();
        Type dataType = arg.getDataType();
        if (!FormulaTypeUtils.isTypeText(dataType) && dataType != RuntimeType.class) {
            throw new IllegalArgumentTypeException(node.getText());
        }

        return dataType == RuntimeType.class ? dataType : String.class;
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards,
        TableAliasRegistry registry) throws FormulaException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        throw new UnsupportedOperationException();
    }

    //Subclass this and implement encode to do the work.
    public static abstract class EncodingCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;

		public EncodingCommand(FormulaCommandInfo formulaCommandInfo) {
            super(formulaCommandInfo);
        }

        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
            Object arg = stack.pop();
            arg = (arg == null) ? "" : arg;
            stack.push(encode(checkStringType(arg)));
        }

        protected abstract String encode(String arg);

    }

}
