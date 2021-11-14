/*
 * Copyright, 1999-2004, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */

package com.force.formula.util;

import java.math.*;
import java.sql.*;

/**
 * BigDecimal helper class
 * 
 * @author cweissman
 */
public class BigDecimalHelper {

    /**
     * This is Oracle's precision.
     */
    public static final int NUMBER_PRECISION_INTERNAL = 39;
    public static final MathContext MC_PRECISION_INTERNAL = new MathContext(39, RoundingMode.HALF_UP);

    /**
     * 6 digits fudge, to guarantee that date-diff operations are second-accurate
     * (Oracle stores these diffs in fractions of days which is 86400 secs, or order 10^5).
     * This is needed to cover inexact to exact transitions (floor/ceil) on number that cannot be
     * represented as decimals (like 1/3 or 1/11).
     */
    public static final int NUMBER_PRECISION_EXTERNAL = 33;
    public static final MathContext MC_PRECISION_EXTERNAL = new MathContext(NUMBER_PRECISION_EXTERNAL, RoundingMode.HALF_UP);

    public static final BigDecimal getNonNullFromRs(ResultSet rs, String colName) throws SQLException {
        BigDecimal bd = rs.getBigDecimal(colName);
        return bd == null ? BigDecimal.ZERO : bd;
    }

    public static final BigDecimal getNonNullFromRs(ResultSet rs, int colIndex) throws SQLException {
        BigDecimal bd = rs.getBigDecimal(colIndex);
        return bd == null ? BigDecimal.ZERO : bd;
    }

    public static final BigDecimal getNonNullFromStmt(CallableStatement stmt, int idx) throws SQLException {
        BigDecimal bd = stmt.getBigDecimal(idx);
        return bd == null ? BigDecimal.ZERO : bd;
    }

    /**
     * Used for Formula fields
     * @param value the value to round
     * @param scale  the scale to round to 
     * @return the value rounded, using special semantics for numbers close to 0 to avoid over rounding
     */
    public static BigDecimal round(BigDecimal value, int scale) {
    	return round(value, scale, RoundingMode.HALF_UP);
    }
    
    /**
     * Used for Formula fields.  This is for backwards compatibility with some interesting behavior
     * near zero.
     * @param value the value to round
     * @param scale  the scale to round to 
     * @param mode the rounding mode to use when rounding
     * @return the value rounded, using special semantics for numbers close to 0 to avoid over rounding
     */
    public static BigDecimal round(BigDecimal value, int scale, RoundingMode mode) {
        BigDecimal result = value.abs();
        if (result.compareTo(BigDecimal.ONE) < 0) {
            // Special handling for numbers < 1 is so that e.g. 0.003 rounds to 2 places as 0.00 instead of 0.0.
            // Salesforce semantics treats 0.003 as having precision 4, scale 3, instead of precision 1 scale 3.
            // This is for consistency with prior releases, though not consistent with SQL version.
            result = result.add(BigDecimal.ONE, MC_PRECISION_INTERNAL);
            final int newPrec = scale + 1;
            if (newPrec <= 0) {
                // If we're rounding to 0 digits, result is just 0 -- BigDecimal.round leaves it unchanged
                result = BigDecimal.ZERO;
            } else {
                result = result.round(new MathContext(newPrec, mode));
                result = result.subtract(BigDecimal.ONE, MC_PRECISION_INTERNAL);
                if (value.signum() == -1) {
                    result = result.negate();
                }
            }
        } else {
            final int newPrec = result.precision() - result.scale()  + scale;
            if (newPrec <= 0) {
                // if we're rounding to 0 digits, result is just 0 -- BigDecimal.round leaves it unchanged
                result = BigDecimal.ZERO;
            } else {
                result = value.round(new MathContext(newPrec, mode));
            }
        }
        // W-1273499 In case when zero scale is passed and the result output has (precision - scale) > input's (precision - scale), the BigDecimal round returns exponential form,
        // so covering that case, toPlainString() converts to decimal form.
        return new BigDecimal(result.toPlainString());
    }

