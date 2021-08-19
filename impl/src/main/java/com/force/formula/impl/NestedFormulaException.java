package com.force.formula.impl;

import com.force.formula.FormulaException;
import com.force.formula.util.FormulaI18nUtils;

public class NestedFormulaException extends FormulaException {
    private static final long serialVersionUID = 1L;

	public NestedFormulaException(Throwable t, String fieldName) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "NestedFormulaException", fieldName, t
            .getMessage()));
    }
}
