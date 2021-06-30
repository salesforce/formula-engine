/*
 * Created on Dec 29, 2004
 *
 */
package sfdc.formula;

import system.context.FormulaInformationContext.Provider;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public abstract class FormulaException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String metaInfo;

	public FormulaException(String message) {
        super(message);
        Provider infoContext = FormulaEngine.getHooks().getInformationContextProvider(); 
        metaInfo = infoContext == null ? null : infoContext.peekStackInfo();
    }

    public FormulaException(Throwable t) {
        super(t);
        Provider infoContext = FormulaEngine.getHooks().getInformationContextProvider(); 
        metaInfo = infoContext == null ? null : infoContext.peekStackInfo();
    }

    public FormulaException(String message, Throwable t) {
        super(message, t);
        Provider infoContext = FormulaEngine.getHooks().getInformationContextProvider(); 
        metaInfo = infoContext == null ? null : infoContext.peekStackInfo();
    }

    public String getMetaInfo() {
        return metaInfo;
    }
}