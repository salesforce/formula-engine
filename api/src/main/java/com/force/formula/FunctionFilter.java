package com.force.formula;

/**
 * An implementation of a function filter that can be reused
 * @author dchasman,stamm
 * @since 144
 */
@FunctionalInterface
public interface FunctionFilter {

    /**
     * @param command the command type 
     * @return whether or not this command is supported in this environment.
     */
    boolean isSupported(FormulaCommandType command);

}
