/*
 * Copyright, 2007, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula;

import java.sql.Connection;

/**
 * @author dchasman
 * @since 150
 */
public interface NameDetokenizer {
    public abstract String fromDurableName(Connection conn, String reference) throws InvalidFieldReferenceException,
        UnsupportedTypeException;
    //TODO AJB get rid of this overload, and ensure conn is provided in all contexts.  Although only a few
    // contexts actually need it...
    public abstract String fromDurableName(String reference) throws InvalidFieldReferenceException,
    	UnsupportedTypeException;
}
