package sfdc.formula.parser.gen4;

import org.junit.BeforeClass;
import org.junit.Test;

import sfdc.formula.*;
import sfdc.formula.impl.*;

/**
 * @author ashanjani
 * @since 220
 */
public class ANTLR4GrammarVisitorTest {

    @BeforeClass
    public static void setUp() {
        MockLocalizerContext.establishMock();
    }
	
    /**
     * Formula Exception
     */

    @Test
    public void testFormulaException() throws Exception {
        parseFormula("func(");
        parseFormula("func((");
        parseFormula("func)");
        parseFormula("func())");
    }

    @Test
    public void testFormulaException2() throws Exception {
        parseFormula("%%");
        parseFormula("func(@)");
        parseFormula("func@)");
    }

    @Test
    public void testFormulaException3() throws Exception {
        parseFormula("DATE YEAR()", false);
    }

    @Test
    public void testFormulaException4() throws Exception {
        parseFormula("abc ^ ");
        parseFormula(" ^ abc");
    }

    @Test
    public void testFormulaException5() throws Exception {
        parseFormula("abc {! def");
    }

    @Test
    public void testFormulaException6() throws Exception {
        parseFormula("/* abc", false);
        parseFormula("abc /*", false);

        parseFormula("abc */");
        parseFormula("*/ abc");

        parseFormula("func(/* abc)", false);
        parseFormula("func(*/ abc)", false);
    }

    @Test
    public void testFormulaException7() throws Exception {
        parseFormula("abc && \n func(");
        parseFormula("abc && \n def && \n \t \t \t func(");
    }

    /**
     * Template Exception
     */

    @Test
    public void testTemplateException() throws Exception {
        parseTemplate("abc {! func( } def", true, false);
        parseTemplate("abc {! func( } def", true, true);
    }

    @Test
    public void testTemplateException2() throws Exception {
        //TODO(arman): the following tests are commented out because we are going with the following behavior:
        //despite failOnEmbeddedFormulaExceptions's value, unclosed formulas will always throw an exception.
        //If this behavior is reverted, we need to put these tests back

        //parseTemplate("{! abc ", true, false);
        parseTemplate("{! abc ", true, true);

        //parseTemplate("abc {! def", true, false);
        parseTemplate("abc {! def", true, true);

        //parseTemplate("abc {! def {! ghi", true, false);
        parseTemplate("abc {! def {! ghi", false, true);

        parseTemplate("}", true, false);
        parseTemplate("}", true, true);

        parseTemplate("} abc", true, false);
        parseTemplate("} abc", true, true);

        parseTemplate("} abc }", true, false);
        parseTemplate("} abc }", true, true);
    }

    @Test
    public void testTemplateException3() throws Exception {
        parseTemplate("{! abc('{! def }') ");
        parseTemplate("{! abc('{! def }' }");
    }

    @Test
    public void testTemplateException4() throws Exception {
        parseTemplate("abc {! def {! ghi } jkl", true, false);
        parseTemplate("abc {! def {! ghi } jkl", true, true);

        parseTemplate("abc \n {! \n def \n {! \n ghi \n } \n jkl", true, false);
        parseTemplate("abc \n {! \n def \n {! \n ghi \n } \n jkl", true, true);
    }

    @Test
    public void testTemplateException5() throws Exception {
        parseTemplate("abc {! ' } def");
        parseTemplate("abc {! \" } def");
    }

    /**
     * Field Reference Exception
     */

    @Test
    public void testFieldReferenceException() throws Exception {
        parseFieldReference("abc [ def");
        parseFieldReference("abc ] def");
        parseFieldReference("abc [ def ] ]");
        parseFieldReference("abc [ def [");
    }

    @Test
    public void testFieldReferenceException2() throws Exception {
        parseFieldReference("abc def", false, true);
        parseFieldReference("abc * def");
        parseFieldReference("abc . def .");
        parseFieldReference("abc . ");
    }

    @Test
    public void testFieldReferenceException3() throws Exception {
        parseFieldReference("\n\nabc\n [\ndef\n]\n ] \n");
        parseFieldReference("\n\nabc\n [\ndef\n]\n\n [ghi\n] \t\n [ [\n\tjkl \n]");
    }

