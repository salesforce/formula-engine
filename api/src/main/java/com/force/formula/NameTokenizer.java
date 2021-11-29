/*
 * Copyright, 2007, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula;

/**
 * Convert references suitable for humans (apinames) into references suitable
 * for storage in the DB (based on primary-keys, ids, etc)
 * @author dchasman
 * @since 150
 */
public interface NameTokenizer {
	/**
	 * @return the durable name corresponding to the name.  (i.e. the key for the field instead of the __c name)
	 * @param name the apiname/human name for the field
	 * @throws InvalidFieldReferenceException if the name cannot be looked up
	 * @throws UnsupportedTypeException 
	 */
    String toDurableName(String name) throws InvalidFieldReferenceException, UnsupportedTypeException;
}
