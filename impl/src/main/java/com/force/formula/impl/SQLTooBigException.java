/*
 * Created on Mar 17, 2005
 *
 */
package com.force.formula.impl;

import com.force.formula.FormulaException;
import com.force.formula.FormulaI18nUtils;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 138
 */
public class SQLTooBigException extends FormulaException {
    private static final long serialVersionUID = 1L;

	public SQLTooBigException(int length, int maxLength) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "SQLTooBigException", length, maxLength));
    }
}