    @Test
    public void testFieldReferenceException4() throws Exception {
        parseFieldReference("abc [ def ]", true, false);
        parseFieldReference("abc . def ", true, false);
    }

    /**
     * Multiline
     */

    @Test
    public void testMultiline() throws Exception {
        parseFormula("abc && def &&\nghi && jkl");
    }

    @Test
    public void testMultiline2() throws Exception {
        parseFormula("func(\nabc,\n def\n, ghi,\n\t\t\t\tjkl\n)");
    }

    @Test
    public void testMultiline3() throws Exception {
        parseFieldReference("abc\n[def]");
    }

    @Test
    public void testMultiline4() throws Exception {
        parseFieldReference("\n\nabc\n [\ndef\n]\n\n");
    }

    @Test
    public void testMultiline5() throws Exception {
        parseFieldReference("\n\nabc\n [\ndef\n]\n\n [ghi\n] \t\n [\n\tjkl \n]");
    }

    /**
     * Comment
     */

    @Test
    public void testComment() throws Exception {
        parseFormula("/* comment */ abc");
        parseFormula("abc /* comment */");
    }

    @Test
    public void testComment2() throws Exception {
        parseFormula("/* comment */ abc /* comment */");
    }

    @Test
    public void testComment3() throws Exception {
        parseFormula("OR(/* comment */ abc)");
        parseFormula("abc && /* comment */ def");
    }

    @Test
    public void testComment4() throws Exception {
        parseTemplate("abc {! /* comment */ def } abc");
        parseTemplate("abc /* comment */ {! /* comment */ def } abc");
    }

    /**
     * Template
     */

    @Test
    public void testTemplate() throws Exception {
        parseTemplate("abc");
        parseTemplate("a b c");
    }

    @Test
    public void testTemplate2() throws Exception {
        parseTemplate("{! def }");
    }

    @Test
    public void testTemplate3() throws Exception {
        parseTemplate("abc {! def }");
        parseTemplate("{! def } abc");
    }

    @Test
    public void testTemplate4() throws Exception {
        parseTemplate("abc {! def } abc");
        parseTemplate("{! def } abc {! def}");
    }

    @Test
    public void testTemplate5() throws Exception {
        parseTemplate(" {! def} ");
        parseTemplate("\n \t \n \t \n {! def} \n \t \n \t \n");
    }

    @Test
    public void testTemplate6() throws Exception {
        parseTemplate("abc abc \t abc \n       abc {! def }\n{! def }");
    }

    @Test
    public void testTemplate7() throws Exception {
        parseTemplate("This is a test of format {!format(\"Test {1}\",\"USD\",\"USD\")}");
        parseTemplate("this is \" {! \"abc\" } \" awesome");
        parseTemplate("this is \" {! \"{! abc }\" } \" awesome");
        parseTemplate(" \" {! abc } \" ");
    }

    @Test
    public void testTemplate8() throws Exception {
        parseTemplate("This is a test of format {!format('Test {1}','USD','USD')}");
        parseTemplate("this is ' {! 'abc' } ' awesome");
        parseTemplate("this is ' {! '{! abc }' } ' awesome");
        parseTemplate(" ' {! abc } ' ");
    }

    @Test
    public void testTemplate9() throws Exception {
        parseTemplate("{!func(\"someone's text\")}");
        parseTemplate("{!func(\"someone's text hasn't been used\")}");

        parseTemplate("{!func('someone\"s text')}");
        parseTemplate("{!func('someone\"s text hasn\"t been used')}");
    }

    /**
     * Escaped quotation marks should not flip inSingleQuote/inDoubleQuote
     */
    @Test
    public void testTemplate10() throws Exception {
        parseTemplate("abc {! '\\'' }");
        parseTemplate("abc {! \" \\\" \" }");

        parseTemplate("abc {! \\' }", true, false);
        parseTemplate("abc {! \\\" }", true, false);
    }

    @Test
    public void testTemplate11() throws Exception {
        for(int i = 0; i < 10; i++) {
            parseTemplate(generateTemplateWithBackslashes(i));
        }
    }

