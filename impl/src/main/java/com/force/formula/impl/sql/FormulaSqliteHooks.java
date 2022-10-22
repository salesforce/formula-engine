/**
 * 
 */
package com.force.formula.impl.sql;

import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.force.formula.FormulaDateTime;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.sql.SQLPair;

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
    
    // Sqlite in JDBC doesn't include math functions, but instead uses a custom extension
    // in the JDBC driver that doesn't have ln().  If your sqlite3 includes SQLITE_ENABLE_MATH_FUNCTIONS,
    // and SQLITE_HAVE_C99_MATH_FUNCS, then replace this hook to return LN() directly
	@Override
    default String sqlTrunc(String argument) {
        return "ROUND(" + argument + ")";
    }
	
    @Override
    default String sqlMod(String number, String modulus) {
        return "(" + number + " % " + modulus + ")";
    }
	
    @Override
    default String sqlLogBaseE(String argument) {
        // Sqlite in JDBC doesn't include math functions, but instead uses a custom extension
        // in the JDBC driver that doesn't have ln().  If your sqlite3 includes SQLITE_ENABLE_MATH_FUNCTIONS,
        // then replace this hook to return LN() directly
        return "LOG10(" + argument + ")*2.30258509299";
    }
	
    
    @Override
    default String sqlLogBase10(String argument) {
        // Sqlite in JDBC doesn't include math functions, but instead uses a custom extension
        // in the JDBC driver that doesn't have two argument log().  If your sqlite3 includes
        // SQLITE_ENABLE_MATH_FUNCTIONS, then replace this hook with LOG(EXP(1),argument)
        return "LOG10(" + argument + ")";
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
        // REGEXP isn't supported in all sqlites.
        return "ABS(%s) > 0 || %<s = 0";
        //return "(%s REGEXP '^[+-]?([0-9]+|[0-9]+\\.|\\.[0-9]+|[0-9]+\\.[0-9]+)([Ee][+-]?[0-9]+)?$')";
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
	    if (type == Date.class) {
	        return "DATE(%s)";
	    } else {
            return "DATETIME(%s)";
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
		return "CAST(%s AS TEXT)";
    }
   
    
    @Override
    default String sqlConstructDate(String yearSql, String monthSql, String daySql) {
        return "(" + yearSql + " || '-' || " + monthSql + " || '-' || " + daySql + ")";
    }
  
    @Override
    default String sqlExtractTimeFromDateTime(String dateTimeExpr) {
        return String.format("DATETIME(%s,'unixepoch')", dateTimeExpr);
    }
    
    @Override
    default String sqlParseTime(String stringExpr) {
        return String.format("MOD(UNIX_MILLIS(PARSE_TIMESTAMP('%%H:%%M:%%E3S', %s, 'UTC')),86400000)", stringExpr);
    }
	
	@Override
    default String sqlToCharTime() {
        // time values are represented by millisecs since midnight.  
        return "FORMAT_TIMESTAMP('%%H:%%M:%%E3S',timestamp_millis(%s),'UTC')";
    }
    
    @Override
    default String sqlAddDaysToDate(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType, boolean isAddition) {
        if (lhsDataType == Date.class ) {
            // <date|timestamp> <+|-> <number>
            if (!isAddition) {
                return String.format("DATE(%s, '-'||ROUND(%s)||' day')", lhsValue, rhsValue);
            } else {
                return String.format("DATE(%s, '+'||ROUND(%s)||' day')", lhsValue, rhsValue);
            }
        } else if (lhsDataType==FormulaDateTime.class) {
            if (!isAddition) {
                return String.format("DATETIME(%s, '-'||ROUND(%s*86400)||' second')", lhsValue, rhsValue);
            } else {
                return String.format("DATETIME(%s, '+'||ROUND(%s*86400)||' second')", lhsValue, rhsValue);
            }
        } else if (rhsDataType == Date.class){
            return String.format("DATE(%s, '+'||ROUND(%s)||' day')", rhsValue, lhsValue);
        } else {
            return String.format("DATETIME(%s, '+'||ROUND(%s*86400)||' second')", rhsValue, lhsValue);
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
	    if (type == FormulaDateTime.class) {
	        return "DATETIME(%s,'unixepoch')";
	    }
    	return "DATE(%s,'unixepoch')";
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
        return "strftime('%%u',%s)";
    }
    
    @Override
    default String sqlGetIsoWeek() {
        return "strftime('%%U',%s)";
    }
    
    @Override
    default String sqlGetIsoYear() {
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
    default String sqlIntervalFromSeconds() {
        return "ROUND(ABS(%s),0,1)";
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
