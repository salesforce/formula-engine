/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Represents a javascript value. Because Javascript is fairly loose with null
 * checks for operators and type checking, in order to maintain parity with SQL,
 * we need to be rather vigilant about checking for nulls.
 *
 * To prevent null pointer checks from exploding the javascript size, especially
 * around decimal math, we want to consolidate all the checks "up front" and
 * remove the need for values that could be null. Various operators can "eat"
 * these guards depending on the Null behavior desired (e.g. IF(null,a,b)
 * returns b)
 *
 * @author stamm
 * @since 0.2
 */
public class JsValue {
    public final String js;
    public final String guard;
    public final boolean couldBeNull;

    public static final JsValue NULL = new JsValue("null", null, true);

    // Assume the value isn't null
    public JsValue(String js, String guard) {
        this(js, guard, true);
    }

    public JsValue(String js, String guard, boolean couldBeNull) {
        this.js = js;
        this.guard = guard;
        this.couldBeNull = couldBeNull;
    }

    /**
     * Generate a new JSValue for the given expression, assuming that you cannot
     * return null if the argument is not null.
     * 
     * @param expression
     * @param values      the expressions coming in to guard against NPEs
     * @param nullCheck   additions to the guard.
     * @param additions   the values that you want to validate can't be null to
     *                    prevent NPEs
     * @param couldBeNull whether the expression could be null.
     * @return
     */
    public static final JsValue forNonNullResult(String expression, JsValue[] values) {
        return generate(expression, values, false, values);
    }

    /**
     * Generate a new JSValue for the given expression, adding in the guards from
     * the value, and validating that the argument provided in additions are not
     * null. Use this if you want to guard against null for only certain arguments,
     * and not all of them.
     * 
     * @param expression
     * @param values      the expressions coming in to guard against NPEs
     * @param nullCheck   additions to the guard.
     * @param additions   the values that you want to validate can't be null to
     *                    prevent NPEs
     * @param couldBeNull whether the expression could be null.
     * @return
     */
    public static final JsValue generate(String expression, JsValue[] values, boolean couldBeNull,
            JsValue... additions) {
        String additionsStr = makeArgumentGuard(additions);
        return generate(expression, values, additionsStr, couldBeNull);
    }

    /**
     * Generate a new JSValue for the given expression, adding in the guards from
     * the value
     *
     * Use this if you have something that returns a boolean expression or if you
     * know it won't be null (like the function IsNull)
     * 
     * @param expression
     * @param values      the expressions coming in to guard against NPEs
     * @param nullCheck   additions to the guard.
     * @param additions   checks to add after the guards for the values.
     * @param couldBeNull whether the expression could be null.
     * @return
     */
    public static final JsValue generate(String expression, JsValue[] values, String additions, boolean couldBeNull) {
        // TODO: Simplify this so that if the guard is already in the guard, you don't
        // add it from additions.
        String guard = makeGuard(values);
        if (additions != null && additions.length() > 0) {
            guard = guard != null ? guard + "&&" + additions : additions;
        }
        return new JsValue(expression, guard, couldBeNull);
    }

    /**
     * Make a combined guard of all the guards in the values (this is if you are
     * fine if the values or null, you just don't want to dereference error)
     * 
     * @param values the values to retrieve the guards from
     * @return the boolean expression to guard against NPE for each of the values,
     *         or null if not needed
     */
    public static final String makeGuard(JsValue[] values) {
        String guard = null;
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].guard == null)
                    continue;
                if (guard == null)
                    guard = values[i].guard;
                else if (!values[i].guard.equals(guard)) // This "helps" optimize but doesn't do everything.
                    guard = values[i].guard + "&&" + guard;
            }
        }
        return guard;
    }

    /**
     * Make a combined guard of all the arguments in the values that could be null.
     * Use this if you want to handle tri-state boolean logic in javascript (see
     * OperatorComparison)
     * 
     * @param values the values to guard against NPE with
     * @return the boolean expression to guard against NPE for each of the values,
     *         or null if not needed
     */
    public static final String makeArgumentGuard(JsValue[] values) {
        if (values == null || values.length == 0)
            return null;
        String result = Arrays.asList(values).stream().filter((a) -> a.couldBeNull).map((a) -> a.js + "!=null")
                .collect(Collectors.joining("&&"));
        return result.length() > 0 ? result : null;
    }

    @Override
    public String toString() {
        return js; // TODO: This is to keep it simple when doing appends.
    }

    public String buildJSWithGuard() {
        return ((guard != null) && (!guard.isEmpty())) ? "(" + guard + ")?(" + js + "):null" : js;
    }
}
