package com.force.formula.commands;


/**
 * Allows to plug additional functions into the formula engine. 
 *
 * @author seberl
 * @since 186
 */
public interface FormulaCommandInfoProvider {
    /**
     * Will be called during initialization of formula engine. Returned formula command infos will be
     * registered to command registry.
     * @return the set of all formula commands allowed
     */
    FormulaCommandInfo[] getFormulas();
}
