/**
 * 
 */
package com.force.formula;

import java.util.Calendar;
import java.util.Locale;

import com.force.formula.util.FormulaFieldReferenceImpl;
import com.force.formula.util.SystemFormulaContext;
import com.force.i18n.grammar.GrammaticalLocalizer;

import junit.framework.TestCase;

/**
 * Validate and the default implementation for FormulaEngineHooks
 *
 * @author stamm
 */
public class DefaultFormulaHooksTest extends TestCase {

    public void testEngineHooks() {
        FormulaEngineHooks def = new FormulaEngineHooks() {};
        Calendar cal = Calendar.getInstance();
        def.adjustCalendarForTestEnvironment(cal);

        FormulaTime time = def.constructTime(8640000);
        assertEquals(0, time.getMillisecond());
        assertEquals(8640000, time.getTimeInMillis());
        
        assertNull(def.constructGeolocation(null, null));
        
        assertNull(def.hook_unwrapForNullable(null));
        assertNull(def.hook_unwrapForNullable(new FormulaDataValue() {
			@Override
			public Object getNativeValue() {
				return "hi";
			}
			@Override
			public boolean isEmpty() {
				return true;
			}
		}));
        assertEquals("hi", def.hook_unwrapForNullable("hi"));
        
    }
    
    
	public void testGetFieldReferenceValue() throws Exception {
        FormulaEngineHooks def = new FormulaEngineHooks() {
			@Override
			public GrammaticalLocalizer getLocalizer() {
				return new GrammaticalLocalizer(Locale.US, Locale.US, null, null, null);
			}

			@Override
			public FormulaDataType getDataTypeByName(String name) {
				switch (name) {
				case "DateTime": return FormulaApiMocks.MockType.DATETIME;
				}
				return FormulaEngineHooks.super.getDataTypeByName(name);
			}
        };
    	FormulaEngine.setHooks(def);
        FormulaRuntimeContext fc = new SystemFormulaContext(null);
    	ContextualFormulaFieldInfo fieldInfo = fc.lookup("OriginDateTime");
    	FormulaFieldReference ffr = new FormulaFieldReferenceImpl(null, "OriginDateTime");
    	Object o = def.getFieldReferenceValue(fieldInfo, fieldInfo.getDataType(), fc, ffr, false);
    	assertNotNull(o);
    	assertEquals(-2208988800000L, ((FormulaDateTime)o).getDate().getTime()); // 1900
	}
	
}
