package com.force.formula.impl;

import java.math.BigDecimal;
import java.util.function.Function;

import com.google.common.collect.Range;

/**
 * Manage the set of trigonometric functions supported and what ranges they have
 * @author stamm
 * @since 0.2
 */
public enum FormulaTrigFunction {
    SIN("SIN","sin",(arg)->BigDecimal.valueOf(Math.sin(arg.doubleValue()))),
    COS("COS","cos",(arg)->BigDecimal.valueOf(Math.cos(arg.doubleValue()))),
    TAN("TAN","tan",(arg)->BigDecimal.valueOf(Math.tan(arg.doubleValue()))),
    ASIN("ASIN","asin",(arg)->BigDecimal.valueOf(Math.asin(arg.doubleValue()))),
    ACOS("ACOS", "acos",(arg)->BigDecimal.valueOf(Math.acos(arg.doubleValue()))),
    ATAN("ATAN", "atan",(arg)->BigDecimal.valueOf(Math.atan(arg.doubleValue()))),
    ;

    private final String sql;
    private final String js;
    private final Function<BigDecimal,BigDecimal> func;
    
    FormulaTrigFunction(String sqlFunction, String javascriptFunction, Function<BigDecimal,BigDecimal> javaFunction) {
        this.sql = sqlFunction;
        this.js = javascriptFunction;
        this.func = javaFunction;
    }
    
    /**
     * @return the trigonometric function to use is SQL
     */
    public String getSqlFunction() {
        return this.sql;
    }

    /**
     * @return the trigonometric function to use with the Math or Decimal package in javascript
     */
    public String getJavascriptFunction() {
        return this.js;
    }
    
    /**
     * @return a function that converts the input (in radians) to the output of the trigonometric function
     */
    public Function<BigDecimal,BigDecimal> getJavaFunction() {
        return this.func;
    }
    
    // Range from -1 to 1, suitable for ArcSine and ArcCosine
    static Range<BigDecimal> NEG_ONE_TO_ONE = Range.closed(BigDecimal.ONE.negate(), BigDecimal.ONE);
    
    /**
     * @return the range if the input must be between two values, or null if any value is allowed
     */
    public Range<BigDecimal> getRange() {
        switch (this) {
        case ASIN:
        case ACOS:
            return NEG_ONE_TO_ONE;
        default:
        }
        return null;
    }
}
