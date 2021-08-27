package com.force.formula;

/**
 * Test all combinations and permutations of valid formula syntax/semantics 
 * 
 * @author dchasman
 * @since 140
 */
public class ParserTest extends ParserTestBase {
    public ParserTest(String name) {
        super(name);
    }

    public void testCaseInsensitivity() throws FormulaException {
        parseTest("abs(1)", " ( abs 1 )");
        parseTest("ABS(1)", " ( ABS 1 )");
        parseTest("Abs(1)", " ( Abs 1 )");
    }

    public void testConstant() throws FormulaException {
        parseTest("true", " true");
        parseTest("false", " false");
    }

    public void testStringConstant() throws FormulaException {
        parseTest("\"mothra\"", " \"mothra\"");
    }

    public void testAdd() throws FormulaException {
        testBinaryOperator("+");
    }

    public void testSubtract() throws FormulaException {
        testBinaryOperator("-");
    }

    public void testMultiply() throws FormulaException {
        testBinaryOperator("*");
    }

    public void testDivide() throws FormulaException {
        testBinaryOperator("/");
    }

    public void testUnaryMinus() throws FormulaException {
        parseTest("-2", " ( - 2 )");
    }

    public void testUnaryPlus() throws FormulaException {
        parseTest("+2", " ( + 2 )");
    }

    public void testFieldReference() throws FormulaException {
        parseTest("a", " a");
    }

    public void testGT() throws FormulaException {
        testBinaryOperator(">");
    }

    public void testGTEQ() throws FormulaException {
        testBinaryOperator(">=");
    }

    public void testLTEQ() throws FormulaException {
        testBinaryOperator("<=");
    }

    public void testIf() throws FormulaException {
        parseTest("if (temperature > 1000, 98.6, temperature)", " ( if ( > temperature 1000 ) 98.6 temperature )");
    }

    public void testPrecedence() throws FormulaException {
        parseTest("(1 + 2) * (3 + 4) / 2", " ( / ( * ( + 1 2 ) ( + 3 4 ) ) 2 )");
        parseTest("1 + 2 * 3 + 4 / 2", " ( + ( + 1 ( * 2 3 ) ) ( / 4 2 ) )");
    }

    public void testWhitespace() throws FormulaException {
        parseTest("WON:Sum /( CLOSED:Sum - WON:Sum) * 100", " ( * ( / WON:Sum ( - CLOSED:Sum WON:Sum ) ) 100 )");
        parseTest("WON:Sum / ( CLOSED:Sum - WON:Sum) * 100", " ( * ( / WON:Sum ( - CLOSED:Sum WON:Sum ) ) 100 )");
        parseTest("IF(Probability = 1, ROUND(Amount * 0.02, 2) , 0)",
            " ( IF ( = Probability 1 ) ( ROUND ( * Amount 0.02 ) 2 ) 0 )");
    }

    public void testGarbage() throws FormulaException {
        try {
            parseTest("AMOUNT:Sum dsa* saaa11- df", null);
            fail();
        }
        catch (Exception e) {}

        try {
            parseTest("1 + 2 garbage", null);
            fail();
        }
        catch (Exception e) {}

        try {
            parseTest("garbage 1 + 2", null);
            fail();
        }
        catch (Exception e) {}
    }

    public void testSpecialCharacters() throws FormulaException {
        parseTest("\"Hello \\\"cruel\\\" world\"", " \"Hello \\\"cruel\\\" world\"");
    }

    private void testBinaryOperator(String operator) throws FormulaException {
        for (int i = 0; i < lhs.length; i++) {
            for (int j = 0; j < rhs.length; j++) {
                parseTest(lhs[i] + " " + operator + " " + rhs[j], " ( " + operator + " " + fieldName(lhs[i]) + " "
                    + fieldName(rhs[j]) + " )");
            }
        }
    }

    private String[] lhs = { "1", "a", "\"foo\"", "true" };
    private String[] rhs = { "2", "b", "\"bar\"", "false" };

}
