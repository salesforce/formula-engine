package com.force.formula.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;

import com.force.i18n.BaseLocalizer;
import com.force.i18n.grammar.GrammaticalLocalizer;

/**
 * Date utils beyond the LibDateUtil that's used for Formula SQL Generation
 * @author stamm
 * @since 0.0.1
 */
public final class FormulaDateUtil {
    public static final int MINUTE_IN_MILLIS = 60 * 1000;
    public static final int HOUR_IN_MILLIS = 60 * 60 * 1000;
    public static final int DAY_IN_MILLIS = 24 * HOUR_IN_MILLIS;
    public static final int WEEK_IN_MILLIS = 7 * DAY_IN_MILLIS;
    public static final BigDecimal SECONDSPERDAY = BigDecimal.valueOf(24 * 60 * 60);
    public static final BigDecimal MILLISECONDSPERDAY = SECONDSPERDAY.movePointRight(3);

	private FormulaDateUtil() {}

	private final static String SQL_TO_DATE_FORMAT = "DD-MM-YYYY";


    private static final ThreadLocal<SimpleDateFormat> ISO8601_FORMATTER = ThreadLocal.withInitial(sdfWithGMT("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    private static final ThreadLocal<SimpleDateFormat> ISO8601_MILLISECOND_FORMATTER = ThreadLocal.withInitial(sdfWithGMT("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));

    private static final ThreadLocal<SimpleDateFormat> SQL_FORMATTER = ThreadLocal.withInitial(() -> new SimpleDateFormat("dd-MM-yyyy"));

    
    static Supplier<SimpleDateFormat> sdfWithGMT(String format) {
    	return () -> {
    		SimpleDateFormat sdf = new SimpleDateFormat(format);
    		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    		return sdf;
    	};
    }
    
    
    /**
     * Creates to_date sql string for java.util.Date
     * @param date Date to be converted
     * @return Date in string format for sql
     */
    public static String dateToSqlToDateString(Date date) {
        return "TO_DATE('" + formatDateToSql(date) + "', '" + SQL_TO_DATE_FORMAT + "')";
    }
    
    /**
     * Milliseconds from start of the day, including effect of daylight saving switch over
     * @param cal Calender to be converted
     * @return millis in int
     */
    public static int millisecondOfDay(Calendar cal) {
        Calendar s = (Calendar)cal.clone();
        toMidnight(s);
        return (int)(cal.getTimeInMillis() - s.getTimeInMillis());
    }
    

    /**
     * Get today's (local) date as a GMT midnight date only
     * @return Date
     */
    public static Date todayGmt() {
        return translateToGMT(new Date(), true);
    }

    /**
     * Translate a Date from its localtime representation to a new Date
     * representing the same calendar day in GMT.
     * @param date the date to translate
     * @param toMidnight if the time portion should be truncated
     * @return a date at midnight in the GMT timezone.
     */
    public static Date translateToGMT(Date date, boolean toMidnight) {
        if (date == null) {
            return null;
        }

        GrammaticalLocalizer l = FormulaI18nUtils.getLocalizer();
        return translate(l.getCalendar(BaseLocalizer.LOCAL), l.getCalendar(BaseLocalizer.GMT), date, toMidnight);
    }
    
    /**
     * Translate a Date from its GMT representation to a new Date
     * representing the same calendar day in Local timezone
     * @param date the date to translate
     * @param toMidnight if the time portion should be truncated
     * @return a date at midnight in the locale timezone.
     */
    public static Date translateToLocal(Date date, boolean toMidnight) {
        if (date == null) {
            return null;
        }

        GrammaticalLocalizer l = FormulaI18nUtils.getLocalizer();
        return translate(l.getCalendar(BaseLocalizer.GMT), l.getCalendar(BaseLocalizer.LOCAL), date, toMidnight);
    }
    