    //template format: "{! ' \\\\\\' ' }", where number of slashes is based on count
    private static String generateTemplateWithBackslashes(int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("{! ' ");
        for(int i = 0 ; i < count; i++) {
            sb.append("\\");
        }

        sb.append("' ' }");
        return sb.toString();
    }

    @Test
    public void testTemplate12() throws Exception {
        parseTemplate("{!IF(/* they're */ 'abc') }");
        parseTemplate("{!IF(/* they\"re */ \"abc\") }");

        parseTemplate("{! ' /* */ ' }");
        parseTemplate("{! \" /* */ \" }");

        parseTemplate("{! /* /* */ */ }");
        parseTemplate("{! /* } */ }");
        parseTemplate("{! /* ");
    }

    /**
     * Constant
     */

    @Test
    public void testConstantNumber() throws Exception {
        parseFormula("1234");
    }

    @Test
    public void testConstantEmptyString() throws Exception {
        parseFormula("\"\"");
    }

    @Test
    public void testConstantString() throws Exception {
        parseFormula("\"hello, world!\"");
    }

    @Test
    public void testConstantMapIdent() throws Exception {
        parseFormula("[abcd = efgh]");
    }

    @Test
    public void testConstantMapIdent2() throws Exception {
        parseFormula("[abcd = (1 + 2)]");
    }

    @Test
    public void testConstantMapIdent3() throws Exception {
        parseFormula("[abcd = (1 + 2), efgh = (1 + 2), ijkl = (1 + 2), mnop = (1 + 2)]");
    }

    @Test
    public void testConstantMapString() throws Exception {
        parseFormula("[\"abcd\" = efgh]");
    }

    @Test
    public void testConstantMapString2() throws Exception {
        parseFormula("[\"abcd\" = (1 + 2)]");
    }

    @Test
    public void testConstantMapString3() throws Exception {
        parseFormula("[\"abcd\" = (1 + 2), \"efgh\" = (1 + 2), \"ijkl\" = (1 + 2), \"mnop\" = (1 + 2)]");
    }

    /**
     * Logical Or Expression
     */

    @Test
    public void testLogicalOrExpressionInfix() throws Exception {
        parseFormula("10 || 15");
    }

    @Test
    public void testLogicalOrExpressionInfix2() throws Exception {
        parseFormula("abc || def");
    }

    @Test
    public void testLogicalOrExpressionInfix3() throws Exception {
        parseFormula("abc || def || ghi || jkl");
    }

    @Test
    public void testLogicalOrExpressionInfix4() throws Exception {
        parseFormula("10 + 15 || 20 * 30 || abc < def");
    }

    @Test
    public void testLogicalOrExpressionInfix5() throws Exception {
        parseFormula("10 && 15 || 20 && 30 || abc && def && ghi");
    }


    @Test
    public void testLogicalOrExpressionComma() throws Exception {
        parseFormula("OR(10)");
    }

    @Test
    public void testLogicalOrExpressionComma2() throws Exception {
        parseFormula("or(10, 15)");
        parseFormula("Or(10, 15)");
        parseFormula("OR(10, 15)");
    }

    @Test
    public void testLogicalOrExpressionComma3() throws Exception {
        parseFormula("OR(abc, def)");
    }

    @Test
    public void testLogicalOrExpressionComma4() throws Exception {
        parseFormula("OR(abc, def, ghi, jkl)");
    }

    @Test
    public void testLogicalOrExpressionComma5() throws Exception {
        parseFormula("OR(10 + 15, 20 * 30, abc < def)");
    }

    @Test
    public void testLogicalOrExpressionComma6() throws Exception {
        parseFormula("OR(AND(10, 15), AND(20, 30), AND(abc, def))");
    }

    /**
     * Logical And Expression
     */

    @Test
    public void testLogicalAndExpressionInfix() throws Exception {
        parseFormula("10 && 15");
    }

    @Test
    public void testLogicalAndExpressionInfix2() throws Exception {
        parseFormula("abc && def");
    }

    @Test
    public void testLogicalAndExpressionInfix3() throws Exception {
        parseFormula("abc && def && ghi && jkl");
    }

