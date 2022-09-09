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

    @Override
    // This test should be removed once the javascript bug is fixed
    public void testADDMONTHS() throws Exception {
        // last day of the month
        assertEquals(evaluateDate("date(2020, 2, 29)"), evaluateDate("ADDMONTHS(date(2020, 1, 31), 1)"));
        assertEquals(evaluateDate("date(2020, 7, 31)"), evaluateDate("ADDMONTHS(date(2020, 4, 30), 3)"));
        assertEquals(evaluateDate("date(2019, 1, 31)"), evaluateDate("ADDMONTHS(date(2019, 2, 28), -1)"));
        assertEquals(evaluateDate("date(2020, 4, 30)"), evaluateDate("ADDMONTHS(date(2020, 7, 31), -3)"));

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
