/**
 * 
 */
package com.force.formula.impl.sql;

import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaTime;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.sql.SQLPair;

/**
 * Implementation of FormulaSqlHooks for Mysql-style DBs
 * @author stamm
 * @since 0.2
 */
public interface FormulaMySQLHooks extends FormulaSqlHooks {
	@Override
	default boolean isMysqlStyle() {
		return true;
	}	
	
	// Mysql doesn't need guards as it returns null for invalid dates instead of an error
	@Override
    default String sqlDatetimeValueGuard() {
   	   return "1=0"; // mysql returns null on error
    }
	@Override
    default String sqlDateValueGuard() {
  	   return "1=0"; // mysql returns null on error
    }
	@Override
    default String sqlTimeValueGuard() {
   	   return "1=0"; // mysql returns null on error
    }
    
    /**
     * @return whether the string is a "number"
     */
	@Override
    default String sqlIsNumber() {
   	   return "REGEXP_REPLACE(%s,'[0-9]+','0') REGEXP '^[+-]?(0|0\\\\.|\\\\.0|0\\\\.0)([Ee][+-]?0)?$'";
    }
    
    /**
     * @return the function to use for NVL.  In mysql, it's usually coalesce, but in oracle, you want NVL.
     */
    @Override
    default String sqlNvl() {
		return "COALESCE";
    }
    
    /**
     * @return the string to use for String.format to convert something to date generically, without a specified
     */
    @Override
    default String sqlToDate(Type dateType) {
		return "CAST(%s AS DATE)";
    }

    /**
     * @return the string to use for String.format to convert something to number generically, without a format
     */
    @Override
    default String sqlToNumber() {
		return "CAST(%s AS DECIMAL(52,18))";   // Override to specify your own default precision.
    }

    /**
     * @return the string to use for String.format to convert something to text generically, without a format
     */
    @Override
    default String sqlToChar() {
		return "CONVERT(%s,CHAR)";
    }
    
    @Override
    default String sqlToCharTimestamp() {
    	return sqlToChar();
     }
    
    @Override
    default String sqlToCharDate() {
    	return sqlToChar();
    }

    @Override
    default String sqlToCharTime() {
        return "SUBSTRING(CONVERT(%s,CHAR),1,12)";  // mysql uses microseconds
    }

    /**
     * @return the function that allows subtraction of two timestamps to get the microsecond/day difference.  This is
     * missing from mysql, but available in oracle.  This allows you to try and fix that.
     */
    @Override
    default String sqlSubtractTwoTimestamps(boolean inSeconds, Type dateType) {
    	return inSeconds ? "-TIMESTAMPDIFF(SECOND,%s,%s)": "(-TIMESTAMPDIFF(SECOND,%s,%s)/86400)";
    	//return "((UNIX_TIMESTAMP(%s)-UNIX_TIMESTAMP(%s))/86400)";
    } 
    
	
	@Override
    default String sqlAddDaysToDate(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType, boolean isAddition) {
        if (lhsDataType == Date.class || lhsDataType==FormulaDateTime.class) {
            // <date|timestamp> <+|-> <number>
        	if (!isAddition) {
                return String.format("DATE_SUB(%s, INTERVAL ROUND(%s*86400) SECOND)", lhsValue, rhsValue);
        	} else {
                return String.format("DATE_ADD(%s, INTERVAL ROUND(%s*86400) SECOND)", lhsValue, rhsValue);
        	}
        } else {
            return String.format("DATE_ADD(%s, INTERVAL ROUND(%s*86400) SECOND)", rhsValue, lhsValue);
        }
     }
	
	@Override
    default String sqlAddMillisecondsToTime(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType,  boolean isAddition) {
   		if (rhsDataType == FormulaTime.class) {
            return "(UNIX_TIMESTAMP(SUBTIME(" + lhsValue + ", " + rhsValue + "))%86400)*1000";
		} else {        	
            if (!isAddition) {
            	return "TIME(DATE_SUB(" + lhsValue + ",INTERVAL MOD(" + rhsValue + "/1000,86400) SECOND))";
            } else {
                return "TIME(DATE_ADD(" + lhsValue + ",INTERVAL MOD(" + rhsValue + "/1000,86400) SECOND))";
            }
		}
    }
	
