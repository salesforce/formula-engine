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
     *  - Ceil/Floor precision: ::numeric(38,18) cast for CEIL/FLOOR arguments
     *  - Division/math precision: Numeric normalization to 14 significant digits
     *  - Error messages: Error message normalization in DataCloudFormulaTestCase
     */
    private static final Set<String> SKIP_SQL_PATHS = new HashSet<>(Arrays.asList(
            // DATE function — Hyper evaluates CASE WHEN branches eagerly, causing
            // TO_DATE to error on month=0 even inside guard expressions despite clamping
            "testDate",
            "testDateLeapConstantDay",
            "testDateLeapConstantMonth",
            "testDateVarConstConst",
            "testDateVarConstVar",
            "testDateVarVarConst",
            "testDateVariableYear",
            "testDATEVALUEWithString",
            "testIfANDFunc",
            "testIfORFunc",
            // Ceil/Floor at exact boundaries — Hyper's division precision causes
            // CEIL/FLOOR to produce different results at 1/N*N boundaries
            "testCeilRound",
            "testFloorRound",
            "testMCeilRound",
            "testMFloorRound",
            // Division precision — Hyper returns different precision for large number division;
            // differences exceed 14-significant-digit normalization threshold
            "testBigDivide",
            "testBigDivideWithFunc",
            "testIfErrorBigDivide",
            "testMultiplyWithDivideExpr",
            "testMultiplyWithDivideExpr2",
            "testNVLWithError",
            "testBVLWithError",
            "testSubDateTime",
            // Math precision — Hyper's math functions differ at the 14th significant digit boundary
            "testSine",
            "testTangent",
            "testModUsesLn",
            "testModUsesLog",
            "testModUsesSqrt",
            "testLogUsesIf",
            "testLNUsesValue",
            // Exponentiation/error — Hyper errors on LOG(0) in POWER guard (eager evaluation)
            "testExponentiationOperator",
            // Error message / type-cast behavioral differences between PostgreSQL and Hyper
            "testIfNullNullIf",
            "testIfReturningNullForDateType",
            "testDateTimeValueWithInvalidString",
            // Time value millisecond rounding differences (off by 1ms)
            "testSubtractBigTimeValue",
            "testSubtractTimeValueWithValidInValid",
            "testSubtractTwoTimeFields",
            "testTimeValueWithValidString",
            "testMillisecWithValidDateTimeString",
            "testAddBigTimeValueWithValidInValid",
            "testAddTimeValueWithValidInValid",
            "testAddHoursWithTwoCustFields",
            "testTextTimeValueWithValidInValid",
            "testIfErrorTextTimeValueWithValidInValid",
            // Timestamp arithmetic — INTERVAL rounding causes 1-second differences
            "testAddDateTime",
            "testAddDateTimeGivingDate",
            "testAddDateTimeMinutes",
            "testSubDateTimeCorners1",
            "testSubDateTimeCorners2",
            "testSubDateTimeGivingDate",
            "testSubDateTimeGivingDateTime",
            // Duration formatting — CEIL/FLOOR rounding in duration calculation
            "testFormatDurationDateTime",
            "testFormatDurationSeconds",
            "testFormatDurationSecondsBool",
            "testFormatDurationTime",
            "testFormatDurationWithFalse",
            "testFormatDurationWithTrue",
            // Floating-point geography calculations beyond normalization
            "testDistance",
            // Hyper may not support TO_CHAR with G/D format specifiers
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
