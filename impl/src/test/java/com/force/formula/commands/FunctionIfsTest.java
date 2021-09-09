package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.force.formula.*;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * A set of tests for the nested-IFs optimization.
 *
 * @author ashanjani
 * @since 234
 */
public class FunctionIfsTest extends BaseCustomizableParserTest {

    public FunctionIfsTest(String name) {
        super(name, new FunctionIfsTestFormulaValidationHooks()) ;
    }

    @Test
    public void testIFS() throws Exception {
        parseTest("ifs(true, 1, 0)", " ( ifs true 1 0 )");
        parseTest("ifs(true, 1, false, 2, 0)", " ( ifs true 1 false 2 0 )");
        assertEquals(BigDecimal.ONE, evaluateBigDecimal("ifs(1=1, 1, 0)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("ifs(1=37, 1, 0)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("ifs(1=37, 1, 2=37, 2, 0)"));
        assertEquals(new BigDecimal("2"), evaluateBigDecimal("ifs(1=37, 1, 2<>37, 2, 0)"));
        assertEquals(Boolean.FALSE, evaluateBoolean("5=ifs(1=37, 1, 0)"));
        assertEquals(BigDecimal.ONE, evaluateBigDecimal("ifs(true, 1, null)"));
        assertEquals(new BigDecimal("37"), evaluateBigDecimal("ifs(false, 1/0, 37)"));
        assertEquals(new BigDecimal("37"), evaluateBigDecimal("ifs(true, 37, 1/0)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("ifs(1=null, 1, 0)"));
        // Use IF(false,true,null) to confuse the parser.  This may break when optimizations are enabled.
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("ifs(IF(false,true,null), 1, 0)"));
        assertEquals(BigDecimal.ZERO, evaluateBigDecimal("ifs(IF(false,true,null), 1, IF(false,true,null), 2, 0)"));
    }
    
    @Test
    public void testIFSJavascript() throws Exception {
    	testIFS();
    }

    // Validate the parsing
    @Test
    public void testIFSParsing() throws Exception {
    	evaluateFail("ifs(null, 1, 0)", WrongArgumentTypeException.class, "Expected Boolean, received Null");
    	evaluateFail("ifs(1=0, 1, 'Foo')", WrongArgumentTypeException.class, "Expected Number, received Text");
    	evaluateFail("ifs()", WrongNumberOfArgumentsException.class, "Expected 3, received 0");
    	evaluateFail("ifs(null)", WrongNumberOfArgumentsException.class, "Expected 3, received 1");
    	evaluateFail("ifs(null, 1)", WrongNumberOfArgumentsException.class, "Expected 3, received 2");
    	evaluateFail("ifs(null, 1, null, 1)", WrongNumberOfArgumentsException.class, "Expected 5, received 4");
    	evaluateFail("ifs(null, 1, null, 1, null, 1)", WrongNumberOfArgumentsException.class, "Expected 7, received 6");
    }
    
    // Because IFs is in beta, keep the javascript testing local here.
    private boolean isJs() {
    	return getName().contains("Javascript");
    }    
    @Override
    protected MockFormulaType getFormulaType() {
    	return isJs() ? MockFormulaType.JAVASCRIPT : MockFormulaType.DEFAULT;
    }
    @Override
    protected Object evaluate(String formulaSource, FormulaDataType columnType) throws Exception {
    	if (!isJs()) {
    		return super.evaluate(formulaSource, columnType);
    	} else {
        	return evaluateJavascript(formulaSource, columnType);
    		
    	}
    }
    
    @Test
    public void testSql() throws Exception {
        Set<TestArguments> testcases = Stream.of(
                new TestArguments()
                        .withFormulaUsingIfFunction("IF(account.amount == 0, '0', '-1')")
                        .withFormulaUsingIfsFunction("IFS(account.amount == 0, '0', '-1')")
                        .withExpectedSqlPair(new SQLPair("CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN '0' ELSE '-1' END", null)),
                new TestArguments()
                        .withFormulaUsingIfFunction("IF(account.amount == 0, '0', '-1') + IF(account.amount == 1, '1', '-2')")
                        .withFormulaUsingIfsFunction("IFS(account.amount == 0, '0', '-1') + IFS(account.amount == 1, '1', '-2')")
                        .withExpectedSqlPair(new SQLPair("(CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN '0' ELSE '-1' END||CASE WHEN (NVL($!s0s!$.amount, 0)=1) THEN '1' ELSE '-2' END)", null)),
                new TestArguments()
                        .withFormulaUsingIfFunction("IF(account.amount == 0, IF(account.amount == 1, '1', '-1'), '-2')")
                        .withFormulaUsingIfsFunction("IFS(account.amount == 0, IFS(account.amount == 1, '1', '-1'), '-2')")
                        .withExpectedSqlPair(new SQLPair("CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN CASE WHEN (NVL($!s0s!$.amount, 0)=1) THEN '1' ELSE '-1' END ELSE '-2' END", null)),
                new TestArguments()
                        .withFormulaUsingIfFunction("IF(account.amount == 0, '0', IF(account.amount == 1, '1', '-1'))")
                        .withFormulaUsingIfsFunction("IFS(account.amount == 0, '0', account.amount == 1, '1', '-1')")
                        .withExpectedSqlPair(new SQLPair("CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN '0' WHEN (NVL($!s0s!$.amount, 0)=1) THEN '1' ELSE '-1' END", null)),
                new TestArguments()
                        .withFormulaUsingIfFunction("IF(account.amount == 0, '0', IF(account.amount == 1, '1', '-1')) + IF(account.amount == 2, '2', IF(account.amount == 3, '3', '-2'))")
                        .withFormulaUsingIfsFunction("IFS(account.amount == 0, '0', account.amount == 1, '1', '-1') + IFS(account.amount == 2, '2', account.amount == 3, '3', '-2')")
                        .withExpectedSqlPair(new SQLPair("(CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN '0' WHEN (NVL($!s0s!$.amount, 0)=1) THEN '1' ELSE '-1' END||CASE WHEN (NVL($!s0s!$.amount, 0)=2) THEN '2' WHEN (NVL($!s0s!$.amount, 0)=3) THEN '3' ELSE '-2' END)", null)),
                new TestArguments()
                        .withFormulaUsingIfFunction("IF(account.amount == 0, '0', IF(account.amount == 1, '1', IF(account.amount == 2, '2', '-1')))")
                        .withFormulaUsingIfsFunction("IFS(account.amount == 0, '0', account.amount == 1, '1', account.amount == 2, '2', '-1')")
                        .withExpectedSqlPair(new SQLPair("CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN '0' WHEN (NVL($!s0s!$.amount, 0)=1) THEN '1' WHEN (NVL($!s0s!$.amount, 0)=2) THEN '2' ELSE '-1' END", null)),
                new TestArguments()
                        .withFormulaUsingIfFunction("IF(account.amount == 0, '0', IF(account.amount == 1, '1', IF(account.amount == 2, '2', IF(account.amount == 3, '3', '-1'))))")
                        .withFormulaUsingIfsFunction("IFS(account.amount == 0, '0', account.amount == 1, '1', account.amount == 2, '2', account.amount == 3, '3', '-1')")
                        .withExpectedSqlPair(new SQLPair("CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN '0' WHEN (NVL($!s0s!$.amount, 0)=1) THEN '1' WHEN (NVL($!s0s!$.amount, 0)=2) THEN '2' WHEN (NVL($!s0s!$.amount, 0)=3) THEN '3' ELSE '-1' END", null)),
                new TestArguments()
                        .withFormulaUsingIfFunction("IF(account.amount == 0, '0', IF(account.amount == 1, '1', IF(account.amount == 2, '2', IF(account.amount == 3, '3', IF(account.amount == 4, '4', '-1')))))")
                        .withFormulaUsingIfsFunction("IFS(account.amount == 0, '0', account.amount == 1, '1', account.amount == 2, '2', account.amount == 3, '3', account.amount == 4, '4', '-1')")
                        .withExpectedSqlPair(new SQLPair("CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN '0' WHEN (NVL($!s0s!$.amount, 0)=1) THEN '1' WHEN (NVL($!s0s!$.amount, 0)=2) THEN '2' WHEN (NVL($!s0s!$.amount, 0)=3) THEN '3' WHEN (NVL($!s0s!$.amount, 0)=4) THEN '4' ELSE '-1' END", null)),
                new TestArguments()
                        .withFormulaUsingIfFunction("IF(account.amount == 0, IF(account.amount == 1, '1', IF(account.amount == 2, '2', IF(account.amount == 3, '3', '-1'))), IF(account.amount == 4, '4', IF(account.amount == 5, '5', '-2')))")
                        .withFormulaUsingIfsFunction("IFS(account.amount == 0, IFS(account.amount == 1, '1', account.amount == 2, '2', account.amount == 3, '3', '-1'), account.amount == 4, '4', account.amount == 5, '5', '-2')")
                        .withExpectedSqlPair(new SQLPair("CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN CASE WHEN (NVL($!s0s!$.amount, 0)=1) THEN '1' WHEN (NVL($!s0s!$.amount, 0)=2) THEN '2' WHEN (NVL($!s0s!$.amount, 0)=3) THEN '3' ELSE '-1' END WHEN (NVL($!s0s!$.amount, 0)=4) THEN '4' WHEN (NVL($!s0s!$.amount, 0)=5) THEN '5' ELSE '-2' END", null)),
                new TestArguments()
                        .withFormulaUsingIfFunction("IF(account.amount == 0, IF(account.amount == 1, '1', IF(account.amount == 2, '2', IF(account.amount == 3, IF(account.amount == 4, '4', IF(account.amount == 5, '5', IF(account.amount == 6, '6', '-1'))), '-2'))), IF(account.amount == 7, '7', IF(account.amount == 8, '8', '-3')))")
                        .withFormulaUsingIfsFunction("IFS(account.amount == 0, IFS(account.amount == 1, '1', account.amount == 2, '2', account.amount == 3, IFS(account.amount == 4, '4', account.amount == 5, '5', account.amount == 6, '6', '-1'), '-2'), account.amount == 7, '7', account.amount == 8, '8', '-3')")
                        .withExpectedSqlPair(new SQLPair("CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN CASE WHEN (NVL($!s0s!$.amount, 0)=1) THEN '1' WHEN (NVL($!s0s!$.amount, 0)=2) THEN '2' WHEN (NVL($!s0s!$.amount, 0)=3) THEN CASE WHEN (NVL($!s0s!$.amount, 0)=4) THEN '4' WHEN (NVL($!s0s!$.amount, 0)=5) THEN '5' WHEN (NVL($!s0s!$.amount, 0)=6) THEN '6' ELSE '-1' END ELSE '-2' END WHEN (NVL($!s0s!$.amount, 0)=7) THEN '7' WHEN (NVL($!s0s!$.amount, 0)=8) THEN '8' ELSE '-3' END", null)),
                new TestArguments()
                        .withFormulaUsingIfFunction("IF(account.amount == 0, TEXT(IF(account.amount == 1, 1, IF(account.amount == 2, 2, IF(account.amount == 3, IF(account.amount == 4, 4, IF(account.amount == 5, 5, IF(account.amount == 6, 6, -1))), -2)))), IF(account.amount == 7, '7', IF(account.amount == 8, '8', '-3')))")
                        .withFormulaUsingIfsFunction("IFS(account.amount == 0, TEXT(IFS(account.amount == 1, 1, account.amount == 2, 2, account.amount == 3, IFS(account.amount == 4, 4, account.amount == 5, 5, account.amount == 6, 6, -1), -2)), account.amount == 7, '7', account.amount == 8, '8', '-3')")
                        .withExpectedSqlPair(new SQLPair("CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN (TO_CHAR(CASE WHEN (NVL($!s0s!$.amount, 0)=1) THEN 1 WHEN (NVL($!s0s!$.amount, 0)=2) THEN 2 WHEN (NVL($!s0s!$.amount, 0)=3) THEN CASE WHEN (NVL($!s0s!$.amount, 0)=4) THEN 4 WHEN (NVL($!s0s!$.amount, 0)=5) THEN 5 WHEN (NVL($!s0s!$.amount, 0)=6) THEN 6 ELSE (-1) END ELSE (-2) END)) WHEN (NVL($!s0s!$.amount, 0)=7) THEN '7' WHEN (NVL($!s0s!$.amount, 0)=8) THEN '8' ELSE '-3' END", null))
        ).collect(Collectors.toSet());

        for(TestArguments testcase : testcases) {
            performTest(testcase);
        }
    }

    @Test
    public void testGuard() throws Exception {
        performTest(
                new TestArguments()
                        .withFormulaUsingIfFunction("IF(account.amount == 0, TEXT(DATEVALUE(account.accountid + '0')), IF(account.amount == 1, TEXT(DATEVALUE(account.accountid + '1')), IF(account.amount == 2, TEXT(DATEVALUE(account.accountid + '2')), '-1')))")
                        .withFormulaUsingIfsFunction("IFS(account.amount == 0, TEXT(DATEVALUE(account.accountid + '0')), account.amount == 1, TEXT(DATEVALUE(account.accountid + '1')), account.amount == 2, TEXT(DATEVALUE(account.accountid + '2')), '-1')")
                        .withExpectedSqlPair(new SQLPair("CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN (TO_CHAR(TO_DATE(($!s1s!$.accountId||'0'), 'YYYY-MM-DD'), 'YYYY-MM-DD')) WHEN (NVL($!s0s!$.amount, 0)=1) THEN (TO_CHAR(TO_DATE(($!s1s!$.accountId||'1'), 'YYYY-MM-DD'), 'YYYY-MM-DD')) WHEN (NVL($!s0s!$.amount, 0)=2) THEN (TO_CHAR(TO_DATE(($!s1s!$.accountId||'2'), 'YYYY-MM-DD'), 'YYYY-MM-DD')) ELSE '-1' END",
                        		"(((NVL($!s0s!$.amount, 0)=0)) AND ( NOT REGEXP_LIKE (($!s1s!$.accountId||'0'), '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$') /*comments to keep size */ )) OR ((CASE WHEN (NVL($!s0s!$.amount, 0)=0) THEN 1 ELSE 0 END = 0) AND ((((NVL($!s0s!$.amount, 0)=1)) AND ( NOT REGEXP_LIKE (($!s1s!$.accountId||'1'), '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$') /*comments to keep size */ )) OR ((CASE WHEN (NVL($!s0s!$.amount, 0)=1) THEN 1 ELSE 0 END = 0) AND ((((NVL($!s0s!$.amount, 0)=2)) AND ( NOT REGEXP_LIKE (($!s1s!$.accountId||'2'), '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$') /*comments to keep size */ ))))))")));
    }

    private void performTest(TestArguments args) throws Exception {
        SQLPair sqlPairUsingIfFunction = getSqlPair(args.formulaUsingIfFunction, MockFormulaDataType.TEXT);
        assertEquals(String.format("Invalid sql for formula using IF function: '%s'", args.formulaUsingIfFunction),
                args.expectedSqlPair.sql,
                sqlPairUsingIfFunction.sql);
        assertEquals(String.format("Invalid guard for formula using IF function: '%s'", args.formulaUsingIfFunction),
                args.expectedSqlPair.guard,
                sqlPairUsingIfFunction.guard);

        SQLPair sqlPairUsingIfsFunction = getSqlPair(args.formulaUsingIfsFunction, MockFormulaDataType.TEXT);
        assertEquals(String.format("Invalid sql for formula using IFS function: %s", args.formulaUsingIfsFunction),
                args.expectedSqlPair.sql,
                sqlPairUsingIfsFunction.sql);
        assertEquals(String.format("Invalid guard for formula using IFS function: %s", args.formulaUsingIfsFunction),
                args.expectedSqlPair.guard,
                sqlPairUsingIfsFunction.guard);
    }


    private static class TestArguments {
        private String formulaUsingIfFunction;
        private String formulaUsingIfsFunction;
        private SQLPair expectedSqlPair;

        public TestArguments withFormulaUsingIfFunction(String formulaUsingIfFunction) {
            this.formulaUsingIfFunction = formulaUsingIfFunction;
            return this;
        }

        public TestArguments withFormulaUsingIfsFunction(String formulaUsingIfsFunction) {
            this.formulaUsingIfsFunction = formulaUsingIfsFunction;
            return this;
        }

        public TestArguments withExpectedSqlPair(SQLPair expectedSqlPair) {
            this.expectedSqlPair = expectedSqlPair;
            return this;
        }
    }

    static class FunctionIfsTestFormulaValidationHooks extends BaseCustomizableParserTest.FieldTestFormulaValidationHooks {

        @Override
        public boolean parseHook_shouldOptimizeNestedIfs() {
            return true;
        }
    }
}