    @Test
    public void testLogicalAndExpressionInfix4() throws Exception {
        parseFormula("10 + 15 && 20 * 30 && abc < def");
    }

    @Test
    public void testLogicalAndExpressionComma() throws Exception {
        parseFormula("AND(10)");
    }

    @Test
    public void testLogicalAndExpressionComma2() throws Exception {
        parseFormula("and(10, 15)");
        parseFormula("AnD(10, 15)");
        parseFormula("AND(10, 15)");
    }

    @Test
    public void testLogicalAndExpressionComma3() throws Exception {
        parseFormula("AND(abc, def)");
    }

    @Test
    public void testLogicalAndExpressionComma4() throws Exception {
        parseFormula("AND(abc, def, ghi, jkl)");
    }

    @Test
    public void testLogicalAndExpressionComma5() throws Exception {
        parseFormula("AND(10 + 15, 20 * 30, abc < def)");
    }

    @Test
    public void testLogicalAndExpressionComma6() throws Exception {
        parseFormula("OR(abc) && OR(def)");
        parseFormula("AND(abc) && AND(def)");
        parseFormula("OR(abc) && AND(def)");
        parseFormula("AND(abc) && OR(def)");
        parseFormula("OR(abc) && AND(def) && OR(abc) && AND(def) && OR(abc) && AND(def)");
        parseFormula("OR(abc) || AND(def) || OR(abc) || AND(def) || OR(abc) || AND(def)");
        parseFormula("OR(abc) || AND(def) && OR(abc) || AND(def) && OR(abc) || AND(def)");
    }

    /**
     * Equality Expression
     */

    @Test
    public void testEqulityExpression() throws Exception {
        parseFormula("10 != 15");
        parseFormula("10 == 15");
        parseFormula("10 <> 15");
        parseFormula("10 = 15");
    }

    @Test
    public void testEqulityExpression2() throws Exception {
        parseFormula("abc != def");
        parseFormula("abc == def");
        parseFormula("abc <> def");
        parseFormula("abc = def");
    }

    @Test
    public void testEqulityExpression3() throws Exception {
        parseFormula("abc != 10 == 15 <> def = 20");
    }

    @Test
    public void testEqulityExpression4() throws Exception {
        parseFormula("abc + 1 < 10 * 2 > 15 ^ 3 <= !def >= -20 != abc + 1 < 10 * 2 > 15 ^ 3 <= !def >= -20 == " +
                "abc + 1 < 10 * 2 > 15 ^ 3 <= !def >= -20 <> abc + 1 < 10 * 2 > 15 ^ 3 <= !def >= -20 = abc + 1 < 10 * 2 > 15 ^ 3 <= !def >= -20");
    }

    /**
     * Relational Expression
     */

    @Test
    public void testRelationalExpression() throws Exception {
        parseFormula("10 < 15");
        parseFormula("10 <= 15");
        parseFormula("10 > 15");
        parseFormula("10 >= 15");
    }

    @Test
    public void testRelationalExpression2() throws Exception {
        parseFormula("abc < def");
        parseFormula("abc <= def");
        parseFormula("abc > def");
        parseFormula("abc >= def");
    }

    @Test
    public void testRelationalExpression3() throws Exception {
        parseFormula("abc < 10 > 15 <= def >= 20");
    }

    @Test
    public void testRelationalExpression4() throws Exception {
        parseFormula("abc + 1 < 10 * 2 > 15 ^ 3 <= !def >= -20");
    }

    /**
     * Additive Expression
     */

    @Test
    public void testAdditiveExpression() throws Exception {
        parseFormula("10+15");
        parseFormula("10-15");
        parseFormula("10&15");
    }

    @Test
    public void testAdditiveExpression2() throws Exception {
        parseFormula("abc+def");
        parseFormula("abc-def");
        parseFormula("abc&def");
    }

    @Test
    public void testAdditiveExpression3() throws Exception {
        parseFormula("abc+10-def+15");
        parseFormula("abc+10-def&15");
        parseFormula("abc&10&def&15");
    }

    @Test
    public void testAdditiveExpression4() throws Exception {
        parseFormula("abc+10-def&15*abc+10-def&15/abc+10-def&15^abc+10-def&15");
    }

