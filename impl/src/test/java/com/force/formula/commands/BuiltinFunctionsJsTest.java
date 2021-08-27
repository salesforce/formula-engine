/*
 * Copyright, 1999-2016, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.commands;

import com.force.formula.*;
import com.force.formula.impl.FormulaInfoFactory;

/**
 * Simple tests of javascript generation of built-in functions
 * @author stamm
 * @since 206
 */
public class BuiltinFunctionsJsTest extends BuiltinFunctionsTest {
    public BuiltinFunctionsJsTest(String name) {
        super(name);
    }
 
    @Override
    protected boolean isJs() {
        return true;
    }
    
    
    @Override
    protected MockFormulaType getFormulaType() {
        return MockFormulaType.JAVASCRIPT;
    }

    @Override
    protected Object evaluate(String formulaSource, FormulaDataType columnType) throws Exception {
        FormulaRuntimeContext context = setupMockContext(columnType);
        RuntimeFormulaInfo formulaInfo = FormulaInfoFactory.create(MockFormulaType.JAVASCRIPT, context, formulaSource);
        Formula formula = formulaInfo.getFormula();
        return FormulaJsTestUtils.get().evaluateFormula(formula, columnType, context, null);
    }

    // Not really applicable or requires fancy functions
    @Override
    public void testLPAD() throws Exception {}
    @Override
    public void testRPAD() throws Exception {}

}
