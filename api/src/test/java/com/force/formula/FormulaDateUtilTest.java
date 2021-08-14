/*
 * Copyright, 1999-2013, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula;

import java.util.*;

import org.junit.Assert;

import junit.framework.TestCase;

/**
 * Tests for {@link FormulaDateUtil}.
 *
 * @author tdowns
 * @since 186
 */
public class FormulaDateUtilTest extends TestCase {
    /**
     * Test to ensure that an ISO8601 string (with milliseconds) is parsed correctly.
     */
    public static void testParseISO8601ToDatetime() throws Exception {
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
    public static void testParseISO8601WithoutMillisecondsToDatetime() throws Exception {
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



}