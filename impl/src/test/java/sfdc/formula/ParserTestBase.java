package sfdc.formula;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.Assert;

import antlr.collections.AST;
import junit.framework.TestCase;
import sfdc.formula.impl.FormulaUtils;
import sfdc.formula.impl.FormulaWithSql;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public abstract class ParserTestBase extends TestCase {

    public ParserTestBase(String name, boolean establishContexts) {
        super(name);
    }

    protected void parseTest(String expression, String expected) throws FormulaException {
        AST ast;

        ast = parse(expression, false);
        Assert.assertEquals("Test failed for ANTLR2", expected, ast.toStringTree());

        ast = parse(expression, true);
        Assert.assertEquals("Test failed for ANTLR4", expected, ast.toStringTree());
    }

    protected AST parse(String expression, boolean useANTLR4) throws FormulaException {
        return useANTLR4 ? parseWithANTLR4(expression) : parseWithANTLR2(expression);
    }

    protected AST parseWithANTLR2(String expression) throws FormulaException {
        FormulaProperties properties = new FormulaProperties();
        return FormulaUtils.parseWithANTLR2(expression, properties).getFirstChild();
    }

    protected AST parseWithANTLR4(String expression) throws FormulaException {
        FormulaProperties properties = new FormulaProperties();
        return FormulaUtils.parseWithANTLR4(expression, properties).getFirstChild();
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
