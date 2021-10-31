/*
 * Copyright, 1999-2010, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import com.force.formula.*;
import com.force.formula.util.FormulaI18nUtils;

/**
 * Used to report runtime type errors in formulas with dynamic references
 *
 * @author aballard
 * @since 166
 */
public class FormulaRuntimeTypeException extends FormulaEvaluationException {

    private static final long serialVersionUID = 1L;

	/**
     * @param function
     */
    public FormulaRuntimeTypeException(String function) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "IllegalArgumentTypeException",
            WrongArgumentTypeException.getDescription(function)));
    }

    public FormulaRuntimeTypeException(String function, Class<?> expectedType, Object actualValue) {
        super(createErrorMessage(function, expectedType, actualValue));
    }

    private static String createErrorMessage(String function, Class<?> expected, Object actualValue) {
        String actualName;
        actualName = FormulaEngine.getHooks().getTypeNameForErrorMessage(actualValue);
        if (actualName == null) {
            actualName = getFormulaTypeName(actualValue.getClass());
        }
        String expectedName= getFormulaTypeName(expected);
        return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "WrongArgumentTypeException",
            WrongArgumentTypeException.getDescription(function), expectedName, actualName);

    }

    public static String getFormulaTypeName(Class<?> clazz) {
        String typeName= FormulaI18nUtils.getLocalizer().getLabelNoThrow("FormulaFieldExceptionDataTypes", clazz.getName());
        if (typeName == null ) {
            // something really unexpected... use the java class name.
            typeName = clazz.getName();
        }
        return typeName;
    }



}
