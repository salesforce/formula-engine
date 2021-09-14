package com.force.formula.commands;


import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;

import com.force.formula.*;
import com.force.formula.util.FormulaDateUtil;
import com.force.i18n.BaseLocalizer;

/*
 * Created on Dec 9, 2004 TODO To change the template for this generated file go to Window - Preferences - Java - Code
 * Style - Code Templates
 */

/**
 * Test for all builtin functions that will be supported in 140
 *
 * @author dchasman
 * @since 140
 */
public class BuiltinFunctionsTest extends ParserTestBase {
    public BuiltinFunctionsTest(String name) {
        super(name);
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
    }

    /**
     * @return whether we're running in javascript mode.  Avoid calling this
     */
    protected boolean isJs() {
        return false;
    }
    
    /**
     * Whether we use "Decimal" or "Number" in javascript
     */
    protected boolean isHighPrecisionJs() {
        return false;
    }

    // equals on BigDecimal is almost never what we want as it compares the Java Object not the mathematical value.
    private void assertEquals(BigDecimal d1, BigDecimal d2) {
        if (d1 == null)
            assertNull(d2);
        if (d2 == null)
            assertNull(d1);
        assertTrue("d1="+d1+";d2="+d2, d1==null && d2==null || d1.compareTo(d2)==0);
    }

    // Operators
    public void testPLUS() throws Exception {

        assertEquals(false, OperatorAddOrSubtractFormulaCommand.notNull(null, null));
        assertEquals(false, OperatorAddOrSubtractFormulaCommand.notNull(null, ""));
        assertEquals(false, OperatorAddOrSubtractFormulaCommand.notNull("", null));
        assertEquals(true, OperatorAddOrSubtractFormulaCommand.notNull("", ""));
        assertEquals(false, OperatorAddOrSubtractFormulaCommand.notNull(new FormulaDateTime(null), ""));
        assertEquals(false, OperatorAddOrSubtractFormulaCommand.notNull("", new FormulaDateTime(null)));
        assertEquals(false, OperatorAddOrSubtractFormulaCommand.notNull( new FormulaDateTime(null), new FormulaDateTime(null) ));
        assertEquals(true, OperatorAddOrSubtractFormulaCommand.notNull( new FormulaDateTime(new Date()), new FormulaDateTime(new Date())));

        assertEquals(new BigDecimal("38"), evaluateBigDecimal("1+37"));
        assertEquals(null, evaluateBigDecimal("null + 37"));

        String TODAY = "date(2005, 4, 5)";
        String TOMORROW = "date(2005, 4, 6)";
        String YESTERDAY = "date(2005, 4, 4)";

        assertEquals(evaluateDate(TODAY), evaluateDate(TODAY + " + 0.7"));
        assertEquals(evaluateDate(TOMORROW), evaluateDate(TODAY + " + 1.9"));
        assertEquals(evaluateDate(TODAY), evaluateDate(TODAY + " + 0.5 + 0.5"));
        assertEquals(evaluateDate(TOMORROW), evaluateDate(TODAY + " + (0.5 + 0.5)"));

        assertEquals(evaluateDate(TODAY), evaluateDate(TODAY + " + (-0.7)"));
        assertEquals(evaluateDate(YESTERDAY), evaluateDate(TODAY + " + (-1.9)"));
        assertEquals(evaluateDate(TODAY), evaluateDate(TODAY + " + (-0.5) + (-0.5)"));
        assertEquals(evaluateDate(YESTERDAY), evaluateDate(TODAY + " + ((-0.5) + (-0.5))"));
    }

    public void testMINUS() throws Exception {
        parseTest("37-1", " ( - 37 1 )");
        assertEquals(new BigDecimal("36"), evaluateBigDecimal("37-1"));
        assertEquals(new BigDecimal("-36"), evaluateBigDecimal("1-37"));
        assertTrue(evaluateBigDecimal("4.0").compareTo(evaluateBigDecimal("date(2005, 4, 5) - date(2005, 4, 1)"))==0);
        assertTrue(evaluateBigDecimal("1.0").compareTo(evaluateBigDecimal("date(2005, 4, 10) - date(2005, 4, 9)"))==0);
        assertEquals(evaluateDate("date(2005, 4, 10)"),
            evaluateDate("date(2005, 4, 9) + (date(2005, 4, 10) - date(2005, 4, 9))"));
        if (!isJs()) { // TODO FIXME SLT
        assertEquals(true, evaluateDateTime("now() + 1").getTime() - (new Date().getTime() + 24 * 60 * 60 * 1000) < 10);
        assertEquals(true, evaluateDateTime("1 + now()").getTime() - (new Date().getTime() + 24 * 60 * 60 * 1000) < 10);
        }
        // assertEquals(evaluateBigDecimal("1"), evaluateBigDecimal("now() + 1 -
        // now()"));
        // assertEquals(evaluateBigDecimal("1"), evaluateBigDecimal("1 + now() -
        // now()"));
        
        assertEquals(new BigDecimal("7200000"), evaluateBigDecimal("timeValue(\"08:34:56.789\") - timeValue(\"06:34:56.789\")"));
        assertEquals(evaluateBigDecimal("3600000 * 22"), evaluateBigDecimal("timeValue(\"10:34:56.789\") - timeValue(\"12:34:56.789\")"));
        assertEquals(new BigDecimal("0"), evaluateBigDecimal("timeValue(\"12:34:56.789\") - timeValue(\"12:34:56.789\")"));
    }

    public void testUnaryPlus() throws Exception {
        parseTest("+2", " ( + 2 )");
        assertEquals(new BigDecimal("37"), evaluateBigDecimal("+37"));
    }

    public void testUnaryMinus() throws Exception {
        parseTest("-2", " ( - 2 )");
        assertEquals(new BigDecimal("-37"), evaluateBigDecimal("-37"));
    }

    public void testTIMES() throws Exception {
        parseTest("1*37", " ( * 1 37 )");
        assertEquals(new BigDecimal("37"), evaluateBigDecimal("1*37"));
        assertEquals(null, evaluateBigDecimal("null*37"));
    }

