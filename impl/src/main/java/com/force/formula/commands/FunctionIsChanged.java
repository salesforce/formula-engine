package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaTextUtil;

/**
 *
 * @author dchasman
 * @since 144
 */

@AllowedContext(section=SelectorSection.ADVANCED, isSql = false, changeOnly=true, nonFlowOnly=true,isJavascript=false)
public class FunctionIsChanged extends FormulaCommandInfoImpl implements FormulaCommandValidator {
    public FunctionIsChanged() {
        super("ISCHANGED");
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        FormulaAST fieldReferenceArgument = FunctionPriorValue.getFieldReference(node);
        return new FunctionIsChangedCommand(this, fieldReferenceArgument.getText());
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
        throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult("context.isChanged(" + args[0] + ")", args);
    }


    @Override
    public Class<?> validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        if (node.getNumberOfChildren() != 1) {
            throw new WrongNumberOfArgumentsException(node.getText(), 1, node);
        }

        FormulaAST fieldReferenceArgument = FunctionPriorValue.getFieldReference(node);
        if (fieldReferenceArgument.getType() != FormulaTokenTypes.IDENT) {
            throw new IllegalArgumentTypeException(node.getText());
        }

        /* ISCHANGED() only makes sense for fields on the current record.  It cannot reference any related object fields.
         Therefore, the check is very simple.  If the field contains a "." char, then the field must not be on the current record,
         and we should throw an exception.
         */
        if ((fieldReferenceArgument.getText().contains(".") && !properties.getAllowBadRelatedFieldReferences()) || FunctionPriorValue.isReferenceToObjectTypeContext(fieldReferenceArgument.getText().toUpperCase()))
            throw new InvalidFieldReferenceForFunctionException(fieldReferenceArgument.getText(), getName());


        return Boolean.class;
    }

    public static boolean isChanged(Object originalValue, Object currentValue) {
        boolean isChanged;

        if (originalValue != null) {
            if (currentValue == null) {
                isChanged = true;
            } else {
                // normalize \r\n to just \n to avoid false positives due to differences in operating systems
                if (originalValue instanceof String) {
                    originalValue = FormulaTextUtil.replaceSimple((String)originalValue, "\r\n", "\n");
                }

                if (currentValue instanceof String) {
                    currentValue = FormulaTextUtil.replaceSimple((String)currentValue, "\r\n", "\n");
                }

                // equals does not give correct result. For example new BigDecimal("20").equals(new BigDecimal("20.00")) == false
                // compareTo scales the BigDecimals correctly before comparing.
                if (originalValue instanceof BigDecimal && currentValue instanceof BigDecimal) {
                    isChanged = ((BigDecimal)originalValue).compareTo((BigDecimal)currentValue) != 0;
                } else
                    isChanged = !originalValue.equals(currentValue);
            }
        } else {
            isChanged = (currentValue != null);
        }

        return isChanged;
    }
}

class FunctionIsChangedCommand extends AbstractFormulaCommand {
	private static final long serialVersionUID = 1L;
	private final String target;

    public FunctionIsChangedCommand(FormulaCommandInfo info, String target) {
        super(info);
        this.target = target;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws FormulaException {
        boolean isChanged = false;

        FormulaRuntimeContext originalValuesContext = context.getOriginalValuesContext();
        if (originalValuesContext != null) {
            FieldReferenceCommand fieldReferenceCommand = new FieldReferenceCommand(getName(), target, false, false);
            fieldReferenceCommand.execute(originalValuesContext, stack);

            Object originalValue = stack.pop();
            Object currentValue = stack.pop();

            isChanged = FunctionIsChanged.isChanged(originalValue, currentValue);
        }

        stack.push(isChanged);
    }

    @Override
    public boolean isDeterministic(FormulaContext formulaContext) {
        return false;
    }
}
