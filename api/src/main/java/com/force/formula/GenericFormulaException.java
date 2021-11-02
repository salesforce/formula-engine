/*
 * Created on Dec 29, 2004
 *
 */
package com.force.formula;

/**
 * A FormulaException that can handle any error message.
 *
 * @author wmacklem
 * @since 154
 */
public class GenericFormulaException extends FormulaException {
	private static final long serialVersionUID = 1L;

    public GenericFormulaException(String message) {
        super(message);
    }
    
    public GenericFormulaException(Throwable t) {
        super(t);
    }
}
