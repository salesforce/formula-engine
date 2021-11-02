/*
 * Copyright, 1999-2010, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.commands;

import com.force.formula.*;
import com.force.formula.template.commands.DynamicReference.DynamicReferenceCommand;
import com.force.formula.util.FormulaFieldReferenceImpl;

/**
 * Common base class used by StaticFieldReferenceCommand and DynamicReferenceCommand
 *
 * @author aballard
 * @since 168
 */
public abstract class BaseFieldReferenceCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

    /**
     * Construct a base field reference type
     * @param commandName the unique command of the subclass
     * @param useUnderlyingType whether to use the type of the field or the template type of the field
     * @param isRoot is this the "root" of the formula.  If so, it'll return the field reference directly so the caller can do it themselves
     * @param isDynamicReferenceBase support for the a[b] syntax in the formula engine
     */
    public BaseFieldReferenceCommand(String commandName, boolean useUnderlyingType, boolean isRoot, boolean isDynamicReferenceBase) {
        super(commandName);
        // We need to record if this is top level field reference, since in that case we will not
        // evaluate to a value.
        this.isRoot = isRoot;
        this.useUnderlyingType = useUnderlyingType;
        this.isDynamicReferenceBase = isDynamicReferenceBase;
    }

    public FormulaDataType getDataType() {
        return this.dataType;
    }

    public Object execute(FormulaRuntimeContext context, FormulaFieldReference fieldReference) throws FormulaException {
        // If this is flagged as the root reference node, then we return a fieldReference.
        // This flag is set only for root nodes, and only when compiled as a reference formula.
        if (isRoot) {
            // We want to return an object+property name. If the element name  part is multiple part, we must combine the base
            // with all parts of the name except the last.
            int lastIndexOf = fieldReference.getElement().lastIndexOf('.');
            if (lastIndexOf >= 0) {
                String propertyName = fieldReference.getElement().substring(lastIndexOf + 1);
                String baseExpressionString = fieldReference.getElement().substring(0, lastIndexOf);
                DynamicReferenceCommand command = new DynamicReferenceCommand(getName(), false, true, true);
                Object base = command.execute(context, new FormulaFieldReferenceImpl(fieldReference.getBase(), baseExpressionString));
                return new FormulaFieldReferenceImpl(base, propertyName);
            }
            // The input FieldReference IS the value.
            return fieldReference;
        }

        // Lookup the Field Id via the runtime context
        ContextualFormulaFieldInfo fieldInfo = FieldReferenceCommandInfo.lookup(context, fieldReference);

        // Get the value of the field and push on to the stack
        Object value ;
        dataType = useUnderlyingType ? fieldInfo.getTemplateDataType() : fieldInfo.getDataType();

        FormulaEngineHooks hooks = FormulaEngine.getHooks();
        value = hooks.getFieldReferenceValue(fieldInfo, dataType, context, fieldReference, useUnderlyingType);


        if (dataType.isId() && value != null && context.convertIdto18Digits()) {
            value = hooks.convertIdTo18Digits(value.toString());
        }

        // Hack for dynamic fields.  If we got a null value here, turn it into NullString object, so
        // comparison operators know whether to do string ops or not.
        // Probably safe to do this all the time, but doing only for dynamic fields to minimize impact.
        if (value == null && fieldReference.isDynamic() && dataType.isTextOrEncrypted() && !dataType.isEncrypted()) {
            value = ConstantString.NullString;
        }

        return value;
    }
    
    public static Object dataTypeCheckHelper(FormulaDataType dt, FormulaRuntimeContext context, FormulaFieldReference fieldReference, boolean useUnderlyingType, boolean escapeStringForSQLGeneration)
            throws FormulaException {
        return FormulaEngine.getHooks().getAndConvertFieldReferenceValue(dt, context, fieldReference, useUnderlyingType, escapeStringForSQLGeneration);
    }


    protected final boolean useUnderlyingType;
    protected final boolean isRoot;
    protected final boolean isDynamicReferenceBase;
    protected FormulaDataType dataType;
}
