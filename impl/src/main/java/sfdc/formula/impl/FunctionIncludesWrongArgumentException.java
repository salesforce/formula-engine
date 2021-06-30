package sfdc.formula.impl;

import sfdc.formula.FormulaException;
import sfdc.formula.FormulaI18nUtils;

/**
 * Displays something like this: "Incorrect parameter for function VLOOKUP(). Expected Record Name field."
 *
 * @author ldelascurain
 * @since 160
 */
public class FunctionIncludesWrongArgumentException extends FormulaException {
    private static final long serialVersionUID = 1L;

	public FunctionIncludesWrongArgumentException() {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "FunctionIncludesWrongArgumentException"));
    }
}
