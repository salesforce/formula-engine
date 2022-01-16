package com.force.formula.commands;


import java.math.BigDecimal;

import com.force.formula.impl.BaseCustomizableParserTest;
import com.force.i18n.BaseLocalizer;

/**
 * Test for all optional functions for easy testing without needing SQL.  
 * Add functions required to BaseCustomizableParserTest.TEST_FACTORY
 *
 * @author dchasman
 * @since 140
 */
public class OptionalFunctionsTest extends BaseCustomizableParserTest {
    public OptionalFunctionsTest(String name) {
        super(name, new FieldTestFormulaValidationHooks() {
			@Override
			public BaseLocalizer getLocalizer() {
				return GMT_LOCALIZER;
			}
        });
    }

    /**
     * @return whether we're running in javascript mode.  Avoid calling this
     */
    protected boolean isJs() {
        return false;
    }
    
    /**
     * Whether we use "Decimal" or "Number" in javascript
     */
    protected boolean isHighPrecisionJs() {
        return false;
    }
    


    // Test FormatCurrency
    public void testFormatCurrency() throws Exception {
    	assertEquals("USD 5,000.00", evaluateString("FORMATCURRENCY('USD', 5000)"));
    	assertEquals("JPY 5,000", evaluateString("FORMATCURRENCY('JPY', 5000)"));
    	
    }

    // Test UNIXTIMESTAMP/FROMUNIXTIME
    public void testUnixTimestamp() throws Exception {
    	assertEquals(new BigDecimal("1112659200"), evaluateBigDecimal("UNIXTIMESTAMP(date(2005, 4, 5))"));
    	assertEquals("2005-04-05 00:00:00Z", evaluateString("TEXT(FROMUNIXTIME(1112659200))"));

    	assertEquals(new BigDecimal("1132074000"), evaluateBigDecimal("UNIXTIMESTAMP(datetimevalue(\"2005-11-15 17:00:00\"))"));
    	assertEquals("2005-11-15 17:00:00Z", evaluateString("TEXT(FROMUNIXTIME(1132074000))"));
    }
    
    // Test ISOWEEK
    public void testIsoWeekNumber() throws Exception {
    	assertEquals(new BigDecimal("14"), evaluateBigDecimal("ISOWEEK(date(2005, 4, 5))"));
    	assertEquals(new BigDecimal("52"), evaluateBigDecimal("ISOWEEK(date(2005, 12, 31))"));
    	assertEquals(new BigDecimal("52"), evaluateBigDecimal("ISOWEEK(date(2006, 1, 1))"));
    	assertEquals(new BigDecimal("46"), evaluateBigDecimal("ISOWEEK(datetimevalue(\"2005-11-15 17:00:00\"))"));
    	assertEquals(new BigDecimal("52"), evaluateBigDecimal("ISOWEEK(datetimevalue(\"2006-12-31 23:59:59\"))"));
    	assertEquals(new BigDecimal("1"), evaluateBigDecimal("ISOWEEK(datetimevalue(\"2007-01-01 00:00:00\"))"));
    	assertEquals(new BigDecimal("53"), evaluateBigDecimal("ISOWEEK(datetimevalue(\"2010-01-01 00:00:00\"))"));
    }

    // Test ISOYEAR
    public void testIsoYearNumber() throws Exception {
    	assertEquals(new BigDecimal("2005"), evaluateBigDecimal("ISOYEAR(date(2005, 4, 5))"));
    	assertEquals(new BigDecimal("2005"), evaluateBigDecimal("ISOYEAR(date(2005, 12, 31))"));
    	assertEquals(new BigDecimal("2005"), evaluateBigDecimal("ISOYEAR(date(2006, 1, 1))"));
    	assertEquals(new BigDecimal("2005"), evaluateBigDecimal("ISOYEAR(datetimevalue(\"2005-11-15 17:00:00\"))"));
    	assertEquals(new BigDecimal("2006"), evaluateBigDecimal("ISOYEAR(datetimevalue(\"2006-12-31 23:59:59\"))"));
    	assertEquals(new BigDecimal("2007"), evaluateBigDecimal("ISOYEAR(datetimevalue(\"2007-01-01 00:00:00\"))"));
    	assertEquals(new BigDecimal("2009"), evaluateBigDecimal("ISOYEAR(datetimevalue(\"2010-01-01 00:00:00\"))"));
    }
    
    // Test DAYOFYEAR
    public void testDayOfYear() throws Exception {
    	assertEquals(new BigDecimal("95"), evaluateBigDecimal("DAYOFYEAR(date(2005, 4, 5))"));
    	assertEquals(new BigDecimal("365"), evaluateBigDecimal("DAYOFYEAR(date(2005, 12, 31))"));
    	assertEquals(new BigDecimal("1"), evaluateBigDecimal("DAYOFYEAR(date(2006, 1, 1))"));
    	assertEquals(new BigDecimal("319"), evaluateBigDecimal("DAYOFYEAR(date(2005, 11, 15))"));
    	assertEquals(new BigDecimal("365"), evaluateBigDecimal("DAYOFYEAR(date(2006, 12, 31))"));
    	assertEquals(new BigDecimal("1"), evaluateBigDecimal("DAYOFYEAR(date(2007, 1, 1))"));
    	assertEquals(new BigDecimal("366"), evaluateBigDecimal("DAYOFYEAR(date(2008, 12, 31))"));
    }

