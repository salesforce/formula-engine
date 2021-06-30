package sfdc.formula.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import sfdc.formula.*;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public class InvalidFormula implements FormulaWithSql {

    private static final long serialVersionUID = 1L;

    public InvalidFormula(RuntimeException cause) {
        this.cause = cause;
    }

    @Override
    public Object evaluate(FormulaRuntimeContext context) throws Exception {
        throw cause;
    }

    @Override
    public Object evaluateRaw(FormulaRuntimeContext context) throws Exception {
        throw cause;
    }

    @Override
    public void bulkProcessingBeforeEvaluation(List<FormulaRuntimeContext> contexts) {
        throw cause;
    }

    @Override
    public String toSQL(FormulaTableRegistry registry) {
        return "NULL";
    }

    @Override
    public String toSQLError(FormulaTableRegistry registry) {
        return "1";
    }

    @Override
    public String getGuard() {
        return null;
    }

    @Override
    public String getSQLRaw() {
        return null;
    }

    @Override
    public int compareTo(Formula f) {
        throw cause;
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public String performLateBinding(String sql, FormulaTableRegistry registry) throws FormulaException {
        return null;
    }

    @Override
    public boolean hasAttribute(int attribute) {
        return false;
    }

    private final RuntimeException cause;

    @Override
    public List<FormulaTableRegistry.TableIdentifier> getDependentTables(FormulaTableRegistry queryTableRegistry) {
        return new ArrayList<>();
    }

    @Override
    public boolean isDeterministic(FormulaContext formulaContext) {
        return false;
    }

    @Override
    public boolean isCustomIndexable(FormulaContext formulaContext) {
        return false;
    }

    @Override
    public boolean isFlexIndexable(FormulaContext formulaContext) {
        return false;
    }

    @Override
    public boolean isStale(FormulaContext formulaContext) {
        return false;
    }

    @Override
    public boolean hasAIPredictionFieldReference(FormulaContext formulaContext) {
        return false;
    }

    @Override
    public boolean isPostSaveIndexUpdated(FormulaContext formulaContext, FormulaDmlType dmlType) {
        return false;
    }

    public void validateMergeFieldsForFormulaType(FormulaContext formulaContext) {}

    @Override
    public void visitFormulaCommands(FormulaCommandVisitor visitor) {}

    @Override
    public TableAliasRegistry getTableAliasRegistry() {
        return null;
    }

    @Override
    public Class<?> getActualReturnType() {
        return null;
    }

    @Override
    public List<FormulaFieldReferenceInfo> getFieldPathIfDirectReferenceToAnotherField(FormulaContext formulaContext, boolean excludeZero, boolean allowDateValue, AtomicBoolean caseSafeIdUsed, String namespace) {
        return null;
    }

    @Override
    public FormulaDataType getDataType() {
        return null;
    }

    @Override
    public String toJavascript() {
        return "null";
    }

    @Override
    public String getJavascriptRaw() {
        return "null";
    }

    @Override
    public String getJavascriptGuard() {
        return null;
    }

    @Override
    public boolean couldJavascriptBeNull() {
        return true;
    }

    @Override
    public int hashCode() {
    	return 0;
    }
}