    @Override
    default String sqlExtractTimeFromDateTime(String dateTimeExpr) {
        return String.format("TIME(%s)", dateTimeExpr);
    }
    
    @Override
    default String sqlParseTime(String stringExpr) {
	    // Note, this doesn't work for MariaDB... 
        return String.format("TIME(%s)", stringExpr);
    }
	
    @Override
    default String sqlConstructDate(String yearSql, String monthSql, String daySql) {
        return "DATE(CONCAT(" + yearSql + ",'-'," + monthSql + ",'-'," + daySql + "))";
    }
	
    /**
     * Function right can be... complicated, especially in Oracle
     * @param stringArg the sql for the string value
     * @param countArg the sql for the number of chars
     * @return the SQL for generating RIGHT()
     */
    @Override
    default String sqlRight(String stringArg, String countArg) {
        return "RIGHT(" + stringArg + ", GREATEST(" + countArg+ ", 0))";
    }
  
    /**
     * @return the the current milliseconds of the day suitable.  You may want to use ::time instead, so overridable
     */
    @Override
    default String sqlTimeNow() {
        return "CAST(UNIX_TIMESTAMP(CURTIME(3)) * 1000 AS unsigned)";
    }
    
    /**
     * @return a date literal suitable for for representing "today"
     * @param c the calendar to use 
     */
    @Override
    default String getDateLiteralFromCalendar(Calendar c) {
    	return "DATE('" + c.get(Calendar.YEAR) + "-"
                + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DATE) + "')";
    }
    
    
    /**
     * @return the format to use for adding months.
     */
    @Override
    default String sqlAddMonths(String dateArg, Type dateArgType, String numMonths) {
		return String.format("DATE_ADD(%s, INTERVAL TRUNCATE(%s,0) MONTH)", dateArg, numMonths);
    }
    

    /**
     * @return how to get the unix epoch from a given date for String.format
     */
	@Override
    default String sqlGetEpoch(Type dateType) {
    	return "UNIX_TIMESTAMP(%s)";
    }
    
    /**
     * @return how to get the number of seconds in a day from a time value for String.format
     */
	@Override
    default String sqlGetTimeInSeconds() {
    	return "TIME_TO_SEC(%s)";
    }
	
    /**
     * @return how to get a DateTime from a unix epoch time, suitable for String.format
     */
    @Override
    default String getDateFromUnixTime() {
    	return "FROM_UNIXTIME(%s)";
    }

    /**
     * @return the format for converting to a datetime value
     */
    @Override
    default String sqlToTimestampIso() {
		return "TIMESTAMP(%s)";
    }
    
    /**
     * @return the format for converting to a datetime value
     */
    @Override
    default String sqlToDateIso() {
		return "STR_TO_DATE(%s, '%%Y-%%m-%%d')";
    }
    
    
    /**
     * @return the function to use for getting the last day of the month of a date.
     */
    @Override
    default String sqlLastDayOfMonth() {
		return "LAST_DAY(%s)";
    }
    
    @Override
    default String sqlDateFromYearAndMonth(String yearValue, String monthValue) {
        return "DATE(CONCAT(" + yearValue + ",'-'," + monthValue + ",'-01'))";
    }
    
    
    
    @Override
    default String sqlChronoUnit(ChronoUnit field, Type dateType) {
        switch (field) {
        case HOURS:
            return "HOUR(%s)";
        case MINUTES:
            return "MINUTE(%s)";
        case SECONDS:
            return "SECOND(%s)";
        case MILLIS:            
            return "1000*MICROSECOND(%s)";
        default:
        }
        return FormulaSqlHooks.super.sqlChronoUnit(field, dateType);
    }
    
    @Override
    default String sqlGetWeekday() {
        return "DAYOFWEEK(%s)";
    }
    
    @Override
    default String sqlGetIsoWeek() {
		return "CAST(DATE_FORMAT(%s,'%%v') AS DECIMAL(52,18))";
    }
    
    @Override
    default String sqlGetIsoYear() {
		return "CAST(DATE_FORMAT(%s,'%%x') AS DECIMAL(52,18))";
    }
    
    @Override
    default String sqlGetDayOfYear() {
		return "CAST(DATE_FORMAT(%s,'%%j') AS DECIMAL(52,18))";
    }
    
    @Override
    default String sqlInitCap(boolean hasLocaleOverride) {
    	return "%s";  // Unsupported https://bugs.mysql.com/bug.php?id=2340
    }

    @Override
    default String sqlChr() {
    	return "CHAR(TRUNCATE(%s,0) USING ucs2)";
    }
    
    @Override
    default String sqlAscii() {
    	// They didn't make this easy...
    	return "CONV(HEX(CONVERT(LEFT(%s,1) using ucs2)),16,10)"; 
    }
    
    /**
     * @return the format for converting to a datetime value
     * @param withSpaces whether spaces should be used around the "||" for compatibility
     */
    @Override
    default String sqlConcat(boolean withSpaces) {
		return "CONCAT_WS(\"\",%s, %s)";  // CONCAT doesn't skip nulls
    }

    /**
     * @return the formula for finding a substring in a string and returning the "position" 1-indexed.
     * In oracle and mysql, it's INSTR.
     * @param strArg the value of the string to search in
     * @param substrArg the value of the substring to search
     */
    @Override
    default String sqlInstr2(String strArg, String substrArg) {
		return String.format("INSTR(binary %s, %s)", strArg, substrArg);
    }
    
    
    /**
     * @return the formula for finding a substring in a string and returning the "position" 1-indexed.
     * In oracle and mysql, it's INSTR.  Use binary for INSTR to keep it case sensitive
     * @param strArg the value of the string to search in
     * @param substrArg the value of the substring to search
     * @param startLocation the value of the start location to use as the offset (1-indexed)
     */
    @Override
    default String sqlInstr3(String strArg, String substrArg, String startLocation) {
		// Use binary comparison to be case sensitive
		return String.format("CASE WHEN COALESCE(INSTR(binary SUBSTR(%s,%s),%s),0) > 0 THEN INSTR(binary SUBSTR(%s,%s),%s) + %s - 1 ELSE 0 END", strArg, startLocation, substrArg, strArg, startLocation, substrArg, startLocation);
    }
    
    // Turn 0 into 1 to match oracle/postgres semantics
    @Override
    default String sqlSubstrWithNegStart(String strArg, String startPosArg) {
        return getSubstringFunction() + "(" + strArg + ", CASE WHEN " + startPosArg + " = 0 THEN 1 ELSE " + startPosArg + " END)";
    }
    
    @Override
    default String sqlSubstrWithNegStart(String strArg, String startPosArg, String lengthArg) {
        return getSubstringFunction() + "(" + strArg + ", CASE WHEN " + startPosArg + " = 0 THEN 1 ELSE " + startPosArg  + " END, " + sqlEnsurePositive(sqlRoundScaleArg(lengthArg)) + ")";
    }
    

	@Override
	default String sqlConvertDateTimeToDate(String dateTime, String userTimezone, String userTzOffset) {
		/* CONVERT_TZ is very finicky to get working.  This doesn't work in testing without some
		 implementation dependent things
		 % mysql_tzinfo_to_sql /usr/share/zoneinfo | mysql -u root -p mysql
		 */
		
		// return "DATE(CONVERT_TZ("+dateTime+",'UTC',"+userTzOffset+"))";

		// This does the java-based offset way which is wrong for daylight savings and
		// historical timezone shifts.
		return "DATE(ADDDATE("+dateTime+",INTERVAL "+userTzOffset + " HOUR))";
	}
	
    /**
     * Intervals in mysql aren't helpful for formatting, so don't use them.
     */
    @Override
    default String sqlIntervalFromSeconds() {
        return "TRUNCATE(ABS(%s),0)";
    }

    @Override
    default String sqlSubtractTwoTimes() {
        return "TIME_TO_SEC(TIMEDIFF(%s,%s))";
    } 
    
    
    @Override
    default String sqlIntervalToDurationString(String arg, boolean includeDays, String daysIsParam) {
        String result;
        if (daysIsParam != null) {
            result = "(CASE WHEN "+daysIsParam+" THEN CONCAT(TRUNCATE(("+arg+")/86400,0),':',TIME_FORMAT(SEC_TO_TIME("+arg+"%86400),'%H:%i:%s')) ELSE TIME_FORMAT(SEC_TO_TIME("+arg+"),'%H:%i:%s') END)";
        } else if (includeDays) {
            result = "CONCAT(TRUNCATE(("+arg+")/86400,0),':',TIME_FORMAT(SEC_TO_TIME("+arg+"%86400),'%H:%i:%s'))";
        } else {
            result = "TIME_FORMAT(SEC_TO_TIME("+arg+"),'%H:%i:%s')";
        }
        return result;
    }
	
	@Override
    default StringBuilder getCurrencyMask(int scale) {
		return new StringBuilder(3).append(' ').append(Integer.toString(scale)).append(' ');
    }

	@Override
	default void appendCurrencyFormat(StringBuilder sql, String isoCodeArg, String amountArg, CharSequence maskStr) {
        sql.append("CONCAT_WS(\"\",").append(isoCodeArg).append(",' ',FORMAT(").append(amountArg).append(',').append(maskStr).append("))");
	}
	
	@Override
    default String sqlTrunc(String argument) {
    	return "TRUNCATE(" + argument + ",0)";
    }
		
	@Override
    default String sqlTrunc(String argument, String scale) {
    	return "TRUNCATE(" + argument + "," + scale + ")";
    }
	
	@Override
    default SQLPair getPowerSql( String[] args, String[] guards) {
        String sql = "POWER(" + args[0] + ", " + args[1] + ")";
		String guard = SQLPair.generateGuard(guards, "TRUNCATE(" + args[1] + ",0)<>" + args[1] +
	            " OR(" + args[0] + "<>0 AND LOG(10,ABS(" + args[0] + "))*" + args[1] + ">38)");    
        return new SQLPair(sql, guard);
    }
	
	@Override
    default String sqlLpad(String str, String amount, String pad) {
		// You must supply the pad in mysql
    	if (pad == null) {
        	return "LPAD(" + str + ", " + amount + ", ' ')";    		
    	} else {
    		return FormulaSqlHooks.super.sqlLpad(str, amount, pad);
    	}
    }

	@Override
    default String sqlRpad(String str, String amount, String pad) {
		// You must supply the pad in mysql
    	if (pad == null) {
        	return "RPAD(" + str + ", " + amount + ", ' ')";    		
    	} else {
    		return FormulaSqlHooks.super.sqlRpad(str, amount, pad);
    	}
    }
	
	@Override
    default Object sqlMakeStringComparable(Object str, boolean forCompare) {
		return "binary " + str;
    }
	
	@Override
    default String sqlLike(String str, String like) {
        return str + " LIKE BINARY " + like;
    }
	
	interface MariaDBHooks extends FormulaMySQLHooks {
	    @Override
	    default String sqlParseTime(String stringExpr) {
	        // TIME() doesn't parse times in MariaDB, unlike Mysql.  So use str_to_date to parse fractional
	        return String.format("TIME(STR_TO_DATE(%s,'%%T.%%f'))", stringExpr);
	    }
	    
	    /**
	     * Unix timestamp is fractional in MariaDB
	     * See https://mariadb.com/kb/en/unix_timestamp/
	     */
	    @Override
	    default String sqlGetEpoch(Type dateType) {
	        return "FLOOR(UNIX_TIMESTAMP(%s))";
	    }
	    
	    @Override
	    default String sqlGetTimeInSeconds() {
	        return "FLOOR(TIME_TO_SEC(%s))";
	    }

	    @Override
	    default String sqlAddMillisecondsToTime(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType,  boolean isAddition) {
	        if (rhsDataType == FormulaTime.class) {
	            // Use TIMEDIFF.  SUBTIME is better in Mysql since it handles negatives correctly.
	            return "FLOOR(TIME_TO_SEC(TIMEDIFF(" + lhsValue + ", " + rhsValue + "))*1000)";
	        } else {
	            return FormulaMySQLHooks.super.sqlAddMillisecondsToTime(lhsValue, lhsDataType, rhsValue, rhsDataType, isAddition);
	        }
	    }
	    
	}
	
}
