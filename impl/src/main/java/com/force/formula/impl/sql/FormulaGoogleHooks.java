/**
 * 
 */
package com.force.formula.impl.sql;

import java.lang.reflect.Type;
import java.util.Date;

import com.force.formula.FormulaDateTime;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.sql.SQLPair;

/**
 * Implementation of FormulaSqlHooks for Google-style DBs (BigTable/Spanner)
 * @author stamm
 * @since 0.3
 */
public interface FormulaGoogleHooks extends FormulaSqlHooks {
	@Override
	default boolean isGoogleStyle() {
		return true;
	}	
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
    @Override
    default String sqlIsNumber() {
       return "REGEXP_CONTAINS(REGEXP_REPLACE(%s,'[0-9]+','0'),'^[+-]?(0|0\\\\.|\\\\.0|0\\\\.0)([Ee][+-]?0)?$')";
    }
	   
    /**
     * @return the function to use for NVL.  In postgres, it's usually coalesce, but in oracle, you want NVL.
     */
	@Override
    default String sqlNvl() {
		return "COALESCE";
    }
    
    /**
     * @return the string to use for String.format to convert something to date generically, without a specified
     */
	@Override
    default String sqlToDate(Type type) {
	    if (type == Date.class) {
	        return "CAST(%s AS DATE)";
	    } else {
            return "CAST(%s AS TIMESTAMP)";
	    }    
    }

    /**
     * @return the string to use for String.format to convert something to number generically, without a format
     */
	@Override
    default String sqlToNumber() {
		return "CAST(%s AS NUMERIC)";
    }

    /**
     * @return the string to use for String.format to convert something to text generically, without a format
     */
	@Override
    default String sqlToChar() {
		return "CAST(%s AS STRING)";
    }
   
    
    @Override
    default String sqlConstructDate(String yearSql, String monthSql, String daySql) {
        // TODO: Doing toChar then back is expensive for little gain
        return "DATE(CAST(" + yearSql + " as INT64),CAST(" + monthSql + " AS INT64),CAST(" + daySql + " AS INT64))";
    }
  
	
    
    @Override
    default String sqlExtractTimeFromDateTime(String dateTimeExpr) {
        return String.format("MOD(UNIX_MILLIS(%s),86400000)", dateTimeExpr);
    }
    
    @Override
    default String sqlParseTime(String stringExpr) {
        return String.format("MOD(UNIX_MILLIS(PARSE_TIMESTAMP('%%H:%%M:%%E3S', %s, 'UTC')),86400000)", stringExpr);
    }
	

	@Override
    default String sqlSubtractTwoTimestamps(boolean inSeconds, Type dateType) {
	    if (dateType == Date.class) {
	        return "CAST(DATE_DIFF(%s,%s,DAY) AS NUMERIC)";
	    }
        return inSeconds ? "CAST(TIMESTAMP_DIFF(%s,%s,SECOND) AS NUMERIC)" 
                : "CAST(TIMESTAMP_DIFF(%s,%s,SECOND) AS NUMERIC)/86400";  
    } 
	
	@Override
    default String sqlToCharTime() {
        // time values are represented by millisecs since midnight.  
        return "FORMAT_TIMESTAMP('%%H:%%M:%%E3S',timestamp_millis(%s),'UTC')";
    }
    