    public void testInitCap() throws Exception {
    	assertEquals("Mr. Smith", evaluateString("INITCAP(\"mr. smith\")"));
    	assertEquals("Mr. 123smith", evaluateString("INITCAP(\"mr. 123smith\")"));
    	assertEquals("Mr. _Smith", evaluateString("INITCAP(\"mr. _smith\")"));
    }

    public void testChr() throws Exception {
    	assertEquals(null, evaluateString("CHR(0)"));
    	assertEquals(" ", evaluateString("CHR(32)"));
		assertEquals(null, evaluateString("CHR(-32)"));
    	assertEquals("\u2601", evaluateString("CHR(9729)")); // cloud
    	assertEquals("\uD83E\uDDF5", evaluateString("CHR(129525)")); // thread
    }


    public void testAscii() throws Exception {
    	assertEquals(null, evaluateBigDecimal("ASCII(null)"));
    	assertEquals(null, evaluateBigDecimal("ASCII(\"\")"));
    	assertEquals(new BigDecimal(32), evaluateBigDecimal("ASCII(\" \")"));
    	assertEquals(new BigDecimal(9729), evaluateBigDecimal("ASCII(\"\u2601\")"));
    	assertEquals(new BigDecimal(129525), evaluateBigDecimal("ASCII(\"\uD83E\uDDF5\")")); // thread
    }
    
    // Test FormatCurrency
    public void testFormatDuration() throws Exception {
        assertEquals("00:00:00", evaluateString("FORMATDURATION(0)"));
        assertEquals("00:00:01", evaluateString("FORMATDURATION(1)"));
        assertEquals("00:00:01", evaluateString("FORMATDURATION(-1)"));
        assertEquals("277:46:40", evaluateString("FORMATDURATION(1000000)"));
        assertEquals("277:46:41", evaluateString("FORMATDURATION(1000001)"));
        assertEquals("277:46:41", evaluateString("FORMATDURATION(-1000001)"));

        assertEquals("00:00:00", evaluateString("FORMATDURATION(0, FALSE)"));
        assertEquals("00:00:01", evaluateString("FORMATDURATION(1, FALSE)"));
        assertEquals("00:00:01", evaluateString("FORMATDURATION(-1, FALSE)"));
        assertEquals("277:46:40", evaluateString("FORMATDURATION(1000000, FALSE)"));
        assertEquals("277:46:41", evaluateString("FORMATDURATION(1000001, FALSE)"));
        assertEquals("277:46:41", evaluateString("FORMATDURATION(-1000001, FALSE)"));

        assertEquals("0:00:00:00", evaluateString("FORMATDURATION(0, TRUE)"));
        assertEquals("0:00:00:01", evaluateString("FORMATDURATION(1, TRUE)"));
        assertEquals("0:00:00:01", evaluateString("FORMATDURATION(-1, TRUE)"));
        assertEquals("11:13:46:40", evaluateString("FORMATDURATION(1000000, TRUE)"));
        assertEquals("11:13:46:41", evaluateString("FORMATDURATION(1000001, TRUE)"));
        assertEquals("11:13:46:41", evaluateString("FORMATDURATION(-1000001, TRUE)"));

        assertEquals("0:00:00:01", evaluateString("FORMATDURATION(datetimevalue(\"2005-11-15 17:00:00\"), datetimevalue(\"2005-11-15 17:00:01\"))"));
        assertEquals("0:00:00:01", evaluateString("FORMATDURATION(datetimevalue(\"2005-11-15 17:00:01\"), datetimevalue(\"2005-11-15 17:00:00\"))"));
        assertEquals("1:00:00:00", evaluateString("FORMATDURATION(datetimevalue(\"2005-11-15 17:00:00\"), datetimevalue(\"2005-11-16 17:00:00\"))"));

        assertEquals("02:00:00", evaluateString("FORMATDURATION(timeValue(\"08:34:56.789\"),timeValue(\"06:34:56.789\"))"));
        assertEquals("02:00:00", evaluateString("FORMATDURATION(timeValue(\"06:34:56.789\"),timeValue(\"08:34:56.789\"))"));
        assertEquals("17:00:00", evaluateString("FORMATDURATION(timeValue(\"06:34:56.789\"),timeValue(\"23:34:56.789\"))"));
    }

    
    public void testTrig() throws Exception {
        // Round to 7 becuase javascript precision is highly variable
        assertEquals(new BigDecimal("3.1415927"), evaluateBigDecimal("ROUND(PI(),7)")); 
        assertEquals(new BigDecimal("3.1415927"), evaluateBigDecimal("ROUND(PI(),7)")); 
    }
   
}
