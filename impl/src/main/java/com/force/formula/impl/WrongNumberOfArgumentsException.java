package com.force.formula.impl;

import com.force.formula.FormulaException;
import com.force.formula.FormulaI18nUtils;

import antlr.Token;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public class WrongNumberOfArgumentsException extends FormulaException {

    private static final long serialVersionUID = 1L;
	public WrongNumberOfArgumentsException(String function, int expected, FormulaAST actual) {
        super(createErrorMessage(function, expected, actual));

        Token token = actual.getToken();
        column = token.getColumn();
        line = token.getLine();
        text = token.getText();
        type = token.getType();
    }

    private static String createErrorMessage(String function, int expected, FormulaAST actual) {
        return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "WrongNumberOfArgumentsException", 
            WrongArgumentTypeException.getDescription(function),
            String.valueOf(expected), String.valueOf(actual.getNumberOfChildren()));
    }

    public int getColumn() {
        return column;
    }

    public int getLine() {
        return line;
    }

    public String getTokenText() {
        return text;
    }

    public int getTokenType() {
        return type;
    }

    private final int line;
    private final int column;
    private final String text;
    private final int type;
}
