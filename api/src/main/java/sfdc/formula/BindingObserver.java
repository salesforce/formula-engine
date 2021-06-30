package sfdc.formula;

/**
 * Observer to provide access to field values and replacement during late binding phase of formula evaluation.
 *
 * @author dchasman
 * @since 140
 */

public interface BindingObserver {
    String bind(String value) throws FormulaException;
}
