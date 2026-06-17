package com.force.formula.impl;

import com.force.formula.util.FormulaTextUtil;
import com.force.formula.v2.FormulaTestCase;
import com.force.formula.v2.FormulaXMLTestSuite;
import com.force.formula.v2.data.FormulaTestData;
import com.force.formula.v2.data.FormulaTestDefinition;
import com.force.formula.v2.impl.ExecutionPaths;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * A custom FormulaTestCase for DataCloud Hyper DB tests that handles numeric
 * value comparison with tolerance for sql/sqlNullAsNull execution paths.
 *
 * Hyper DB returns slightly different values for:
 * - Math functions (SQRT, LOG, LN, SIN, etc.) - last 1-2 digit differences
 * - Decimal scale - fewer trailing zeros than PostgreSQL (scale 15 vs 30-32)
 *
 * This test case normalizes both expected and actual numeric values for SQL paths
 * by rounding to 14 significant digits and stripping trailing zeros.
 */
public class DataCloudFormulaTestCase extends FormulaTestCase {

    private static final int SIGNIFICANT_DIGITS = 14;

    public DataCloudFormulaTestCase(FormulaTestDefinition testCase, FormulaXMLTestSuite testSuite) {
        super(testCase, testSuite);
    }

    @Override
    protected void runTestCase() {
        for (String executionPath : this.testCase.getExecutionPaths()) {
            if (ExecutionPaths.get(executionPath) != null) {
                for (FormulaTestData testData : this.testCase.getTestData()) {
                    String output = ExecutionPaths.get(executionPath)
                            .execute(this.testCase.getTestCaseFieldInfo().getFormula(),
                                    this.testCase.getTestCaseFieldInfo().getDataType(),
                                    testData.getInput(),
                                    this.testEntity,
                                    this.testSuite.getDbTester());

                    String expected = FormulaTextUtil.escapeToXml(testData.getExpectedOutput().get(executionPath));
                    String actual = FormulaTextUtil.escapeToXml(output).trim();

                    // For SQL paths, normalize numeric values and error messages before comparison
                    if (isSqlPath(executionPath)) {
                        expected = normalizeNumericValue(expected);
                        actual = normalizeNumericValue(actual);
                        expected = normalizeErrorMessage(expected);
                        actual = normalizeErrorMessage(actual);
                    }

                    assertEquals(this.testCase.getTestName() + " failed for execution path: " + executionPath
                                    + " and for testData: " + testData,
                            expected, actual);
                }
            }
        }
    }

    private boolean isSqlPath(String executionPath) {
        return "sql".equals(executionPath) || "sqlNullAsNull".equals(executionPath);
    }

    /**
     * Normalize a numeric string by rounding to a fixed number of significant
     * digits and stripping trailing zeros. Non-numeric strings (errors, dates,
     * booleans, nulls) are returned as-is.
     */
    static String normalizeNumericValue(String value) {
        if (value == null || value.startsWith("Error:") || value.equals("null")
                || value.equals("true") || value.equals("false")) {
            return value;
        }
        // Timestamps contain date patterns like YYYY-MM-DD HH:MM:SS
        if (value.length() > 10 && value.charAt(4) == '-' && value.contains(":")) {
            return value;
        }
        try {
            BigDecimal bd = new BigDecimal(value);
            if (bd.compareTo(BigDecimal.ZERO) == 0) {
                return "0";
            }
            bd = bd.round(new MathContext(SIGNIFICANT_DIGITS, RoundingMode.HALF_UP));
            return bd.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            return value;
        }
    }

    /**
     * Normalize error messages to account for differences between PostgreSQL
     * and Hyper DB error message formatting. Both databases surface errors
     * as "Error: ..." strings, but the core message text may differ
     * (e.g., different capitalization, wording, or error codes).
     *
     * This normalizes known-equivalent error patterns rather than collapsing
     * all errors into one bucket, so genuine semantic differences are still caught.
     */
    static String normalizeErrorMessage(String value) {
        if (value == null || !value.startsWith("Error:")) {
            return value;
        }
        String msg = value.substring("Error:".length()).trim().toLowerCase();

        // Normalize known-equivalent error pairs between PostgreSQL and Hyper DB
        if (msg.contains("division by zero") || msg.contains("divide by zero")) {
            return "Error: division by zero";
        }
        if (msg.contains("numeric field overflow") || msg.contains("numeric overflow")
                || msg.contains("out of range")) {
            return "Error: numeric field overflow";
        }
        if (msg.contains("invalid regular expression") || msg.contains("regular expression error")
                || msg.contains("regexp")) {
            return "Error: invalid regular expression";
        }
        if (msg.contains("invalid input syntax") || msg.contains("invalid value")
                || msg.contains("cannot be cast") || msg.contains("bad cast")) {
            return "Error: invalid input syntax";
        }
        if (msg.contains("date/time field value out of range") || msg.contains("invalid date")
                || msg.contains("date out of range")) {
            return "Error: date/time field value out of range";
        }
        if (msg.contains("cannot take logarithm of zero")) {
            return "Error: cannot take logarithm of zero";
        }
        return value;
    }
}
