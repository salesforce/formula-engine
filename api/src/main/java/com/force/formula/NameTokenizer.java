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
    public abstract String toDurableName(String name) throws InvalidFieldReferenceException, UnsupportedTypeException;
}
