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
    
    public FormulaProperties getDefaultProperties();
    
    public boolean isTemplate();
    
    public boolean allowsEncryptedAtRestFields();
    
    public boolean allowsLegacyEncryptedFields();
    
    public boolean isFilter();
    
    public boolean allowSpanningFormulas();
    
    public boolean allowMultiEnumFields();
    
    public int getMaxTreeDepth();
    
    public String getDisplay();
    
    /**
     * @return whether this formula type supports picklists.  
     */
    public boolean allowPicklistTextConversion();
    
    /**
     * Return true if sobjectrow self referencing is allowed ("this").
     */
    public boolean allowSObjectRowReference();
    
}