    @Override
    default String sqlAddDaysToDate(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType, boolean isAddition) {
        if (lhsDataType == Date.class) {
            // <date|timestamp> <+|-> <number>
            if (!isAddition) {
                return String.format("DATE_SUB(%s, INTERVAL CAST(ROUND(%s) AS INT64) DAY)", lhsValue, rhsValue);
            } else {
                return String.format("DATE_ADD(%s, INTERVAL CAST(ROUND(%s) AS INT64) DAY)", lhsValue, rhsValue);
            }
        } else if (lhsDataType==FormulaDateTime.class) {
            if (!isAddition) {
                return String.format("TIMESTAMP_SUB(%s, INTERVAL CAST(ROUND(%s*86400) AS INT64) SECOND)", lhsValue, rhsValue);
            } else {
                return String.format("TIMESTAMP_ADD(%s, INTERVAL CAST(ROUND(%s*86400) AS INT64) SECOND)", lhsValue, rhsValue);
            }
        } else if (rhsDataType == Date.class){
            return String.format("DATE_ADD(%s, INTERVAL CAST(ROUND(%s) AS INT64) DAY)", rhsValue, lhsValue);
        } else {
            return String.format("TIMESTAMP_ADD(%s, INTERVAL CAST(ROUND(%s*86400) AS INT64) SECOND)", rhsValue, lhsValue);
        }
    }

    /**
     * Function right can be... complicated, especially in Oracle
     * @param stringArg the sql for the string value
     * @param countArg the sql for the number of chars
     * @return the SQL for generating RIGHT()
     */
	@Override
    default String sqlRight(String stringArg, String countArg) {
	    return "SUBSTR(" + stringArg + ", -CAST(" + countArg+ " AS INT64))";
    }
    /**
     * @return the the current milliseconds of the day suitable.  
     */
	@Override
    default String sqlTimeNow() {
        return "MOD(UNIX_MILLIS(CURRENT_TIMESTAMP()),86400000)";
    }
    
    /**
     * @see specification in FunctionAddMonths.java
     * @return the format to use for adding months.
     * 
     * This is made rather difficult because interval month isn't supported for DateTimes in spanner.
     * Which is... unpleasant.
     */
	@Override
    default String sqlAddMonths(String dateArg, Type dateArgType, String numMonths) {
	            
	    StringBuffer sb = new StringBuffer();
	    sb.append(" (CASE");
	    if (dateArgType == Date.class) {
	        sb.append(" WHEN extract(day FROM (date_sub(date_add(date_trunc("+dateArg+", month),interval 1 month), interval 1 day))) =");
	        sb.append(" extract(day FROM (date_trunc("+ dateArg+", day)))");
	    } else {
            sb.append(" WHEN extract(day FROM (date_sub(date_add(DATE("+dateArg+",'UTC'),interval 1 month), interval 1 day))) =");
            sb.append(" extract(day FROM (timestamp_trunc("+dateArg+", day, 'UTC')))");
	    }
	    sb.append(" THEN 1 ELSE 0 END)");
        String dayAddition = sb.toString();

        if (dateArgType == Date.class) {
            return String.format("date_sub(date_add(date_add(%s, interval " + dayAddition + " day),interval CAST(TRUNC(%s) AS INT64) month), interval " + dayAddition + " day)", dateArg, numMonths);
        } else {
            // We can't add a month interval to a timestamp, so we have to do something... fun.
            // Which is do all the date math and then add the millisecond part back at the end...
            return String.format("timestamp_add(timestamp(date_sub(date_add(date_add(DATE("+dateArg+",'UTC'), interval " + dayAddition + " day),interval CAST(TRUNC(%s) AS INT64) month),interval " + dayAddition + " day),'UTC'), INTERVAL MOD(UNIX_MILLIS(%s),86400000) MILLISECOND)", numMonths, dateArg);
        }
        
        
    }

    @Override
    default String sqlToCharTimestamp() {
        return "FORMAT_TIMESTAMP('%%Y-%%m-%%d %%H:%%M:%%S', %s, 'UTC')";
    }

    /**
     * @return the format for String.format for converting from a date value to a string YYYY-MM-DD
     */
    @Override
    default String sqlToCharDate() {
        return "FORMAT_TIMESTAMP('%%Y-%%m-%%d', %s, 'UTC')";
    }
	
    /**
     * @return the format for converting to a datetime value
     */
	@Override
    default String sqlToTimestampIso() {
		return "PARSE_TIMESTAMP('%%Y-%%m-%%d %%H:%%M:%%S', TRIM(%s), 'UTC')";
    }
    
