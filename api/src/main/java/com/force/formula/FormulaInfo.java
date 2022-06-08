/*
 * Copyright, 2006, SALESFORCE.com All Rights Reserved Company Confidential
 */
package com.force.formula;

import java.util.regex.Pattern;

/**
 * Provides access to the metadata describing a formula
 *
 * @author dchasman
 * @param <T> the formula field type associated with this Formula
 * @since 140
 */
public interface FormulaInfo<T extends FormulaFieldInfo> {
    int MAX_FORMULA_LENGTH = 3900;
    int MAX_STORAGE_SIZE = 4000;
    int MAX_SQL_SIZE_NEW_FORMULA = 5000;
    int MAX_SQL_SIZE_LIMIT = 15000;  // Oracle's limit is 400K on a statement, so 100K should be enough
    int MAX_STRING_VALUE_LENGTH = 1300;
    /** The maximum JavaScript size for formulas that generate JS. */
    int MAX_JS_SIZE = 5000;
    /** The JavaScript string size that halts JS generation if passed. */
    int MAX_JS_SIZE_HARD_LIMIT = 15000;

    Pattern ENCODED_REFERENCE_PATTERN = Pattern.compile("\\{!ID:([^}]+)\\}");

    String FIELD_NAME_OVERRIDE = "common.config.field.FIELD_NAME_OVERRIDE";

    /**
     * Return the FormulaFieldInfo(s) for fields directly referenced by this formula
     *
     * @return List of formula field infos referenced from this formula.
     * @throws FormulaException if an exception occurs while parsing the formula
     */
    T[] getReferences() throws FormulaException;

    /**
     * Return the source (contains field name using merge field syntax)
     *
     * @return formula source.
     * @throws FormulaException if an exception occurs while processing the formula
     */
    String getSource() throws FormulaException;

    /**
     * Return the encoded source (field names replaced w/ durable ids)
     *
     * @return encoded source.
     * @throws FormulaException if an exception occurs while processing the formula
     */
    String encode() throws FormulaException;

    /**
     *
     * @return FormulaProperties for this FormulaInfo
     */
    FormulaProperties getProperties();

    /**
     *
     * @return Size of the generated SQL
     */
    int getSQLSize();

    /**
     *
     * @return Size of the generated SQL for the guard
     */
    int getGuardSQLSize();

    /**
     * Checks whether a formula is deterministic. A formula is deterministic if it always returns the same when called with the same inputs (EntityContexts in this case).
     * If a formula is not deterministic it cannot be summarized by a summary field.
     *
     * @return true if the formula is deterministic
     */
    boolean isDeterministic();

    /**
     * Checks whether a formula references any AI Prediction Target Field.
     * @return true if the formula references AI Prediction Target Field.
     */
    boolean hasAIPredictionFieldReference();

    boolean referenceEncryptedFields();

    /**
     * Check that all merge field referenced directly (in the formula itself) or indirectly (via
     * a formula field reference inside of this formula) are available for this FormulaType.
     *
     * If a merge field is invalid, throw a FormulaExcpetion
     * @throws FormulaException if a merge field is invalid
     */
    void validateMergeFieldsForFormulaType() throws FormulaException;

    /**
     * @return if this formula contains the FormulaCurrency command
     */
    boolean hasFormatCurrencyCommand();
}
