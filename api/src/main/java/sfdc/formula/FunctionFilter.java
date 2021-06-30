package sfdc.formula;

/**
 * An implementation of a function filter that can be reused
 * @author dchasman,stamm
 * @since 144
 */
@FunctionalInterface
public interface FunctionFilter {

    boolean isSupported(FormulaCommandType command);

}
