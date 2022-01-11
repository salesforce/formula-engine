package com.force.formula.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.force.formula.FormulaEngine;
import com.force.formula.FormulaEvaluationException;
import com.force.formula.FormulaInfo;
import com.force.formula.FormulaReturnType;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.FormulaTooLongException;
import com.force.i18n.BaseLocalizer;
import com.force.i18n.LabelReference;
import com.force.i18n.Renameable;
import com.force.i18n.grammar.GrammaticalLocalizer;
import com.force.i18n.settings.SettingsSectionNotFoundException;
import com.google.common.base.Joiner;

/**
 * Class with some sample Internationalization and Localization Utilities
 *
 * @author stamm
 * @since 146
 */
public enum FormulaI18nUtils {
    INSTANCE;  // a modern singleton

    public static GrammaticalLocalizer getLocalizer() {
    	return (GrammaticalLocalizer) FormulaEngine.getHooks().getLocalizer();
    }
    
    /**
     * Render the given label reference.
     * @param labelRef the label reference returend from LABEL()
     * @param nouns optional set of entities that can be used to render renameable nouns.
     * @return a string representing the label reference for the current localizer's language
     */
    public static String renderLabelReference(LabelReference labelRef, Renameable... nouns) {
        try {
            BaseLocalizer localizer = FormulaEngine.getHooks().getLocalizer();
            if (nouns != null && nouns.length > 0) {
                return ((GrammaticalLocalizer)localizer).getLabel(labelRef.getSection(), nouns, labelRef.getKey(), labelRef.getArguments());
            }
            return localizer.getLabel(labelRef.getSection(), labelRef.getKey(), labelRef.getArguments());
        } catch (SettingsSectionNotFoundException ex) {
            throw new FormulaEvaluationException(ex);
        }
    }
    
    /**
     * Format the result based on the information in the FormulaReturnType.
     * Mostly around scale of the numbers and making sure things that are suppoed
     * to be strings are actually strings.
     * @param context the context to used to evaluate the result
     * @param formulaFieldInfo the return type to use to format the result
     * @param result the raw result of the formula evaluation
     * @return the raw value formatted appropriately to the return type
     */
    public static Object formatResult(FormulaRuntimeContext context, FormulaReturnType formulaFieldInfo,
            Object result) {
        if (result != null) {
            if (formulaFieldInfo.getDataType().isSimpleText()) {
                // Truncate strings to max string length
                String stringValue;
                if (result instanceof String) {
                    stringValue = (String)result;
                } else if (result instanceof Object[]) {
                    stringValue = Joiner.on("").useForNull("null").join((Object[])result);
                } else if (result instanceof LabelReference) {
                    stringValue = renderLabelReference((LabelReference)result);
                } else {
                	// This is an error. A non-text value is being treated as text.  However, instead of throwing an exception
                	// allow the caller to manage it.
                	return result;
                }

                result = stringValue.substring(0, Math.min(stringValue.length(), FormulaInfo.MAX_STRING_VALUE_LENGTH));
            } else if (formulaFieldInfo.getDataType().isInteger()) {
                result = new BigDecimal(new BigInteger(result.toString()));
            } else if (formulaFieldInfo.getDataType().isPercent()) {
                // Set the scale as determined by the field
                // Multiply percent field by 100 for display
                result = BigDecimalHelper.round(((BigDecimal)result).movePointRight(2), formulaFieldInfo.getScale());
            } else if (formulaFieldInfo.getDataType().isDecimal()) {
                final int scale = ((BigDecimal)result).scale();
                final int MAX_FORMULA_LENGTH = 3900;
                // Prevent evaluating numbers with negative scales that are beyond the max formula length,
                // as this evaluation can be extremely CPU intensive and slow
                if (scale < 0 && Math.abs(scale) > MAX_FORMULA_LENGTH) {
                    throw new FormulaEvaluationException(new FormulaTooLongException(scale, MAX_FORMULA_LENGTH));
                }
                // Set the scale as determined by the field
                result = BigDecimalHelper.round((BigDecimal)result, formulaFieldInfo.getScale());
            }
        }

        return result;
    }
}
