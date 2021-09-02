/*
 * Created on Dec 14, 2004
 */
package com.force.formula;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides access to runtime representation of a formula
 *
 * @author dchasman
 * @since 140
 */
public interface Formula extends Comparable<Formula>, Serializable {
    /**
     * Execute the formula based on field data retrieved from the provided context
     *
     * @param context
     *            data source for field references
     * @return result of executing the formula
     * @throws Exception
     */
    Object evaluate(FormulaRuntimeContext context) throws Exception;

    /**
     * Execute the formula based on field data retrieved from the provided context. Does no final processing of the
     * result so it can be used in nested computations.
     *
     * @param context
     *            data source for field references
     * @return result of executing the formula
     * @throws Exception
     */
    Object evaluateRaw(FormulaRuntimeContext context) throws Exception;

    /**
     * Use this hook to perform bulk processing before the formulas are evaluated. Useful for formula functions like
     * vlookup that perform a query for all contexts and cache the value for runtime evaluation.
     *
     * @param contexts
     * @throws Exception
     */
    void bulkProcessingBeforeEvaluation(List<FormulaRuntimeContext> contexts) throws Exception;

    /**
     * Checks whether this formula is simply a direct reference to another field, and if so, returns the field path to
     * the referenced field. Otherwise, it returns null.
     *
     * @param zeroExcluded
     *            If set, the caller is not checking against numeric 0, and this allows the direct field reference to be
     *            returned even if the numeric formula has the NVL-as-ZERO option set. N/A if this is not a numeric
     *            formula.
     * @param allowDateValue
     *            If set, this will return the directFieldPath even if there is a wrapping DATEVALUE(...) function (e.g.
     *            convert to date).
     * @param caseSafeIdUsed
     *            It will be set to true (by code inside the method) if formula uses CASESAFEID() function
     * @param namespace
     *            Namespace of a field, This is important for extension package because usually extension package
     *            namespace is on the stack, however we can find field only from the original developer namespace who
     *            delievered field.
     * @throws InvalidFieldReferenceException
     * @throws UnsupportedTypeException
     */
    List<FormulaFieldReferenceInfo> getFieldPathIfDirectReferenceToAnotherField(FormulaContext formulaContext,
            boolean zeroExcluded, boolean allowDateValue, AtomicBoolean caseSafeIdUsed, String namespace)
            throws UnsupportedTypeException, InvalidFieldReferenceException;

    /**
     * Checks whether a formula is deterministic. A formula is deterministic if it always returns the same when called
     * with the same inputs (EntityContexts in this case). If a formula is not deterministic it cannot be summarized by
     * a summary field.
     *
     * @return true if the formula is deterministic
     */
    boolean isDeterministic(FormulaContext formulaContext);

    /**
     * Checks whether a formula reference any AI Prediction Target field.
     *
     * @return true if formula references AI Prediction Target Field.
     */
    boolean hasAIPredictionFieldReference(FormulaContext formulaContext);

    /**
     * @param attribute
     *            defined in the <code>Formula</code> interface
     * @return true if the formula has this attribute
     */
    boolean hasAttribute(int attribute);

    /**
     * @return the formula's actual return type
     */
    public Type getActualReturnType();

    FormulaDataType getDataType();

    /**
     * @return the current formula converted to javascript for client side evaluation, or <tt>null</tt> if the formula
     *         is not convertable.
     */
    String toJavascript();

    /**
     * Return a javascript snippet representation of the formula. Does not include null pointer exception guard. Used in
     * building nested formulas.
     *
     * @return Javascript snippet
     */
    String getJavascriptRaw();

    /**
     * Return guard for SQL snippet capturing possible null pointer exceptions when evaluating the formula. Baked into
     * results of toJavascript. Used in building nested formulas.
     *
     * @return Javascript snippet
     */
    String getJavascriptGuard();

    /**
     * @return whether in javascript, the value of this formula could be null. This is helpful for knowing whether to
     *         guard against NPE's in javascript when using nested formulae
     */
    boolean couldJavascriptBeNull();
}
