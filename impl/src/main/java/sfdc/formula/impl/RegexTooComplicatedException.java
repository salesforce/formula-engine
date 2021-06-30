package sfdc.formula.impl;

import sfdc.formula.FormulaException;
import sfdc.formula.FormulaI18nUtils;

public class RegexTooComplicatedException extends FormulaException {
    private static final long serialVersionUID = 1L;

	public RegexTooComplicatedException(String regex) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "RegexTooComplicatedException", regex));
    }
    
}
