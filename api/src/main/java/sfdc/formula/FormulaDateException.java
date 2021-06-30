/**
 * 
 */
package sfdc.formula;

/**
 * Used when there's an invalid date.
 * @author stamm
 * @since 0.0.1
 */
public class FormulaDateException extends FormulaException {

	private static final long serialVersionUID = 1L;

	public FormulaDateException(String message) {
		super(message);
	}

	public FormulaDateException(Throwable t) {
		super(t);
	}

}
