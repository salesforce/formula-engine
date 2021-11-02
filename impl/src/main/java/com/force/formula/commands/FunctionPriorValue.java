package com.force.formula.commands;


import java.lang.reflect.Type;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.SQLPair;

/**
 *
 * @author dchasman
 * @since 144
 */

@AllowedContext(section=SelectorSection.LOGICAL,changeOnly=true,nonFlowOnly=true,isJavascript=false)
public class FunctionPriorValue extends FormulaCommandInfoImpl implements FormulaCommandValidator {
    public static final String OBJECT_TYPE_PREFIX = "$ObjectType";
    public static final String OBJECT_TYPE_PREFIX_UPPER = OBJECT_TYPE_PREFIX.toUpperCase(); // optimization to avoid repeated calls of toUpperCase()

    public FunctionPriorValue() {
        super("PRIORVALUE");
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        FormulaAST fieldReferenceArgument = getFieldReference(node);
        return new FunctionPriorValueCommand(this, fieldReferenceArgument.getText());
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
        throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult("context.old."+args[0]+"", args);  // Guard isn't right
    }
    
    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        if (node.getNumberOfChildren() != 1) {
            throw new WrongNumberOfArgumentsException(node.getText(), 1, node);
        }
        
        FormulaAST arg = (FormulaAST)node.getFirstChild();
        Type argDataType = arg.getDataType();
        if (argDataType == FormulaGeolocation.class)
            throw new IllegalArgumentTypeException(node.getText());

        FormulaAST fieldReferenceArgument = getFieldReference(node);
        if (fieldReferenceArgument.getType() != FormulaTokenTypes.IDENT) {
            throw new IllegalArgumentTypeException(node.getText());
        }

        /* PRIORVALUE() only makes sense for fields on the current record.  It cannot reference any related object fields.
        Therefore, the check is very simple.  If the field contains a "." char, then the field must not be on the current record,
        and we should throw an exception.
        */
        // When we have versioned formulas, we should disallow all references to context variables.
       if ((fieldReferenceArgument.getText().contains(".") && !properties.getAllowBadRelatedFieldReferences()) || isReferenceToObjectTypeContext(fieldReferenceArgument.getText().toUpperCase()))
           throw new InvalidFieldReferenceForFunctionException(fieldReferenceArgument.getText(), getName());

        // "Inherit" the type of the args
        node.setColumnType(fieldReferenceArgument.getColumnType());

        return fieldReferenceArgument.getDataType();
    }

    public static FormulaAST getFieldReference(FormulaAST node) {
        FormulaAST fieldReferenceArgument = (FormulaAST)node.getFirstChild();

        // Check to see if the function has been wrapped for null value handling
        if ("NULLVALUE".equals(fieldReferenceArgument.getText())) {
            fieldReferenceArgument = (FormulaAST)fieldReferenceArgument.getFirstChild();
        }
        return fieldReferenceArgument;
    }

    public static boolean isReferenceToObjectTypeContext(String mergeFieldNameUpper) {
        return mergeFieldNameUpper != null && mergeFieldNameUpper.startsWith(OBJECT_TYPE_PREFIX_UPPER);
    }

}

class FunctionPriorValueCommand extends AbstractFormulaCommand {
	private static final long serialVersionUID = 1L;
    private String target;

    public FunctionPriorValueCommand(FormulaCommandInfo info, String target) {
        super(info);
        this.target = target;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws FormulaException {
        stack.pop(); // Throw away the current field value

        FormulaRuntimeContext originalValuesContext = context.getOriginalValuesContext();
        if (originalValuesContext != null) {
            FieldReferenceCommand fieldReferenceCommand = new FieldReferenceCommand(getName(), target, false, false);
            fieldReferenceCommand.execute(originalValuesContext, stack);
        } else {
            stack.push(null);
        }
    }

    @Override
    public boolean isDeterministic(FormulaContext formulaContext) {
        return false;
    }
}
