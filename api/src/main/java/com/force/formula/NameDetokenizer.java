/*
 * Copyright, 2007, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula;

import java.sql.Connection;

/**
 * Convert references suitable for storage in the DB (based on primary-keys, ids, etc)
 * into references suitable for humans (apinames)
 * @author dchasman
 * @since 150
 */
public interface NameDetokenizer {
    /**
     * @param conn
     * @param reference
     * @return
     * @throws InvalidFieldReferenceException
     * @throws UnsupportedTypeException
     */
    default String fromDurableName(Connection conn, String reference) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        return fromDurableName(reference);
    }
    //TODO AJB get rid of this overload, and ensure conn is provided in all contexts.  Although only a few
    // contexts actually need it...
    public abstract String fromDurableName(String reference) throws InvalidFieldReferenceException,
    	UnsupportedTypeException;
}
