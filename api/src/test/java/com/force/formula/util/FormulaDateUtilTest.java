/*
 * Copyright, 1999-2013, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.util;

import java.math.BigDecimal;
import java.util.*;

import org.junit.Assert;

import com.force.formula.FormulaEngine;
import com.force.formula.FormulaEngineHooks;
import com.force.i18n.BaseLocalizer;
import com.force.i18n.grammar.GrammaticalLocalizer;

import junit.framework.TestCase;

/**
 * Tests for {@link FormulaDateUtil}.
 *
 * @author tdowns
 * @since 186
 */
public class FormulaDateUtilTest extends TestCase {
    
    FormulaEngineHooks oldHooks;
    @Override
    protected void setUp() throws Exception {
        oldHooks = FormulaEngine.getHooks();
    }

    @Override
    protected void tearDown() throws Exception {
        FormulaEngine.setHooks(oldHooks);
    }

    /**
     * Test to ensure that an ISO8601 string (with milliseconds) is parsed correctly.
     */
    public void testParseISO8601ToDatetime() throws Exception {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        calendar.set(Calendar.YEAR, 2012);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 3);
        calendar.set(Calendar.SECOND, 2);
        calendar.set(Calendar.MILLISECOND, 1);
        
        final Date actualValue = FormulaDateUtil.parseISO8601("2012-01-01T10:03:02.001Z");
        Assert.assertNotNull(actualValue);
        Assert.assertEquals(calendar.getTimeInMillis(), actualValue.getTime());
    }

    /**
     * Test to ensure that an ISO8601 string without milliseconds is parsed correctly.
     */
    public void testParseISO8601WithoutMillisecondsToDatetime() throws Exception {
        final Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        calendar.set(Calendar.YEAR, 2012);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        final Date actualValue = FormulaDateUtil.parseISO8601("2012-01-01T10:00:00Z");
        Assert.assertNotNull(actualValue);
        Assert.assertEquals(calendar.getTimeInMillis(), actualValue.getTime());
    }

    /**
     * Test to ensure that an ISO8601 string without milliseconds is parsed correctly.
     */
    public void testDateToSqlToDateString() throws Exception {
        final Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        calendar.set(Calendar.YEAR, 2012);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        assertEquals("TO_DATE('01-01-2012', 'DD-MM-YYYY')", FormulaDateUtil.dateToSqlToDateString(calendar.getTime()));
    }

    /**
     * Test that toMignight truncates
     */
    public void testToMidnight() throws Exception {
        final Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        calendar.set(Calendar.YEAR, 2012);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        final Date actualValue = FormulaDateUtil.parseISO8601("2012-01-01T00:00:00Z");
        Assert.assertEquals(actualValue.getTime(), FormulaDateUtil.toMidnight(calendar).getTimeInMillis());
    }
    

    /**
     * Test that toMignight truncates
     */
    public void testMillisecondOfDay() throws Exception {
        final Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        calendar.set(Calendar.YEAR, 2012);
        calendar.set(Calendar.MONTH, 0);
        calendar.set(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 10);
        calendar.set(Calendar.MINUTE, 3);
        calendar.set(Calendar.SECOND, 2);
        calendar.set(Calendar.MILLISECOND, 196);

        Assert.assertEquals(36182196L, FormulaDateUtil.millisecondOfDay(calendar));
    }

    
    /**
     * Test todayGmt is might
     */
    public void testAddDurationToDate() throws Exception {
        FormulaEngine.setHooks(new HooksWithLocalizer(TimeZone.getTimeZone("GMT")));

        final Date baseValue = FormulaDateUtil.parseISO8601("2012-01-01T10:10:00Z");
        Date actualValue = FormulaDateUtil.parseISO8601("2012-01-02T10:10:00Z");
        Assert.assertEquals(actualValue, FormulaDateUtil.addDurationToDate(true, baseValue, new BigDecimal("1"), false));
        actualValue = FormulaDateUtil.parseISO8601("2011-12-31T10:10:00Z");
        Assert.assertEquals(actualValue, FormulaDateUtil.addDurationToDate(false, baseValue, new BigDecimal("1"), false));
        actualValue = FormulaDateUtil.parseISO8601("2012-01-02T10:10:00Z");
        Assert.assertEquals(actualValue, FormulaDateUtil.addDurationToDate(true, baseValue, new BigDecimal("1"), true));
    }
    
    
    /**
     * Test todayGmt is might
     */
    public void testTodayGmt() throws Exception {
        FormulaEngine.setHooks(new HooksWithLocalizer(TimeZone.getTimeZone("GMT")));

        Date d = FormulaDateUtil.todayGmt();
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.setTime(d);
        assertEquals(0, c.get(Calendar.MILLISECOND));
        assertEquals(0, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, c.get(Calendar.SECOND));
        assertEquals(0, c.get(Calendar.MINUTE));
    }
    
    /**
     * Test that toMignight truncates
     */
    public void testTruncate() throws Exception {
        FormulaEngine.setHooks(new HooksWithLocalizer(TimeZone.getTimeZone("GMT")));

        final Date toTruncate = FormulaDateUtil.parseISO8601("2012-01-01T10:10:00Z");
        final Date actualValue = FormulaDateUtil.parseISO8601("2012-01-01T00:00:00Z");
        Assert.assertEquals(actualValue, FormulaDateUtil.truncateDateToOwnersGmtMidnight(TimeZone.getTimeZone("GMT"), toTruncate));
    }
    
    /**
     * Test translation with GMT works.
     */
    public void testTranslate() throws Exception {
        FormulaEngine.setHooks(new HooksWithLocalizer(TimeZone.getTimeZone("GMT")));

        final Date toTranslate = FormulaDateUtil.parseISO8601("2012-01-01T10:10:00.000Z");
        Date actualValue = FormulaDateUtil.parseISO8601("2012-01-01T10:10:00Z");
        Assert.assertEquals(actualValue, FormulaDateUtil.translateToLocal(toTranslate, false));
        Assert.assertEquals(actualValue, FormulaDateUtil.translateToGMT(toTranslate, false));        
        actualValue = FormulaDateUtil.parseISO8601("2012-01-01T00:00:00Z");
        Assert.assertEquals(actualValue, FormulaDateUtil.translateToLocal(toTranslate, true));
        Assert.assertEquals(actualValue, FormulaDateUtil.translateToGMT(toTranslate, true));
        
        // TODO: Test with PST.
    }
    
    static class HooksWithLocalizer implements FormulaEngineHooks {
    	private final TimeZone tz;
    	public HooksWithLocalizer(TimeZone tz) {
    		this.tz = tz;
    	}
		@Override
		public BaseLocalizer getLocalizer() {
			return new GrammaticalLocalizer(Locale.US, Locale.US, tz, null, null);
		}
    	
    	
    }

}