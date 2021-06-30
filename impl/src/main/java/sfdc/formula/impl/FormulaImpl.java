/*
 * Copyright, 2006, salesforce.com All Rights Reserved Company Confidential
 */

package sfdc.formula.impl;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Joiner;

import sfdc.formula.*;
import sfdc.formula.commands.ConstantNumber.NumberConstantCommand;
import sfdc.formula.commands.ConstantString.StringWrapper;
import sfdc.formula.commands.FormulaCommand;
import sfdc.formula.commands.FormulaCommandInfoRegistry;
import sfdc.formula.commands.FunctionDateValue.OperatorDateValueFormulaCommand;
import sfdc.formula.commands.FunctionNullValue.FunctionNullValueFormulaCommand;
import sfdc.formula.util.BigDecimalHelper;

/**
 * Used for the runtime evaluation of a formula in both pointwise and bulk contexts.
 *
 * @author dchasman
 * @since 140
 */
public class FormulaImpl implements FormulaWithSql {

    private static final long serialVersionUID = 1L;

    public FormulaImpl() {
        dataType = null;
    }

    public FormulaImpl(FormulaCommand[] commands, String sqlInitial, String guard, JsValue javascript, FormulaProperties properties, FormulaReturnType formulaFieldInfo,  // NOPMD
            Type actualReturnType, boolean producesHTML, boolean getsSessionId, boolean usesCustomFields, 
            TableAliasRegistry registry) { 
        this.commands = commands;
        this.sqlRaw = sqlInitial;
        this.guard = guard;
        this.javascript = javascript != null ? javascript.js : null;
        this.jsGuard = javascript != null ? javascript.guard : null;
        this.registry = registry;
        this.actualReturnType = actualReturnType;
        this.dataType = formulaFieldInfo.getDataType();

        this.attributes = new BitSet();

        // only text fields can produce html
        if (producesHTML && formulaFieldInfo.getDataType().isSimpleText()) {
            attributes.set(PRODUCES_HTML);
        }

        if (getsSessionId) {
            attributes.set(GETS_SESSION_ID);
        }

        if (usesCustomFields) {
            attributes.set(REFERENCES_CUSTOM_FIELDS);
        }

        if (javascript != null && javascript.couldBeNull) {
            attributes.set(JS_COULD_BE_NULL);
        }

        String tempSql = massageSqlForType(formulaFieldInfo, sqlInitial);

        // Sanity check guard is the same, since the next statement assumes that the error column
        // is determined by the Oracle guard presense.
        if (guard != null) {
            tempSql = "CASE WHEN " + guard + " THEN NULL ELSE " + tempSql + " END";
            attributes.set(PRODUCES_SQL_ERROR_COLUMN);
        }

        if (this.sqlRaw != null && this.sqlRaw.length() > 0 && tempSql.length() > this.sqlRaw.length()) {
            int idx = tempSql.indexOf(this.sqlRaw);
            if (idx != -1) {
                this.sqlRaw = tempSql.substring(idx, idx + sqlRaw.length());
            }
        }

        this.sql = tempSql;
    }

    /**
     * We apply final formatting syntax to deal with the expected length/precision.
     */
    static String massageSqlForType(FormulaReturnType formulaFieldInfo, String sqlInitial) {
        if (formulaFieldInfo.getDataType().isSimpleText()) {
            // Truncate strings to max string length
            return "SUBSTR(" + sqlInitial + ", 0, " + FormulaInfo.MAX_STRING_VALUE_LENGTH + ")";
        } else if (formulaFieldInfo.getDataType().isPercent()) {
            // Set the scale as determined by the field
            // Multiply by 100 to display correctly.
            return "ROUND(" + sqlInitial + " * 100.0, " + formulaFieldInfo.getScale() + ")";
        } else if (formulaFieldInfo.getDataType().isDecimal()) { // Double or currency
            // Set the scale as determined by the field
            return "ROUND(" + sqlInitial + ", " + formulaFieldInfo.getScale() + ")";
        } else if (formulaFieldInfo.getDataType().isBoolean()) {
            // Need to guard with "CASE WHEN so that SQL doesn't freak out"
            return "CASE WHEN " + sqlInitial + " THEN '1' ELSE '0' END";
        } else {
            return sqlInitial;
        }
    }

    @Override
    public Object evaluate(FormulaRuntimeContext context) throws Exception {
        assert commands != null;
        return formatResult(context, context.getFormulaReturnType(), evaluateRaw(context));
    }

