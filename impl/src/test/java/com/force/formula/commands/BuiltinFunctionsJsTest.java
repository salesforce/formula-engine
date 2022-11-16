/*
 * Copyright, 1999-2016, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.commands;

import java.util.TimeZone;

import com.force.formula.FormulaDataType;
import com.force.formula.FormulaException;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.MockFormulaContext;
import com.force.formula.MockFormulaType;

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
	protected void setUp() throws Exception {
		super.setUp();
		// TODO: Time functions are dependent on javascript timezone.
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
	}

	@Override
    protected MockFormulaType getFormulaType() {
        return MockFormulaType.JAVASCRIPT;
    }

    @Override
    protected Object evaluate(String formulaSource, FormulaDataType columnType) throws FormulaException {
    	return evaluateJavascript(formulaSource, columnType);
    }

    // Not really applicable or requires fancy functions
    @Override
    public void testLPAD() throws Exception {}
    @Override
    public void testRPAD() throws Exception {}

    @Override
    protected FormulaRuntimeContext setupMockContext(FormulaDataType columnType) {
        return new MockFormulaContext(getFormulaType(), columnType) {

            @Override
            public String getJsEngMod() {
                // Use FormulaEngine to make sure it works with the "default".  Use $F in the gold files for brevity
                return "FormulaEngine";
            }
            
        };
    }
    
}
