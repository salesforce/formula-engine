package com.force.formula.v2.exception;

public class FormulaFileParseException extends RuntimeException{

    public FormulaFileParseException(Throwable ex){
        super("[FE] FormulaFileParseException: " + ex.getMessage(), ex);
    }

    public FormulaFileParseException(String message, Throwable ex){
        super("[FE] FormulaFileParseException: " + message, ex);
    }

}
