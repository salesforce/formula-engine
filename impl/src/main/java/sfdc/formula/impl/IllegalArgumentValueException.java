package sfdc.formula.impl;

import sfdc.formula.FormulaException;
import sfdc.formula.FormulaI18nUtils;

/**
 * Please use this exception when any unacceptable values are found in the formula function parameters, instead of
 * creating Function specific exception classes. This is a generic exception class which can be used for any formula
 * function.
 * 
 * @author rbakthavachalam
 * @since 206
 */
public class IllegalArgumentValueException extends FormulaException {

    private static final long serialVersionUID = 1L;

	public IllegalArgumentValueException(String message) {
        super(message);
    }

    /**
     * Constructor to accept function name and function specific error message key
     * 
     * @param function
     * @param functionSpecificMessageKey
     */
    public IllegalArgumentValueException(String function, String functionSpecificMessageKey) {
        super(createFunctionSpecificErrorMessage(function, functionSpecificMessageKey));
    }

    /**
     * Forms the error message using both function name and the kind of error on that function value
     * 
     * @param function
     * @param functionSpecificMessageKey
     * @return Error message
     */
    private static String createFunctionSpecificErrorMessage(String function, String functionSpecificMessageKey) {
        return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages",
                new StringBuilder().append("IllegalArgumentValueException").append("_")
                        .append(functionSpecificMessageKey).toString(),
                WrongArgumentTypeException.getDescription(function));
    }

}
