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
     * @param conn the DB connection that can be used.  Very options
     * @param reference the name of the reference that is durable (i.e. external ID)
     * @return the name of the reference that isn't durable (i.e. internal ID)
     * @throws InvalidFieldReferenceException the the field reference is illegal
     * @throws UnsupportedTypeException if the field reference is of an unsupported type
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
