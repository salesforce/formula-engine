/**
 * 
 */
package com.force.formula.impl.sql;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

import com.force.formula.FormulaDateTime;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaDateUtil;

/**
 * Implementation of FormulaSqlHooks for Sqlite3
 * @author stamm
 * @since 0.3
 */
public interface FormulaSqliteHooks extends FormulaSqlHooks {
    @Override
	default boolean isSqliteStyle() {
		return true;
	}	
	
    @Override
    default int getExternalPrecision() {
        return -1;  // Don't do any rounding for ceil/floor because reals.
    }

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
      * @return the function to use for NVL.  In postgres, it's usually coalesce, but in oracle, you want NVL.
      */
     @Override
     default String sqlNvl() {
         return "COALESCE";
     }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a number Value
     */
    @Override
    default String sqlIsNumber() {
         return "(%s REGEXP '^[+-]?([0-9]+|[0-9]+\\.|\\.[0-9]+|[0-9]+\\.[0-9]+)([Ee][+-]?[0-9]+)?$')";
    }
    
    @Override
    default String sqlGreatest(String arg1, String arg2) {
        return "MAX(" + arg1 + "," + arg2 + ")";
    }
    
    
    @Override
    default String sqlChronoUnit(ChronoUnit field, Type dateType) {
        switch (field) {
        case YEARS: 
            return "strftime('%%Y',%s)";
        case MONTHS:
            return "strftime('%%m',%s)";
        case DAYS:
            return "strftime('%%d',%s)";
        case HOURS:
            return "strftime('%%H',%s)";
        case MINUTES:
            return "strftime('%%M',%s)";
        case SECONDS:
            return "strftime('%%S',%s)";
        case MILLIS:            
            return "strftime('%%f',%s)";
        default:
        }
        return FormulaSqlHooks.super.sqlChronoUnit(field, dateType);
    }

    /**
     * @return the string to use for String.format to convert something to date generically, without a specified
     */
	@Override
    default String sqlToDate(Type type) {
	    return "%s";
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
		return "CAST(%s AS TEXT)";
    }
	
    // Scale isn't supported for truncation...
    @Override
    default String sqlTrunc(String argument, String scale) {
        return "TRUNC("+argument+"*POWER(10,"+scale+"))"+"/POWER(10,"+scale+") END";
    }

    // Negative scale isn't supported for rounding...
    @Override
    default String sqlRound(String argument, String scale) {
        return "CASE WHEN "+scale+" >= 0 THEN ROUND(" + argument + ", " + scale + ") ELSE "
                + " ROUND("+argument+"*POWER(10,"+scale+"))"+"/POWER(10,"+scale+") END";
    }

	
    @Override
    default String sqlConstructDate(String yearSql, String monthSql, String daySql) {
        return "printf('%04d-%02d-%02d', " + yearSql + "," + monthSql + "," + daySql + ")";
    }
  
    @Override
    default String sqlExtractTimeFromDateTime(String dateTimeExpr) {
        // Strip off 'YYYY-MM-DD '
        return String.format("substr(DATETIME(%s),12)", dateTimeExpr);
    }
    
    @Override
    default String sqlParseTime(String stringExpr) {
        return stringExpr;
    }
	
	@Override
    default String sqlToCharTime() {
        // time values are represented by millisecs since midnight.  
        return "strftime('%%H:%%M:%%f',%s)";
    }
    