    @Override
    default String sqlToDateIso() {
        return "PARSE_DATE('%%Y-%%m-%%d', %s)";
    }
    
	
    /**
     * @return the function to use for getting the last day of the month of a date.
     */
	@Override
    default String sqlLastDayOfMonth() {
	    // LastDay works in bigtable, but not spanner as of 2022
	    //return "LAST_DAY(%s)";
	    return "EXTRACT(DAY FROM DATE_SUB(DATE_ADD(DATE_TRUNC(%s,month),interval 1 month), interval 1 day))";
    }
	
    @Override
    default String sqlDateFromYearAndMonth(String yearValue, String monthValue) {
        return "DATE(CAST(" + yearValue + " AS INT64),CAST(" + monthValue + " AS INT64),1)";
    }

    /**
     * @return how to get the unix epoch from a given date for String.format
     */
	@Override
    default String sqlGetEpoch(Type type) {
	    if (type == FormulaDateTime.class) {
	        return "UNIX_SECONDS(%s)";
	    }
    	return "UNIX_SECONDS(TIMESTAMP(%s,'UTC'))";
    }
    
    /**
     * @return how to get a DateTime from a unix epoch time, suitable for String.format
     */
    @Override
    default String getDateFromUnixTime() {
    	return "TIMESTAMP_SECONDS(CAST(%s AS INT64))";
    }

    @Override
    default String sqlGetWeekday() {
        return "EXTRACT (DAYOFWEEK FROM %s)";
    }
    
    @Override
    default String sqlGetIsoWeek() {
		return "EXTRACT(ISOWEEK FROM %s)";
    }
    
    @Override
    default String sqlGetIsoYear() {
		return "EXTRACT(ISOYEAR FROM %s)";
    }
    
    @Override
    default String sqlGetDayOfYear() {
		return "EXTRACT(DAYOFYEAR FROM %s)";
    }
    
    @Override
    default String sqlInitCap(boolean hasLocaleOverride) {
        return "%s";  // not implementable in google standard sql.
    }
        
    @Override
    default String sqlChr() {
    	return "CODE_POINTS_TO_STRING(ARRAY(SELECT CAST(TRUNC(%s) AS INT64)))";
    }
    
    @Override
    default String sqlAscii() {
    	return "ARRAY_FIRST(TO_CODE_POINTS(%s))";
    }
    
    /**
     * @return the format for converting to a datetime value
     * @param withSpaces whether spaces should be used around the "||" for compatibility
     */
	@Override
    default String sqlConcat(boolean withSpaces) {
	    // No concat_ws or anything, so we go with this.  Null behavior is different though.
        return "COALESCE(%s,'')||COALESCE(%s,'')";
    }

    /**
     * @return the formula for finding a substring in a string and returning the "position" 1-indexed.
     * In oracle and mysql, it's INSTR.
     * @param strArg the value of the string to search in
     * @param substrArg the value of the substring to search
     */
	@Override
    default String sqlInstr2(String strArg, String substrArg) {
		return String.format("STRPOS(%s, %s)", strArg, substrArg);
    }
    
    
    /**
     * @return the formula for finding a substring in a string and returning the "position" 1-indexed.
     * In oracle and mysql, it's INSTR.  StrPos doesn't take an argument in spanner, so we have to 
     *  evaluate all the strings a lot.
     * @param strArg the value of the string to search in
     * @param substrArg the value of the substring to search
     * @param startLocation the value of the start location to use as the offset (1-indexed)
     */
	@Override
    default String sqlInstr3(String strArg, String substrArg, String startLocation) {
        return String.format("CASE WHEN COALESCE(STRPOS(SUBSTR(%s,CAST(%s AS INT64)),%s),0) > 0 THEN STRPOS(SUBSTR(%s,CAST(%s AS INT64)),%s) + %s - 1 ELSE 0 END", strArg, startLocation, substrArg, strArg, startLocation, substrArg, startLocation);
    }
    
