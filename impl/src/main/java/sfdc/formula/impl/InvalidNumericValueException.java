/*
 * Copyright, 1999-2010, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package sfdc.formula.impl;

import com.force.i18n.commons.text.TextUtil;

import sfdc.formula.FormulaEvaluationException;
import sfdc.formula.FormulaI18nUtils;

/**
 * Exception class for numeric conversion during formula evaluation
 *
 * @author aballard
 * @since 170
 */
public class InvalidNumericValueException extends FormulaEvaluationException {

    private static final long serialVersionUID = 1L;

	/**
     * @param function
     */
    public InvalidNumericValueException(String value) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "InvalidNumericValueException", TextUtil.escapeToHtml(value)));
    }

    /**
     * For operator-based runtime exceptions, eg "<"
     *
     * @param value
     * @param operator
     */
    public InvalidNumericValueException(String value, String operator) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "InvalidValueException_ForOperator", TextUtil.escapeToHtml(value), operator));
    }
}
