package sfdc.formula.impl;

import sfdc.formula.FormulaException;
import sfdc.formula.FormulaI18nUtils;

/**
 * 
 * @author dchasman
 * @since 140
 */
public class FormulaTooLongException extends FormulaException {

    private static final long serialVersionUID = 1L;

	public FormulaTooLongException(int length, int maxLength) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "FormulaTooLongException", length, maxLength));
    }

}