    /**
     * This method is called from CompositeCommand to avoid the final jiggering for precision and scale and percent
     * fields.
     */
    @Override
    public Object evaluateRaw(FormulaRuntimeContext context) throws Exception {
        long runtimeInitial = System.currentTimeMillis();
        Exception thrownException = null;
        Object result = null;

        try {
            Deque<Object> stack = new FormulaStack();
            for (FormulaCommand command : commands) {
                command.execute(context, stack);
            }
            result = stack.pop();
            // Don't pass back out internal type wrapper for null strings.
            if (result instanceof StringWrapper) {
                result = result.toString();
            }
        } catch (Exception e) {
            thrownException = e;
            throw e;
        } finally {
            if (FormulaEngine.getHooks().shouldLogRuntime()) {
                FormulaEngine.getHooks().logFormulaRuntime(runtimeInitial, 0, null, null,
                        getSQLRaw() != null ? getSQLRaw().length() : 0,
                        toJavascript() != null ? toJavascript().length() : 0, context.getClass().getSimpleName(), true,
                        thrownException);
            }
        }
        return result;
    }

    public static Object formatResult(FormulaRuntimeContext context, FormulaReturnType formulaFieldInfo,
            Object result) throws Exception{
        if (result != null) {
            if (formulaFieldInfo.getDataType().isSimpleText()) {
                // Truncate strings to max string length
                String stringValue;
                if (result instanceof String) {
                    stringValue = (String)result;
                } else {
                    stringValue = Joiner.on("").join((Object[])result);
                }

                result = stringValue.substring(0, Math.min(stringValue.length(), FormulaInfo.MAX_STRING_VALUE_LENGTH));
            } else if (formulaFieldInfo.getDataType().isInteger()) {
                result = new BigDecimal(new BigInteger(result.toString()));
            } else if (formulaFieldInfo.getDataType().isPercent()) {
                // Set the scale as determined by the field
                // Multiply percent field by 100 for display
                result = BigDecimalHelper.round(((BigDecimal)result).movePointRight(2), formulaFieldInfo.getScale());
            } else if (formulaFieldInfo.getDataType().isDecimal()) {
                final int scale = ((BigDecimal)result).scale();
                final int MAX_FORMULA_LENGTH = 3900;
                // Prevent evaluating numbers with negative scales that are beyond the max formula length,
                // as this evaluation can be extremely CPU intensive and slow
                if (scale < 0 && Math.abs(scale) > MAX_FORMULA_LENGTH) {
                    throw new FormulaEvaluationException(new FormulaTooLongException(scale, MAX_FORMULA_LENGTH));
                }
                // Set the scale as determined by the field
                result = BigDecimalHelper.round((BigDecimal)result, formulaFieldInfo.getScale());
            }
        }

        return result;
    }

    @Override
    public void bulkProcessingBeforeEvaluation(List<FormulaRuntimeContext> contexts) throws Exception {
        for (FormulaCommand command : commands) {
            command.preExecuteInBulk(contexts);
        }
    }

    @Override
    public String toSQL(FormulaTableRegistry registry) throws FormulaException {
        return performLateBinding(this.sql, registry);
    }

    @Override
    public String toSQLError(FormulaTableRegistry registry) throws FormulaException {
        String result = null;
        if (guard != null) {
            String sqlError = "CASE WHEN " + guard + " THEN 1 ELSE 0 END";
            result = performLateBinding(sqlError, registry);
        }

        return result;
    }

    @Override
    public String performLateBinding(String sql, FormulaTableRegistry queryTableRegistry) throws FormulaException {
        if (sql == null) { return null; }

        String result = this.registry.lateBindTableAliases(sql, queryTableRegistry);

        for (BindingObserver bindingObserver : FormulaCommandInfoRegistry.getBindingObservers()) {
            result = bindingObserver.bind(result);
        }

        return result;
    }

    @Override
    public String getSQLRaw() {
        return sqlRaw;
    }

    @Override
    public String getGuard() {
        return guard;
    }

    @Override
    public String getJavascriptRaw() {
        return javascript;
    }

    @Override
    public String getJavascriptGuard() {
        return jsGuard;
    }

    @Override
    public boolean couldJavascriptBeNull() {
        return attributes.get(JS_COULD_BE_NULL);
    }

    @Override
    public String toJavascript() {
        // Resolve try-state boolean.
        String ifNull = getDataType() != null && getDataType().isBoolean() ? "false" : "null";
        return jsGuard != null ? "(" + jsGuard + ")?(" + javascript + "):" + ifNull : javascript;
    }

    @Override
    public boolean hasAttribute(int attribute) {
        return attributes.get(attribute);
    }

