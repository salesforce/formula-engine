package com.force.formula;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.*;

import com.force.formula.commands.FormulaJsTestUtils;
import com.force.formula.sql.*;
import com.force.i18n.BaseLocalizer;

import junit.framework.TestCase;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 0.1.0
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

    /**
     * Evaluate the formula using the java engine and the mock formula context in {@link #setupMockContext(FormulaDataType)}
     * @param formulaSource the source of the formula
     * @param columnType the column type expected 
     * @return the result of evaluating the formula
     * @throws Exception if there is an issue during parsing or evaluation
     */
    protected Object evaluate(String formulaSource, FormulaDataType columnType) throws Exception {
        FormulaRuntimeContext context = setupMockContext(columnType);
        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, formulaSource);
        Formula formula = formulaInfo.getFormula();
        return formula.evaluate(context);
    }
    
    /**
     * Evaluate the formula using the javascript test engine and the mock formula context in {@link #setupMockContext(FormulaDataType)}
     * @param formulaSource the source of the formula
     * @param columnType the column type expected 
     * @return the result of evaluating the formula
     * @throws Exception if there is an issue during parsing or evaluation
     */

    protected Object evaluateJavascript(String formulaSource, FormulaDataType columnType) throws Exception {
        FormulaRuntimeContext context = setupMockContext(columnType);
        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(MockFormulaType.JAVASCRIPT, context, formulaSource);
        Formula formula = formulaInfo.getFormula();
        return FormulaJsTestUtils.get().evaluateFormula(formula, columnType, context, null);
    }

    /**
     * Evaluate the formulaSource and make sure it fails during evaluation.  DataType is assumed to be text.
     * @param formulaSource the formula to parse and evaluate.
     * @param exceptionType the exception type expected
     * @param message the message asso
     * @throws Exception in case some other exception is thrown.
     */
    protected void evaluateFail(String formulaSource, Class<? extends Exception> exceptionType, String message) throws Exception {
    	evaluateFail(formulaSource, MockFormulaDataType.TEXT, exceptionType, message);
    }
    
    /**
     * Evaluate the formulaSource and make sure it fails during evaluation.
     * @param formulaSource the formula to parse and evaluate.
     * @param dataType the data type expected fro the result
     * @param exceptionType the exception type expected
     * @param message the message asso
     * @throws Exception in case some other exception is thrown.
     */
    protected void evaluateFail(String formulaSource, FormulaDataType dataType, Class<? extends Exception> exceptionType, String message) throws Exception {
    	try {
    		evaluate(formulaSource, MockFormulaDataType.TEXT);
    		fail("Should have gotten exception: " + exceptionType.getName());
    	} catch (Exception e) {
    		if (exceptionType.isInstance(e)) {
    			if (!e.getMessage().contains(message)) {
    	    		fail("Should have gotten message: " + message + ": but was " + e.getMessage());
    			}
    		} else {
        		fail("Got wrong exception: was " + e);
    		}
    	}
    }


    protected SQLPair getSqlPair(String formulaSource, FormulaDataType columnType) throws Exception {
        FormulaRuntimeContext context = setupMockContext(columnType);
        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, formulaSource);
        FormulaWithSql formula = (FormulaWithSql) formulaInfo.getFormula();
        return new SQLPair(formula.getSQLRaw(), formula.getGuard());
    }
    
    /**
     * Evaluate as a template, with the {!...} syntax
     * @param formulaSource the formula to evaluate
     * @return the result of evaluating the formula agianst a mock context.
     * @throws Exception
     */
    protected Object evaluateTemplate(String formulaSource) throws Exception {
        FormulaRuntimeContext context = setupMockContext(MockFormulaDataType.TEXT);
        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(context, formulaSource, getTemplateProperties());
        Formula formula = formulaInfo.getFormula();
        return formula.evaluate(context);
    }
    
    @Override
	protected void setUp() throws Exception {
		MockLocalizerContext.establishMock();
	}

    protected FormulaRuntimeContext setupMockContext(FormulaDataType columnType) {
    	return new MockFormulaContext(getFormulaType(), columnType);
    }

    /**
     * @return FormulaProperties suitable for testing templates (i.e. no sql, parseAsTemplate)
     */
    protected static FormulaProperties getTemplateProperties() {
    	return templateProperties;
    }

    private static final FormulaProperties templateProperties;
    static {
    	templateProperties = new FormulaProperties();
    	templateProperties.setGenerateSQL(false);
    	templateProperties.setAllowCycles(false);
    	templateProperties.setPolymorphicReturnType(true);
    	templateProperties.setParseAsTemplate(true);
    }
    

    /**
     * Parse the expression like a template (with {!...} syntax) and validate that it matches
     * @param expectedResult the result of the text
     * @param expression the expression to parse
     */
    public void assertTemplateFormula(String expectedResult, String expression) throws Exception {
        FormulaRuntimeContext context  = setupMockContext(MockFormulaDataType.TEXT);
        RuntimeSqlFormulaInfo formulaInfo = (RuntimeSqlFormulaInfo) FormulaEngine.getFactory().create(context, expression, getTemplateProperties());
        Formula formula = formulaInfo.getFormula();
        String result = (String)formula.evaluate(context);
        assertEquals(expectedResult, result);
    }
    
    
    protected static final BaseLocalizer GMT_LOCALIZER = new MockLocalizerContext.MockLocalizer();
    protected static final BaseLocalizer PST_LOCALIZER = new MockLocalizerContext.MockLocalizer(Locale.US, Locale.US, 
            TimeZone.getTimeZone("PST"), GMT_LOCALIZER.getUserLanguage(), MockLocalizerContext.getLabels());

    protected FormulaEngineHooks getHooksOverrideLocalizer(final FormulaEngineHooks hooks, final BaseLocalizer loc) {
    	Class<?>[] intfs = hooks.getClass().getInterfaces();
    	return (FormulaEngineHooks) Proxy.newProxyInstance(
    			hooks.getClass().getClassLoader(),
    			intfs,
    			(proxy, method, methodArgs) -> {
    				if ("getLocalizer".equals(method.getName())) {
    					return loc;
    				} else {
    					try {
    						return method.invoke(hooks, methodArgs);
    					} catch (InvocationTargetException ex) {
    						throw ex.getCause();
    					}
    				}
    			});
    }
}
