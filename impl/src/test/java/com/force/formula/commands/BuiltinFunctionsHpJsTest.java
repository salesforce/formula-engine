/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.commands;

import com.force.formula.FormulaContext;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaRuntimeContext;

/**
 * Test the javascript evaluator using the "high precision" decimals from decimal.js
 *
 * @author stamm
 * @since 212
 */
public class BuiltinFunctionsHpJsTest extends BuiltinFunctionsJsTest {
    public BuiltinFunctionsHpJsTest(String name) {
        super(name);
    }
    
    @Override
    protected boolean isHighPrecisionJs() {
        return true;
    }

    @Override
    protected FormulaRuntimeContext setupMockContext(FormulaDataType columnType) {
        FormulaRuntimeContext context = super.setupMockContext(columnType); 
        context.setProperty(FormulaContext.HIGHPRECISION_JS, Boolean.TRUE);
        return context;
    }

}
