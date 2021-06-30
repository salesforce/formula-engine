/*
 * Copyright, 1999-2013, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package sfdc.formula;

/**
 * An extension of FormulaFieldInfo that tracks the context in which it was created.
 *
 * @author aroyfaderman
 * @since 186
 */
public interface ContextualFormulaFieldInfo extends FormulaFieldInfo {
    /**
     * @return the formula context that this was created from
     */
    FormulaContext getFormulaContext();
}
