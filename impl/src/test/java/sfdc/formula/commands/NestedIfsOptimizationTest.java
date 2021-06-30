package sfdc.formula.commands;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.google.common.collect.Maps;

import junit.framework.TestCase;
import sfdc.formula.*;
import sfdc.formula.impl.*;

/**
 * A set of tests for the nested-IFs optimization.
 *
 * @author ashanjani
 * @since 234
 */
public class NestedIfsOptimizationTest extends TestCase {

    FormulaEngineHooks oldHooks = FormulaEngine.getHooks();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        oldHooks = FormulaEngine.getHooks();
        FormulaEngine.setHooks(new FormulaValidationHooks() {
            @Override
            public boolean parseHook_shouldOptimizeNestedIfs() {
                return true;
            }
        });
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        FormulaEngine.setHooks(oldHooks);
    }

    /**
     * Tests various scenarios to ensure we find and correctly translate nested-IFs to IFS function.
     */
    @Test
    public void testVarious() throws Exception {
        Set<Map.Entry<String, String>> testcases = Stream.of(
                Maps.immutableEntry("'test'", (String)null),

                Maps.immutableEntry("IF(AccountNumber == 0, '0', '-1')",
                        (String)null),

                Maps.immutableEntry("IF(AccountNumber == 0, '0', '-1') + IF(AccountNumber == 1, '1', '-2')",
                        (String)null),

                //only count nested-IFs through else clause
                Maps.immutableEntry("IF(AccountNumber == 0, IF(NumberOfEmployees == 1, '1', '-1'), '-2')",
                        (String)null),

                Maps.immutableEntry("IF(AccountNumber == 0, '0', IF(AccountNumber == 1, '1', '-1'))",
                        "IFS(AccountNumber == 0, '0', AccountNumber == 1, '1', '-1')"),

                Maps.immutableEntry("IF(AccountNumber == 0, '0', IF(AccountNumber == 1, '1', '-1')) + IF(AccountNumber == 2, '2', IF(AccountNumber == 3, '3', '-2'))",
                        "IFS(AccountNumber == 0, '0', AccountNumber == 1, '1', '-1') + IFS(AccountNumber == 2, '2', AccountNumber == 3, '3', '-2')"),

                Maps.immutableEntry("IF(AccountNumber == 0, '0', IF(AccountNumber == 1, '1', IF(AccountNumber == 2, '2', '-1')))",
                        "IFS(AccountNumber == 0, '0', AccountNumber == 1, '1', AccountNumber == 2, '2', '-1')"),


                Maps.immutableEntry("IF(AccountNumber == 0, '0', IF(AccountNumber == 1, '1', IF(AccountNumber == 2, '2', IF(AccountNumber == 3, '3', '-1'))))",
                        "IFS(AccountNumber == 0, '0', AccountNumber == 1, '1', AccountNumber == 2, '2', AccountNumber == 3, '3', '-1')"),

                Maps.immutableEntry("IF(AccountNumber == 0, '0', IF(AccountNumber == 1, '1', IF(AccountNumber == 2, '2', IF(AccountNumber == 3, '3', IF(AccountNumber == 4, '4', '-1')))))",
                        "IFS(AccountNumber == 0, '0', AccountNumber == 1, '1', AccountNumber == 2, '2', AccountNumber == 3, '3', AccountNumber == 4, '4', '-1')"),


                //even though we don't count nested-IFs through if clause, we should check it for another set of nested-IFs

                Maps.immutableEntry("IF(AccountNumber == 0, IF(AccountNumber == 1, '1', IF(AccountNumber == 2, '2', IF(AccountNumber == 3, '3', '-1'))), IF(AccountNumber == 4, '4', IF(AccountNumber == 5, '5', '-2')))",
                        "IFS(AccountNumber == 0, IFS(AccountNumber == 1, '1', AccountNumber == 2, '2', AccountNumber == 3, '3', '-1'), AccountNumber == 4, '4', AccountNumber == 5, '5', '-2')"),

                Maps.immutableEntry("IF(AccountNumber == 0, IF(AccountNumber == 1, '1', IF(AccountNumber == 2, '2', IF(AccountNumber == 3, IF(AccountNumber == 4, '4', IF(AccountNumber == 5, '5', IF(AccountNumber == 6, '6', '-1'))), '-2'))), IF(AccountNumber == 7, '7', IF(AccountNumber == 8, '8', '-3')))",
                        "IFS(AccountNumber == 0, IFS(AccountNumber == 1, '1', AccountNumber == 2, '2', AccountNumber == 3, IFS(AccountNumber == 4, '4', AccountNumber == 5, '5', AccountNumber == 6, '6', '-1'), '-2'), AccountNumber == 7, '7', AccountNumber == 8, '8', '-3')"),


                Maps.immutableEntry("IF(AccountNumber == 0, VALUE(IF(AccountNumber == 1, '1', IF(AccountNumber == 2, '2', IF(AccountNumber == 3, IF(AccountNumber == 4, '4', IF(AccountNumber == 5, '5', IF(AccountNumber == 6, '6', '-1'))), '-2')))), IF(AccountNumber == 7, '7', IF(AccountNumber == 8, '8', '-3')))",
                        "IFS(AccountNumber == 0, VALUE(IFS(AccountNumber == 1, '1', AccountNumber == 2, '2', AccountNumber == 3, IFS(AccountNumber == 4, '4', AccountNumber == 5, '5', AccountNumber == 6, '6', '-1'), '-2')), AccountNumber == 7, '7', AccountNumber == 8, '8', '-3')")

        ).collect(Collectors.toSet());

        for (Map.Entry<String, String> testcase: testcases) {
            assertNestedIfsOptimizationCorrectness(testcase.getKey(), testcase.getValue());
        }
    }

    /**
     * Ensures we only find nested-IFs, and not other nested functions.
     */
    @Test
    public void testIgnoreOtherNestedFunctions() throws Exception {
        assertNestedIfsOptimizationCorrectness("CASE(AccountNumber == 0, '0', CASE(AccountNumber == 1, '1', '-1'))", null);
    }

    /**
     * Ensure we ignore the capitalization pattern used for the IF function's name.
     */
    @Test
    public void testIgnoreCase() throws Exception {
        String formulaUsingIfsFunction = "IFS(AccountNumber == 0, '0', AccountNumber == 1, '1', '-1')";

        assertNestedIfsOptimizationCorrectness("if(AccountNumber == 0, '0', IF(AccountNumber == 1, '1', '-1'))", formulaUsingIfsFunction);
        assertNestedIfsOptimizationCorrectness("If(AccountNumber == 0, '0', IF(AccountNumber == 1, '1', '-1'))", formulaUsingIfsFunction);
        assertNestedIfsOptimizationCorrectness("iF(AccountNumber == 0, '0', IF(AccountNumber == 1, '1', '-1'))", formulaUsingIfsFunction);
        assertNestedIfsOptimizationCorrectness("IF(AccountNumber == 0, '0', iF(AccountNumber == 1, '1', '-1'))", formulaUsingIfsFunction);
        assertNestedIfsOptimizationCorrectness("If(AccountNumber == 0, '0', iF(AccountNumber == 1, '1', '-1'))", formulaUsingIfsFunction);
    }


    /**
     * Parses and optimizes {@code formulaUsingNestedIfFunctions} formula and then compares its AST with the AST of
     * the already-optimized {@code formulaUsingIfsFunction} formula.
     *
     * @param formulaUsingNestedIfFunctions Formula that is parsed and optimized to use IFS function.
     * @param formulaUsingIfsFunction Formula that is already using IFS function.
     *                                If null, non-optimized version of {@code formulaUsingNestedIfFunctions} will
     *                                be used for comparison. Pass null if you don't expect any optimization
     *                                opportunities to be found in {@code formulaUsingNestedIfFunctions}.
     */
    private void assertNestedIfsOptimizationCorrectness(String formulaUsingNestedIfFunctions, String formulaUsingIfsFunction) throws Exception {
        FormulaAST nestedIfFunctionsAST = FormulaUtils.parse(formulaUsingNestedIfFunctions, new FormulaProperties());
        FormulaAST optimizedAST = optimizeNestedIfs(nestedIfFunctionsAST);

        FormulaAST ifsFunctionAST = FormulaUtils.parse(formulaUsingIfsFunction == null ? formulaUsingNestedIfFunctions : formulaUsingIfsFunction, new FormulaProperties());

        FormulaUtils.compareFormulaASTs(optimizedAST, ifsFunctionAST, false);
    }

    /**
     * Walks through {@code ast} in the same order as {@link sfdc.formula.impl.BaseFormulaInfoImpl#optimizeParseTree}.
     */
    private FormulaAST optimizeNestedIfs(FormulaAST ast) {
        if(ast == null || ast.getNumberOfChildren() == 0) {
            return ast;
        }

        FormulaAST child = (FormulaAST)ast.getFirstChild();
        while (child != null) {
            child = optimizeNestedIfs(child);
            child = (FormulaAST)child.getNextSibling();
        }

        if(FormulaAST.isFunctionNode(ast, FunctionIf.CAPITALIZED_NAME)) {
            FunctionIf.optimizeNestedIfs(ast);
        }

        return ast;
    }
}