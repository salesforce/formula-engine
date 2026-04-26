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

    /** Tests where SQL execution path has fundamental Hyper DB behavioral differences
     *  that cannot be resolved through hooks, normalization, or expected value adjustments.
     *
     *  Previously resolved entries (now passing with SQL paths enabled):
     *  - Time/EPOCH: sqlParseTime/sqlExtractTimeFromDateTime compute seconds-since-midnight
     *  - Timestamp arithmetic: sqlAddDaysToDate uses ROUND for rounding parity
     *  - Timestamp precision: DATE_TRUNC('second', ...) matches PostgreSQL's ::timestamp(0)
     *  - Time formatting: sqlToCharTime/sqlIntervalToDurationString use LPAD/EXTRACT arithmetic
     *  - Ceil/Floor precision: ::numeric(38,18) cast for CEIL/FLOOR arguments
     *  - Division/math precision: Numeric normalization to 14 significant digits
     *  - Error messages: Error message normalization in DataCloudFormulaTestCase
     *  - Millisecond/duration/distance rounding: expected values adjusted in formulaTestV2.xml
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
            "testIfANDFunc",
            "testIfORFunc",
            // Currency formatting — Hyper does not support TO_CHAR with numeric types
            "testFormatCurrency",
            // Duration formatting — numeric field overflow for very large date ranges (year 1780-3999)
            "testFormatDurationDateTime",
            // Math function precision — last-digit differences across multiple testData entries
            "testModUsesLog",
            "testModUsesSqrt",
            "testSine",
            "testTangent",
            // Error message / type-cast behavioral differences
            "testIfNullNullIf",
            "testDateTimeValueWithInvalidString",
            "testIfReturningNullForDateType",
            // Floating-point geography — precision differences across multiple testData entries
            "testDistance"
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
