package com.force.formula.commands;




import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.force.formula.*;
import com.force.formula.impl.*;

import java.util.Deque;

/**
 * Retrieve the value for associated field from the context and push onto the stack.
 * 
 * This assumes the reference is directly reference in the formula
 *
 * @author dchasman
 * @since 140
 */
public class FieldReferenceCommand extends BaseFieldReferenceCommand implements FormulaFieldReference {
    private static final long serialVersionUID = 1L;

    public FieldReferenceCommand(String commandName, String fieldName, boolean useUnderlyingType, boolean isRoot) {
        this(commandName, fieldName, useUnderlyingType, isRoot, false);
    }

    public FieldReferenceCommand(String commandName, String fieldName, boolean useUnderlyingType, boolean isRoot, boolean isDynamicReferenceBase) {
        super(commandName, useUnderlyingType, isRoot, isDynamicReferenceBase);
        assert fieldName != null;
        this.fieldName = fieldName;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        stack.push(execute(context));
    }

    public Object execute(FormulaRuntimeContext context) throws Exception {
        Object value = execute(context, this);
        // Some formula uses (SFX Email Templates) require a value, else it's an error.
        if (context.getGlobalProperties().shouldThrowOnEmptyFieldValue() && 
                (value == null || 
                (getDataType().isSimpleText() && FormulaTextUtil.isNullEmptyOrWhitespace(value.toString())))) {
            throw new MissingFieldValueException(fieldName);
        }
        return value;
    }

    @Override
    public FormulaException validateMergeFieldsForFormulaType(FormulaContext formulaContext) {
        try {
            formulaContext.lookup(this);
        }
        catch (FormulaException x) {
            return x;
        }
        return null;
    }

    @Override
    public List<FormulaFieldReferenceInfo> getDirectReference(FormulaContext formulaContext, ITableAliasRegistry reg,
                                                       boolean zeroExcluded, boolean allowDateValue, AtomicBoolean caseSafeIdUsed,
                                                       FormulaDataType formulaResultDataType) throws UnsupportedTypeException, InvalidFieldReferenceException {
        if (!BaseCompositeFormulaContext.isGlobalContextFieldReference(fieldName)) {
            return FormulaValidationHooks.get().parseHook_getFieldReferenceInfos(getElement(), isDynamicBase(), formulaResultDataType, formulaContext, reg);
        }
        return null;
    }



    // Methods implementing FormulaFieldReference
    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override
    public boolean isDynamicBase() {
        return isDynamicReferenceBase;
    }

    @Override
    public Object getBase() {
        return null;
    }

    @Override
    public String getElement() {
        return fieldName;
    }

    @Override
    public Object getSelector() {
        return fieldName;
    }

    @Override
    public String toString() {
        return fieldName;
    }

    @Override
    public boolean isFieldReference() {
        return true;
    }

    private final String fieldName;

}
