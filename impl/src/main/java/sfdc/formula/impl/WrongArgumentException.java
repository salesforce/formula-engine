package sfdc.formula.impl;

import sfdc.formula.FormulaException;
import sfdc.formula.FormulaI18nUtils;

/**
 * Displays something like this: "Incorrect parameter for function VLOOKUP()."
 *
 * @author wmacklem
 * @since 152
 */
public class WrongArgumentException extends FormulaException {
    private static final long serialVersionUID = 1L;

	public WrongArgumentException(String function) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "WrongArgumentException"+"_General", 
            WrongArgumentTypeException.getDescription(function)));
    }
    
    public WrongArgumentException(String function, String expectedInputValue, FormulaAST actual) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "WrongArgumentException", 
                WrongArgumentTypeException.getDescription(function), expectedInputValue, actual.getText()));
    }
}
