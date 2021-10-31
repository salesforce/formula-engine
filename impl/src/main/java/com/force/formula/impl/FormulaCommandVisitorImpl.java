package com.force.formula.impl;

import java.util.*;

import com.force.formula.*;
import com.force.formula.commands.AbstractFormulaCommand;
import com.google.common.base.Joiner;

/**
 * This will visit all FormulaCommands in a formula and provide a way to inspect each one for
 * validation and integrity checks.
 *
 * @author wmacklem
 * @since 156
 */
public abstract class FormulaCommandVisitorImpl implements FormulaCommandVisitor {
    private final FormulaContext formulaContext;
    private final Deque<String> nestedFormulaNames;

    protected FormulaCommandVisitorImpl(FormulaContext formulaContext) {
        this.formulaContext = formulaContext;
        this.nestedFormulaNames = new ArrayDeque<String>();
    }

    @Override
    public FormulaContext getFormulaContext() {
        return this.formulaContext;
    }

    /**
     * Indicate that we're are in a nested formula.  This should always be called
     * with popNestedFormula() in a finally block.
     */
    @Override
    public void pushNestedFormula(String formulaName) {
        this.nestedFormulaNames.push(formulaName);
    }

    @Override
    public void popNestedFormula() {
        this.nestedFormulaNames.pop();
    }

    String getNestedFormulaNames() {
        return nestedFormulaNames.size() > 0 ? Joiner.on('.').join(nestedFormulaNames) : null;
    }

    protected int getNestedFormulaSize() {
        return nestedFormulaNames.size();
    }

    /**
     * Check that all merge field referenced directly (in the formula itself) or indirectly (via
     * a formula field reference inside of this formula) are available for this FormulaType.
     *
     * If a merge field is invalid, throw a FormulaException
     */
    static class MergeFieldValidator extends FormulaCommandVisitorImpl {
        private FormulaException exception;

        MergeFieldValidator(FormulaContext formulaContext) {
            super(formulaContext);
        }

        @Override
        public void visit(FormulaCommand formulaCommand) {
            if (exception == null) {
                exception = formulaCommand.validateMergeFieldsForFormulaType(getFormulaContext());

                String formulaNames = getNestedFormulaNames();
                if (exception != null && formulaNames != null) {
                    exception = new NestedFormulaException(exception, formulaNames);
                }
            }
        }

        public void throwIfNecessary() throws FormulaException {
            if (exception != null) {
                throw exception;
            }
        }
    }

    /**
     * A Formula field is stale if it (directly or indirectly) refers to a stale summary field.
     */
    static class StaleSummaryField extends FormulaCommandVisitorImpl {
        private boolean isStale = false;

        StaleSummaryField(FormulaContext formulaContext) {
            super(formulaContext);
        }

        @Override
        public void visit(FormulaCommand formulaCommand) {
            this.isStale |= formulaCommand.isStale(getFormulaContext());
        }

        public boolean isStale() {
            return this.isStale;
        }
    }

    /**
     * Checks whether a formula is deterministic. A formula is deterministic if it always returns the same when called with the same inputs (EntityContexts in this case).
     * If a formula is not deterministic it cannot be summarized by a summary field.
     */
    static class DeterministicFormula extends FormulaCommandVisitorImpl {
        private boolean isDeterministic = true;

        DeterministicFormula(FormulaContext formulaContext) {
            super(formulaContext);
        }

        @Override
        public void visit(FormulaCommand formulaCommand) {
            // FindBugs doesn't like &=
            this.isDeterministic = this.isDeterministic && formulaCommand.isDeterministic(getFormulaContext());
        }

        public boolean isDeterministic() {
            return this.isDeterministic;
        }
    }

    /**
     * Checks whether a formula reference any AI Prediction Target field.
     */
    static class AIPredictionFieldReference extends FormulaCommandVisitorImpl {
        private boolean hasAIPredictionFieldReference = false;

        AIPredictionFieldReference(FormulaContext formulaContext) {
            super(formulaContext);
        }

        @Override
        public void visit(FormulaCommand formulaCommand) {
            this.hasAIPredictionFieldReference |= formulaCommand.hasAIPredictionFieldReference(getFormulaContext());
        }

        public boolean hasAIPredictionFieldReference() {
            return this.hasAIPredictionFieldReference;
        }
    }

    /**
     * Checks whether a formula is custom indexable. A formula is custom indexable if it is deterministic and
     * if it does not reference any fields whose updates might be missed by our custom index maintenance.
     */
    static class CustomIndexableFormula extends FormulaCommandVisitorImpl {
        private boolean isCustomIndexable = true;

