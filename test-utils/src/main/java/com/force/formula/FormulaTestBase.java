package com.force.formula;

import java.math.BigDecimal;
import java.util.Date;

import com.force.formula.sql.FormulaWithSql;

import junit.framework.TestCase;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public abstract class FormulaTestBase extends TestCase {

    public FormulaTestBase(String name) {
        super(name);
    }

    protected String fieldName(String mergeFieldName) {
        return mergeFieldName.replaceFirst("\\{\\!", "").replaceFirst("\\}", "");
    }

    protected BigDecimal evaluateBigDecimal(String formulaSource) throws Exception {
        return (BigDecimal)evaluate(formulaSource, MockFormulaDataType.DOUBLE);
    }

    protected Boolean evaluateBoolean(String formulaSource) throws Exception {
        return (Boolean)evaluate(formulaSource, MockFormulaDataType.BOOLEAN);
    }

    protected String evaluateString(String formulaSource) throws Exception {
        return (String)evaluate(formulaSource, MockFormulaDataType.TEXT);
    }

    protected Date evaluateDate(String formulaSource) throws Exception {
        return (Date)evaluate(formulaSource, MockFormulaDataType.DATEONLY);
    }

    protected FormulaDateTime evaluateDateTime(String formulaSource) throws Exception {
        return (FormulaDateTime)evaluate(formulaSource, MockFormulaDataType.DATETIME);
    }

    protected FormulaTime evaluateTime(String formulaSource) throws Exception {
        return (FormulaTime)evaluate(formulaSource, MockFormulaDataType.TIMEONLY);
    }
    
    protected MockFormulaType getFormulaType() {
        return MockFormulaType.DEFAULT;
    }

    protected Object evaluate(String formulaSource, FormulaDataType columnType) throws Exception {
        FormulaRuntimeContext context = setupMockContext(columnType);
        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, formulaSource);
        Formula formula = formulaInfo.getFormula();
        return formula.evaluate(context);
    }

    protected String getSqlGuard(String formulaSource, FormulaDataType columnType) throws Exception {
        FormulaRuntimeContext context = setupMockContext(columnType);
        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, formulaSource);
        Formula formula = formulaInfo.getFormula();
        return ((FormulaWithSql)formula).getGuard();
    }
    
    @Override
	protected void setUp() throws Exception {
		MockLocalizerContext.establishMock();
	}

    protected FormulaRuntimeContext setupMockContext(FormulaDataType columnType) {
    	return new MockFormulaContext(getFormulaType(), columnType);
    }
}
