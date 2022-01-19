/*
 * Copyright, 1999-2016, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.commands;

import java.util.TimeZone;

import com.force.formula.FormulaDataType;
import com.force.formula.FormulaException;
import com.force.formula.MockFormulaType;

/**
 * Simple tests of javascript generation of optional functions
 * @author stamm
 * @since 0.1
 */
public class OptionalFunctionsJsTest extends OptionalFunctionsTest {
    public OptionalFunctionsJsTest(String name) {
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

}
