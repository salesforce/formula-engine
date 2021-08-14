/*
 * Created on Dec 8, 2004 
 */
package com.force.formula;

/**
 * Some fields are not allowed in certain functions.  For instance, you cannot use a related object fields in
 * the ISCHANGED() or PRIORVALUE() functions.
 *
 * @author wmacklem
 * @since 148
 */
public class InvalidFieldReferenceForFunctionException extends FormulaException {
	private static final long serialVersionUID = 1L;
    public InvalidFieldReferenceForFunctionException(String fieldName, String functionName) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "InvalidFieldReferenceForFunctionException", functionName, fieldName));
    }
}