    @Override
    default String sqlAddDaysToDate(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType, boolean isAddition) {
        if (lhsDataType == Date.class ) {
            // <date|timestamp> <+|-> <number>
            if (!isAddition) {
                return String.format("DATE(%s, ROUND(-(%s))||' day')", lhsValue, rhsValue);
            } else {
                return String.format("DATE(%s, ROUND(%s)||' day')", lhsValue, rhsValue);
            }
        } else if (lhsDataType==FormulaDateTime.class) {
            if (!isAddition) {
                return String.format("DATETIME(%s, ROUND(-(%s*86400))||' second')", lhsValue, rhsValue);
            } else {
                return String.format("DATETIME(%s, ROUND(%s*86400)||' second')", lhsValue, rhsValue);
            }
        } else if (rhsDataType == Date.class){
            return String.format("DATE(%s, ROUND(%s)||' day')", rhsValue, lhsValue);
        } else {
            return String.format("DATETIME(%s, ROUND(%s*86400)||' second')", rhsValue, lhsValue);
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
	    return "SUBSTR(" + stringArg + ", -(" + countArg+ " ))";
    }
    /**
     * @return the the current milliseconds of the day suitable.  
     */
	@Override
    default String sqlTimeNow() {
        return "date('now')";
    }
	
	@Override
    default String sqlSubtractTwoTimestamps(boolean inSeconds, Type dateType) {
        return inSeconds ? "(unixepoch(%s)-unixepoch(%s))" : "(julianday(%s)-julianday(%s))";
    } 

    @Override
    default String sqlSubtractTwoTimes() {
        return "(unixepoch(%s)-unixepoch(%s))";
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
        sb.append(" WHEN strftime('%%d', " + dateArg + ", 'start of month', '+1 month', '-1 day') =");
        sb.append(" strftime('%%d', " + dateArg + ")");
	    sb.append(" THEN '1' ELSE '0' END)");
        String dayAddition = sb.toString();

        // Note: this should be trunc of the months, but it doesn't work right anyway
        if (dateArgType == Date.class) {
            // The %% 1 is a way to do trunc
            return String.format("date(%s, " + dayAddition + " || ' day', %s || ' month', '-' || " + dayAddition + " || ' day')", dateArg, sqlTrunc(numMonths));
        } else {
            // We can't add a month interval to a timestamp, so we have to do something... fun.
            // Which is do all the date math and then add the millisecond part back at the end...
            return String.format("datetime(%s, " + dayAddition + " || ' day', %s || ' month', '-' || " + dayAddition + " || ' day')", dateArg, sqlTrunc(numMonths));
        }
    }

    @Override
    default String sqlToCharTimestamp() {
        return "%s";
    }

    /**
     * @return the format for String.format for converting from a date value to a string YYYY-MM-DD
     */
    @Override
    default String sqlToCharDate() {
        return "%s";
    }
	
    /**
     * @return the format for converting to a datetime value
     */
	@Override
    default String sqlToTimestampIso() {
		return "%s";
    }
    
    @Override
    default String sqlToDateIso() {
        return "%s";
    }
    
	
    /**
     * @return the function to use for getting the last day of the month of a date.
     */
	@Override
    default String sqlLastDayOfMonth() {
	    return "DATE(%s,'start of month','+1 month','-1 day')";
    }
	
    @Override
    default String sqlDateFromYearAndMonth(String yearValue, String monthValue) {
        return "printf('%04d-%02d-01', " + yearValue + "," + monthValue + ")";
    }

    /**
     * @return how to get the unix epoch from a given date for String.format
     */
	@Override
    default String sqlGetEpoch(Type type) {
	    return "unixepoch(%s)";
    }

	/**
     * @return how to get the number of seconds in a day from a time value for String.format
     */
	@Override
    default String sqlGetTimeInSeconds() {
        return "ROUND(unixepoch(%s)%%86400)";
    }
    
    /**
     * @return how to get a DateTime from a unix epoch time, suitable for String.format
     */
    @Override
    default String getDateFromUnixTime() {
    	return "DATETIME(%s, 'unixepoch')";
    }

    @Override
    default String sqlGetWeekday() {
        return "1+strftime('%%w',%s)";
    }
    
    @Override
    default String sqlGetIsoWeek() {
        // Not correct.
        return "strftime('%%U',%s)";
    }
    
    @Override
    default String sqlGetIsoYear() {
        // Not correct.
        return "strftime('%%Y',%s)";
    }
    
    @Override
    default String sqlGetDayOfYear() {
        return "strftime('%%j',%s)";
    }
    
    @Override
    default String sqlInitCap(boolean hasLocaleOverride) {
        return "%s";  // not implementable in sqlite
    }
        
    @Override
    default String sqlChr() {
    	return "CHAR(%s)";
    }
    
    @Override
    default String sqlAscii() {
    	return "UNICODE(%s)";
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

	@Override
    default String sqlInstr2(String strArg, String substrArg) {
		return String.format("INSTR(%s, %s)", strArg, substrArg);
    }
    
	@Override
    default String sqlInstr3(String strArg, String substrArg, String startLocation) {
        return String.format("CASE WHEN COALESCE(INSTR(SUBSTR(%s,%s),%s),0) > 0 THEN INSTR(SUBSTR(%s,%s),%s) + %s - 1 ELSE 0 END", strArg, startLocation, substrArg, strArg, startLocation, substrArg, startLocation);
    }
	
    @Override
    default String sqlSubstrWithNegStart(String strArg, String startPosArg) {
        return getSubstringFunction() + "(" + strArg + ", CASE WHEN " + startPosArg + " = 0 THEN 1 ELSE " + startPosArg + " END)";
    }
    
    @Override
    default String sqlSubstrWithNegStart(String strArg, String startPosArg, String lengthArg) {
        return getSubstringFunction() + "(" + strArg + ", CASE WHEN " + startPosArg + " = 0 THEN 1 ELSE " + startPosArg  + " END, " + sqlEnsurePositive(sqlRoundScaleArg(lengthArg)) + ")";
    }
	
    @Override
    default String sqlLpad(String str, String amount, String pad) {
        // No lpad/rpad in sqlite, so use the zeroblob/replace/hex trick to create the
        // repeating string and use substr to get the right amount
        // lpad need to remove the length of the string to make sure it's aligned correclty.
        return "substr(substr(replace(hex(zeroblob("+amount+")),'00',"+
        (pad != null ? pad : "' '")
        + "),1," + amount + "-length(" + str + "))||"+str+",1," + amount + ")";
    }

    @Override
    default String sqlRpad(String str, String amount, String pad) {
        // rpad doesn't need to check the length since it's appended to the end
        return "substr(" + str + "||substr(replace(hex(zeroblob("+amount+")),'00',"+
        (pad != null ? pad : "' '")
        + "),1," + amount + "),1," + amount+ ")";
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
		return dateTime;
	}

	@Override
	default String sqlExponent(String argument) {
        // tests showed double precision was only 2.5% faster than numeric (i.e. the rest of
        // the query processing machinery dominates, so go for max precision
		return "EXP(" + argument + ")";
	}
    
    @Override
    default String sqlTrigConvert(String argument) {
        return argument;   // Override to specify your own default precision.
    }
    
    @Override
    default SQLPair getPowerSql( String[] args, String[] guards) {
        String sql = "POWER(" + args[0] + ", " + args[1] + ")";
        // Google's doesn't do short circuits for AND, so we have to use IF here
        String guard = SQLPair.generateGuard(guards, "ROUND(" + args[1] + ")<>" + args[1] +
                    " OR(IIF(" + args[0] + "<>0,LOG10(ABS(" + args[0] + "))*" + args[1] + ">38,FALSE))");     
        return new SQLPair(sql, guard);
    }
    
    @Override
    default String sqlLogBase10(String argument) {
        // Sqlite in JDBC doesn't include math functions, but instead uses a custom extension
        // in the JDBC driver that doesn't have two argument log().  If your sqlite3 includes
        // SQLITE_ENABLE_MATH_FUNCTIONS, then replace this hook with LOG(EXP(1),argument)
        return "LOG10(" + argument + ")";
    }
    
    @Override
    default String sqlIntervalFromSeconds(Type dateType) {
        return "FLOOR(ABS(%s))";
    }

    @Override
    default String sqlIntervalToDurationString(String arg, boolean includeDays, String daysIsParam) {
        String result;
        if (daysIsParam != null) {
            result = "(CASE WHEN "+daysIsParam+" THEN FORMAT('%.0f',FLOOR("+arg+"/86400))||':'||TIME("+arg+", 'unixepoch') ELSE FORMAT('%02.0f',FLOOR("+arg+"/3600))||':'||strftime('%M:%S', "+arg+", 'unixepoch') END)";
        } else if (includeDays) {
            result = "FORMAT('%.0f',FLOOR("+arg+"/86400))||':'||TIME("+arg+",'unixepoch')";
        } else {
            result = "FORMAT('%02.0f',FLOOR("+arg+"/3600))||':'||strftime('%M:%S', "+arg+", 'unixepoch')";
        }
        return result;
    }
    
    @Override
    default String sqlAddMillisecondsToTime(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType,  boolean isAddition) {
        if (rhsDataType == BigDecimal.class) {
            rhsValue = rhsDataType == BigDecimal.class ? rhsValue + "/1000" : "unixepoch("+rhsValue+")";
            // to prevent negative values when subtracting, always add FormulaDateUtil.MILLISECONDSPERDAY, and take the mod
            return "strftime('%H:%M:%f'," + lhsValue + ",("+(isAddition ? "" : "-") +"(" + rhsValue + "))|| ' seconds')";
        } else {
            return "ROUND((1+julianday("+lhsValue+")-julianday("+rhsValue+"))*"+FormulaDateUtil.MILLISECONDSPERDAY+")%"+FormulaDateUtil.MILLISECONDSPERDAY;
                        
//            return "((" + FormulaDateUtil.MILLISECONDSPERDAY + "+((julianday("+rhsValue+")-julianday("+lhsValue+"))*"+FormulaDateUtil.MILLISECONDSPERDAY+"))";
        }
    }
    
    @Override
    default StringBuilder getCurrencyMask(int scale) {
        // Note: sqlite doesn't support group separators, so this will be off.
        return new StringBuilder(6).append("\"%,.").append(Integer.toString(scale)).append("f\"");
    }

    @Override
    default void appendCurrencyFormat(StringBuilder sql, String isoCodeArg, String amountArg, CharSequence maskStr) {
        sql.append("COALESCE(").append(isoCodeArg).append(",'')||' '||FORMAT(").append(maskStr).append(',').append(amountArg).append(")");
    }
    
    @Override
    default String getDateLiteralFromCalendar(Calendar c) {
        // Date literals are always strings in sqlite
        return "'" + FormulaDateUtil.formatDatetimeToSqlLiteral(c.getTime()) + "'";
    }

    @Override
    default String sqlRegexpLike(String text, String regexp) {
        return text + " REGEXP " + regexp ;
    }

    
    /**
     * The Sqlite versions as of 2022 don't use the new Sqlite3 math functions, but instead custom ones
     * that break compatibility with old versions of sqlite.
     * https://github.com/xerial/sqlite-jdbc/pull/763
     */
    interface SqliteJdbcCompatHooks extends FormulaSqliteHooks {
        @Override
        default String sqlLogBaseE(String argument) {
            // Sqlite in JDBC doesn't include math functions, but instead uses a custom extension
            // in the JDBC driver that doesn't have ln().  If your sqlite3 includes SQLITE_ENABLE_MATH_FUNCTIONS,
            // then replace this hook to return LN() directly
            return "(LOG10(" + argument + ")*2.30258509299)";
        }
        
        @Override
        default String sqlTrunc(String argument) {
            return "CASE WHEN (" + argument + ") > 0 THEN FLOOR(" + argument + ") ELSE CEIL(" + argument + ") END";
        }

        /**
         * @param argument the value to truncate
         * @param scale the scale to truncate to
         * @return how to call truncate to drop to the given number of decimal places
         */
        @Override
        default String sqlTrunc(String argument, String scale) {
            return "CASE WHEN (" + argument + ") > 0 THEN FLOOR("+argument+"/POWER(10,-("+scale+")))*POWER(10,-("+scale+")) ELSE CEIL("+argument+"*POWER(10,"+scale+"))/POWER(10,"+scale+") END";
        }
        
        @Override
        default String sqlIsNumber() {
            // REGEXP isn't supported in all sqlites.
            return "ABS(%s) > 0 || %<s = 0";
        }

        // Only supports integer modulus without the math functions.  So we have to bust out
        // this complexity...
        @Override
        default String sqlMod(String number, String modulus) {
            return "CASE WHEN SIGN(" + modulus + ") = SIGN("+ number+ ") THEN (" + number + "  - (FLOOR(" + number + "/" + modulus + ") * " + modulus + ")) ELSE " +
                    "(" + number + "  - (CEIL(" + number + "/" + modulus + ") * " + modulus + ")) END ";
        }
    }
    
}
