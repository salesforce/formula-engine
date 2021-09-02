/*
 * Created on Jan 14, 2005
 */
package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.force.formula.*;
import com.force.formula.impl.*;
import com.force.formula.sql.ITableAliasRegistry;
import com.force.formula.util.BaseCompositeFormulaContext;

/**
 * Represents a composed sub formula when referenced in another formula through a field reference.
 *
 * @author dchasman
 * @since 140
 */
public class CompositeCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;
    // Note: type is here to support cases where the value returned by the composed sub-formula is
    // not what is actually expected by the declared type of the formula. Currently, the only
    // known case of this is date formula fields that actually return a date-time result.
    // Also removed tristate logic from explicit booleans
    private final FormulaDataType type;
    private final Formula formula;
    private final String fieldName;

    public CompositeCommand(FormulaCommandInfo info, String fieldName, Formula formula, FormulaDataType type) {
        super(info);
        assert formula != null;
        this.formula = formula;
        this.type = type;
        this.fieldName = fieldName;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        ContextualFormulaFieldInfo fieldInfo = FieldReferenceCommandInfo.lookup(context, fieldName, false);
        boolean shouldFollowFls = context.getGlobalProperties().getFormulaType().isTemplate()
                && !context.getGlobalProperties().shouldIgnoreFls();
        if (shouldFollowFls
                && !FormulaEngine.getHooks().isFieldReadable(fieldInfo.getFieldOrColumnInfo().getFieldInfo())) {
            // If we're in normal mode (e.g. we're evaluating a template), and we don't have FLS access to this field,
            // we shouldn't evaluate it.
            stack.push(null);
            return;
        }

        /*
         * !!! Make sure this currency stuff stays in synch with FieldReferenceCommand
         */
        String sourceIsoCode = null;
        String targetIsoCode = null;
        if (type.isCurrency()) {
            sourceIsoCode = context.getCurrencyIsoCode(fieldName);
            targetIsoCode = context.getCurrencyIsoCode();
        }
        FormulaContextSwitcher switcher = new FormulaContextSwitcher(context);
        switcher.switchFormulaContext(fieldInfo);
        FormulaInformationContext.Provider fixProvider = FormulaEngine.getHooks().getInformationContextProvider();
        FormulaInformationContext fic = fixProvider != null
                ? FormulaEngine.getHooks().getInformationContextProvider().push(context)
                : null;

        try {
            Object result = formula.evaluateRaw(context);
            // Do any necessary conversions of the returned result
            if (type.isDateOnly()) {
                if (result instanceof FormulaDateTime) {
                    result = ((FormulaDateTime)result).getDate();
                }
            } else if (type.isCurrency() && sourceIsoCode != null && targetIsoCode != null) {
                BigDecimal value = checkNumberType(result);
                if (!sourceIsoCode.equals(targetIsoCode)) {
                    value = FormulaEngine.getHooks().convertCurrency(value, targetIsoCode, false);
                    value = FormulaEngine.getHooks().convertCurrency(value, sourceIsoCode, true);
                    result = value;
                }
            } else if (type.isBoolean() && result == null) {
                result = Boolean.FALSE;
            }
            stack.push(result);
        } finally {
            switcher.revertToOriginalFormulaContext();
            if (fixProvider != null) {
                fixProvider.pop(fic);
            }
        }
    }

    @Override
    public void visit(FormulaCommandVisitor visitor) {
        if (Boolean.FALSE.equals(visitor.getFormulaContext().getProperty(FormulaContext.EXPAND_FORMULA_REFERENCES))) {
            visitor.visit(this);
            return;
        }

        FormulaContextSwitcher switcher = new FormulaContextSwitcher(visitor.getFormulaContext());

        ContextualFormulaFieldInfo ffi;
        try {
            // Check for GlobalContextFieldReference's which begins with '$' in CompositeCommand formulas
            // bug W-2458104
            visitor.visit(this);
            ffi = FieldReferenceCommandInfo.lookup(visitor.getFormulaContext(), fieldName, false);
            switcher.switchFormulaContext(ffi);
        } catch (FormulaException x) {
            throw new RuntimeException(x);
        }

        visitor.pushNestedFormula(fieldName);

        try {
            FormulaSchema.FieldOrColumn fci = ffi.getFieldOrColumnInfo();
            FormulaValidationHooks.get().parseHook_visitCompositeFormulaCommand(formula, visitor, fci);
        } finally {
            switcher.revertToOriginalFormulaContext();
            visitor.popNestedFormula();
        }
    }

    public boolean hasAttribute(int bit) {
        return formula.hasAttribute(bit);
    }

    @Override
    public boolean isDeterministic(FormulaContext formulaContext) {
        // For Composite commands check if the composite command belongs to Global Context fields
        if (BaseCompositeFormulaContext.isGlobalContextFieldReference(this.fieldName)) { return false; }

        // additional check for spanning formula here. This is a subset from FieldReferenceCommand.isDeterministic.
        // This is needed here for cases where a reference is to a constant field and is optimized to one of the
        // constant commands
        // e.g. ConstantFalseCommand.
        try {
            ContextualFormulaFieldInfo ffi = formulaContext.lookup(this.fieldName);

            // this field is on a related entity via a spanning formula
            if (ffi.getFormulaContext() != null && ffi.getFormulaContext().getParentContext() != null
            // Special case for ParentedDefaultValueFormulaContext.ParentObjectFormulaContext where the root
            // formulaContext has a parentContext, but it's the same as the context itself
                    && !(ffi.getFormulaContext().equals(ffi.getFormulaContext().getParentContext()))) { return false; }
        } catch (FormulaException x) {
            FormulaValidationHooks.get().parseHook_logDeterminismFailure(x, this.fieldName);
            return false;
        }
        return true;
    }

    @Override
    public void preExecuteInBulk(List<FormulaRuntimeContext> contexts) throws Exception {
        FormulaContextSwitcher switcher = new FormulaContextSwitcher(contexts);

        switcher.switchFormulaContext(FieldReferenceCommandInfo.lookup(contexts.get(0), fieldName, false));

        try {
            formula.bulkProcessingBeforeEvaluation(contexts);
        } finally {
            switcher.revertToOriginalFormulaContext();
        }
    }

    @Override
    public List<FormulaFieldReferenceInfo> getDirectReference(FormulaContext formulaContext, ITableAliasRegistry reg,
            boolean zeroExcluded, boolean allowDateValue, AtomicBoolean caseSafeIdUsed, FormulaDataType formulaResultDataType) throws UnsupportedTypeException, InvalidFieldReferenceException {
        // Switch formula context to evaluate the nested formula
        FormulaContextSwitcher switcher = new FormulaContextSwitcher(formulaContext);
        ContextualFormulaFieldInfo fieldInfo = FieldReferenceCommandInfo.lookup(formulaContext, fieldName, false);
        switcher.switchFormulaContext(fieldInfo);
        try {
            // This method will walk back using the formula context and return the full path based on the original formula field passing
            // passing null namespace here makes sense given that composite command is referring to another formula field and we should consider that namespace for directreferencing another field.
            return formula.getFieldPathIfDirectReferenceToAnotherField(formulaContext, zeroExcluded, allowDateValue, caseSafeIdUsed, null);
        } finally {
            switcher.revertToOriginalFormulaContext();
        }
    }

    @Override
    public String toString() {
        return fieldName;
    }

}