    /**
     * Multiplicative Expression
     * Note: "multiplicativeExpression" really represents exponent expressions
     */

    @Test
    public void testMultiplicativeExpression() throws Exception {
        parseFormula("10^15");
    }

    @Test
    public void testMultiplicativeExpression2() throws Exception {
        parseFormula("abc^def");
    }

    @Test
    public void testMultiplicativeExpression3() throws Exception {
        parseFormula("abc^10^def^15");
    }

    @Test
    public void testMultiplicativeExpression4() throws Exception {
        parseFormula("abc*10^def/15");
        parseFormula("abc^10*def/15");
        parseFormula("abc/10*def^15");
    }

    /**
     * Exponent Expression
     * Note: "exponentExpression" really represents multiplicative expressions
     */

    @Test
    public void testExponentExpression() throws Exception {
        parseFormula("10*15");
        parseFormula("10/15");
    }

    @Test
    public void testExponentExpression2() throws Exception {
        parseFormula("abc*def");
        parseFormula("abc/def");
    }

    @Test
    public void testExponentExpression3() throws Exception {
        parseFormula("abc*10*def*15");
        parseFormula("abc/10/def/15");
        parseFormula("abc*10/def*15");
    }

    /**
     * Unary Expression
     */

    @Test
    public void testUnaryExpressionPlus() throws Exception {
        parseFormula("+10");
    }

    @Test
    public void testUnaryExpressionPlus2() throws Exception {
        parseFormula("+abc");
    }

    @Test
    public void testUnaryExpressionPlus3() throws Exception {
        parseFormula("+(1 - 2)");
    }

    @Test
    public void testUnaryExpressionMinus() throws Exception {
        parseFormula("-10");
    }

    @Test
    public void testUnaryExpressionMinus2() throws Exception {
        parseFormula("-abc");
    }

    @Test
    public void testUnaryExpressionMinus3() throws Exception {
        parseFormula("-(1 - 2)");
    }

    @Test
    public void testUnaryExpressionNot() throws Exception {
        parseFormula("not abc");
    }

    @Test
    public void testUnaryExpressionNot2() throws Exception {
        parseFormula("not(true)");
    }

    @Test
    public void testUnaryExpressionBang() throws Exception {
        parseFormula("!abc");
    }

    @Test
    public void testUnaryExpressionBang2() throws Exception {
        parseFormula("!(true)");
    }

    /**
     * Primary Expression
     */

    @Test
    public void testPrimaryExpressionFieldReference() throws Exception {
        parseFormula("abcd");
    }

    @Test
    public void testPrimaryExpressionConstant() throws Exception {
        parseFormula("1234");
    }

    @Test
    public void testPrimaryExpressionTrue() throws Exception {
        parseFormula("true");
        parseFormula("TrUe");
        parseFormula("TRUE");
    }

    @Test
    public void testPrimaryExpressionFalse() throws Exception {
        parseFormula("false");
        parseFormula("FaLse");
        parseFormula("FALSE");
    }

    @Test
    public void testPrimaryExpressionNull() throws Exception {
        parseFormula("null");
        parseFormula("NuLl");
        parseFormula("NULL");
    }

    @Test
    public void testPrimaryExpressionLogicalOrExpression() throws Exception {
        parseFormula("(abcd)");
        parseFormula("(true)");
        parseFormula("(1 + 2)");
    }

    @Test
    public void testPrimaryExpressionFunctionCall() throws Exception {
        parseFormula("IF(TRUE, '1', '2')");
    }

    /**
     * Function Call
     */

    @Test
    public void testFunctionCallNoParameters() throws Exception {
        parseFormula("abc()");
    }

    @Test
    public void testFunctionCallSingleParameter() throws Exception {
        parseFormula("abc(1)");
    }

    @Test
    public void testFunctionCallMultipleParameters() throws Exception {
        parseFormula("abc(1, 2, 3, 4)");
    }

    @Test
    public void testFunctionCallRecursiveParameter() throws Exception {
        parseFormula("abc(abc(def()))");
    }

    @Test
    public void testFunctionCallComplexParameters() throws Exception {
        parseFormula("abc(1, 1+2, 5*9, 10/4, abc, 'hello', \"world\", abc(abc(def())))");
    }