    public void testDIVIDE() throws Exception {
        parseTest("4/2", " ( / 4 2 )");
        assertTrue(new BigDecimal("2.0").compareTo(evaluateBigDecimal("4/2")) == 0);
        assertEquals(null, evaluateBigDecimal("4/null"));
        assertEquals(null , evaluateBigDecimal("null/4")); 
    }

    public void testPower() throws Exception {
        parseTest("2^3", " ( ^ 2 3 )");
        assertEquals(new BigDecimal("8"), evaluateBigDecimal("2^3"));
        assertEquals(new BigDecimal("1"), evaluateBigDecimal("0^0"));
        assertEquals(new BigDecimal("1"), evaluateBigDecimal("0.0^0.0"));
        try {
            BigDecimal val = evaluateBigDecimal("64^64");
            if (!isJs()) {
                // For now we ignores this, but it uses the built-in fast algorithm that loses precision.
                fail("Shouldn't allow crazy powers: " + val);
            }
        } catch (FormulaEvaluationException ex) {
            // Good
        } catch (NumberFormatException ex) {
            assertTrue(isJs());
        }
        
    }

    public void testEQUALS() throws Exception {
        parseTest("1 = 1", " ( = 1 1 )");
        assertEquals(Boolean.TRUE, evaluateBoolean("1=1"));
        assertEquals(Boolean.FALSE, evaluateBoolean("37=1"));
        assertEquals(null, evaluateBoolean("37=null"));
        assertEquals(Boolean.TRUE, evaluateBoolean("\"ab\"=\"ab\""));
        assertEquals(Boolean.FALSE, evaluateBoolean("\"ab\"=\"ba\""));
        assertEquals(Boolean.FALSE, evaluateBoolean("\"ab\"=\"\""));
        assertEquals(Boolean.FALSE, evaluateBoolean("null=\"ab\""));
    	assertEquals(Boolean.TRUE, evaluateBoolean("null=\"\""));
    }

    public void testNOTEQUALS() throws Exception {
        parseTest("1 <> 2", " ( <> 1 2 )");
        assertEquals(Boolean.FALSE, evaluateBoolean("1 <> 1"));
        assertEquals(Boolean.TRUE, evaluateBoolean("37<>1"));
        assertEquals(null, evaluateBoolean("37<>null"));
        assertEquals(Boolean.FALSE, evaluateBoolean("\"ab\"<>\"ab\""));
        assertEquals(Boolean.TRUE, evaluateBoolean("\"ab\"<>\"ba\""));
        assertEquals(Boolean.TRUE, evaluateBoolean("\"ab\"<>\"\""));
        assertEquals(Boolean.TRUE, evaluateBoolean("null<>\"ab\""));
    	assertEquals(Boolean.FALSE, evaluateBoolean("null<>\"\""));
    }

    public void testLT() throws Exception {
        parseTest("1 < 2", " ( < 1 2 )");
        assertEquals(Boolean.TRUE, evaluateBoolean("1<37"));
        assertEquals(Boolean.FALSE, evaluateBoolean("37<37"));
        assertEquals(null, evaluateBoolean("37<null"));
    }

    public void testLE() throws Exception {
        parseTest("1 <= 2", " ( <= 1 2 )");
        assertEquals(Boolean.TRUE, evaluateBoolean("37<=37"));
        assertEquals(Boolean.FALSE, evaluateBoolean("38<=37"));
        assertEquals(null, evaluateBoolean("null<=37"));
        assertEquals(null, evaluateBoolean("null<=date(2015, 3, 20)"));
        assertEquals(Boolean.TRUE, evaluateBoolean("date(2014, 3, 20)<=date(2015, 3, 20)"));
        assertEquals(Boolean.TRUE, evaluateBoolean("date(2015, 3, 20)<=date(2015, 3, 20)"));
    }

    public void testGT() throws Exception {
        parseTest("1 > 2", " ( > 1 2 )");
        assertEquals(Boolean.TRUE, evaluateBoolean("38>37"));
        assertEquals(Boolean.FALSE, evaluateBoolean("37>37"));
    }

    public void testGE() throws Exception {
        parseTest("1 >= 2", " ( >= 1 2 )");
        assertEquals(Boolean.TRUE, evaluateBoolean("37>=37"));
        assertEquals(Boolean.FALSE, evaluateBoolean("36>=37"));
    }

    // Number Functions
    public void testABS() throws Exception {
        parseTest("abs(3.5)", " ( abs 3.5 )");
        assertEquals(new BigDecimal("3.5"), evaluateBigDecimal("abs(3.5)"));
        parseTest("abs(-3.5)", " ( abs ( - 3.5 ) )");
        assertEquals(new BigDecimal("3.5"), evaluateBigDecimal("abs(-3.5)"));
    }

    public void testSQRT() throws Exception {
        parseTest("sqrt(16)", " ( sqrt 16 )");
        assertEquals(new BigDecimal("4"), evaluateBigDecimal("sqrt(16)"));
    }

    public void testEXP() throws Exception {
        parseTest("exp(2)", " ( exp 2 )");
        assertEquals(new BigDecimal("36"), evaluateBigDecimal("exp(ln(36))"));
    }

    public void testLN() throws Exception {
        parseTest("ln(32)", " ( ln 32 )");
        assertEquals(new BigDecimal("3"), evaluateBigDecimal("ln(exp(3))"));
    }

    public void testLOG() throws Exception {
        parseTest("log(100)", " ( log 100 )");
        assertEquals(new BigDecimal("2"), evaluateBigDecimal("log(100)"));
        assertEquals(new BigDecimal("2"), evaluateBigDecimal("log(abs(100))"));
        assertEquals(new BigDecimal("2"), evaluateBigDecimal("abs(log(100))"));
        assertEquals(new BigDecimal("2"), evaluateBigDecimal("abs(log(sqrt(9000+1000)))"));
    }

