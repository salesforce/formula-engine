/**
 * 
 */
package com.force.formula.util;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Date;
import java.util.concurrent.Callable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.force.formula.ContextualFormulaFieldInfo;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaEvaluationException;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.FormulaTime;
import com.force.formula.FormulaTypeSpec;
import com.force.formula.InvalidFieldReferenceException;
import com.force.formula.UnsupportedTypeException;


/**
 * Test of SingleValueFormulaContext.
 *
 * @author stamm
 * @since 0.2
 */
public class SingleValueFormulaContextTest {
    
    @Before
    public void setup() {
        BaseCompositeFormulaContextTest.setLocalizer();
    }
    
    @Test(expected = FormulaEvaluationException.class)
    public void testBoolean() throws Exception {
        SingleValueFormulaContext context = new TestSingleValueFormulaContext(null, null);
        context.getBoolean("Value");
    }

    @Test
    public void testGetString() throws Exception {
        SingleValueFormulaContext context = new TestSingleValueFormulaContext(null, "Hi");
        checkValidValue(context, (key)->context.getString(key, false), "Hi");
    }
    
    @Test
    public void testLookup() throws Exception {
        SingleValueFormulaContext context = new TestSingleValueFormulaContext(null, "Hi");
        ContextualFormulaFieldInfo ffi = context.lookup("value");
        Assert.assertEquals(null, ffi.getDataType());
        Assert.assertEquals("Value", ffi.getName());
    }
    

    @Test
    public void testGetMethods() throws Exception {
        SingleValueFormulaContext c0 = new TestSingleValueFormulaContext(null, "Hi");
        checkValidValue(c0, (key)->c0.getString(key, false), "Hi");
        checkValidValue(c0, (key)->c0.getMaskedString(key), "Hi");

        Date d = new Date();
        final SingleValueFormulaContext c1 = new TestSingleValueFormulaContext(null, d);
        checkValidValue(c1, (key)->c1.getDate(key), d);
   
        FormulaDateTime dt = new FormulaDateTime(new Date());
        final SingleValueFormulaContext c2 = new TestSingleValueFormulaContext(null, dt);
        checkValidValue(c2, (key)->c2.getDateTime(key), dt);

        BigDecimal n = BigDecimal.ONE;
        final SingleValueFormulaContext c3 = new TestSingleValueFormulaContext(null, n);
        checkValidValue(c3, (key)->c3.getNumber(key), n);
   
        FormulaTime t = new FormulaTime.TimeWrapper(LocalTime.now());
        final SingleValueFormulaContext c4 = new TestSingleValueFormulaContext(null, t);
        checkValidValue(c4, (key)->c4.getTime(key), t);
    }
    
    // Test lowercase/uppercase and getObject for each type
    <T> void checkValidValue(FormulaRuntimeContext context, ContextLookup<T> v, T value) throws Exception {
        Assert.assertEquals(value, v.apply("Value"));
        Assert.assertEquals(value, v.apply("value"));
        Assert.assertEquals(value, context.getObject("Value"));
        Assert.assertEquals(value, context.getObject("value"));
    }
    
    
    @FunctionalInterface
    interface ContextLookup<V> {
        V apply(String t) throws InvalidFieldReferenceException, UnsupportedTypeException;
    }
    
    @Test
    public void testInvalidValue() throws Exception {
        SingleValueFormulaContext context = new TestSingleValueFormulaContext(null, "Hi");
        checkInvalidValue(() -> context.getString("invalid", false));
        checkInvalidValue(() -> context.getDate("invalid"));
        checkInvalidValue(() -> context.getDateTime("invalid"));
        checkInvalidValue(() -> context.getBoolean("invalid"));
        checkInvalidValue(() -> context.getObject("invalid"));
        checkInvalidValue(() -> context.getCurrency("invalid"));
        checkInvalidValue(() -> context.getLocation("invalid"));
        checkInvalidValue(() -> context.getMaskedString("invalid"));
        checkInvalidValue(() -> context.getNumber("invalid"));
        checkInvalidValue(() -> context.getTime("invalid"));
    }
  
    void checkInvalidValue(Callable<?> v) throws Exception {
        try {
            v.call();
            Assert.fail();
        } catch (FormulaEvaluationException x) {
            Assert.assertTrue(x.getCause() instanceof InvalidFieldReferenceException);
        }
    }

    @Test(expected = ClassCastException.class)
    public void testGetWrongType() throws Exception {
        SingleValueFormulaContext context = new TestSingleValueFormulaContext(null, new Date());
        context.getString("Value", false);
    }

    
    @Test
    public void testAlternateName() throws Exception {
        SingleValueFormulaContext context = new TestSingleValueFormulaContext(null, BigDecimal.ONE) {
            @Override
            public String getValueName() {
                return "Num";
            }
            
        };
        ContextualFormulaFieldInfo ffi = context.lookup("num");
        Assert.assertEquals(null, ffi.getDataType());
        Assert.assertEquals("Num", ffi.getName());
        Assert.assertEquals(BigDecimal.ONE, context.getNumber("num"));
    }
    
    
    static class TestSingleValueFormulaContext extends SingleValueFormulaContext<Object> {
        public TestSingleValueFormulaContext(FormulaDataType type, Object value) {
            super(null, new FormulaTypeSpec() {
                @Override
                public int getMaxLength() {
                    return 0;
                }
                @Override
                public String getDisplay() {
                    return "display";
                }
                @Override
                public FormulaProperties getDefaultProperties() {
                    return new FormulaProperties();
                }
            }, type, value);
        }
    }
}
