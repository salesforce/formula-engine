/*
 * Created on Dec 8, 2004
 */
package com.force.formula.impl;

import com.force.formula.*;
import com.force.formula.sql.RuntimeSqlFormulaInfo;


/**
 * Factory class for formulas.
 *
 * @author dchasman
 * @since 140
 */
public class FormulaInfoFactory {

    public static final String ENCODED_PREFIX = "ENCODED:";

    /**
     * Use this method to get default values for all formula properties.
     *
     * @param type the type of formula being constructed
     * @param context the context of the formula evaluation
     * @param source formula specification in textual format
     * @return Formula object
     * @throws FormulaException
     */
    public static RuntimeSqlFormulaInfo create(FormulaTypeSpec type, FormulaContext context, String source) throws FormulaException {
    	return (RuntimeSqlFormulaInfo) FormulaEngine.getFactory().create(type, context, source);
    }

    /**
     * Use this method to get default values for all formula properties. Allows setting
     * of whether this is an existing formula, which effects error checking.
     *
     * @param type the type of formula being constructed
     * @param context the context of the formula evaluation
     * @param source
     *            formula specification in textual format
     * @param existingFormula
     *            whether an existing formula is being loaded
     * @param forceDisabled
     *             whether to create the formula as a disabled formula, even if the encoded text doesn't say it's disabled
     * @return Formula object
     * @throws FormulaException
     */
    public static RuntimeSqlFormulaInfo create(FormulaTypeSpec type, FormulaContext context, String source, boolean existingFormula,
        boolean forceDisabled) throws FormulaException {
    	return (RuntimeSqlFormulaInfo) FormulaEngine.getFactory().create(type, context, source, existingFormula, forceDisabled);
    }


    /**
     * Use this method to get default values for all formula properties. Allows setting
     * of whether this is an existing formula, which effects error checking.
     *
     * @param type the type of formula being constructed
     * @param context the context of the formula evaluation
     * @param source
     *            formula specification in textual format
     * @param existingFormula
     *            whether an existing formula is being loaded
     * @param forceDisabled
     *             whether to create the formula as a disabled formula, even if the encoded text doesn't say it's disabled
     * @param isCreateOrEdit
     *              whether this action is design time (edit/create) or runtime
     * @return Formula object
     * @throws FormulaException
     */
    public static RuntimeSqlFormulaInfo create(FormulaTypeSpec type, FormulaContext context, String source, boolean existingFormula,
        boolean forceDisabled, boolean isCreateOrEdit) throws FormulaException {
    	return (RuntimeSqlFormulaInfo) FormulaEngine.getFactory().create(type, context, source, existingFormula, forceDisabled, isCreateOrEdit);
    }

    /**
     * Use this method to specify values for all formula properties.
     *
     * @param context the context of the formula evaluation
     * @param source
     *            formula specification in textual format
     * @param properties
     *            the generation properties of the formula
     * @return Formula object
     * @throws FormulaException
     */
    public static RuntimeSqlFormulaInfo create(FormulaContext context, String source, FormulaProperties properties)
        throws FormulaException {
    	return (RuntimeSqlFormulaInfo) FormulaEngine.getFactory().create(context, source, properties);
    }

    /**
     * Return the names for objects directly referenced by this formula in encoded form by regex matching only
     *
     * @return List of names.
     */
    public static String[] getReferencesFromEncodedSource(String source) {
        return FormulaUtils.getReferencesFromEncodedSource(source);
    }

    /**
     * Return the decoded source directly w/out compiling
     *
     * @return List of names.
     * @throws FormulaException
     */
    public static String decode(NameDetokenizer nameTokenizer, String source) throws FormulaException {
        return decode(nameTokenizer, source, null);
    }

    public static String decode(NameDetokenizer nameTokenizer, String source, FormulaProperties properties)
        throws FormulaException {
        return FormulaUtils.decode(nameTokenizer, source, properties);
    }

    public static boolean isEncoded(String formulaSource) {
        return BaseFormulaInfoImpl.isEncoded(formulaSource);
    }

    public static boolean isDisabled(String formulaSource) {
        return BaseFormulaInfoImpl.isDisabled(formulaSource);
    }

    /*
     * Determine if a string represents a formula with dynamic references (subscripts etc.)
     */
    public static boolean isDynamicReferenceExpression(String expr) {
        return expr != null ? expr.indexOf('[') >= 0 && FormulaEngine.getHooks().formulaAllowSubscripts() : false;
    }

}