    // Catchable exception for routine below
    public static class FormulaPowerException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public FormulaPowerException(String s) {
            super(s);
        }
    }

    /**
     * Used by Formula field ^ operator
     * @param arg1 the operand
     * @param arg2 the exponent
     * @return arg1 ^ arg2, but don't allow it to hog the processor with giant numbers
     */
    public static BigDecimal formulaPower(BigDecimal arg1, BigDecimal arg2) {
        try {
            // If log_10(arg1)*arg2 > 64, bail out. This number is too big.
            double arg1Test = (Math.log(arg1.abs().doubleValue()) / Math.log(10));
            double test = arg1Test * arg2.intValue();
            if (test > 64)
                throw new FormulaPowerException("Out of range argument to POWER() function");
            else {
                BigDecimal res = arg1.pow(arg2.intValueExact(), POWER_MC);

                // See bug: 200512
                // Large powers of small number leads to _very_ small numbers (represented in BigDecimal by very large scale values).
                // Many BigDecimal operations create char-arrays of the size <scale> (and hence use a _lot_ of memory).
                // So here we limit the smallest absolute value to (+-)1e-39
                if (res.abs().compareTo(POWER_SMALLEST) == -1) 
                    return BigDecimal.ZERO;

                return res;
            }
        } catch (ArithmeticException e) {
            // Exponent must be a positive integer
            throw new FormulaPowerException("Out of range argument to POWER() function"); // NOPMD
        }
    }
    
    private static final MathContext POWER_MC = new MathContext(18, RoundingMode.HALF_UP); // why 18?
    private static final BigDecimal POWER_SMALLEST = BigDecimal.ONE.movePointLeft(NUMBER_PRECISION_INTERNAL);

    /**
     * Used by Formula field: TEXT() function
     * @param n the number to format
     * @return the number formated as a plain string with zeros stripped
     */
    public static String formatBigDecimal(BigDecimal n) {
        if (n.compareTo(BigDecimal.ZERO) == 0) {
            n = BigDecimal.ZERO;
        } else {
            n = n.stripTrailingZeros();
        }
        return n.toPlainString();
    }
    
    /**
     * Used by Formula field: ISNUMBER() function
     * @param input the number to parse
     * @return whether input is a number
     */
    // CDW: Stamm suggests a numeric regex pattern (but I didn't want to change any semantics...)
    public static boolean functionIsNumber(String input) {
        try {
            new BigDecimal(input, MC_PRECISION_INTERNAL);
            return true;
        } catch (NumberFormatException x) {
            return false;
       }
    }

    // This stuff has been pulled out of NumberInputElement
    // Some convenient static constants for finding the size of a number base10
    private static final int MAX_PRECISION = 24;
    private static final double[] POW10 = new double[MAX_PRECISION + 1];
    private static final BigDecimal[] BPOW10 = new BigDecimal[MAX_PRECISION + 1];
    static {
        for (int i = 0; i < POW10.length; i++) {
            POW10[i] = Math.pow(10, i);
            BPOW10[i] = new BigDecimal(String.valueOf(POW10[i]));
        }
    }

    public static final BigDecimal getMaxBigDecimal() {
        return BPOW10[MAX_PRECISION];
    }

    public static int getMaxPrecision() {
        return MAX_PRECISION;
    }

    public static BigDecimal getMaxValueForPrecisionScale(int precision, int scale) {
        return BPOW10[precision - scale];
    }

    /**
     * @param n the number to test
     * @param precision the precision specified (significant digits)
     * @param scale  the scale specified
     * @return true iff the given number is within the given precision/scale settings
     */
    public static boolean exceedsNumericLimits(Number n, int precision, int scale) {
        try {
            if(n instanceof BigDecimal) {
                // W-1506306
                // Don't bother trying to round if n is an unreasonable number.
                // This can happen due to corruption or if a number with a large exponent is entered.
                final int reasonableScaleValue = 1000;
                BigDecimal d = (BigDecimal) n;
                if(Math.abs(d.scale()) > reasonableScaleValue) {
                    return true;
                }
            }
            BigDecimal bd = roundNumberToScale(n, scale);
            // value may be negative- take absolute value
            return bd.abs().compareTo(getMaxValueForPrecisionScale(precision, scale)) > -1;
        }
        catch (NumberFormatException e) {
            // This can happen, for instance, if Number is the Double value Infinity
            return true;
        }
    }

    /**
     * @param n the number to scale
     * @param scale the new scale
     * @return a BigDecimal form of the given number, with the correct number of digits to the right of the decimal point
     */
    public static final BigDecimal roundNumberToScale(Number n, int scale) {
        if(n != null) {
            BigDecimal bd = (n instanceof BigDecimal) ? (BigDecimal)n : new BigDecimal(String.valueOf(n.doubleValue()));
            return bd.setScale(scale, RoundingMode.HALF_UP);
        }
        return null;
    }
}
