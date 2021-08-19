package com.force.formula.impl;

import com.force.formula.FormulaException;
import com.force.formula.util.FormulaI18nUtils;

/**
 *
 * @author djacobs
 * @since 140
 */
public class IllegalArgumentTypeException extends FormulaException {

    private static final long serialVersionUID = 1L;

	public IllegalArgumentTypeException(String function) {
        super(createErrorMessage(function));
    }

    private static String createErrorMessage(String function) {
        return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "IllegalArgumentTypeException", 
            WrongArgumentTypeException.getDescription(function));
    }

}
