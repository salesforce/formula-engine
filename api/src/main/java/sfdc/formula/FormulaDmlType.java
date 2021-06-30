package sfdc.formula;

/**
 * Represents the type of operation being performed with the formula, if it's
 * a save, delete, undelete, etc.
 * @author stamm
 * @since 0.0.1
 */
public interface FormulaDmlType {
	boolean isSave();
}
