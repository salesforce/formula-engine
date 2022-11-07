/**
 * 
 */
package com.force.formula.impl;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import com.force.formula.sql.FormulaSqlStyle;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.BigDecimalHelper;
import com.force.formula.util.FormulaDateUtil;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * A set of SQL hooks used in standard functionality to provide differences in SQL implementations.
 * 
 * @author stamm
 * @since 0.1.0
 */
public interface FormulaSqlHooks extends FormulaSqlStyle {
    /**
     * Precision to use rounding for ceil/floor to prevent issues with inexact to exact transitions
     * like 1/3 or 1/11.  Return -1 if you don't want any rounding.
     * @return 33 by default
     * @see BigDecimalHelper#NUMBER_PRECISION_EXTERNAL
     */
    default int getExternalPrecision() {
        return BigDecimalHelper.NUMBER_PRECISION_EXTERNAL;
    }
    	
    // Handle plsql regexp differences (where oracle needs regexp_like and postgres wants ~ or similar to)
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for DateTime Value
     */
    default String sqlDatetimeValueGuard() {
   	   return "1=0"; // assume null on error
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
    default String sqlDateValueGuard() {
	   return "1=0"; // assume null on error
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
    default String sqlTimeValueGuard() {
	   return "1=0"; // assume null on error
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a number Value
     */
    default String sqlIsNumber() {
    	throw new UnsupportedOperationException();
    }
    
    /**
     * @return the function to use for NVL.  In postgres, it's usually coalesce, but in oracle, you want NVL.
     */
    default String sqlNvl() {
		return "COALESCE";
    }
    
    /**
     * @return the string to use for String.format to convert something to date generically, without a specified type
     * @deprecated use the version with a type, for those DBs that distinguish Date and Timestamp 
     */
    @Deprecated(forRemoval=true, since="0.3")
    default String sqlToDate() {
		return "CAST(%s AS DATE)";
    }
        
    /**
     * @return the string to use for String.format to convert something to date, with a specified type
     * @param type the type (whether FormulaDateTime.class or Date.class)
     */
    default String sqlToDate(Type type) {
        return sqlToDate();
    }

    /**
     * @return the string to use for String.format to convert something to number generically, without a format
     */
    default String sqlToNumber() {
		return "CAST(%s AS NUMERIC)";
    }

    /**
     * @return the string to use for String.format to convert something to text generically, without a format
     */
    default String sqlToChar() {
		return "CAST(%s AS VARCHAR)";
    }
    
    /**
     * @return the string to use for TO_TIMESTAMP to get seconds.microseconds.  It's a subtle difference
     */
    default String sqlSecsAndMsecs() {
		return "SSSS.MS";
    }
    
    /**
     * @return the string to use for TO_TIMESTAMP to get seconds.microseconds.  It's a subtle difference
     */
    default String sqlHMSAndMsecs() {
		return "HH24:mi:ss.MS";
    }
    
    /**
     * @return the string to use for TO_TIMESTAMP to get seconds.  It's a subtle difference
     */
    default String sqlSecsInDay() {
		return "SSSS";
    }
    
    
    /**
     * @return extract the TIME value from a date time.  For those DBs with a useful time type this
     * could be easy, for others that pass around milliseconds, this converts to a number
     */
    default String sqlExtractTimeFromDateTime(String dateTimeExpr) {
        return String.format(sqlToNumber(), String.format("TO_CHAR(%s, '"+sqlSecsInDay()+"')", dateTimeExpr)) + " * 1000"; // date does not have millisec info
    }

    /**
     * @return the TIME value for the given string of the format "HH:MM:SS.UUU".
     * @param stringExpr the string containing the expression
     */
    default String sqlParseTime(String stringExpr) {
        return String.format(sqlToNumber(), String.format("TO_CHAR(TO_TIMESTAMP(%s, '"+sqlHMSAndMsecs()+"'),'"+sqlSecsAndMsecs()+"')", stringExpr)) + " * 1000" ;
    }
    
    /**
     * @return the function to use for conversion to Date generically.
     * @deprecated use the version with a type, for those DBs that distinguish Date and Timestamp 
     */
    @Deprecated(forRemoval=true, since="0.3")
    default String sqlNullToDate() {
    	return String.format(sqlToDate(), "NULL");
    }

    
    /**
     * @return the function to use for conversion to Date specifically.
     * @param type the specific date time to use.
     */
    default String sqlNullToDate(Type type) {
        return String.format(sqlToDate(type), "NULL");
    }
    
    /**
     * @return the function to use for conversion to Date generically.
     */
    default String sqlNullToNumber() {
    	return String.format(sqlToNumber(), "NULL");
    }
	
    /**
     * PSQL doesn't include a locale-specific upper function.  This allows the override of that function
     * if you have installed one locally, like icu_transform.
     * @param hasLocaleOverride if the locale override should be used.  If not, the second format argument will be 'en'
     * @return the sql expression to use for uppercase with a locale
     */
    default String sqlUpperCaseWithLocaleFormat(boolean hasLocaleOverride) {
    	// You could do UPPER(%s COLLATE %s), but that doesn't work in general as collate isn't a parameter.
    	return "UPPER(%s)";
    }

    /**
     * PSQL doesn't include a locale-specific lower function.  This allows the override of that function 
     * if you have one locally, like icu_transform
     * @param hasLocaleOverride if the locale override should be used.  If not, the second format argument will be 'en'
     * @return the sql expression to use for lowercase with a locale
     */
    default String sqlLowerCaseWithLocaleFormat(boolean hasLocaleOverride) {
        return "LOWER(%s)";
    }
    
	
    /**
     * Perform the InitCap function to make "proper" names.
     * @param hasLocaleOverride if the locale override should be used.  If not, the second format argument will be 'en'
     * @return the sql expression to use for uppercase with a locale
     */
    default String sqlInitCap(boolean hasLocaleOverride) {
    	return "INITCAP(%s COLLATE \"en_US\")";  // Use en_US so it isn't ascii only
    }

    /**
     * @return the sql expression to convert a number to a string containing that number as a Unicode codepoint
     */
    default String sqlChr() {
    	return "CHR(%s)";
    }
    
    /**
     * @return the sql expression to convert a number to a string containing that number as a Unicode codepoint
     */
    default String sqlAscii() {
    	return "ASCII(%s)";
    }
    
    /**
     * @return the function that allows subtraction of two timestamps to get the microsecond/day difference.  This is
     * missing from psql, but available in oracle.  This allows you to try and fix that.
     * 
     * @param inSeconds, should difference be returned in seconds or in days.  This will allow precision errors to be
     * handled better.
     * @deprecated use the version that passes in the DateType to support DBs that distinguish Date and Timestamp
     */
    @Deprecated(forRemoval=true, since="0.3")
    default String sqlSubtractTwoTimestamps(boolean inSeconds) {
        return inSeconds ? "(%s-%s)*86400" : "(%s-%s)";
    } 
    
    /**
     * @return the function that allows subtraction of two timestamps to get the microsecond/day difference.  This is
     * missing from psql, but available in oracle.  This allows you to try and fix that.
     * 
     * @param inSeconds, should difference be returned in seconds or in days.  This will allow precision errors to be
     * handled better.
     * @param dateType the type of date being subtracted
     */
    default String sqlSubtractTwoTimestamps(boolean inSeconds, Type dateType) {
        return sqlSubtractTwoTimestamps(inSeconds);
    } 

    /**
     * @return the function that allows subtraction of two time values to get the second/day difference.
     * Return the difference in the two times *in seconds*
     */
    default String sqlSubtractTwoTimes() {
        return "((%s)-(%s))/1000";
    } 
    
    /**
     * Format the sql for adding a given number of days (fractionally) to a date
     * @param lhsValue the left hand side (must be date or datetime if subtraction)
     * @param lhsDataType the type of the left hand side
     * @param rhsValue the right hand side (will be number if subtraction)
     * @param rhsDataType the type of the right hand side
     * @param isAddition whether this is addition or subtraction
     * @return a SQL expression for adding the given number of days to the date
     */
    default String sqlAddDaysToDate(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType,  boolean isAddition) {
    	return "(" + lhsValue + (isAddition ? "+" : "-") + rhsValue + ")";
    } 
    
    /**
     * Format the sql for adding a given number of milliseconds (fractionally) to a time
     * @param lhsValue the left hand side (must be date or datetime if subtraction)
     * @param lhsDataType the type of the left hand side
     * @param rhsValue the right hand side (will be number if subtraction)
     * @param rhsDataType the type of the right hand side
     * @param isAddition whether this is addition or subtraction
     * @return a SQL expression for adding the given number of days to the date
     */
    default String sqlAddMillisecondsToTime(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType,  boolean isAddition) {
        rhsValue = rhsDataType == BigDecimal.class ? "ROUND(MOD(" +String.format(sqlToNumber(), rhsValue) + ", " + FormulaDateUtil.MILLISECONDSPERDAY + "))" : rhsValue;
        // to prevent negative values when subtracting, always add FormulaDateUtil.MILLISECONDSPERDAY, and take the mod
        return "MOD(" + lhsValue + (isAddition ? "+" : "-") + rhsValue + "+" + FormulaDateUtil.MILLISECONDSPERDAY + "," + FormulaDateUtil.MILLISECONDSPERDAY + ")";
    }

    /**
     * @return how to get the unix epoch from a given date for String.format
     * @deprecated use the version that takes the date type
     */
    @Deprecated(forRemoval=true, since="0.3")
    default String sqlGetEpoch() {
    	return "ROUND((%s - DATE '1970-01-01') * 86400)";
    }

    /**
     * @return how to get the unix epoch from a given date for String.format
     * @param dateType the type of the date from which to retrieve the epoch
     */
    default String sqlGetEpoch(Type dateType) {
        return sqlGetEpoch();
    }
    
    /**
     * @return how to get the number of seconds in a day from a time value for String.format
     */
    default String sqlGetTimeInSeconds() {
    	return sqlTrunc("%s/1000");
    }

    /**
     * @return how to get a DateTime from a unix epoch time, suitable for String.format
     */
    default String getDateFromUnixTime() {
    	return "(DATE '1970-01-01' + (%s/86400))";
    }
    
    /**
     * @return how to get the day of the week (1-7, with Sunday being 1) from the date provided, suitable for String.format
     */
    default String sqlGetWeekday() {
        return "TO_NUMBER(TO_CHAR(%s,'d'))";
    }
    
    /**
     * @return how to get the ISO 8601 week number from a date or datetime, suitable for String.format
     */
    default String sqlGetIsoWeek() {
        return "TO_NUMBER(TO_CHAR(%s, 'IW'))";
    }
        
    /**
     * @return how to get the ISO 8601 year number from a date or datetime, suitable for String.format
     */
    default String sqlGetIsoYear() {
        return "TO_NUMBER(TO_CHAR(%s, 'IYYY'))";
    }
    
    /**
     * @return how to get the day of the year from a date or datetime, suitable for String.format
     */
    default String sqlGetDayOfYear() {
        return "TO_NUMBER(TO_CHAR(%s, 'DDD'))";
    }
    
    /**
     * @return the sql expression used to extract the chrono unit from the given datetime field
     * @param field the ChronoField to use.  Will be in the set of
     * MICROSECONDS, SECONDS, MINUTES, HOURS, DAYS, MONTHS, YEARS
     * @param dateType the type of date (Date, FormulaDateTime or FormulaType)
     */
    default String sqlChronoUnit(ChronoUnit field, Type dateType) {
        switch (field) {
        case YEARS: 
            return "EXTRACT(YEAR FROM %s)";
        case MONTHS:
            return "EXTRACT(MONTH FROM %s)";
        case DAYS:
            return "EXTRACT(DAY FROM %s)";
        case HOURS:
            return "TRUNC(%s/" + FormulaDateUtil.HOUR_IN_MILLIS + ")";
            //return "EXTRACT(HOUR FROM %s)";
        case MINUTES:
            // This is for backward compatibility.  It should be removed soon
            // convert muillisecs since midnight to minutes portion of time  trunc((args[0] -trunc(args[0]/3600000) * 3600000)/60000)
            return "TRUNC((%s-TRUNC(%<s/" + FormulaDateUtil.HOUR_IN_MILLIS+ ") * " + FormulaDateUtil.HOUR_IN_MILLIS + ")/" + FormulaDateUtil.MINUTE_IN_MILLIS + ")";
            //return "EXTRACT(MINUTE FROM %s)";
        case SECONDS:
            // This is for backward compatibility.  It should be removed soon
            // convert muillisecs since midnight to minutes portion of time  trunc((args[0] -trunc(args[0]/60000) * 60000)/1000)
            return "TRUNC((%s-TRUNC(%<s/" + FormulaDateUtil.MINUTE_IN_MILLIS+ ") * " + FormulaDateUtil.MINUTE_IN_MILLIS + ")/1000)";
            // return "EXTRACT(SECOND FROM %s)";
        case MILLIS:            
            // convert muillisecs since midnight to millseconds portion of time  trunc((args[0] -trunc(args[0]/1000) * 1000))
            return "TRUNC(%s -TRUNC(%<s/1000) * 1000)";
        default:
            // Others are unsupported
        }
        throw new UnsupportedOperationException();
    }
    

    /**
     * @return how to construct a date from the given expressions for year, month and day
     * @param yearSql a sql expression returning the year, possibly as a string
     * @param monthSql a sql expression returning the month, possibly as a string
     * @param daySql a sql expression returning the year, possibly as a string
     */
    default String sqlConstructDate(String yearSql, String monthSql, String daySql) {
        return "TO_DATE(" + yearSql + " || '-' || " + monthSql + " || '-' || " + daySql + ", 'YYYY-MM-DD')";
    }
    
    /**
     * Function right can be... complicated, especially in Oracle
     * @param stringArg the sql for the string value
     * @param countArg the sql for the number of chars
     * @return the SQL for generating RIGHT()
     */
    default String sqlRight(String stringArg, String countArg) {
        return "RIGHT(" + stringArg + ", " + sqlEnsurePositive(countArg) + ")";
    }
    
    /**
     * @return the the date.  Overridable in case you want to change the timezone functionality with ::timestamp.
     */
    default String sqlNow() {
        return "NOW()";
    }
    
    /**
     * @return a date literal suitable for for representing "today"
     * @param c the calendar to use 
     */
    default String getDateLiteralFromCalendar(Calendar c) {
    	return "DATE '" + c.get(Calendar.YEAR) + "-"
    		    + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DATE) + "'";
    }
    
    /**
     * @return the the current milliseconds of the day suitable.  You may want to use ::time instead, so overridable
     */
    default String sqlTimeNow() {
    	throw new UnsupportedOperationException();
    }
    
    /**
     * @return the expression to use for adding a number of months to a date
     * @deprecated use the version that take the date type
     */
    @Deprecated(forRemoval=true, since="0.3")
    default String sqlAddMonths(String dateArg, String numMonths) {
    	throw new UnsupportedOperationException();
    }
    
    /**
     * @return the expression to use for adding a number of months to a date
     * @param dateArg the argument of the date or datetime
     * @param dateArgType the type of argument for the date
     * @param numMonths the expression with the number of months to add
     */
    default String sqlAddMonths(String dateArg, Type dateArgType, String numMonths) {
        return sqlAddMonths(dateArg, numMonths);
    }    
    
    /**
     * @return the format for converting to a datetime value
     */
    default String sqlToTimestampIso() {
		return "TO_DATE(%s, 'YYYY-MM-DD HH24:MI:SS')";
    }
    
    /**
     * @return the format for converting to a datetime value
     */
    default String sqlToDateIso() {
		return "TO_DATE(%s, 'YYYY-MM-DD')";
    }
    
    /**
     * @return the format for String.format for converting from a datetime value to a string YYYY-MM-DD HH24:MI:SS
     */
    default String sqlToCharTimestamp() {
		return "TO_CHAR(%s, 'YYYY-MM-DD HH24:MI:SS')";
    }
    
    /**
     * @return the format for String.format for converting from a date value to a string YYYY-MM-DD
     */
    default String sqlToCharDate() {
		return "TO_CHAR(%s, 'YYYY-MM-DD')";
    }
    
    
    /**
     * @return the format for String.format for converting a number to an interval suitable for to {@link #sqlIntervalToDurationString(String, boolean, String)}
     */
    default String sqlIntervalFromSeconds(Type dateType) {
        return "(INTERVAL '1 second' * ABS(%s))";
    }
    
    /**
     * @return the sql expression for converting from HH:MM:SS or (DDD:HH:MM:SS) if includeDays is true
     * @param intervalArg the argument resulting from the call to {@link #sqlIntervalFromSeconds()}
     * @param includeDays whether days should be included in the strings
     * @param daysIsParam whether the "days" should be the second parameter passed in (if not null)
     */
    default String sqlIntervalToDurationString(String intervalArg, boolean includeDays, String daysIsParam) {
        if (daysIsParam != null) {
            return "CASE WHEN "+daysIsParam+" THEN EXTRACT(HOUR FROM "+intervalArg+")::int/24||':'||TO_CHAR(EXTRACT(HOUR FROM "+intervalArg+")::int%24,'FM09')||':'||TO_CHAR("+intervalArg+",'MI:SS') ELSE TO_CHAR("+intervalArg+",'HH24:MI:SS') END";
        } else if (includeDays) {
            return "EXTRACT(HOUR FROM "+intervalArg+")::int/24 || ':' || TO_CHAR(EXTRACT(HOUR FROM "+intervalArg+")::int%24,'FM09') || ':' || TO_CHAR("+intervalArg+", 'MI:SS')";
        } else {
            return "TO_CHAR("+intervalArg+", 'HH24:MI:SS')";
        }
    }

   
   

    /**
     * @return the format for for String.format for getting the time value as HH:MM:SS.mmm
     */
    default String sqlToCharTime() {
        // time values are represented by millisecs since midnight.  Divide by 1000 to get it of the form SSSSS.FF3
		return "TO_CHAR(TO_TIMESTAMP(TO_CHAR(%s/1000, 'FM99990D999'), '"+sqlSecsAndMsecs()+"'), '"+sqlHMSAndMsecs()+"')";
    }

        
    /**
     * @return the function to use for getting the last day of the month of a date.
     */
    default String sqlLastDayOfMonth() {
    	throw new UnsupportedOperationException();
    }
    
    /**
     * Use in the guard for FunctionDate, this returns a date in the given month and year.
     * @param yearValue a number or string representing the year
     * @param monthValue a number of string representing the month
     * @return how to convert from a year and month to a day.
     */
    default String sqlDateFromYearAndMonth(String yearValue, String monthValue) {
        return "TO_DATE(" + yearValue + " || '-' || " + monthValue + ",'YYYY-MM')";
    }
    
    /**
     * @return the format for concatenating strings
     * @param withSpaces whether spaces should be used around the "||" for compatibility
     */
    default String sqlConcat(boolean withSpaces) {
		// Formula engine generally allows || with nulls, as it's less confusing
		return "CONCAT(%s, %s)";
    }

    /**
     * @return how to perform a regular expression check where the text matches java pattern matching... 
     * @param text the argument to validate
     * @param regexp the argument that comprises the regular expression
     */
    default String sqlRegexpLike(String text, String regexp) {
        return "REGEXP_LIKE(" + sqlNvl() + "(" + text + ",'')," + regexp + ")";
    }
    
    /**
     * @return the formula for finding a substring in a string and returning the "position" 1-indexed.
     * In oracle and mysql, it's INSTR.
     * @param strArg the value of the string to search in
     * @param substrArg the value of the substring to search
     */
    default String sqlInstr2(String strArg, String substrArg) {
		return String.format("INSTR(%s, %s)", strArg, substrArg);
    }
    
    
    /**
     * @return the formula for finding a substring in a string and returning the "position" 1-indexed.
     * In oracle and mysql, it's INSTR.  Use binary for INSTR to keep it case sensitive
     * @param strArg the value of the string to search in
     * @param substrArg the value of the substring to search
     * @param startLocation the value of the start location to use as the offset (1-indexed)
     */
    default String sqlInstr3(String strArg, String substrArg, String startLocation) {
		return String.format("INSTR(%s, %s, %s)", strArg, substrArg, startLocation);
    }
    
    /**
     * @return the formula for returning a substring from the given strarg to the right of 
     * the startPosArg, where startPosArg can be negative, with zero startPosArg treated like 1.
     * @param strArg the value of the string to substring
     * @param startPosArg the number of the start position
     */
    default String sqlSubstrWithNegStart(String strArg, String startPosArg) {
        return getSubstringFunction() + "(" + strArg + ", " + sqlRoundScaleArg(startPosArg) + ")";
    }
    
    /**
     * @return the formula for returning a substring from the given strarg to the right of 
     * the startPosArg, where startPosArg can be negative, with zero startPosArg treated like 1,
     * and lengthArgs treated as positive
     * @param strArg the value of the string to substring
     * @param startPosArg the number of the start position
     * @param length the number of characters to return, with negative numbers treated as zero
     */
    default String sqlSubstrWithNegStart(String strArg, String startPosArg, String lengthArg) {
        return getSubstringFunction() + "(" + strArg + ", " + sqlRoundScaleArg(startPosArg) + ", " + sqlEnsurePositive(sqlRoundScaleArg(lengthArg)) + ")";
    }
    
    /**
     * @return a sql expression to convert the date time to a date in the user's timezone
     * (__TZ_ID__)
     * @param dateTime the expression for the date time value.
     * @param userTimezone a string representing the user timezone as an ID
     * @param userTzOffset a string representing the user timezone offset as a number.
     */
    default String sqlConvertDateTimeToDate(String dateTime, String userTimezone, String userTzOffset) {
    	return dateTime;  // Ignore the timezone argument.  This is almost always wrong
    }
    
    /**
     * @param scale the number of digits to the right of the radix
     * @return the SQL string to use to in TO_CHAR for the given scale.  This is used by 
     * {@link #getCurrencyFormat(String, String, boolean)}
     */
    default StringBuilder getCurrencyMask(int scale) {
        StringBuilder mask = new StringBuilder(40).append("'FM9G999G999G999G999G999G990");
        if (scale > 0) {
            mask.append('D');
            for (int i = 0; i < scale; i++) mask.append('0');
        }
        mask.append('\'');
        return mask;
    }
    
	/**
	 * Append the currency format to use for FORMAT_CURRENCY where the maskStr was generated above.
	 * @param sql
	 * @param isoCodeArg
	 * @param amountArg
	 * @param maskStr
	 */
	default void appendCurrencyFormat(StringBuilder sql, String isoCodeArg, String amountArg, CharSequence maskStr) {
        sql.append("CONCAT(").append(isoCodeArg).append(",' ',TO_CHAR(").append(amountArg).append(',').append(maskStr).append("))");
	}
	
    /**
     * String in many DBs are compared using special rules for equality
     * or inequality which doesn't match java/javascript.  This switches
     * equality to match by making them use binary, case-sensitive comparison.
     * 
     * @param str the SQL expression for a string to be compared
     * @param forCompare is it for Greater/LessThan.  otherwise it's for equality which may want different rules.
     * @return the SQL expression that compare the strings using a binary expression
     */

    default Object sqlMakeStringComparable(Object str, boolean forCompare) {
    	return str;
    }
    
    /**
     * @return the SQL string to use to format currency.
     * @param isoCodeArg the argument with the 3 character (ISO 4217) currency code 
     * @param amountArg the argument with the numeric value of the currency
     * @param canAmountBeNull whether the argument can be null (should this be in a coalesce statement)
     */
    default String getCurrencyFormat(String isoCodeArg, String amountArg, boolean canAmountBeNull) {
        // Start with all statically-known currencies in the world and their default scales.
        Map<String,Integer> scaleByIsoCode = FormulaValidationHooks.get().getCurrencyScaleByIsoCode();

        // Now group all currencies by their scale.  Skip all isocodes where the scale is 2.
        // Among the 187 currencies today, 152 of them have a scale of 2, so we're not going
        // to list them all in the sql.  Make sure it's sorted with a TreeSet
        Multimap<Integer,String> isoCodesByScale = Multimaps.newSetMultimap(new HashMap<>(4), ()->new TreeSet<String>());
        for (Map.Entry<String,Integer> entry : scaleByIsoCode.entrySet()) {
            int scale = entry.getValue();
            if (scale != 2) {
                isoCodesByScale.put(scale, entry.getKey());
            }
        }
        
        // Generate the format mask for the Oracle to_char() function.
        StringBuilder maskStr;
        if (isoCodesByScale.isEmpty()) {
            // For the unlikely event where you override every currency in the world to a scale of 2
            maskStr = getCurrencyMask(2);
        } else {
            // Generate a case statement to switch masks according to currency code
            maskStr = new StringBuilder(400).append("CASE ");
            for (Map.Entry<Integer,Collection<String>> entry : isoCodesByScale.asMap().entrySet()) {
                maskStr.append("WHEN ").append(isoCodeArg).append(" IN(");
                for (String isoCode : entry.getValue()) {
                    maskStr.append('\'').append(isoCode).append("',");
                }
                maskStr.deleteCharAt(maskStr.length()-1).append(")THEN").append(getCurrencyMask(entry.getKey()));
            }
            maskStr.append("ELSE").append(getCurrencyMask(2)).append("END");
        }
    	
        // Assume postgres
        // NOTE: This doesn't do the grouping/decimal correction like in oracle, as it isn't possible with postgres
        StringBuilder sql = new StringBuilder(800);
        if (canAmountBeNull) {
            // Handle null amount
            sql.append("CASE WHEN ").append(amountArg).append(" IS NULL THEN NULL ELSE ");
        }
        appendCurrencyFormat(sql, isoCodeArg, amountArg, maskStr);
        if (canAmountBeNull) {
            // Handle null amount
            sql.append("END");
        }
        return sql.toString();
    }
    
    /**
     * Formulas are usually numeric, but some functions, like round or trunc, require a cast to ::int in postgres
     * @param argument scale argument
     * @return the argument converted to an integer for rounding
     */
    default String sqlRoundScaleArg(String argument) {
    	return argument;
    }

    /**
     * Formulas are usually numeric, but some functions, like round or trunc, return an integer that may
     * cause type cast issues
     * @param argument argument that's in an integer
     * @return the argument converted from an integer to numeric for use
     */
    default String sqlMakeDecimal(String argument) {
    	return argument;
    }
    
    
    /**
     * @param argument the SQL argument that has a percent value stored in the DB (i.e. 100.0 is 100% and should be treated at 1)
     * @return a sql expression that will convert the arguemnt from a percent to a decimal fractions
     */
    default String sqlConvertPercent(String argument) {
        return "(" + argument + " / 100.0)";
    }
    
    /**
     * @param argument the value to truncate
     * @return how to call truncate to drop all decimal places
     */
    default String sqlTrunc(String argument) {
    	return "TRUNC(" + argument + ")";
    }
    
    
    /**
     * @param argument the value to truncate
     * @param scale the scale to truncate to
     * @return how to call truncate to drop to the given number of decimal places
     */
    default String sqlTrunc(String argument, String scale) {
    	return "TRUNC(" + argument + ", " + scale + ")";
    }

    /**
     * Parameterized for those DBs that don't handle negative rounding
     * @param argument the value to round
     * @param scale the scale to truncate to
     * @return how to call round to drop to the given number of decimal places
     */
    default String sqlRound(String argument, String scale) {
        return "ROUND(" + argument + ", " + scale + ")";
    }
    
    
    /**
     * @param argument the value to exponent.
     * @return how to call exponent on the function.  (i.e. 2^n)
     */
    default String sqlExponent(String argument) {
    	return "EXP(" + argument + ")";
    }
    
    /**
     * @param argument the value to get the natural log.
     * @return how to call exponent on the function.  (i.e. log base e)
     */
    default String sqlLogBaseE(String argument) {
        return "LN(" + argument + ")";
    }
    
    /**
     * @param argument the value to get the natural log.
     * @return how to call exponent on the function.  (i.e. log base e)
     */
    default String sqlLogBase10(String argument) {
        return "LOG(10, " + argument + ")";
    }
    
    /**
     * @param number the number to get the remainer 
     * @param modulus the modulus 
     * @return how to do modulo operation
     */
    default String sqlMod(String number, String modulus) {
        return "MOD(" + number + ", " + modulus + ")";
    }
    
    /**
     * @param argument the value that is a function call to a trig function.
     * @return how to call a trigonometric on the function and return the right numeric value (cast to decimal)
     */
    default String sqlTrigConvert(String argument) {
        return argument;
    }
    
    /**
     * @param arg the numeric value to ensure that it is positive
     * @return a sql expression that will return the argument, or 0 if the argument is negative
     */
    default String sqlEnsurePositive(String arg) {
    	return sqlGreatest(arg, "0");
    }
    
    /**
     * @param arg1 the numeric value to compare
     * @param arg2 the other numeric value to compare
     * @return a sql expression that will return the max of the two arguments
     */
    default String sqlGreatest(String arg1, String arg2) {
    	return "GREATEST(" + arg1 + "," + arg2 + ")";
    }
    
    /**
     * Get the sql guard and value for executing A^B.
     * @param args the arguments where arg[0] is the operands and arg[1] is the exponent
     * @param guards the guards to prevent errors when evaluating the args
	 */
    default SQLPair getPowerSql( String[] args, String[] guards) {
        String sql = "POWER(" + args[0] + ", " + args[1] + ")";
        String guard = SQLPair.generateGuard(guards, "TRUNC(" + args[1] + ")<>" + args[1] +
    	            " OR(" + args[0] + "<>0 AND LOG(10,ABS(" + args[0] + "))*" + args[1] + ">38)");    	
        return new SQLPair(sql, guard);
    }
    
    /**
     * @param str the string to pad
     * @param amount the number of characters to ensure (which will be rounded with sqlScaleArg and sqlGreatest)
     * @param pad optional argument (may be null) with the string to pad with
     * @return a sql expression that will return LPAD 
     */
    default String sqlLpad(String str, String amount, String pad) {
        if (pad != null) {
        	return "LPAD(" + str + ", " + amount + ", " + pad + ")";
        } else {
        	return "LPAD(" + str + ", " + amount + ")";
        }
    }
    
    /**
     * @param str the string to pad
     * @param amount the number of characters to ensure (which will be rounded with sqlScaleArg and sqlGreatest)
     * @param pad optional argument (may be null) with the string to pad with
     * @return a sql expression that will return RPAD 
     */
    default String sqlRpad(String str, String amount, String pad) {
        if (pad != null) {
        	return "RPAD(" + str + ", " + amount + ", " + pad + ")";
        } else {
        	return "RPAD(" + str + ", " + amount + ")";
        }
    }

    /**
     * @return How to generate a "LIKE" comparison with an escape of a backslash character
     * @param str the string to compare
     * @param like the LIKE style string
     */
    default String sqlLike(String str, String like) {
        return str + " LIKE " + like + " ESCAPE '\\'";
    }
}