        CustomIndexableFormula(FormulaContext formulaContext) {
            super(formulaContext);
        }

        @Override
        public void visit(FormulaCommand formulaCommand) {
            // FindBugs doesn't like &=
            this.isCustomIndexable = this.isCustomIndexable && formulaCommand.isCustomIndexable(getFormulaContext());
        }

        public boolean isCustomIndexable() {
            return this.isCustomIndexable;
        }
    }

    /**
     * Checks whether a formula is flex indexable. A formula is custom indexable if it is deterministic and
     * if it does not reference any fields whose updates might be missed by our custom index maintenance.
     */
    static class FlexIndexableFormula extends FormulaCommandVisitorImpl {
        private boolean isFlexIndexable = true;

        FlexIndexableFormula(FormulaContext formulaContext) {
            super(formulaContext);
        }

        @Override
        public void visit(FormulaCommand formulaCommand) {
            // FindBugs doesn't like &=
            this.isFlexIndexable = this.isFlexIndexable && formulaCommand.isFlexIndexable(getFormulaContext());
        }

        public boolean isFlexIndexable() {
            return this.isFlexIndexable;
        }
    }


    /**
     * Checks whether a formula requires a post-save index update. A formula requires this if it references any
     * fields which are postSaveIndexUpdate, such as an autonumber.
     */
    static class CustomIndexablePostSaveIndexUpdated extends FormulaCommandVisitorImpl {
        private final FormulaDmlType dmlType;
        private boolean postSaveIndexUpdated = false;

        CustomIndexablePostSaveIndexUpdated(FormulaContext formulaContext, FormulaDmlType dmlType) {
            super(formulaContext);
            this.dmlType = dmlType;
        }

        @Override
        public void visit(FormulaCommand formulaCommand) {
            // FindBugs doesn't like &=
            this.postSaveIndexUpdated = this.postSaveIndexUpdated || formulaCommand.isPostSaveIndexUpdated(getFormulaContext(), dmlType);
        }

        public boolean isPostSaveIndexUpdated() {
            return this.postSaveIndexUpdated;
        }
    }

    /**
     * Get all the field references and their evaluated values
     *
     * @author eli
     * @since 182
     */
    public static class FieldReferenceCommandVisitor extends FormulaCommandVisitorImpl {
        private Map<String, Object> refValuesMap = new HashMap<String, Object>();
        private final boolean getValues;
        private final boolean shouldExpand;

        /**
         * @param formulaContext
         * @param getValues true if this should also execute the field reference to get its value
         */
        public FieldReferenceCommandVisitor(FormulaContext formulaContext, boolean getValues) {
            super(formulaContext);
            this.getValues = getValues;
            this.shouldExpand = Boolean.TRUE.equals(formulaContext.getProperty(FormulaContext.EXPAND_FORMULA_REFERENCES));
        }

        @Override
        public void visit(FormulaCommand formulaCommand) {
            boolean shouldTrack;
            if (shouldExpand) {
                // Don't track formula fields here, just end field references
                shouldTrack = formulaCommand.isFieldReference();
            } else {
                // Only track the top level fields that are referenced directly from this formula
                shouldTrack = formulaCommand instanceof AbstractFormulaCommand
                        && "IDENT".equals(((AbstractFormulaCommand)formulaCommand).getName())
                        && getNestedFormulaSize() == 0;
            }
            if (shouldTrack) {
                String referenceName = formulaCommand.toString();
                try {
                    Object value = null;
                    if (this.getValues && this.getFormulaContext() instanceof FormulaRuntimeContext) {
                        // Only evaluate the value when debug formula is on (getValues is true)
                        Deque<Object> stack = new FormulaStack();
                        formulaCommand.execute((FormulaRuntimeContext)this.getFormulaContext(), stack);
                        value = stack.pop();
                    }
                    refValuesMap.put(referenceName, value);
                } catch (Exception x) {
                    refValuesMap.put(referenceName,
                            "ERROR: NEED MLA's LABEL: FormulaDebugErrorGeneric" + x.getMessage());
                }
            }
        }

        @Override
        public void pushNestedFormula(String formulaName) {
            // If we're not expanding formulas, only track the fields that are referenced directly from this formula.
            if (this.getNestedFormulaSize() == 0 && !this.shouldExpand) {
                refValuesMap.put(formulaName, null);
            }
            super.pushNestedFormula(formulaName);
        }

        public Map<String, Object> getRefValues() {
            return this.refValuesMap;
        }
    }
}