    @Override
    public int compareTo(Formula f) {
        FormulaImpl rhs = (FormulaImpl)f;

        if (commands.length != rhs.commands.length) { return -1; }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof FormulaImpl)) { return false; }
        FormulaImpl f = (FormulaImpl)o;
        return this.attributes.equals(f.attributes) && Arrays.equals(this.commands, f.commands);
    }

    @Override
    public int hashCode() {
        return this.attributes.hashCode() ^ Arrays.hashCode(this.commands);
    }

    @Override
    public boolean isDeterministic(FormulaContext formulaContext) {
        FormulaCommandVisitor.DeterministicFormula visitor = new FormulaCommandVisitor.DeterministicFormula(
                formulaContext);
        visitFormulaCommands(visitor);
        return visitor.isDeterministic();
    }

    @Override
    public boolean hasAIPredictionFieldReference(FormulaContext formulaContext) {
        FormulaCommandVisitor.AIPredictionFieldReference visitor = new FormulaCommandVisitor.AIPredictionFieldReference(
                formulaContext);
        visitFormulaCommands(visitor);
        return visitor.hasAIPredictionFieldReference();

    }

    @Override
    public boolean isCustomIndexable(FormulaContext formulaContext) {
        FormulaCommandVisitor.CustomIndexableFormula visitor = new FormulaCommandVisitor.CustomIndexableFormula(
                formulaContext);
        visitFormulaCommands(visitor);
        return visitor.isCustomIndexable();
    }

    @Override
    public boolean isFlexIndexable(FormulaContext formulaContext) {
        FormulaCommandVisitor.FlexIndexableFormula visitor = new FormulaCommandVisitor.FlexIndexableFormula(
                formulaContext);
        visitFormulaCommands(visitor);
        return visitor.isFlexIndexable();
    }

    @Override
    public boolean isPostSaveIndexUpdated(FormulaContext formulaContext, FormulaDmlType dmlType) {
        FormulaCommandVisitor.CustomIndexablePostSaveIndexUpdated visitor = new FormulaCommandVisitor.CustomIndexablePostSaveIndexUpdated(
                formulaContext, dmlType);
        visitFormulaCommands(visitor);
        return visitor.isPostSaveIndexUpdated();
    }

    @Override
    public List<FormulaFieldReferenceInfo> getFieldPathIfDirectReferenceToAnotherField(FormulaContext formulaContext,
            boolean zeroExcluded, boolean allowDateValue, AtomicBoolean caseSafeIdUsed, String namespace) throws UnsupportedTypeException, InvalidFieldReferenceException {
        boolean isNamespacePushed = false;
        if (namespace != null) {            
            FormulaEngine.getHooks().hook_pushComponentNamespace(namespace);
            isNamespacePushed = true;
        }
        try {
            // Global Context field references, like $RecordType.Name are excluded here
            if (this.commands.length == 1) {
                return commands[0].getDirectReference(formulaContext, getTableAliasRegistry(), zeroExcluded, allowDateValue, caseSafeIdUsed, dataType);
            } else if (zeroExcluded && this.commands.length == 3 && this.commands[2] instanceof FunctionNullValueFormulaCommand && 
                    this.commands[1] instanceof NumberConstantCommand && ((NumberConstantCommand)this.commands[1]).getValue().equals(BigDecimal.ZERO)) {
                // If the caller indicates numeric 0 is excluded (i.e. it can't be queried for), we can return a direct path
                // when this is a NVL(<field>,0)
                return commands[0].getDirectReference(formulaContext, getTableAliasRegistry(), zeroExcluded, allowDateValue, caseSafeIdUsed, dataType);
            } else if (this.commands.length == 2 && this.commands[1] instanceof OperatorDateValueFormulaCommand && allowDateValue) {
                return commands[0].getDirectReference(formulaContext, getTableAliasRegistry(), zeroExcluded, allowDateValue, caseSafeIdUsed, dataType);
            } else if (this.commands.length == 2 && "CASESAFEID".equals(commands[1].getName())) {
                caseSafeIdUsed.set(true);
                return commands[0].getDirectReference(formulaContext, getTableAliasRegistry(), zeroExcluded, allowDateValue, caseSafeIdUsed, dataType);
            }
        } finally {
            if (isNamespacePushed) {                
                FormulaEngine.getHooks().hook_popComponentNamespace();
            }
        }
        return null;
    }

    @Override
    public boolean isStale(FormulaContext formulaContext) {
        FormulaCommandVisitor.StaleSummaryField visitor = new FormulaCommandVisitor.StaleSummaryField(formulaContext);
        visitFormulaCommands(visitor);
        return visitor.isStale();
    }

    @Override
    public <T extends FormulaTableRegistry.TableIdentifier> List<T> getDependentTables(
            FormulaTableRegistry queryTableRegistry) {
        return getTableAliasRegistry().getDependentTables(sql, queryTableRegistry);
    }

    @Override
    public TableAliasRegistry getTableAliasRegistry() {
        if (registry == null) { throw new UnsupportedOperationException("Registry cannot be null!"); }

        return registry;
    }

    @Override
    public void visitFormulaCommands(FormulaCommandVisitor visitor) {
        for (FormulaCommand command : commands) {
            command.visit(visitor);
        }
    }

    @Override
    public Type getActualReturnType() {
        return actualReturnType;
    }

    @Override
    public FormulaDataType getDataType() {
        return this.dataType;
    }

    private FormulaCommand[] commands;
    private BitSet attributes;
    private Type actualReturnType;
    private final FormulaDataType dataType;

    // TODO
    //@edu.umd.cs.findbugs.annotations.SuppressWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    private transient String sql;
    private transient String sqlRaw;
    private transient String guard;
    private transient TableAliasRegistry registry;
    private transient String javascript;
    private transient String jsGuard;

}