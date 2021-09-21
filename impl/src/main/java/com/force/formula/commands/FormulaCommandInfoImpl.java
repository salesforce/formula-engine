package com.force.formula.commands;

import java.lang.reflect.Type;
import java.util.Arrays;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.*;
import com.force.formula.util.FormulaTextUtil;

/**
 * These objects are shared per command type. No mutable instance-level data is allowed in the object, and there's an
 * ftest to ensure that.
 *
 * @author dchasman
 * @since 140
 */
@AllowedContext(section = SelectorSection.MATH)
public abstract class FormulaCommandInfoImpl implements FormulaCommandInfo {

    private final String name;
    private final Type returnType;
    private final Type[] argumentTypes;

    public FormulaCommandInfoImpl(String name, Type returnType, Type[] argumentTypes) {
        assert argumentTypes != null;

        this.name = name;
        this.returnType = returnType;
        this.argumentTypes = argumentTypes;
    }

    public FormulaCommandInfoImpl(String name) {
        this(name, null, new Class[0]);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Type getReturnType(FormulaAST node, FormulaContext context) throws FormulaException {
        assert this.returnType != null;
        return this.returnType;
    }

    @Override
    public Type[] getArgumentTypes(FormulaAST node, FormulaContext context) throws FormulaException {
        return this.argumentTypes;
    }

    public static boolean isNull(Object o) {
        if (o == null) { return true; }

        // Unwrap FormulaDateTime and check contained Date for null
        if (o instanceof FormulaDateTime) {
            FormulaDateTime formulaDateTime = (FormulaDateTime)o;
            return (formulaDateTime.getDate() == null);
        }

        return false;
    }

    /**
     * Helpful function for generating a NVL for use in javascript
     * 
     * @param value
     *            the value to test, and return if not null
     * @param ifNull
     *            the default value to return if test is null.
     * @return a javascript string that will return ifNull if value is null
     */
    public static String jsNvl(String value, String ifNull) {
        // If we were given null, then return ifNull
        if (value == null || value.equalsIgnoreCase("null")) {
            // return "(ifNull)"
            return new StringBuilder("(").append(ifNull).append(")").toString();
        }

        if (isNeverNull(value)) {
            // return "(value)"
            return new StringBuilder("(").append(value).append(")").toString();
        }

        // return "$F.nvl(value, ifNull)"
        return new StringBuilder("$F.nvl(").append(value).append(",").append(ifNull).append(")").toString();
    }

    /**
     * Helpful function for generating a NOE for use in javascript
     * 
     * @param value
     *            the value to test, and return if (not null nor empty)
     * @param ifNull
     *            the default value to return if test is (null or empty).
     * @return a javascript string that will return ifNull if value is null
     */
    public static String jsNoe(String value, String ifNull) {
        // If we were given null, then return ifNull
        if (value == null || value.equalsIgnoreCase("null") || (value != null && FormulaTextUtil.isEmptyOrWhitespace(value))) {
            // return "(ifNull)"
            return new StringBuilder("(").append(ifNull).append(")").toString();
        }

        if (isNeverNull(value)) {
            // return "(value)"
            return new StringBuilder("(").append(value).append(")").toString();
        }

        // return "$F.nvl(value, ifNull)"
        return new StringBuilder("$F.noe(").append(value).append(",").append(ifNull).append(")").toString();
    }

    /**
     * Helpful function for generating a NVL2 for use in javascript
     * 
     * @param test
     *            the value to test
     * @param value
     *            the value to return if test is not null
     * @param ifNull
     *            the default value to return
     * @return a javascript string that will return ifNull if test is null
     */
    public static String jsNvl2(JsValue test, String value, String ifNull) {
        return jsNvl2WithGuard(test, value, ifNull, false);
    }

    /**
     * Helpful function for generating a NVL2 for use in javascript, where you may want to include the guard as one of
     * the tests to "swallow" it whole. Used to map the fact that LEN() in sql & java returns 0 for nul.
     * 
     * @param test
     *            the value to test
     * @param value
     *            the value to return if test is not null
     * @param ifNull
     *            the default value to return
     * @Param withGuard should the guard be included in the test.
     * @return a javascript string that will return ifNull if test is null
     */
    public static String jsNvl2WithGuard(JsValue test, String value, String ifNull, boolean withGuard) {
        // If we were given null, then return ifNull
        if (test == null || test.js.equalsIgnoreCase("null")) {
            // return "(ifNull)"
            return new StringBuilder("(").append(ifNull).append(")").toString();
        }

        if (!test.couldBeNull) {
            // return "(value)"
            return new StringBuilder("(").append(value).append(")").toString();
        }

        // return "($F.anl([test])?ifNull:value)"
        String guard = test.js;
        if (withGuard && test.guard != null) {
            guard = test.guard + "&&" + guard;
        }
        return new StringBuilder("($F.anl([").append(guard).append("])?").append(ifNull).append(":").append(value)
                .append(")").toString();
    }

    // Simplify the javascript stuff that won't ever return null
    private static boolean isNeverNull(String test) {
        assert test != null;
        return test.startsWith("Number(") // Number conversion won't be null
                || test.startsWith("new ") // new object isn't null
                || test.startsWith("Date.") // new date isn't null
                || test.startsWith("\"") // string constants
                || test.startsWith("'") || FormulaTextUtil.isNumeric(test) // numeric constant
                || "true".equals(test) // boolean constants
                || "false".equals(test);
    }

    /**
     * Helper method for high precision javascript
     * 
     * @return whether to use the "Decimal" or "Math" packages for math functions
     */
    static String jsMathPkg(FormulaContext context) {
		return context.useHighPrecisionJs() ? "$F.Decimal" : "Math";
    }

    /**
     * Convert the value *to* a high precision decimal from a javascript number
     */
    static String jsToDec(FormulaContext context, String val) {
        return context.useHighPrecisionJs() ? "(new $F.Decimal(" + val + "))" : val;
    }

    /**
     * Convert the value to a javascript number from a high precision decimal (for use in things like dates)
     * 
     * @param context
     * @param val
     * @return
     */
    static String jsToNum(FormulaContext context, String val) {
        return context.useHighPrecisionJs() ? val + ".toNumber()" : val;
    }

    // TODO: REMOVE once TableAliasRegistry is moved around
    @Override
    public final SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards,
            ITableAliasRegistry registry) throws FormulaException {
        return getSQL(node, context, args, guards, (TableAliasRegistry)registry);
    }

    public abstract SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards,
            TableAliasRegistry registry) throws FormulaException;

    protected FormulaCommandType.AllowedContext getDefaultContext() {
        return FormulaCommandInfoImpl.class.getAnnotation(FormulaCommandType.AllowedContext.class);
    }

    @Override
    public FormulaCommandType.AllowedContext getAllowedContext() {
        FormulaCommandType.AllowedContext context = getClass().getAnnotation(FormulaCommandType.AllowedContext.class);
        return context == null ? getDefaultContext() : context;
    }

    @Override
    public String toString() {
        return "FormulaCommandInfoImpl [name=" + this.name + ", returnType=" + this.returnType + ", argumentTypes="
                + Arrays.toString(this.argumentTypes) + ", getAllowedContext()=" + this.getAllowedContext() + "]";
    }
    
    protected static final FormulaValidationHooks hooks() {
    	return FormulaValidationHooks.get();
    }

    protected static final FormulaSqlHooks getSqlHooks(FormulaContext context) {
    	return (FormulaSqlHooks) context.getSqlStyle();
    }

}
