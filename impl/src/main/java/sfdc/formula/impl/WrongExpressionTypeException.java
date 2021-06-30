package sfdc.formula.impl;

import java.lang.reflect.Type;

import sfdc.formula.*;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
public class WrongExpressionTypeException extends FormulaException {

    private static final long serialVersionUID = 1L;

	public WrongExpressionTypeException(FormulaDataType expectedType, Type actualInputType, FormulaSchema.FieldOrColumn targetField) {
        super(createErrorMessage(expectedType, actualInputType, targetField));
    }

    private static String createErrorMessage(FormulaDataType expectedType, Type actualInputType, FormulaSchema.FieldOrColumn targetField) {
        String actual = FormulaTypeUtils.getTypeLabel(actualInputType);
        String expected;
        if (FormulaUtils.isTypeSobjectRow(expectedType) && targetField != null) {
            expected = FormulaTypeUtils.getTypeLabel(FormulaValidationHooks.get().constructIdType(targetField, true));
        } else if (expectedType.isId() && targetField != null) {
            expected = FormulaTypeUtils.getTypeLabel(FormulaValidationHooks.get().constructIdType(targetField, false));
        }
        else {
            expected = FormulaI18nUtils.getLocalizer().getLabelNoThrow("CustomFieldDatatypes", expectedType.getDatatype());
        }
        if (expected == null)
            expected = expectedType.getLabel();

        return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "WrongExpressionTypeException",
                FormulaTextUtil.escapeForJavascriptString(expected), FormulaTextUtil.escapeToHtml(actual));
    }

}
