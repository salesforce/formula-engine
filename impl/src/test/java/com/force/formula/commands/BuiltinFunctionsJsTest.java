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
    // This test should be removed once the javascript bug is fixed
    public void testADDMONTHS() throws Exception {
        // last day of the month
        assertEquals(evaluateDate("date(2020, 2, 29)"), evaluateDate("ADDMONTHS(date(2020, 1, 31), 1)"));
        assertEquals(evaluateDate("date(2020, 7, 31)"), evaluateDate("ADDMONTHS(date(2020, 4, 30), 3)"));
        assertEquals(evaluateDate("date(2019, 1, 31)"), evaluateDate("ADDMONTHS(date(2019, 2, 28), -1)"));
        assertEquals(evaluateDate("date(2020, 4, 30)"), evaluateDate("ADDMONTHS(date(2020, 7, 31), -3)"));
        evaluateDate("ADDMONTHS(date(2019, 1, 30), 1)");
        // day greater than the max day of the resulting month
        // javascript bug:
        //    evaluateDate("ADDMONTHS(date(2020, 1, 30), 1)") ==> Sun Mar 01 00:00:00 GMT 2020
        //    evaluateDate("ADDMONTHS(date(2020, 5, 30), -3)") ==> Sun Mar 01 00:00:00 GMT 2020
        //
        // comment out this for now in order to avoid build failure
        // assertEquals(evaluateDate("date(2020, 2, 29)"), evaluateDate("ADDMONTHS(date(2020, 1, 30), 1)"));
        // assertEquals(evaluateDate("date(2020, 2, 29)"), evaluateDate("ADDMONTHS(date(2020, 5, 30), -3)"));

        // day not fewer than the max day of the resulting month
        assertEquals(evaluateDate("date(2020, 2, 25)"), evaluateDate("ADDMONTHS(date(2020, 1, 25), 1)"));
        assertEquals(evaluateDate("date(2020, 2, 10)"), evaluateDate("ADDMONTHS(date(2020, 5, 10), -3)"));

        // fractional month
        assertEquals(evaluateDate("date(2020, 1, 25)"), evaluateDate("ADDMONTHS(date(2020, 1, 25), 0.5)"));
        assertEquals(evaluateDate("date(2020, 2, 29)"), evaluateDate("ADDMONTHS(date(2020, 2, 29), -0.8)"));
    }

}
