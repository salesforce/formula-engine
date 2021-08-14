/*
 * Copyright, 2006, SALESFORCE.com All Rights Reserved Company Confidential
 */

package com.force.formula.impl;

/**
 * @author dchasman
 * @since 146
 */
public interface FormulaExceptionListerner {

    String onException(Exception x) throws Exception;

}
