package sfdc.formula;

/**
 * Used for merge fields that cannot be accessed in the current formula context.
 *
 * @author wmacklem
 * @since 154
 */
public class FieldNotAllowedException extends InvalidFieldReferenceException {
	private static final long serialVersionUID = 1L;
    public FieldNotAllowedException(String fieldName) {
        super(fieldName, null, FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "FieldNotAllowedException", fieldName));
    }
}
