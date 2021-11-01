/*
 * Copyright, 1999-2013, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula;

/**
 * An extension of FormulaInfo that can provide a runtime Formula and
 * keeps track of OrgFeatureData
 *
 * @author aroyfaderman
 * @since 186
 */
public interface RuntimeFormulaInfo extends FormulaInfo<ContextualFormulaFieldInfo> {
    /**
     * Return the runtime representation of this object
     * @return Formula object
     * @throws FormulaException if an exception occurs while processing the formula
     */
    Formula getFormula() throws FormulaException;
   
}