    /**
     * Field Reference Literal
     */

    @Test
    public void testFieldReferenceLiteralDollarSign() throws Exception {
        parseFieldReference("$");
        parseFieldReference("$abc");
        parseFieldReference("$abc123");
        parseFieldReference("$123abc");
    }

    @Test
    public void testFieldReferenceLiteralLowerCase() throws Exception {
        parseFieldReference("abc");
        parseFieldReference("abc123");
    }

    @Test
    public void testFieldReferenceLiteralUpperCase() throws Exception {
        parseFieldReference("ABC");
        parseFieldReference("ABC123");
    }

    @Test
    public void testFieldReferenceLiteralMixedCase() throws Exception {
        parseFieldReference("AbCDefghI");
        parseFieldReference("A1b2C3D4e5f6g7h8I9");
    }

    @Test
    public void testFieldReferenceLiteralSpecialCharacters() throws Exception {
        parseFieldReference("$$$___:::...###abc_abc:abc.abc#");
    }

    /**
     * Subscripted Expression
     */

    @Test
    public void testSubscriptedExpression() throws Exception {
        parseFieldReference("abc [def]");
    }

    @Test
    public void testSubscriptedExpression2() throws Exception {
        parseFieldReference("abc [def] [ghi] [jkl]");
    }

    @Test
    public void testSubscriptedExpression3() throws Exception {
        parseFieldReference("$abc [a + 10] [10 ^ 5 * 3 + 1] [a && b]");
    }

    @Test
    public void testSubscriptedExpressionDot() throws Exception {
        parseFieldReference("abc .def");
        parseFieldReference("abc . def");
    }

    @Test
    public void testSubscriptedExpressionDot2() throws Exception {
        parseFieldReference("abc .def .ghi .jkl");
        parseFieldReference("abc . def . ghi . jkl");
    }

    /**
     * String Literals
     */

    @Test
    public void testStringLiteralsEscapedCharacters() throws Exception {
        parseFormula("FUNC(' \n ')", false);
        parseFormula("FUNC(\" \n \")", false);
        parseFormula("[' \n ' = (1 + 2), ' \n ' = (1 + 2)]", false);
        parseFormula("[\" \n \" = (1 + 2), \" \n \" = (1 + 2)]", false);

        parseFormula("FUNC(' \t ')");
        parseFormula("FUNC(\" \t \")");
        parseFormula("[' \t ' = (1 + 2), ' \t ' = (1 + 2)]", false);
        parseFormula("[\" \t \" = (1 + 2), \" \t \" = (1 + 2)]", false);

        parseFormula("FUNC(' \r ')", false);
        parseFormula("FUNC(\" \r \")", false);
        parseFormula("[' \r ' = (1 + 2), ' \r ' = (1 + 2)]", false);
        parseFormula("[\" \r \" = (1 + 2), \" \r \" = (1 + 2)]", false);

        parseFormula("FUNC(' \n\r ')", false);
        parseFormula("FUNC(\" \n\r \")", false);
        parseFormula("[' \n\r ' = (1 + 2), ' \n\r ' = (1 + 2)]", false);
        parseFormula("[\" \n\r \" = (1 + 2), \" \n\r \" = (1 + 2)]", false);

        parseFormula("FUNC(' \' ')", false);
        parseFormula("FUNC(\" \' \")");
        parseFormula("[' \' ' = (1 + 2), ' \' ' = (1 + 2)]", false);
        parseFormula("[\" \' \" = (1 + 2), \" \' \" = (1 + 2)]", false);

        parseFormula("FUNC(' \" ')");
        parseFormula("FUNC(\" \" \")", false);
        parseFormula("[' \" ' = (1 + 2), ' \" ' = (1 + 2)]", false);
        parseFormula("[\" \" \" = (1 + 2), \" \" \" = (1 + 2)]", false);
    }

