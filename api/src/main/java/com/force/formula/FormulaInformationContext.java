/*
 * Copyright, 1999-2018, salesforce.com All Rights Reserved Company Confidential
 */
package com.force.formula;

import java.util.Map;

/**
 * FormulaInformationContext is a context used by the formula engine for debugging information. Callers of the formula
 * engine should call update FormulaEngineHooks.getInformationContextProvider and push(context, keysAndValues) when 
 * invoking the formula engine with any additional information that would help with debugging formulas.
 *
 * @author a.rich
 * @since 214
 */
public interface FormulaInformationContext /* implements Context */ {

    /**
     * @return Additional information which may be set by the caller of the formula engine
     */
    public Map<String, String> getAdditionalInfo();

    /**
     * Each formula context can override {@link FormulaRuntimeContext#getMetaInformation()} to provide the formula engine
     * useful debugging information relevant to that specific context.
     * @return useful debugging information.
     */
    public Map<String, String> getContextInfo();

    /**
     * @return The name of the FormulaContext that this context was created with
     */
    public String getContextName();

    /**
     * @param stackLevel
     *            what level is this in the context stack?
     * @return a message suitable for appending to gack messages
     */
    public String toGackMessage(int stackLevel);

    /**
     * A FormulaInformationContext Provider
     */
    public interface Provider /* extends StackableContextProvider<FormulaInformationContext> */ {
        /**
         * The new context of a process
         * @param context the context to push onto the stack
         * @param keysAndValues the keys and values to push onto the FIC as key,value,[key,value...]
         * @return a new FormulaInformationContext
         */
    	FormulaInformationContext push(FormulaContext context, String... keysAndValues);

        /**
         * @param assertContext the expected context which should get popped.
         * @return the last context in the stack and remove it from the stack
         * @throws IllegalStateException if you are trying to pop the "last" value
         */
        FormulaInformationContext pop(FormulaInformationContext assertContext);

        String peekStackInfo();
    }

}