	@Override
    default Object sqlMakeStringComparable(Object str, boolean forCompare) {
    	return str;
    }
    
    /**
     * Formulas are usually numeric, but some functions, like round or trunc, require a cast to ::int in postgres
     * @param argument scale argument
     * @return the argument converted to an integer for rounding
     */
    @Override
    default String sqlRoundScaleArg(String argument) {
		return "CAST("+argument + " AS INT64)";
    }

    /**
     * Formulas are usually numeric, but some functions, like round or trunc, return an integer that may
     * cause type cast issues
     * @param argument argument that's in an integer
     * @return the argument converted from an integer to numeric for use
     */
    @Override
    default String sqlMakeDecimal(String argument) {
		return argument;
    }

	@Override
	default String sqlConvertDateTimeToDate(String dateTime, String userTimezone, String userTzOffset) {
		return "DATE("+dateTime+",'"+userTimezone+"')";
	}

	@Override
	default String sqlExponent(String argument) {
        // tests showed double precision was only 2.5% faster than numeric (i.e. the rest of
        // the query processing machinery dominates, so go for max precision
		return "EXP(" + argument + ")";
	}

    @Override
    default String sqlLogBase10(String argument) {
        return String.format(sqlToNumber(), "LOG10(" + argument + ")");
    }
    
    @Override
    default String sqlTrigConvert(String argument) {
        return argument;   // Override to specify your own default precision.
    }
    
    @Override
    default SQLPair getPowerSql( String[] args, String[] guards) {
        String sql = "POWER(" + args[0] + ", " + args[1] + ")";
        // Google's doesn't do short circuits for AND, so we have to use IF here
        String guard = SQLPair.generateGuard(guards, "TRUNC(" + args[1] + ")<>" + args[1] +
                    " OR(IF(" + args[0] + "<>0,LOG10(ABS(" + args[0] + "))*" + args[1] + ">38,FALSE))");     
        return new SQLPair(sql, guard);
    }
    
    // Google Standard SQL doesn't have any interval or time formatting functions, so we just use timestamps for everything
    @Override
    default String sqlIntervalFromSeconds() {
        return "CAST(TRUNC(ABS(%s),0) AS INT64)";
    }

    @Override
    default String sqlIntervalToDurationString(String arg, boolean includeDays, String daysIsParam) {
        String result;
        if (daysIsParam != null) {
            result = "(CASE WHEN "+daysIsParam+" THEN CONCAT(FORMAT('%.0f',TRUNC("+arg+"/86400)),':',FORMAT_TIMESTAMP('%H:%M:%S', timestamp_seconds(MOD("+arg+",86400)),'UTC')) ELSE CONCAT(FORMAT('%02.0f',TRUNC("+arg+"/3600)),':',FORMAT_TIMESTAMP('%M:%S',timestamp_seconds("+arg+"))) END)";
        } else if (includeDays) {
            result = "CONCAT(FORMAT('%.0f',TRUNC("+arg+"/86400)),':',FORMAT_TIMESTAMP('%H:%M:%S', timestamp_seconds(MOD("+arg+",86400)),'UTC'))";
        } else {
            result = "CONCAT(FORMAT('%02.0f',TRUNC("+arg+"/3600)),':',FORMAT_TIMESTAMP('%M:%S', timestamp_seconds("+arg+"), 'UTC'))";
        }
        return result;
    }
    
    @Override
    default StringBuilder getCurrencyMask(int scale) {
        return new StringBuilder(6).append("\"%'.").append(Integer.toString(scale)).append("f\"");
    }

    @Override
    default void appendCurrencyFormat(StringBuilder sql, String isoCodeArg, String amountArg, CharSequence maskStr) {
        sql.append("CONCAT(COALESCE(").append(isoCodeArg).append(",''),' ',FORMAT(").append(maskStr).append(',').append(amountArg).append("))");
    }

    
}
