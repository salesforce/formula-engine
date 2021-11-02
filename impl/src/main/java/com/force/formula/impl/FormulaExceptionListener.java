/*
 * Copyright, 2006, SALESFORCE.com All Rights Reserved Company Confidential
 */

package com.force.formula.impl;

import com.force.formula.FormulaException;

/**
 * @author dchasman
 * @since 146
 */
public interface FormulaExceptionListener {

    String onException(Exception x) throws FormulaException;

}
