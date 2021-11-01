package com.force.formula;

import java.util.Set;

/**
 * A factory for parsing and manipulating both runtime and design time formulas 
 * Was implemented by FormulaInfoFactory
 * 
 * @author stamm
 * @since 0.0.1
 */
public interface FormulaFactory {
	FormulaCommandTypeRegistry getTypeRegistry();
	
    /**
     * Use this method to get default values for all formula properties.
     *
     * @param type the type of formula being constructed
     * @param context the context of the formula evaluation
     * @param source formula specification in textual format
     * @return Formula object
     * @throws FormulaException if an exception occurs when creating the formula
     */
    RuntimeFormulaInfo create(FormulaTypeSpec type, FormulaContext context, String source) throws FormulaException;

    /**
     * Use this method to get default values for all formula properties. Allows setting
     * of whether this is an existing formula, which effects error checking.
     *
     * @param type the type of formula being constructed
     * @param context
     *            field definition that the formula is bound to
     * @param source
     *            formula specification in textual format
     * @param existingFormula
     *            whether an existing formula is being loaded
     * @param forceDisabled
     *             whether to create the formula as a disabled formula, even if the encoded text doesn't say it's disabled
     * @return Formula object
     * @throws FormulaException if an exception occurs when creating the formula
     */
    RuntimeFormulaInfo create(FormulaTypeSpec type, FormulaContext context, String source, boolean existingFormula,
        boolean forceDisabled) throws FormulaException;


    /**
     * Use this method to get default values for all formula properties. Allows setting
     * of whether this is an existing formula, which effects error checking.
     *
     * @param type the type of formula being constructed
     * @param context
     *            field definition that the formula is bound to
     * @param source
     *            formula specification in textual format
     * @param existingFormula
     *            whether an existing formula is being loaded
     * @param forceDisabled
     *             whether to create the formula as a disabled formula, even if the encoded text doesn't say it's disabled
     * @param isCreateOrEdit
     *              whether this action is design time (edit/create) or runtime
     * @return Formula object
     * @throws FormulaException if an exception occurs when creating the formula
     */
    RuntimeFormulaInfo create(FormulaTypeSpec type, FormulaContext context, String source, boolean existingFormula,
        boolean forceDisabled, boolean isCreateOrEdit) throws FormulaException;

    /**
     * Use this method to specify values for all formula properties.
     *
     * @param context
     *            field definition that the formula is bound to
     * @param source
     *            formula specification in textual format
     * @param properties
     *            the formula properties that specify the formula creation parameters
     * @return Formula object
     * @throws FormulaException if an exception occurs when creating the formula
     */
    RuntimeFormulaInfo create(FormulaContext context, String source, FormulaProperties properties)
        throws FormulaException;

    /**
     * Return the names for objects directly referenced by this formula in encoded form by regex matching only
     * @param source the encoded source
     *
     * @return List of names.
     */
    String[] getReferencesFromEncodedSource(String source);
    
    /**
     * Return the decoded source directly w/out compiling
     * @param nameTokenizer the de-durable-namizer
     * @param source the source string stored with durable names
     *
     * @return List of names.
     * @throws FormulaException if an exception occurs when decoding the formula
     */
    default String decode(NameDetokenizer nameTokenizer, String source) throws FormulaException {
        return decode(nameTokenizer, source, null);
    }

    /**
     * Return the decoded source directly w/out compiling
     * @param nameTokenizer the de-durable-namizer
     * @param source the source string stored with durable names
     * @param properties the input properties to use when decoding (for things like null/blank semantics)
     *
     * @return List of names.
     * @throws FormulaException if an exception occurs when creating the formula
     */
    String decode(NameDetokenizer nameTokenizer, String source, FormulaProperties properties)
        throws FormulaException;

    /**
	 * Return the set of external references from the formula
     * @param source the formula source
     * @param properties the input properties to use when decoding
	 *
	 * @return List of names.
     * @throws FormulaException if an exception occurs when parsing the formula
	 */    
    Set<String> getReferences(String source, FormulaProperties properties) throws FormulaException;
}
