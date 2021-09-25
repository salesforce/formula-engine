package com.force.formula;

import com.force.formula.util.FormulaI18nUtils;

/**
 * Used when global properties is set to require a field value for all field references
 *
 * @author rstevens 
 * @since 198
 */
public class MissingFieldValueException extends InvalidFieldReferenceException {
	private static final long serialVersionUID = 1L;

	public MissingFieldValueException(String fieldName) {
        this(fieldName, (String) null);
    }

    public MissingFieldValueException(String fieldName, String reason) {
        super(fieldName, reason, FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages_runtime", "MissingFieldValueException", fieldName));
    }

	public MissingFieldValueException(String fieldName, Throwable t) {
        super(fieldName, null, FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages_runtime", "MissingFieldValueException", fieldName), t);
    }


}
