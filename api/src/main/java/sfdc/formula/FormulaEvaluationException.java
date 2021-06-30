package sfdc.formula;

/**
 * Used to wrap exceptions with unchecked container to allow exception to propagate out
 *
 * @author dchasman
 * @since 140
 */
public class FormulaEvaluationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

	public FormulaEvaluationException(Throwable cause) {
        super(cause);
    }

    /**
     * Reporting uses this constructor to handle cases where the db auxiliary column
     * tells us that there is an error.
     */
    public FormulaEvaluationException() {
        super();
    }

    public FormulaEvaluationException(String message) {
        super(message);
    }

}
