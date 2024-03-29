package com.force.formula.impl;

import java.lang.reflect.Type;

import com.force.formula.*;
import com.force.formula.util.FormulaI18nUtils;

import antlr.Token;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public class WrongArgumentTypeException extends FormulaException {

    private static final long serialVersionUID = 1L;
	public WrongArgumentTypeException(String function, Type[] expectedInputTypes, FormulaAST actual) {
        super(createErrorMessage(function, expectedInputTypes, actual));

        Token token = actual.getToken();
        if(token != null) {
            location = token.getColumn();
            text = token.getText();
            type = token.getType();
        } else {
            location = null;
            text = null;
            type = null;
        }
    }

    private static String createErrorMessage(String function, Type[] expectedInputTypes, FormulaAST actual) {
        Type actualInputType = actual.getDataType();

        StringBuffer expected = new StringBuffer();
        for (Type expectedInputType : expectedInputTypes) {
            // Avoid nonsense error messages like "Error: Incorrect parameter for function -. Expected Number, Date, DateTime, received Date"
            if (actualInputType == expectedInputType)
                continue;

            if (expected.length() > 0)
                expected.append(", ");

            expected.append(FormulaTypeUtils.getTypeLabel(expectedInputType));
        }

        String actualMessage = FormulaTypeUtils.getTypeLabel(actualInputType);

        return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "WrongArgumentTypeException",
            getDescription(function),
            expected.toString(), actualMessage);
    }

    public static String getDescription(String tokenText) {
        String label;
        if (tokenText == null || tokenText.length() == 0 || Character.isLetter(tokenText.charAt(0))) {
            label = "function";
        } else if ("[]".equals(tokenText)) {
            label = "subscript";
        } else {
            label = "operator";
        }
        return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", label, tokenText);
    }

    public WrongArgumentTypeException(String function, Type[] expectedInputTypes, FormulaAST actual, FormulaDataType columnType) {
        super(createErrorMessage(function, expectedInputTypes, columnType));

        Token token = actual.getToken();
        location = token.getColumn();
        text = token.getText();
        type = token.getType();
    }

    private static String createErrorMessage(String function, Type[] expectedInputTypes, FormulaDataType columnType) {
        StringBuffer expected = new StringBuffer();
        for (Type expectedInputType : expectedInputTypes) {
            if (expected.length() > 0)
                expected.append(", ");

            expected.append(FormulaTypeUtils.getTypeLabel(expectedInputType));
        }

        return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "WrongArgumentTypeException", function,
            expected.toString(), columnType.getLabel());
    }


    public int getLocation() {
        return location;
    }

    public String getTokenText() {
        return text;
    }

    public int getTokenType() {
        return type;
    }

    private final Integer location;
    private final String text;
    private final Integer type;
}