    @Test
    public void testStringLiteralsCapitalizedEscapedCharacters() throws Exception {
        parseFormula("FUNC(' \\T ')");
        parseFormula("FUNC(\" \\T \")");
        parseFormula("[' \\T ' = (1 + 2), ' \\T ' = (1 + 2)]");
        parseFormula("[\" \\T \" = (1 + 2), \" \\T \" = (1 + 2)]");

        parseFormula("FUNC(' \\R ')");
        parseFormula("FUNC(\" \\R \")");
        parseFormula("[' \\R ' = (1 + 2), ' \\R ' = (1 + 2)]");
        parseFormula("[\" \\R \" = (1 + 2), \" \\R \" = (1 + 2)]");

        parseFormula("FUNC(' \\N ')");
        parseFormula("FUNC(\" \\N \")");
        parseFormula("[' \\N ' = (1 + 2), ' \\N ' = (1 + 2)]");
        parseFormula("[\" \\N \" = (1 + 2), \" \\N \" = (1 + 2)]");
    }

    @Test
    public void testStringLiteralsRandomEscapedCharacters() throws Exception {
        parseFormula("FUNC(' \\d ')", false);
        parseFormula("FUNC(\" \\d \")", false);
        parseFormula("[' \\d ' = (1 + 2), ' \\d ' = (1 + 2)]", false);
        parseFormula("[\" \\d \" = (1 + 2), \" \\d \" = (1 + 2)]", false);

        parseFormula("FUNC(' \\D ')", false);
        parseFormula("FUNC(\" \\D \")", false);
        parseFormula("[' \\D ' = (1 + 2), ' \\D ' = (1 + 2)]", false);
        parseFormula("[\" \\D \" = (1 + 2), \" \\D \" = (1 + 2)]", false);
    }

    /**
     * ignoreFailOnEmbeddedFormulaParserExceptionsForParsing Tests
     */

    private static String MALFORMED_TEMPLATE = "Hello {! FirstName +( LastName }, welcome to Salesforce!";

    @Test
    public void testIgnoreFailOnEmbeddedFormulaParserExceptionsForParsingFlag_False_False() throws Exception {
        FormulaProperties properties = new FormulaProperties();
        properties.setParseAsTemplate(true);
        properties.setFailOnEmbeddedFormulaExceptions(false);
        properties.setIgnoreFailOnEmbeddedFormulaParserExceptionsForParsing(false);

        FormulaUtils.parseWithANTLR2(MALFORMED_TEMPLATE, properties);
        FormulaUtils.parseWithANTLR4(MALFORMED_TEMPLATE, properties);
    }

    @Test
    public void testIgnoreFailOnEmbeddedFormulaParserExceptionsForParsingFlag_False_True() throws Exception {
        FormulaProperties properties = new FormulaProperties();
        properties.setParseAsTemplate(true);
        properties.setFailOnEmbeddedFormulaExceptions(false);
        properties.setIgnoreFailOnEmbeddedFormulaParserExceptionsForParsing(true);
        testParsingMalformedTemplate(MALFORMED_TEMPLATE, properties);
    }

    @Test
    public void testIgnoreFailOnEmbeddedFormulaParserExceptionsForParsingFlag_True_False() throws Exception {
        FormulaProperties properties = new FormulaProperties();
        properties.setParseAsTemplate(true);
        properties.setFailOnEmbeddedFormulaExceptions(true);
        properties.setIgnoreFailOnEmbeddedFormulaParserExceptionsForParsing(false);
        testParsingMalformedTemplate(MALFORMED_TEMPLATE, properties);
    }

    @Test
    public void testIgnoreFailOnEmbeddedFormulaParserExceptionsForParsingFlag_True_True() throws Exception {
        FormulaProperties properties = new FormulaProperties();
        properties.setParseAsTemplate(true);
        properties.setFailOnEmbeddedFormulaExceptions(true);
        properties.setIgnoreFailOnEmbeddedFormulaParserExceptionsForParsing(true);
        testParsingMalformedTemplate(MALFORMED_TEMPLATE, properties);
    }

    private void testParsingMalformedTemplate(String malformedTemplate, FormulaProperties properties) {
        try {
            FormulaUtils.parseWithANTLR2(malformedTemplate, properties);
            throw new RuntimeException("parseWithANTLR2 should have thrown an exception");
        }
        catch(FormulaException e) { }

        try {
            FormulaUtils.parseWithANTLR4(malformedTemplate, properties);
            throw new RuntimeException("parseWithANTLR4 should have thrown an exception");
        }
        catch (FormulaException e) { }
    }

