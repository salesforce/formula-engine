package com.force.formula.v2.exception;

/**
 * An exception wrapper class that extends a RuntimeException to be used through out the test framework to convert any
 * runtime exception thrown during file parsing
 */
public class FormulaFileParseException extends RuntimeException{

    /**
     * Creates an instance of FormulaFileParseException using the given throwable
     *
     * @param ex an exception that needs to be wrapped into FormulaFileParseException
     */
    public FormulaFileParseException(Throwable ex){
        super("[FE] FormulaFileParseException: " + ex.getMessage(), ex);
    }

    /**
     * Creates an instance of FormulaFileParseException using a given message and throwable
     *
     * @param message a message that needs to be a part of FormulaFileParseException
     * @param ex an exception that needs to be wrapped into FormulaFileParseException
     */
    public FormulaFileParseException(String message, Throwable ex){
        super("[FE] FormulaFileParseException: " + message, ex);
    }

}
