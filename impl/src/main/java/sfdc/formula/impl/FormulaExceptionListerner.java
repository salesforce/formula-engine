/*
 * Copyright, 2006, SALESFORCE.com All Rights Reserved Company Confidential
 */

package sfdc.formula.impl;

/**
 * @author dchasman
 * @since 146
 */
public interface FormulaExceptionListerner {

    String onException(Exception x) throws Exception;

}
