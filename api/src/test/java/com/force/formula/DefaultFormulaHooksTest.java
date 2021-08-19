/**
 * 
 */
package com.force.formula;

import java.util.Calendar;

import junit.framework.TestCase;

/**
 * Validate and the default implementation for FormulaEngineHooks
 *
 * @author stamm
 */
public class DefaultFormulaHooksTest extends TestCase {

    public DefaultFormulaHooksTest() {
    }

    
    public void testEngineHooks() {
        FormulaEngineHooks def = new FormulaEngineHooks() {};
        Calendar cal = Calendar.getInstance();
        def.adjustCalendarForTestEnvironment(cal);

        FormulaTime time = def.constructTime(8640000);
        assertEquals(8640000, time.getMillisecond());
    }
}
