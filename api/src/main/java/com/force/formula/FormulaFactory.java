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
     * @throws FormulaException
     */
    RuntimeFormulaInfo create(FormulaTypeSpec type, FormulaContext context, String source) throws FormulaException;

    /**
     * Use this method to get default values for all formula properties. Allows setting
     * of whether this is an existing formula, which effects error checking.
     *
     * @param fieldInfo
     *            field definition that the formula is bound to
     * @param source
     *            formula specification in textual format
     * @param existingFormula
     *            whether an existing formula is being loaded
     * @param forceDisabled
     *             whether to create the formula as a disabled formula, even if the encoded text doesn't say it's disabled
     * @return Formula object
     * @throws FormulaException
     */
    RuntimeFormulaInfo create(FormulaTypeSpec type, FormulaContext context, String source, boolean existingFormula,
        boolean forceDisabled) throws FormulaException;


    /**
     * Use this method to get default values for all formula properties. Allows setting
     * of whether this is an existing formula, which effects error checking.
     *
     * @param fieldInfo
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
     * @throws FormulaException
     */
    RuntimeFormulaInfo create(FormulaTypeSpec type, FormulaContext context, String source, boolean existingFormula,
        boolean forceDisabled, boolean isCreateOrEdit) throws FormulaException;

    /**
     * Use this method to specify values for all formula properties.
     *
     * @param fieldInfo
     *            field definition that the formula is bound to
     * @param source
     *            formula specification in textual format
     * @param generateSQL
     *            flag to control SQL (bulk) evaluator content generation
     * @return Formula object
     * @throws FormulaException
     */
    RuntimeFormulaInfo create(FormulaContext context, String source, FormulaProperties properties)
        throws FormulaException;

    /**
     * Return the names for objects directly referenced by this formula in encoded form by regex matching only
     *
     * @return List of names.
     * @throws FormulaException
     */
    String[] getReferencesFromEncodedSource(String source);
    
    /**
     * Return the decoded source directly w/out compiling
     *
     * @return List of names.
     * @throws FormulaException
     */
    default String decode(NameDetokenizer nameTokenizer, String source) throws FormulaException {
        return decode(nameTokenizer, source, null);
    }

    /**
     * Return the decoded source directly w/out compiling
     *
     * @return List of names.
     * @throws FormulaException
     */
    String decode(NameDetokenizer nameTokenizer, String source, FormulaProperties properties)
        throws FormulaException;

    /**
	 * Return the set of external references from the formula
	 *
	 * @return List of names.
	 * @throws FormulaException
	 */    
    Set<String> getReferences(String source, FormulaProperties properties) throws FormulaException;
}
