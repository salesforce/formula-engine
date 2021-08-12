/*
 * Copyright, 1999-2013, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula;

/**
 * Represents a kind of formula we use in the system.
 * This is used when determining how it should be retreived from the cache.
 * Implementations of this interface are in the enum FormulaType; they also
 * provide info on which context to use when validating a formula
 *
 * @author aroyfaderman
 * @since 186
 */
public interface FormulaTypeSpec {
    /**
     * @return the maximum length of the field containing the encoded value
     */
    public int getMaxLength();
    
    public String getDisplay();
    
    public FormulaProperties getDefaultProperties();
    
    default boolean isTemplate() {
        return false;
    }
   
    default boolean allowsLegacyEncryptedFields() {
        return false;
    }
    
    default boolean allowSpanningFormulas() {
        return false;
    }
    
    // Max depth of the AST tree
    default int getMaxTreeDepth() {
        return 10;
    }
           
    /**
     * @return whether this formula type supports picklists.  
     */
    default boolean allowPicklistTextConversion() {
        return false;
    }
    
    /**
     * Return true if sobjectrow self referencing is allowed ("this").
     */
    default boolean allowSObjectRowReference() {
        return false;
    }
    
}