    /**
     * Other Tests
     */

    @Test
    public void testIdentUnicodeCharacters() throws Exception {
        String[] unicodeCharacterCodes = {"0130", "212A"};
        for(String code : unicodeCharacterCodes) {
            String str = new String(Character.toChars(Integer.parseInt(code, 16)));
            parseFormula(str + "abc()"); //testing starting character
            parseFormula("abc" + str + "()"); //testing non-starting character
        }
    }

    /**
     * Helper Methods
     */

    private void parseTemplate(String template) throws Exception {
        parseTemplate(template, true, true);
    }

    private void parseTemplate(String template, boolean compareExceptionDetails, boolean failOnEmbeddedFormulaExceptions) throws Exception {
        FormulaProperties properties = new FormulaProperties();
        properties.setParseAsTemplate(true);
        properties.setFailOnEmbeddedFormulaExceptions(failOnEmbeddedFormulaExceptions);
        parse(template, properties, compareExceptionDetails);
    }

    private void parseFormula(String formula) throws Exception {
        parseFormula(formula, true);
    }

    private void parseFormula(String formula, boolean compareExceptionDetails) throws Exception {
        FormulaProperties properties = new FormulaProperties();
        parse(formula, properties, compareExceptionDetails);
    }

    private void parseFieldReference(String formula) throws Exception {
        parseFieldReference(formula, true, true);
    }

    private void parseFieldReference(String formula, boolean compareExceptionDetails, boolean allowSubscripts) throws Exception {
        FormulaProperties properties = new FormulaProperties();
        properties.setParseAsReference(true);
        properties.setAllowSubscripts(allowSubscripts);
        parse(formula, properties, compareExceptionDetails);
    }

    private void parse(String formula, FormulaProperties properties, boolean compareExceptionDetails) throws Exception {
        FormulaAST antlr2AST = null;
        FormulaAST antlr4AST = null;
        FormulaException antlr2Exception = null;
        FormulaException antlr4Exception = null;

        try {
            antlr2AST = FormulaUtils.parseWithANTLR2(formula, properties);
        }
        catch(FormulaException e) {
            antlr2Exception = e;
        }

        try {
            antlr4AST = FormulaUtils.parseWithANTLR4(formula, properties);
        }
        catch(FormulaException e) {
            antlr4Exception = e;
        }

        compareExceptions(antlr2Exception, antlr4Exception, compareExceptionDetails);
        FormulaUtils.compareFormulaASTs(antlr2AST, antlr4AST);
    }

    private static void compareExceptions(FormulaException antlr2Exception, FormulaException antlr4Exception, boolean compareDetails) throws Exception {
        if(antlr2Exception == null && antlr4Exception == null) {
            return;
        }

        if(antlr2Exception == null) {
            throw new Exception("ANTLR4 threw an exception but ANTLR2 did not");
        }

        if(antlr4Exception == null) {
            throw new Exception("ANTLR2 threw an exception but ANTLR4 did not");
        }

        if(!compareDetails) {
            return;
        }

        FormulaParseException antlr2ParseException = (FormulaParseException)antlr2Exception;
        FormulaParseException antlr4ParseException = (FormulaParseException)antlr4Exception;

        String antlr2ExceptionType = antlr2ParseException.getCauseException().getClass().getCanonicalName();
        String antlr4ExceptionType = antlr4ParseException.getCauseException().getClass().getCanonicalName();
        if(!antlr2ExceptionType.equals(antlr4ExceptionType)) {
            throw new Exception("Exception types do not match: " + "[ANTLR2]" + antlr2ExceptionType + " != " + "[ANTLR4]" + antlr4ExceptionType);
        }

        int antlr2Location = antlr2ParseException.getLocation();
        int antlr4Locartion = antlr4ParseException.getLocation();
        if(antlr2Location != antlr4Locartion) {
            throw new Exception("Exception locations do not match: " + "[ANTLR2]" + antlr2Location + " != " + "[ANTLR4]" + antlr4Locartion);
        }
    }
}
