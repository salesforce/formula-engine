package com.force.formula.impl;

import com.force.formula.v2.IFormulaTestCaseFilter;
import com.force.formula.v2.data.FormulaTestDefinition;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Test case filter for DataCloud Hyper DB tests.
 *
 * Hyper DB is PostgreSQL-compatible but has behavioral differences in:
 * - Unsupported functions (JSON, certain to_char overloads)
 * - Strict date validation (TO_DATE rejects invalid dates)
 * - EPOCH calculation differences for time functions
 * - INTERVAL rounding for timestamp arithmetic
 *
 * Note: Floating-point precision and decimal scale differences are handled by
 * DataCloudFormulaTestCase which normalizes numeric comparisons to 14 significant digits.
 *
 * This filter:
 * 1. Excludes tests that use unsupported Hyper DB features entirely
 * 2. Removes sql/sqlNullAsNull execution paths from tests with non-numeric value differences
 */
public class DataCloudTestCaseFilter implements IFormulaTestCaseFilter<FormulaTestDefinition> {

    /** Tests to exclude entirely — use unsupported Hyper DB features or have ARM floating-point issues */
    private static final Set<String> EXCLUDED_TESTS = new HashSet<>(Arrays.asList(
            // JSON functions not supported in Hyper DB
            "testJsonValue",
            "testJsonPathValue",
            // ARM (Apple Silicon) floating-point precision differences in Java Math.log/exp/etc.
            // These fail on the 'formula' execution path regardless of DB engine.
            "testAbsUsesExp",
            "testAbsUsesLn",
            "testAbsUsesLog",
            "testExpSimple",
            "testExpUsesAbs",
            "testExpUsesCeil",
            "testExpUsesFloor",
            "testExpUsesIf",
            "testExpUsesLen",
            "testExpUsesMOD",
            "testExpUsesMinus",
            "testExpUsesPlus",
            "testExpUsesRound",
            "testExpUsesSqrt",
            "testExpUsesValue",
            "testLNSimple",
            "testLNUsesLn",
            "testLNUsesLog",
            "testLNUsesMOD",
            "testLogSimple",
            "testLogUsesLn",
            "testLogUsesLog",
            "testLogUsesMOD",
            "testLogUsesMinus",
            "testModUsesExpCeil"
    ));

    /** Tests where SQL execution path has non-numeric behavioral differences that
     *  cannot be resolved through hooks or normalization.
     *
     *  Many previous entries have been resolved via:
     *  - Time/EPOCH: sqlParseTime/sqlExtractTimeFromDateTime now compute seconds-since-midnight
     *  - Timestamp precision: DATE_TRUNC('second', ...) matches PostgreSQL's ::timestamp(0)
     *  - TO_DATE validation: LEAST/GREATEST clamping prevents errors on invalid dates
     *  - Time formatting: sqlToCharTime/sqlIntervalToDurationString use LPAD/EXTRACT arithmetic
     *  - Division/math precision: Numeric normalization to 14 significant digits
     *  - Error messages: Error message normalization in DataCloudFormulaTestCase
     */
    private static final Set<String> SKIP_SQL_PATHS = new HashSet<>(Arrays.asList(
            // DATE function - Hyper's TO_DATE still errors on some edge cases involving
            // month=0 in guard expression evaluation despite LEAST/GREATEST clamping
            "testDate",
            "testDateLeapConstantDay",
            "testDateLeapConstantMonth",
            "testDateVarConstConst",
            "testDateVarConstVar",
            "testDateVarVarConst",
            "testDateVariableYear",
            // Ceil/Floor at exact boundaries - Hyper's division precision causes
            // CEIL/FLOOR to produce different results at 1/N*N boundaries
            "testCeilRound",
            "testFloorRound",
            "testMCeilRound",
            "testMFloorRound",
            // Division precision - Hyper returns different precision for large number division
            "testBigDivide",
            "testBigDivideWithFunc",
            "testNVLWithError",
            "testBVLWithError",
            "testMultiplyWithDivideExpr",
            "testMultiplyWithDivideExpr2",
            // Exponentiation with zero base - Hyper errors on LOG(0) in POWER guard
            "testExponentiationOperator",
            // Math precision - Hyper's math functions differ beyond 14 significant digits
            "testSine",
            "testTangent",
            "testSqrtSwap",
            "testSqrtUsesCeil",
            "testSqrtUsesMinus",
            "testAbsUsesSqrt",
            "testModUsesLn",
            "testModUsesLog",
            "testModUsesSqrt",
            "testLogUsesAbs",
            "testLogUsesIf",
            "testLogUsesSqrt",
            // Time value millisecond rounding differences
            "testSubtractBigTimeValue",
            "testSubtractTimeValueWithValidInValid",
            "testSubtractTwoTimeFields",
            "testTimeValueWithValidString",
            "testMillisecWithValidDateTimeString",
            // Error message / behavioral differences
            "testRegex",
            "testIfNullNullIf",
            "testIfErrorBigDivide",
            "testIfANDFunc",
            "testIfORFunc",
            "testIfReturningNullForDateType",
            // Other Hyper behavioral differences
            "testDistance",
            "testFormatCurrency"
    ));

    @Override
    public List<FormulaTestDefinition> filter(List<FormulaTestDefinition> formulaTestCaseInfo) {
        return formulaTestCaseInfo.stream()
                .filter(test -> !EXCLUDED_TESTS.contains(test.getTestName()))
                .map(test -> {
                    if (SKIP_SQL_PATHS.contains(test.getTestName())) {
                        return removeSqlPaths(test);
                    }
                    return test;
                })
                .collect(Collectors.toList());
    }

    private FormulaTestDefinition removeSqlPaths(FormulaTestDefinition test) {
        List<String> filteredPaths = test.getExecutionPaths().stream()
                .filter(path -> !"sql".equals(path) && !"sqlNullAsNull".equals(path))
                .collect(Collectors.toList());
        if (filteredPaths.isEmpty()) {
            filteredPaths = test.getExecutionPaths();
        }
        return new FormulaTestDefinition(
                test.getTestName(),
                test.getTestCaseFieldInfo(),
                test.getReferenceFields(),
                filteredPaths,
                test.getTestData()
        );
    }
}
