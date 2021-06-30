/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package sfdc.formula.commands;

import sfdc.formula.*;

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