    /**
     * Truncates a date to a users localtime midnight, and then converts that date
     * to a Gmt midnight date only.
     * @param tz the owners timezone
     * @param date  the date to trucate
     * @return the date at GMT midnight.
     */
    public static Date truncateDateToOwnersGmtMidnight(TimeZone tz, Date date) {
        if (date == null) {
            return null;
        }

        Calendar localCal = FormulaI18nUtils.getLocalizer().getCalendar(tz);
        Calendar gmtCal = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.GMT);
        localCal.setTime(date);
        gmtCal.set(Calendar.YEAR, localCal.get(Calendar.YEAR));
        gmtCal.set(Calendar.MONTH, localCal.get(Calendar.MONTH));
        gmtCal.set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH));
        return toMidnight(gmtCal).getTime();
    }

    
    /**
     * Converts java.util.Date to format appropriate for oracle sql using sqlFormatter
     * @param date the date to format
     * @return format the date to SQL format
     */
    public static String formatDateToSql(Date date) {
        return SQL_FORMATTER.get().format(date);
    }
    
    /**
     * Truncates the given calendar to midnight
     * @param cal Calender object to truncate
     * @return The same Calender object passed in
     */
    public static Calendar toMidnight(Calendar cal) {
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.AM_PM, Calendar.AM);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal;
    }
    
    /**
     * add a duration to a date(units of duration is days)
     * @param performAddition if true, add, otherwise subtract
     * @param value  the date value
     * @param duration the number of seconds to add to the date
     * @param truncate should the milliseconds be truncated, or rounded
     * @return the value with the date added
     */
    public static Date addDurationToDate(boolean performAddition, Date value, BigDecimal duration, boolean truncate) {

        if (!performAddition) {
            duration = duration.negate();
        }

        if (truncate) {
            // Truncate any fractional part
            duration = new BigDecimal(duration.toBigInteger()).multiply(MILLISECONDSPERDAY);
        } else {
            // otherwise do what Oracle does: Round to the nearest second
            duration = duration.multiply(SECONDSPERDAY).setScale(0, RoundingMode.HALF_UP).movePointRight(3);
        }

        return new Date(value.getTime() + duration.longValue());
    }


    /**
     * Parses a string in ISO8601 format, with both date and time. This method handles parsing 
     * with and without milliseconds
     * Must be in universal GMT timezone and contain both date and time
     * e.g. 2011-01-31T22:59:48.317Z
     * e.g. 2011-01-31T22:59:48Z
     * @param date the date to parse
     * @return the parsed date
     * @throws ParseException if the date format is invalid
     */
    public static Date parseISO8601(String date) throws ParseException {
    	try {
    		return ISO8601_MILLISECOND_FORMATTER.get().parse(date);
    	} catch(ParseException exc) {
    		return ISO8601_FORMATTER.get().parse(date);
    	}
    }
    
    /**
     * Returns a string in ISO8601 format, with both date and time
     * e.g. 2011-01-31T22:59:48Z
     * @param date the date to format
     * @return the value of the date formatted with ISO8601 format.
     */
    public static String formatDatetimeToISO8601(Date date) {
        return ISO8601_FORMATTER.get().format(date);
    }

    
    public static Calendar translateCal(Calendar from, Calendar to, Date date, boolean toMidnight) {
        from.setTime(date);
                to.set(from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH));
        if (toMidnight) {
            to = toMidnight(to);
        } else {
            to.set(Calendar.HOUR_OF_DAY, from.get(Calendar.HOUR_OF_DAY));
            to.set(Calendar.MINUTE, from.get(Calendar.MINUTE));
            to.set(Calendar.SECOND, from.get(Calendar.SECOND));
            to.set(Calendar.MILLISECOND, from.get(Calendar.MILLISECOND));
        }
        return to;
    }

    public static Date translate(Calendar from, Calendar to, Date date, boolean toMidnight) {
        return translateCal(from, to, date, toMidnight).getTime();
    }
    
    
    
}
