/*
 * Created on Dec 8, 2004 
 */
package sfdc.formula.impl;

import sfdc.formula.FormulaException;
import sfdc.formula.FormulaI18nUtils;

/**
 * Thrown when a reference is encountered to an unknown  function
 * 
 * @author dchasman
 * @since 144
 */
public class InvalidFunctionReferenceException extends FormulaException {

    private static final long serialVersionUID = 1L;
	public InvalidFunctionReferenceException(String function, int location) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "InvalidFunctionReferenceException", function));

        this.function = function;
        this.location = location;
    }

    public String getFunction() {
        return function;
    }

    public int getlocation() {
        return location;
    }
    
    private final String function;
    private final int location;
}
