package com.force.formula.commands;

import java.util.Locale;
import java.util.TimeZone;

import com.force.formula.Formula;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaEngineHooks;
import com.force.formula.FormulaEvaluationException;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.MockFormulaDataType;
import com.force.formula.MockLocalizerContext.MockLocalizer;
import com.force.formula.impl.BaseCustomizableParserTest;
import com.force.formula.impl.FormulaInfoFactory;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.sql.RuntimeSqlFormulaInfo;
import com.force.i18n.BaseLocalizer;
import com.force.i18n.HumanLanguage;
import com.force.i18n.LanguageProviderFactory;

/**
 * Test of the sfdc only format functions.  Also template parsing.
 * @author stamm
 * @since 0.2.9
 */
public class FunctionFormatTest extends BaseCustomizableParserTest {
    public FunctionFormatTest(String name) throws Exception {
        super(name);
    }
    
    public void assertFormula(String expectedResult, String expression) throws Exception {
        FormulaRuntimeContext context  = setupMockContext(MockFormulaDataType.TEXT);
        RuntimeSqlFormulaInfo formulaInfo = FormulaInfoFactory.create(context, expression, getTemplateProperties());
        Formula formula = formulaInfo.getFormula();
        String result = (String)formula.evaluate(context);
        assertEquals(expectedResult, result);
    }


    private FormulaEngineHooks oldHooks = null;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        oldHooks = FormulaEngine.getHooks();
        FormulaEngine.setHooks(getHooksOverrideLocalizer(oldHooks, GMT_LOCALIZER));
    }

    @Override
    protected void tearDown() throws Exception {
        FormulaEngine.setHooks(oldHooks);
        super.tearDown();
    }
    
    /**
     * Verify various formats for number formula type ( valid and invalid)
     */
    public void testFormatNumber() throws Exception {
        String expression = "This is a test of format {!format(100.0)}";
        assertFormula("This is a test of format 100", expression);

        expression = "This is a test of format {!format(10000.05)}";
        assertFormula("This is a test of format 10,000.05", expression);

        expression = "This is a test of format {!format(100.0,\"0.0#'%'\")}";
        assertFormula("This is a test of format 100.0%", expression);

        expression = "This is a test of format {!format(100.0,\"INVALID\")}";
        assertFormula("This is a test of format INVALID100", expression);
    }

    /**
     * Verify various formats for Date formula type ( valid and invalid)
     */
    public void testFormatDate() throws Exception {
        String expression = "This is a test of format {!format(DATE(2007,7,26))}";
        assertFormula("This is a test of format 7/26/2007", expression);

        expression = "This is a test of format {!format(DATE(2007,7,26),\"yyyy-MM-dd\")}";
        assertFormula("This is a test of format 2007-07-26", expression);


        expression = "This is a test of format {!format(DATE(2007,7,26),\"INVALID\")}";
        try {
            assertFormula(null, expression);
            fail();
        } catch (FormulaEvaluationException ex) {

        }
    }



    /**
     * Verify various formats for Text formula type ( valid and invalid)
     */
    public void testFormatText() throws Exception {
        String expression = "This is a test of format {!format()}";
        try {
            assertFormula(null, expression);
            fail();
        } catch (WrongNumberOfArgumentsException ex) {
        }

        expression = "This is a test of format {!format(\"USD\")}";
        assertFormula("This is a test of format USD", expression);

        expression = "This is a test of format {!format(\"USD\",\"USD\")}";
        assertFormula("This is a test of format USD", expression);

        expression = "This is a test of format {!format(\"Test {1}\",\"USD\",\"USD\")}";
        assertFormula("This is a test of format Test USD", expression);

        expression = "This is a test of format {!format(\"Test {0}\",\"USD\",\"USD\")}";
        assertFormula("This is a test of format Test USD", expression);

        expression = "This is a test of format {!format(\"Test {0}\",null,\"USD\")}";
        assertFormula("This is a test of format Test ", expression);

        expression = "This is a test of format {!format(\"Test {0}\",10.0,\"USD\")}";
        assertFormula("This is a test of format Test 10", expression);

        expression = "This is a test of format {!format(\"Test {0,number}\",10.0,\"USD\")}";
        assertFormula("This is a test of format Test 10", expression);

        expression = "This is a test of format {!format(\"Test {0,number}\",10.50,\"USD\")}";
        assertFormula("This is a test of format Test 10.5", expression);

        expression = "This is a test of format {!format(\"Test {0,date}\",DATETIMEVALUE(\"2016-02-29 13:15:10\"),\"USD\")}";
        assertFormula("This is a test of format Test Feb 29, 2016", expression);

        expression = "This is a test of format {!format(\"Test {0,date}\",DATE(2016,2,29),\"USD\")}";
        assertFormula("This is a test of format Test Feb 29, 2016", expression);

        expression = "This is a test of format {!format(\"Test {3}\",\"USD\",\"USD\")}";
        assertFormula("This is a test of format Test {3}", expression);
        
        HumanLanguage language = LanguageProviderFactory.get().getLanguage(Locale.GERMAN);
        BaseLocalizer localizer = new MockLocalizer(Locale.GERMANY, Locale.GERMANY, TimeZone.getTimeZone("PST"), language, null);
        FormulaEngine.setHooks(getHooksOverrideLocalizer(oldHooks, localizer));

        expression = "This is a test of format {!format(\"Test {0,number}\",10.50,\"USD\")}";
        assertFormula("This is a test of format Test 10,5", expression);

        expression = "This is a test of format {!format(\"Test {0,date}\",DATETIMEVALUE(\"2016-02-29 13:15:10\"),\"USD\")}";
        assertFormula("This is a test of format Test 29.02.2016", expression);

        expression = "This is a test of format {!format(\"Test {0,date}\",DATE(2016,2,29),\"USD\")}";
        // Yes, it's PST, so it's backwards
        assertFormula("This is a test of format Test 28.02.2016", expression);

    }
}
