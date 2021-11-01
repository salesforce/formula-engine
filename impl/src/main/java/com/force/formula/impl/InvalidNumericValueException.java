/*
 * Copyright, 1999-2010, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import com.force.formula.FormulaEvaluationException;
import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.commons.text.TextUtil;

/**
 * Exception class for numeric conversion during formula evaluation
 *
 * @author aballard
 * @since 170
 */
public class InvalidNumericValueException extends FormulaEvaluationException {

    private static final long serialVersionUID = 1L;

	/**
     * @param value the invalid value
     */
    public InvalidNumericValueException(String value) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "InvalidNumericValueException", TextUtil.escapeToHtml(value)));
    }

    /**
     * For operator-based runtime exceptions, eg "&lt;"
     *
     * @param value the value that wasn't a number
     * @param operator the expression used while evaluating the number
     */
    public InvalidNumericValueException(String value, String operator) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "InvalidValueException_ForOperator", TextUtil.escapeToHtml(value), operator));
    }
}