    public void testMOD() throws Exception {
        parseTest("mod(100, 10)", " ( mod 100 10 )");
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("mod(100, 10)"));
        assertEquals(new BigDecimal("-1"), evaluateBigDecimal("mod(-101, 10)"));
        assertEquals(new BigDecimal("0.5"), evaluateBigDecimal("mod(100.5, -10)"));
    }

    public void testMAX() throws Exception {
        parseTest("max(100, 10)", " ( max 100 10 )");
        parseTest("max(100, 10, 34)", " ( max 100 10 34 )");
        assertEquals(new BigDecimal("100"), evaluateBigDecimal("max(100, 10)"));
        assertEquals(new BigDecimal("500"), evaluateBigDecimal("max(100*5, -10)"));
        assertEquals(BigDecimal.TEN, evaluateBigDecimal("max(-101, 10)"));
        assertEquals(new BigDecimal("100.5"), evaluateBigDecimal("max(100.5, -10)"));
        assertEquals(new BigDecimal("1000"), evaluateBigDecimal("max(100, -10, 1000)"));
        assertEquals(null, evaluateBigDecimal("max(100.5, null, -10)"));
    }

    public void testMIN() throws Exception {
        parseTest("min(100, 10)", " ( min 100 10 )");
        parseTest("min(100, 10, 34)", " ( min 100 10 34 )");
        assertEquals(BigDecimal.TEN, evaluateBigDecimal("min(100, 10)"));
        assertEquals(new BigDecimal("-101"), evaluateBigDecimal("min(-101, 10)"));
        assertEquals(new BigDecimal("-100.5"), evaluateBigDecimal("min(-100.5, -10)"));
        assertEquals(BigDecimal.TEN, evaluateBigDecimal("min(100, 10, 1000)"));
        assertEquals(null, evaluateBigDecimal("min(100.5, null, -10)"));
    }

    public void testROUND() throws Exception {
        parseTest("round(1.3, 1)", " ( round 1.3 1 )");
        assertEquals(new BigDecimal("1.1"), evaluateBigDecimal("round(1.13, 1)"));
        assertEquals(new BigDecimal("-1.1"), evaluateBigDecimal("round(-1.13, 1)"));
        assertEquals(new BigDecimal("1.2"), evaluateBigDecimal("round(1.17, 1)"));
        assertEquals(new BigDecimal("-1.2"), evaluateBigDecimal("round(-1.17, 1)")); 
        assertEquals(BigDecimal.ONE, evaluateBigDecimal("round(1.13, 0)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("round(0.00, 0)"));
        assertEquals(new BigDecimal("0.0001"), evaluateBigDecimal("round(0.0001, 4)"));
        assertEquals(new BigDecimal("0.001"), evaluateBigDecimal("round(0.0006, 3)"));
        assertEquals(new BigDecimal("-0.001"), evaluateBigDecimal("round(-0.0006, 3)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("round(0, 3)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("round(0, -2)"));
        assertEquals(new BigDecimal("-123500"), evaluateBigDecimal("round(-123456.3335, -2)"));
    }

    public void testCEILING() throws Exception {
        parseTest("ceiling(1.3)", " ( ceiling 1.3 )");
        assertEquals(new BigDecimal("2"), evaluateBigDecimal("ceiling(1.3)"));
        assertEquals(new BigDecimal("2"), evaluateBigDecimal("ceiling(1.7)"));
        assertEquals(new BigDecimal("-2"), evaluateBigDecimal("ceiling(-1.3)"));
        assertEquals(new BigDecimal("-2"), evaluateBigDecimal("ceiling(-1.7)"));
        assertEquals(BigDecimal.ONE, evaluateBigDecimal("ceiling(0.07)"));
        assertEquals(new BigDecimal("-1"), evaluateBigDecimal("ceiling(-0.07)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("ceiling(0)"));
        assertEquals(null, evaluateBigDecimal("ceiling(null)"));
        assertEquals(new BigDecimal(6), evaluateBigDecimal("ceiling(6/11*11)"));
    }

    public void testFLOOR() throws Exception {
        parseTest("floor(1.3)", " ( floor 1.3 )");
        assertEquals(BigDecimal.ONE, evaluateBigDecimal("floor(1.3)"));
        assertEquals(BigDecimal.ONE, evaluateBigDecimal("floor(1.7)"));
        assertEquals(new BigDecimal("-1"), evaluateBigDecimal("floor(-1.3)"));
        assertEquals(new BigDecimal("-1"), evaluateBigDecimal("floor(-1.7)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("floor(0.07)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("floor(-0.07)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("floor(0)"));
        assertEquals(null, evaluateBigDecimal("floor(null)"));
        assertEquals(new BigDecimal(6), evaluateBigDecimal("floor(6/11*11)"));
    }


    public void testMCEILING() throws Exception {
        parseTest("mceiling(1.3)", " ( mceiling 1.3 )");
        assertEquals(new BigDecimal("2"), evaluateBigDecimal("mceiling(1.3)"));
        assertEquals(new BigDecimal("2"), evaluateBigDecimal("mceiling(1.7)"));
        assertEquals(new BigDecimal("-1"), evaluateBigDecimal("mceiling(-1.3)"));
        assertEquals(new BigDecimal("-1"), evaluateBigDecimal("mceiling(-1.7)"));
        assertEquals(BigDecimal.ONE, evaluateBigDecimal("mceiling(0.07)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("mceiling(-0.07)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("mceiling(0)"));
        assertEquals(null, evaluateBigDecimal("mceiling(null)"));
        assertEquals(new BigDecimal(6), evaluateBigDecimal("mceiling(6/11*11)"));
    }

    public void testMFLOOR() throws Exception {
        parseTest("mfloor(1.3)", " ( mfloor 1.3 )");
        assertEquals(BigDecimal.ONE, evaluateBigDecimal("mfloor(1.3)"));
        assertEquals(BigDecimal.ONE, evaluateBigDecimal("mfloor(1.7)"));
        assertEquals(new BigDecimal("-2"), evaluateBigDecimal("mfloor(-1.3)"));
        assertEquals(new BigDecimal("-2"), evaluateBigDecimal("mfloor(-1.7)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("mfloor(0.07)"));
        assertEquals(new BigDecimal("-1"), evaluateBigDecimal("mfloor(-0.07)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("mfloor(0)"));
        assertEquals(null, evaluateBigDecimal("mfloor(null)"));
        assertEquals(new BigDecimal(6), evaluateBigDecimal("mfloor(6/11*11)"));
    }
    
    // Logic Functions
    public void testNOT() throws Exception {
        parseTest("not true", " ( not true )");
        assertEquals(Boolean.TRUE, evaluateBoolean("not false"));
        assertEquals(Boolean.FALSE, evaluateBoolean("not(1=1)"));
        assertEquals(null, evaluateBoolean("not(null)"));
    }

    public void testAND() throws Exception {
        parseTest("and(true, true)", " ( and true true )");
        parseTest("and(true, true, true)", " ( and true true true )");
        assertEquals(Boolean.TRUE, evaluateBoolean("and(true, true)"));
        assertEquals(Boolean.FALSE, evaluateBoolean("and(1=1, 1=0)"));
        assertEquals(Boolean.TRUE, evaluateBoolean("and(1=1, 3<37, 8<>12)"));
        assertEquals(Boolean.FALSE, evaluateBoolean("and(1=1, 3<37, null)"));
        assertEquals(Boolean.FALSE, evaluateBoolean("and(1=1, 3>37, null)"));
        assertEquals(Boolean.FALSE, evaluateBoolean("and(1<>1, 1/0=0)"));
    }

    public void testOR() throws Exception {
        parseTest("or(true, true)", " ( or true true )");
        parseTest("or(true, true, true)", " ( or true true true )");
        assertEquals(Boolean.TRUE, evaluateBoolean("or(true, false)"));
        assertEquals(Boolean.FALSE, evaluateBoolean("or(1=37, 1=0)"));
        assertEquals(Boolean.FALSE, evaluateBoolean("or(1=0, 3>37, 8=12)"));
        assertEquals(Boolean.FALSE, evaluateBoolean("or(1=0, 3>37, null)"));
        assertEquals(Boolean.TRUE, evaluateBoolean("or(1=0, 3<37, null)"));
        assertEquals(Boolean.TRUE, evaluateBoolean("or(1=1, 1/0=0)"));
    }

    public void testBooleanConstants() throws Exception {
        parseTest("true", " true");
        parseTest("false", " false");
        assertEquals(Boolean.TRUE, evaluateBoolean("true"));
        assertEquals(Boolean.FALSE, evaluateBoolean("false"));
    }

    // Conditional Logic Functions
    public void testIF() throws Exception {
        parseTest("if(true, 1, 0)", " ( if true 1 0 )");
        assertEquals(BigDecimal.ONE, evaluateBigDecimal("if(1=1, 1, 0)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("if(1=37, 1, 0)"));
        assertEquals(Boolean.FALSE, evaluateBoolean("5=if(1=37, 1, 0)"));
        assertEquals(BigDecimal.ONE, evaluateBigDecimal("if(true, 1, null)"));
        assertEquals(new BigDecimal("37"), evaluateBigDecimal("if(false, 1/0, 37)"));
        assertEquals(new BigDecimal("37"), evaluateBigDecimal("if(true, 37, 1/0)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("if(1=null, 1, 0)"));
    }
    
    public void testCASE() throws Exception {
        parseTest("case(10, 9, 0, 10, 1, 11, 0, 2)", " ( case 10 9 0 10 1 11 0 2 )");
        assertEquals(BigDecimal.ONE, evaluateBigDecimal("case(10, 9, 0, 10, 1, 11, 0, 2)"));
        assertEquals(new BigDecimal("2"), evaluateBigDecimal("case(37, 9, 0, 10, 1, 11, 0, 2)"));
        assertEquals(Boolean.TRUE, evaluateBoolean("1=case(10, 9, 0, 10, 1, 11, 0, 2)"));
        assertEquals("chico", evaluateString("case(10, 9, \"groucho\", 10, \"chico\", 11, \"harpo\", \"zeppo\")"));
        assertEquals(new BigDecimal("2"), evaluateBigDecimal("case(null, 9, 0, 10, 1, 11, 0, 2)"));
        assertEquals(new BigDecimal("2"), evaluateBigDecimal("case(35, null, 0, 10, 1, 11, 0, 2)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("case(9, 9, 0, 10, null, 2)"));
        assertEquals(new BigDecimal(400), evaluateBigDecimal("ABS(CASE(DATE(2004,12,5),DATE(2004,12,4),200,DATE(2004,12,4),300,400))"));
        assertEquals(new BigDecimal(400), evaluateBigDecimal("ABS(CASE(DATE(2004,12,5),NULL,200,DATE(2004,12,4),300,400))"));
        assertEquals(new BigDecimal(400), evaluateBigDecimal("ABS(CASE(NULL,NULL,200,DATE(2004,12,4),300,400))"));
    }
    

    // Null Handling Functions
    public void testNullConstant() throws Exception {
        parseTest("null", " null");
        assertEquals(null, evaluateBoolean("null"));
        assertEquals(null, evaluateBigDecimal("3+null"));
    }

    public void testIsNull() throws Exception {
        parseTest("isnull(5)", " ( isnull 5 )");
        assertEquals(Boolean.TRUE, evaluateBoolean("isnull(null)"));
        assertEquals(Boolean.FALSE, evaluateBoolean("isnull(5)"));
        assertEquals(Boolean.TRUE, evaluateBoolean("isnull(if(true, null, 37))"));
        assertEquals(Boolean.FALSE, evaluateBoolean("isnull(\"abc\")"));
        assertEquals(Boolean.FALSE, evaluateBoolean("isnull(\"\")"));
        assertEquals(Boolean.FALSE, evaluateBoolean("isnull(if(true, null, \"abc\"))"));
    }

    public void testNVL() throws Exception {
        parseTest("nullvalue(1, 1)", " ( nullvalue 1 1 )");
        assertEquals(new BigDecimal("37"), evaluateBigDecimal("nullvalue(37, 6)"));
        assertEquals(new BigDecimal("6"), evaluateBigDecimal("nullvalue(null, 6)"));
        assertEquals(Boolean.FALSE, evaluateBoolean("5 = nullvalue(null, 6)"));
        assertEquals("abc", evaluateString("nullvalue(\"abc\", \"123\")"));
        assertEquals(null, evaluateString("nullvalue(\"\", \"123\")"));
        assertEquals(null, evaluateString("nullvalue(null, \"123\")"));
    }

    // Text Functions
    public void testCONCAT() throws Exception {
        parseTest("\"abc\" & \"def\"", " ( & \"abc\" \"def\" )");
        assertEquals("abcdef", evaluateString("\"abc\" & \"def\""));
        assertEquals("abc", evaluateString("\"abc\" & null"));
        assertEquals("abc", evaluateString("null & \"abc\""));
    }
    
    public void testTEXT() throws Exception {
        assertEquals("123456", evaluateString("text(123456)"));

        assertEquals("1968-12-20", evaluateString("text(date(1968, 12, 20))"));
    }


    public void testVALUE() throws Exception {
        parseTest("value(\"123456\")", " ( value \"123456\" )");
        assertEquals(123456, evaluateBigDecimal("value(\"123456\")").intValue());
    }

    public void testBEGINS() throws Exception {
        parseTest("begins(\"123456\", \"123\")", " ( begins \"123456\" \"123\" )");
        assertEquals(Boolean.TRUE, evaluateBoolean("begins(\"123456\", \"123\")"));
        assertEquals(Boolean.FALSE, evaluateBoolean("begins(\"123456\", \"23\")"));
        assertEquals(Boolean.TRUE, evaluateBoolean("begins(\"123456\", \"\")"));
        assertEquals(Boolean.TRUE, evaluateBoolean("begins(\"123456\", null)"));
        assertEquals(Boolean.TRUE, evaluateBoolean("begins(null, null)"));
    	assertEquals(null, evaluateBoolean("begins(\"\", \"123\")"));
        assertEquals(null, evaluateBoolean("begins(null, \"123\")"));
    }

    public void testCONTAINS() throws Exception {
        parseTest("contains(\"123456\", \"123\")", " ( contains \"123456\" \"123\" )");
        assertEquals(Boolean.TRUE, evaluateBoolean("contains(\"123456\", \"23\")"));
        assertEquals(Boolean.TRUE, evaluateBoolean("contains(\"123456\", \"123\")"));
        assertEquals(Boolean.FALSE, evaluateBoolean("contains(\"123456\", \"235\")"));
        assertEquals(Boolean.TRUE, evaluateBoolean("contains(\"123456\", \"\")"));
        assertEquals(Boolean.TRUE, evaluateBoolean("contains(\"123456\", null)"));
        assertEquals(Boolean.TRUE, evaluateBoolean("contains(null, null)"));
    	assertEquals(null, evaluateBoolean("contains(null, \"235\")"));
    	assertEquals(null, evaluateBoolean("contains(\"\", \"235\")"));
    }

    public void testLEN() throws Exception {
        parseTest("len(\"123456\")", " ( len \"123456\" )");
        assertEquals(new BigDecimal("6"), evaluateBigDecimal("len(\"123456\")"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("len(null)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("len(\"\")"));
    }

    public void testLEFT() throws Exception {
        parseTest("left(\"123456\", 3)", " ( left \"123456\" 3 )");
        assertEquals("123", evaluateString("left(\"123456\", 3)"));
        assertEquals("123456", evaluateString("left(\"123456\", 37)"));
        assertEquals(null, evaluateString("left(\"\", 37)"));
        assertEquals(null, evaluateString("left(\"123456\", 0)"));
        assertEquals(null, evaluateString("left(\"123456\", -2)"));
        assertEquals(null, evaluateString("left(null, 3)"));
    }

    public void testMID() throws Exception {
        parseTest("mid(\"123456\", 3, 2)", " ( mid \"123456\" 3 2 )");
        assertEquals("45", evaluateString("mid(\"123456\", 4, 2)"));
        assertEquals("456", evaluateString("mid(\"123456\", 4, 37)"));
        assertEquals("12", evaluateString("mid(\"123456\", -4, 2)"));
        assertEquals("12", evaluateString("mid(\"123456\", 0, 2)"));
        assertEquals(null, evaluateString("mid(\"123456\", 2, -2)"));
        assertEquals(null, evaluateString("mid(\"123456\", 2, 0)"));
        assertEquals(null, evaluateString("mid(\"\", 4, 2)"));
        assertEquals(null, evaluateString("mid(\"123456\", 4, null)"));
        assertEquals(null, evaluateString("mid(\"123456\", 37, 2)"));
    	assertEquals(null, evaluateString("mid(\"123456\", null, 2)"));
    }

    public void testRIGHT() throws Exception {
        parseTest("right(\"123456\", 3)", " ( right \"123456\" 3 )");
        assertEquals("456", evaluateString("right(\"123456\", 3)"));
        assertEquals("123456", evaluateString("right(\"123456\", 37)"));
        assertEquals(null, evaluateString("right(\"\", 37)"));
        assertEquals(null, evaluateString("right(\"123456\", 0)"));
        assertEquals(null, evaluateString("right(\"123456\", -2)"));
        assertEquals(null, evaluateString("right(\"123456\", 0)"));
        assertEquals(null, evaluateString("right(null, 3)"));
    }

    public void testIMAGE() throws Exception {
        parseTest("image(\"http://www.gifs.com/images/fathead.gif\", \"fathead\")",
            " ( image \"http://www.gifs.com/images/fathead.gif\" \"fathead\" )");
        parseTest("image(\"http://www.gifs.com/images/fathead.gif\", \"fathead\", 20, 30)",
            " ( image \"http://www.gifs.com/images/fathead.gif\" \"fathead\" 20 30 )");
    }

    public void testTRIM() throws Exception {
        parseTest("trim(\"123456\")", " ( trim \"123456\" )");
        assertEquals("1234", evaluateString("trim(\"  1234  \")"));
        assertEquals(null, evaluateString("trim(\"    \")"));
        assertEquals(null, evaluateString("trim(\"\")"));
        assertEquals(null, evaluateString("trim(null)"));
    }

    public void testISPICKLISTVALUE() throws Exception {
        parseTest("isPickVal(a, \"Mothra\")", " ( isPickVal a \"Mothra\" )");
    }

    public void testUPPER() throws Exception {
        assertEquals(null, evaluateString("UPPER(null)"));
        assertEquals("STRING", evaluateString("UPPER(\"string\")"));
        assertEquals("STRING", evaluateString("UPPER(\"STRING\")"));
        assertEquals("STRING ", evaluateString("UPPER(\"StRiNg \")"));
        assertEquals("GROSSE", evaluateString("UPPER(\"gro\u00DFe\")"));
        assertEquals("IDEMPOTENT", evaluateString("UPPER(\"\u0131dempotent\")"));  // Dotless lower i
        assertEquals("\u00C5NGSTROM", evaluateString("UPPER(\"\u00E5ngstrom\")"));
        assertEquals("\u0130DEMPOTENT", evaluateString("UPPER(\"idempotent\",\"tr\")"));  // Dotted capital i
        assertEquals("\u0130DEMPOTENT", evaluateString("UPPER(\"idempotent\",\"tr_TR\")"));  // Dotted capital i
    }

    public void testLOWER() throws Exception {
        assertEquals(null, evaluateString("LOWER(null)"));
        assertEquals("string", evaluateString("LOWER(\"string\")"));
        assertEquals("string", evaluateString("LOWER(\"STRING\")"));
        assertEquals("string ", evaluateString("LOWER(\"StRiNg \")"));
        assertEquals("gro\u00DFe", evaluateString("LOWER(\"gro\u00DFe\")"));
        // Note: this is broken with Postgres.
        //assertEquals("idempotent", evaluateString("LOWER(\"\u0130DEMPOTENT\")"));  // From capital i
        assertEquals("\u00E5ngstrom", evaluateString("LOWER(\"\u00C5NGSTROM\")"));  // Dotless lower i
        assertEquals("idempotent", evaluateString("LOWER(\"\u0130DEMPOTENT\",\"tr\")"));  // From capital i
        assertEquals("\u0131dempotent", evaluateString("LOWER(\"IDEMPOTENT\",\"tr\")"));  // Dotless lower i
        assertEquals("\u0131dempotent", evaluateString("LOWER(\"IDEMPOTENT\",\"tr_TR\")"));  // Dotless lower i i
        Assert.assertNotEquals("Didn't handle turkish correctly", "idempotent", evaluateString("LOWER(\"IDEMPOTENT\",\"tr_TR\")"));  // Dotless lower i i
    }

    // Time & Date Functions
    public void testNOW() throws Exception {
        parseTest("now()", " now");
        BigDecimal nowMinusNow = evaluateBigDecimal("now() - now()");
        if (isJs()) {
            // Note: in javascript "now" is evaluated multiple times instead of caching like in oracle & java.  So this could be flapping
            assertTrue(nowMinusNow.abs().compareTo(new BigDecimal("2000")) < 0);
        } else {
            assertTrue(nowMinusNow.compareTo(new BigDecimal("0")) == 0);
        }
    }

    public void testTIMENOW() throws Exception {
        parseTest("timenow()", " timenow");
        BigDecimal timenowMinusTimenow = evaluateBigDecimal("timenow() - timenow()");
        if (isJs()) {
            assertTrue("value is " + timenowMinusTimenow.abs() + " & comp value is " + timenowMinusTimenow.abs().compareTo(new BigDecimal("86400000")), timenowMinusTimenow.abs().compareTo(new BigDecimal("86400000")) < 0);
        } else {
            assertTrue(timenowMinusTimenow.compareTo(new BigDecimal("0")) == 0);
        }
    }

    public void testTODAY() throws Exception {
        parseTest("today()", " today");
        Date today = FormulaDateUtil.todayGmt();
        assertEquals(today, evaluateDate("today()"));
    }

    public void testSqlTIMEVALUE() throws Exception {
        assertEquals(null, getSqlPair("timeValue(\"12:34:56.789\")", MockFormulaDataType.TIMEONLY).guard);
    }

    public void testInvalidSqlTIMEVALUE() throws Exception {
        assertEquals("0=0", getSqlPair("timeValue(\"12:34:56.789Z\")", MockFormulaDataType.TIMEONLY).guard);
    }
    
    public void testNoOpTIMEVALUE() throws Exception {
    	// , -TimeZone.getDefault().getRawOffset()?
        assertEquals(new FormulaTime.TimeWrapper(LocalTime.of(12, 34, 56, 789 * 1000000)), evaluateTime("timeValue(timeValue(\"12:34:56.789\"))"));
    }

    public void testInvalidTIMEVALUE() throws Exception {
        if (isJs()) return; // TODO SLT: Javascript parser doesn't handle time values correctly
        try {
            evaluateTime("timeValue(\"12:34:56.789Z\")");
            fail("Expected FormulaDateException");
        } catch (RuntimeException e) {
            assertTrue("Expected FormulaDateException, but got " + e, e.getCause() instanceof FormulaDateException);
        }
    }

    public void testTIMEVALUE() throws Exception {
    	// , -TimeZone.getDefault().getRawOffset()?
        assertEquals(new FormulaTime.TimeWrapper(LocalTime.of(12, 34, 56, 789 * 1000000)), evaluateTime("timeValue(\"12:34:56.789\")"));
        assertEquals(new FormulaTime.TimeWrapper(LocalTime.of(14, 34, 56, 789 * 1000000)), evaluateTime("timeValue(\"12:34:56.789\")+93600000"));
    }

    public void testDATEVALUE() throws Exception {
        Date today = FormulaDateUtil.todayGmt();
        assertEquals(today, evaluateDate("dateValue(now())"));

        assertEquals(evaluateDate("date(2005,1,3)"), evaluateDate("dateValue(dateTimeValue(\"2004-12-31 11:32:10\")+3.00)"));
        assertEquals(evaluateDate("date(2005,1,4)"), evaluateDate("dateValue(dateTimeValue(\"2004-12-31 11:32:10\")+3.60)"));
        assertEquals(evaluateDate("date(2004,3,4)"), evaluateDate("dateValue(dateTimeValue(\"2004-02-28 10:34:00\")+4.60)"));

        if (!isJs()) {
            // DateValue uses the user's timezone.  This tests with PST.  Setting the timezone in the browser is harder.
            FormulaEngine.setHooks(getHooksOverrideLocalizer(oldHooks, PST_LOCALIZER));
            assertEquals(evaluateDate("date(2005,1,3)"), evaluateDate("dateValue(dateTimeValue(\"2004-12-31 11:32:10\")+3.00)"));
            assertEquals(evaluateDate("date(2005,1,3)"), evaluateDate("dateValue(dateTimeValue(\"2004-12-31 11:32:10\")+3.60)"));
            assertEquals(evaluateDate("date(2004,3,3)"), evaluateDate("dateValue(dateTimeValue(\"2004-02-28 10:34:00\")+4.60)"));
        }

    }

    public void testDATE() throws Exception {
        parseTest("date(1958, 1, 15)", " ( date 1958 1 15 )");
        Calendar c = Calendar.getInstance(BaseLocalizer.GMT_TZ);
        c.clear();
        c.set(1958, 0, 15);
        assertEquals(c.getTime(), evaluateDate("date(1958, 1, 15)"));
        if (!isJs()) { // null vs 0 in js is not worth figuring out
            assertEquals(null, evaluateDate("date(1958, null, 15)"));
        }
    }

    public void testDAY() throws Exception {
        parseTest("day(today())", " ( day today )");
        assertEquals(new BigDecimal("15"), evaluateBigDecimal("day(date(1958, 1, 15))"));
        assertEquals(null, evaluateBigDecimal("day(null)"));
    }

    public void testMONTH() throws Exception {
        parseTest("month(today())", " ( month today )");
        assertEquals(new BigDecimal("1"), evaluateBigDecimal("month(date(1958, 1, 15))"));
        assertEquals(null, evaluateBigDecimal("month(null)"));
    }

    public void testYEAR() throws Exception {
        parseTest("year(today())", " ( year today )");
        assertEquals(new BigDecimal("1958"), evaluateBigDecimal("year(date(1958, 1, 15))"));
        assertEquals(null, evaluateBigDecimal("year(null)"));
    }

    // Composite calls
    public void testCompositeCall() throws Exception {
        assertEquals(12345600, evaluateBigDecimal("100 * value(\"123456\")").intValue());
        assertEquals(12345600, evaluateBigDecimal("value(\"123456\") * 100").intValue());
        assertEquals(6172800, evaluateBigDecimal("(value(\"123456\") * 100) / 2").intValue());
        assertEquals(19, evaluateBigDecimal("value(\"10\") * 2 - 1").intValue());
        assertEquals("19", evaluateString("text(value(\"10\") * 2 - 1)"));
    }

    public void testFIND() throws Exception {
        assertEquals(8, evaluateBigDecimal("find(\"asman\", \"Doug Chasman\")").intValue());
        assertEquals(23, evaluateBigDecimal("find(\"w\", \"Something wicked this way comes\", 15)").intValue());
        assertEquals(0, evaluateBigDecimal("find(\"apple\", \"Doug Chasman\")").intValue());
    }

    public void testRPAD() throws Exception {
        assertEquals(null, evaluateString("rpad(\"string\",0)"));
        assertEquals("s", evaluateString("rpad(\"string\",1)"));
        assertEquals("strin", evaluateString("rpad(\"string\",5)"));
        assertEquals("string", evaluateString("rpad(\"string\",6)"));
        assertEquals("string ", evaluateString("rpad(\"string\",7)"));
        assertEquals("string  ", evaluateString("rpad(\"string\",8)"));
        assertEquals("string   ", evaluateString("rpad(\"string\",9)"));
        assertEquals("string    ", evaluateString("rpad(\"string\",10)"));

        assertEquals(null, evaluateString("rpad(\"string\",0,\"x\")"));
        assertEquals("s", evaluateString("rpad(\"string\",1,\"x\")"));
        assertEquals("strin", evaluateString("rpad(\"string\",5,\"x\")"));
        assertEquals("string", evaluateString("rpad(\"string\",6,\"x\")"));
        assertEquals("stringx", evaluateString("rpad(\"string\",7,\"x\")"));
        assertEquals("stringxx", evaluateString("rpad(\"string\",8,\"x\")"));


        assertEquals(null, evaluateString("rpad(\"string\",0,\",.;\")"));
        assertEquals("s", evaluateString("rpad(\"string\",1,\",.;\")"));
        assertEquals("strin", evaluateString("rpad(\"string\",5,\",.;\")"));
        assertEquals("string", evaluateString("rpad(\"string\",6,\",.;\")"));
        assertEquals("string,", evaluateString("rpad(\"string\",7,\",.;\")"));
        assertEquals("string,.", evaluateString("rpad(\"string\",8,\",.;\")"));
        assertEquals("string,.;", evaluateString("rpad(\"string\",9,\",.;\")"));
        assertEquals("string,.;,", evaluateString("rpad(\"string\",10,\",.;\")"));
        assertEquals("string,.;,.;,.;", evaluateString("rpad(\"string\",15,\",.;\")"));
    }

    public void testLPAD() throws Exception {
        assertEquals(null, evaluateString("lpad(\"string\",0)"));
        assertEquals("s", evaluateString("lpad(\"string\",1)"));
        assertEquals("strin", evaluateString("lpad(\"string\",5)"));
        assertEquals("string", evaluateString("lpad(\"string\",6)"));
        assertEquals(" string", evaluateString("lpad(\"string\",7)"));
        assertEquals("  string", evaluateString("lpad(\"string\",8)"));
        assertEquals("   string", evaluateString("lpad(\"string\",9)"));
        assertEquals("    string", evaluateString("lpad(\"string\",10)"));

        assertEquals(null, evaluateString("lpad(\"string\",0,\"x\")"));
        assertEquals("s", evaluateString("lpad(\"string\",1,\"x\")"));
        assertEquals("strin", evaluateString("lpad(\"string\",5,\"x\")"));
        assertEquals("string", evaluateString("lpad(\"string\",6,\"x\")"));
        assertEquals("xstring", evaluateString("lpad(\"string\",7,\"x\")"));
        assertEquals("xxstring", evaluateString("lpad(\"string\",8,\"x\")"));


        assertEquals(null, evaluateString("lpad(\"string\",0,\",.;\")"));
        assertEquals("s", evaluateString("lpad(\"string\",1,\",.;\")"));
        assertEquals("strin", evaluateString("lpad(\"string\",5,\",.;\")"));
        assertEquals("string", evaluateString("lpad(\"string\",6,\",.;\")"));
        assertEquals(",string", evaluateString("lpad(\"string\",7,\",.;\")"));
        assertEquals(",.string", evaluateString("lpad(\"string\",8,\",.;\")"));
        assertEquals(",.;string", evaluateString("lpad(\"string\",9,\",.;\")"));
        assertEquals(",.;,string", evaluateString("lpad(\"string\",10,\",.;\")"));
        assertEquals(",.;,.;,.;string", evaluateString("lpad(\"string\",15,\",.;\")"));
    }

    public void testADDMONTHS() throws Exception {
        String PRELEAP = "date(2016, 2, 28)";
        String LEAP = "date(2016, 2, 29)";
        String MARCH = "date(2016, 3, 31)";
        String MARCH28 = "date(2016, 3, 28)";
        String JANUARY = "date(2016, 1, 31)";

        assertEquals(evaluateDate(MARCH), evaluateDate("ADDMONTHS(" + LEAP + ",1)"));
        assertEquals(evaluateDate(MARCH28), evaluateDate("ADDMONTHS(" + PRELEAP + ",1)"));
        assertEquals(evaluateDate(JANUARY), evaluateDate("ADDMONTHS(" + LEAP + ",-1)"));
        assertEquals(evaluateDate(LEAP), evaluateDate("ADDMONTHS(" + LEAP + ",0.5)"));
        assertEquals(evaluateDate(LEAP), evaluateDate("ADDMONTHS(" + JANUARY + ",1)"));
        assertEquals(evaluateDate(JANUARY), evaluateDate("ADDMONTHS(" + MARCH + ",-2)"));
    }
    
    private FormulaDateTime parseDT(String dt) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(BaseLocalizer.GMT_TZ);
        return new FormulaDateTime(dateFormat.parse(dt));
        
    }

    
    public void testDateTimeMath() throws Exception {
        String LEAP = "2016-02-29 13:15:10";

        assertEquals(parseDT(LEAP), evaluateDateTime("DATETIMEVALUE(\"" + LEAP + "\")"));
        assertEquals(parseDT("2016-03-01 13:15:10"), evaluateDateTime("DATETIMEVALUE(\"" + LEAP + "\")+1.0"));
        assertEquals(parseDT("2016-02-28 13:15:10"), evaluateDateTime("DATETIMEVALUE(\"" + LEAP + "\")-1.0"));
        assertEquals(parseDT("2016-02-29 17:15:09"), evaluateDateTime("DATETIMEVALUE(\"" + LEAP + "\")+0.16666"));
        assertEquals(parseDT("2016-02-29 17:15:10"), evaluateDateTime("DATETIMEVALUE(\"" + LEAP + "\")+0.166667"));
        assertEquals(parseDT("2016-03-01 13:15:10"), evaluateDateTime("1.0+DATETIMEVALUE(\"" + LEAP + "\")"));
        assertEquals(parseDT("2016-02-28 13:15:10"), evaluateDateTime("DATETIMEVALUE(\"" + LEAP + "\")-1.0"));
        assertEquals(parseDT("2016-02-29 17:15:09"), evaluateDateTime("0.16666+DATETIMEVALUE(\"" + LEAP + "\")"));
        assertEquals(parseDT("2016-02-29 17:15:10"), evaluateDateTime("0.166667+DATETIMEVALUE(\"" + LEAP + "\")"));
        assertEquals(parseDT("2016-02-29 17:15:10"), evaluateDateTime("0.166666+DATETIMEVALUE(\"" + LEAP + "\")"));
        assertEquals(parseDT("2016-02-29 17:14:09"), evaluateDateTime("0.16596+DATETIMEVALUE(\"" + LEAP + "\")"));
        assertEquals(parseDT("2016-02-29 17:14:10"), evaluateDateTime("0.16597+DATETIMEVALUE(\"" + LEAP + "\")"));
        assertEquals(parseDT("2016-02-29 17:14:10"), evaluateDateTime("DATETIMEVALUE(\"" + LEAP + "\")+0.16597"));
        assertEquals(parseDT("2016-03-04 03:39:10"), evaluateDateTime("3.6+DATETIMEVALUE(\"" + LEAP + "\")"));
        assertEquals(parseDT("2016-03-04 03:39:10"), evaluateDateTime("DATETIMEVALUE(\"" + LEAP + "\")+3.6"));

    }

    public void testWEEKDAY() throws Exception {
        assertEquals(new BigDecimal(5), evaluateBigDecimal("WEEKDAY(DATE(2016,11,3))"));  // Thursday is 5
        assertEquals(new BigDecimal(Calendar.THURSDAY), evaluateBigDecimal("WEEKDAY(DATE(2016,11,3))"));
        assertEquals(new BigDecimal(4), evaluateBigDecimal("WEEKDAY(DATE(1969,2,12))"));  // Thursday is 5
    }

    public void testIsNumber() throws Exception {
        assertFalse( evaluateBoolean("ISNUMBER(\"\")")); 
        assertTrue( evaluateBoolean("ISNUMBER(\"5\")")); 
        assertTrue( evaluateBoolean("ISNUMBER(\"5.0\")")); 
        assertTrue( evaluateBoolean("ISNUMBER(\"+5.0\")")); 
        assertTrue( evaluateBoolean("ISNUMBER(\"-5.0\")")); 
        assertFalse( evaluateBoolean("ISNUMBER(\"No\")")); 
    } 

}
