/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.commands;

import com.force.formula.FormulaContext;
import com.force.formula.FormulaDataType;
import com.force.formula.impl.BeanFormulaContext;

/**
 * Test the javascript evaluator using the "high precision" decimals from decimal.js
 *
 * @author stamm
 * @since 0.1
 */
public class OptionalFunctionsHpJsTest extends OptionalFunctionsJsTest {
    public OptionalFunctionsHpJsTest(String name) {
        super(name);
    }
    
    @Override
    protected boolean isHighPrecisionJs() {
        return true;
    }

    @Override
    protected BeanFormulaContext setupMockContext(FormulaDataType columnType) {
    	BeanFormulaContext context = super.setupMockContext(columnType); 
        context.setProperty(FormulaContext.HIGHPRECISION_JS, Boolean.TRUE);
        return context;
    }
}
