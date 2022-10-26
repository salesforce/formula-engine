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
import com.force.formula.util.FormulaDateUtil;

/**
 * Implementation of FormulaSqlHooks for Sybase/MS Sql Server style DBs
 * @author stamm
 * @since 0.2
 */
public interface FormulaTransactSQLHooks extends FormulaSqlHooks {
	@Override
	default boolean isTransactSqlStyle() {
		return true;
	}	

	
    /**
     * @return the string to use for String.format to convert something to date generically, without a specified
     */
    @Override
    default String sqlToDate(Type dateType) {
		return "CONVERT(DATETIME,%s)";
    }
    
    
    /**
     * @return the format for converting to a datetime value
     */
    @Override
    default String sqlToTimestampIso() {
		return "CONVERT(DATETIME, %s)";
    }
    
    /**
     * @return the format for converting to a datetime value
     */
    @Override
    default String sqlToDateIso() {
		return "CONVERT(DATE, %s)";
    }
    
    @Override
    default String sqlConstructDate(String yearSql, String monthSql, String daySql) {
        return "DATEFROMPARTS(" + yearSql + "," + monthSql + "," + daySql + ")";
    }
  
    /**
     * @return the string to use for String.format to convert something to number generically, without a format
     */
    @Override
    default String sqlToNumber() {
		return "CAST(%s AS DECIMAL(38,18))";   // Override to specify your own default precision.
    }

	@Override
    default String sqlTrigConvert(String argument) {
        return "CAST("+argument+" AS DECIMAL(38,18))";   // Override to specify your own default precision.
    }

    @Override
    default String sqlTrunc(String argument) {
    	return "ROUND(" + argument + ",0,1)"; // The third argument means truncate
    }

	@Override
    default String sqlTrunc(String argument, String scale) {
    	return "ROUND(" + argument + ","+scale+",1)";
    }
	
    @Override
    default String getDateLiteralFromCalendar(Calendar c) {
    	return "DATEFROMPARTS(" + c.get(Calendar.YEAR) + ","
                + (c.get(Calendar.MONTH) + 1) + "," + c.get(Calendar.DATE) + ")";
    }

	// Make things case sensitive
	@Override
    default String sqlInstr2(String strArg, String substrArg) {
		return String.format("CHARINDEX(%s, %s COLLATE Latin1_General_CS_AS)", substrArg, strArg);
    }
    
	@Override
    default String sqlInstr3(String strArg, String substrArg, String startLocation) {
		return String.format("CHARINDEX(%s COLLATE Latin1_General_CS_AS, %s, %s )", substrArg, strArg, startLocation);
    }
	
	
    // TransactQL doesn't allow negative length for startPos, so this simulates it. 
	// Use NullIf to have extra negative length return null.  
	// Use 1000000 for length since it's required
    @Override
    default String sqlSubstrWithNegStart(String strArg, String startPosArg) {
        return getSubstringFunction() + "(" + strArg + ", " + "CASE WHEN " + startPosArg + " >= 0 THEN " + sqlGreatest(sqlRoundScaleArg(startPosArg),"1")  
        + " ELSE NULLIF("+sqlGreatest("LEN(" + strArg + ") + 1 + " + sqlRoundScaleArg(startPosArg),"0")+",0) END, 1000000)";
    }
    
    @Override
    default String sqlSubstrWithNegStart(String strArg, String startPosArg, String lengthArg) {
        return getSubstringFunction() + "(" + strArg + ", " + "CASE WHEN " + startPosArg + " >= 0 THEN " + sqlGreatest(sqlRoundScaleArg(startPosArg),"1") 
        + " ELSE NULLIF("+sqlGreatest("LEN(" + strArg + ") + 1 + " + sqlRoundScaleArg(startPosArg),"0")+",0) END, " + sqlEnsurePositive(sqlRoundScaleArg(lengthArg)) + ")";
    }
    
	@Override
    default String sqlIsNumber() {
		// This function is not very good, as it does a simple character test and 
		// allows - and + in all positions.  MSSqlServer doesn't support regex natively,
		// So this is about all we can do without doing NOT LIKE '%[^0-9.+-]'...
    	return "ISNUMERIC(%s)=1";
    }
   
	
    @Override
	default String sqlNow() {
		return "SYSUTCDATETIME()";
	}

	@Override
	default String sqlTimeNow() {
        return "CAST(SYSUTCDATETIME() AS TIME)";
	}

	@Override
	default String sqlLastDayOfMonth() {
		return "DAY(EOMONTH(%s))";
	}
    
    @Override
    default String sqlDateFromYearAndMonth(String yearValue, String monthValue) {
        return "DATEFROMPARTS(" + yearValue + "," + monthValue + ",1)";
    }
    
	// See https://docs.microsoft.com/en-us/sql/t-sql/functions/cast-and-convert-transact-sql?view=sql-server-ver15
    @Override
    default String sqlToCharTimestamp() {
        return "CONVERT(VARCHAR,%s,120)";  // 120 = "yyyy-mm-dd hh:mi:ss (24h)"
     }
    
    @Override
    default String sqlToCharDate() {
        return "CONVERT(VARCHAR,%s,105)";  // 105 = "dd-mm-yyyy"
    }

    @Override
    default String sqlToCharTime() {
        return "SUBSTRING(CONVERT(VARCHAR,%s),1,12)";  // mysql uses microseconds
    }
	
	@Override
    default String sqlRight(String stringArg, String countArg) {
        return "RIGHT(" + stringArg + ", " + sqlEnsurePositive(countArg) + ")";
    }

    /**
     * @return the function that allows subtraction of two timestamps to get the microsecond/day difference.  This is
     * missing from mysql, but available in oracle.  This allows you to try and fix that.
     */
    @Override
    default String sqlSubtractTwoTimestamps(boolean inSeconds, Type dateType) {
    	// If you are older than sqlserver 16, you'll need to use DATEDIFF and deal with the errors
    	return inSeconds ? "CAST(-DATEDIFF_BIG(SECOND,%s,%s) AS DECIMAL(38,10))" 
    	        : "(CAST(-DATEDIFF_BIG(SECOND,%s,%s) AS DECIMAL(38,10))/86400)";  
    } 
    

    @Override
    default String sqlSubtractTwoTimes() {
        return "CAST(-DATEDIFF_BIG(SECOND,%s,%s) AS DECIMAL(38,10))";
    } 
	
	@Override
    default String sqlAddDaysToDate(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType, boolean isAddition) {
        if (lhsDataType == Date.class) {
            // <date|timestamp> <+|-> <number>
        	if (!isAddition) {
                return String.format("DATEADD(day,%s,%s)", rhsValue, lhsValue);
        	} else {
                return String.format("DATEADD(day,%s,%s)", rhsValue, lhsValue);
        	}
        } else if (lhsDataType==FormulaDateTime.class) {
            // <date|timestamp> <+|-> <number>
        	if (!isAddition) {
                return String.format("DATEADD(second,-ROUND(%s*86400,0),%s)", rhsValue, lhsValue);
        	} else {
                return String.format("DATEADD(second,ROUND(%s*86400,0),%s)", rhsValue, lhsValue);
        	}
        } else if (rhsDataType == Date.class){
            return String.format("DATEADD(day,%s,%s)", lhsValue, rhsValue);
        } else {
        	assert rhsDataType == FormulaDateTime.class : "Cannot add " + rhsDataType + " to " + lhsDataType;
            return String.format("DATEADD(second,ROUND(%s*86400,0),%s)", lhsValue, rhsValue);
        }
    }
	
	@Override
    default String sqlAddMillisecondsToTime(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType,  boolean isAddition) {
		if (rhsDataType == FormulaTime.class) {
			// Diff needs to be positive
        	return "(DATEDIFF(millisecond," + rhsValue + "," + lhsValue + ")+"+FormulaDateUtil.MILLISECONDSPERDAY+")%"+FormulaDateUtil.MILLISECONDSPERDAY;
		} else {        	
            if (!isAddition) {
            	return "DATEADD(millisecond,-(" + rhsValue + ")," + lhsValue + ")";
            } else {
            	return "DATEADD(millisecond," + rhsValue + "," + lhsValue + ")";
            }
		}
    }
    
	
	/**
     * @return the format to use for adding months.
     */
	@Override
    default String sqlAddMonths(String dateArg, Type dateArgType, String numMonths) {
    	return String.format("DATEADD(MONTH, %s, %s)", numMonths, dateArg);
    }
	

    /**
     * @return how to get the unix epoch from a given date for String.format
     */
	@Override
    default String sqlGetEpoch(Type dateType) {
    	return "DATEDIFF_BIG(second, '1970-01-01', %s)";
    }
    
    /**
     * @return how to get the number of seconds in a day from a time value for String.format
     */
	@Override
    default String sqlGetTimeInSeconds() {
    	return "DATEDIFF(second, '00:00:00', %s)";
    }
	
    /**
     * @return how to get a DateTime from a unix epoch time, suitable for String.format
     */
    @Override
    default String getDateFromUnixTime() {
    	return "DATEADD(second, %s, '1970-01-01')";
    }
    
    @Override
    default String sqlChronoUnit(ChronoUnit field, Type dateType) {
        switch (field) {
        case YEARS: 
            return "YEAR(%s)";
        case MONTHS:
            return "MONTH(%s)";
        case DAYS:
            return "DAY(%s)";
        case HOURS:
            return "DATEPART(hour,%s)";
        case MINUTES:
            return "DATEPART(minute,%s)";
        case SECONDS:
            return "DATEPART(second,%s)";
        case MILLIS:            
            return "DATEPART(MILLISECOND,%s)";
        default:
        }
        return FormulaSqlHooks.super.sqlChronoUnit(field, dateType);
    }
	
    @Override
    default String sqlGetWeekday() {
        return "DATEPART(weekday,%s)";
    }
    
    @Override
    default String sqlGetIsoWeek() {
		return "DATEPART(isoww, %s)";
    }
    
    @Override
    default String sqlGetIsoYear() {
		return "YEAR(DATEADD(day, 26 - DATEPART(isoww, %s), %s))";
    }
    
    @Override
    default String sqlGetDayOfYear() {
		return "DATEPART(dayofyear, %s)";
    }
    
    @Override
    default String sqlExtractTimeFromDateTime(String dateTimeExpr) {
        return String.format("CAST(%s as TIME)", dateTimeExpr);
    }
    
    @Override
    default String sqlParseTime(String stringExpr) {
        return String.format("CAST(%s as TIME)", stringExpr);
    }
    

    /**
     * Intervals in sqlserver aren't helpful, so don't use them.
     */
    @Override
    default String sqlIntervalFromSeconds() {
        return "ROUND(ABS(%s),0,1)";
        // return "DATEADD(second, ROUND(ABS(%s),0,1), '1970-01-01')";   // intervals aren't available in sqlserver
    }
   
    // Use expensive overcalculating date math.   Sorry, but oracle doesn't support formatting a duration
    // and the format it does use is difficult to work with if not including days
    // TODO: Use the interval output and SUBSTR what is needed
    @Override
    default String sqlIntervalToDurationString(String arg, boolean includeDays, String daysIsParam) {
        String result;
        if (daysIsParam != null) {
            result = "CONCAT(CASE WHEN "+daysIsParam+" THEN CONCAT(CONVERT(int,("+arg+")/86400),':',FORMAT(CONVERT(int,(("+arg+")/3600)%24),'00')) ELSE FORMAT(CONVERT(int,"+arg+"/3600),'########00') END,':',FORMAT(CONVERT(int,(("+arg+")/60)%60),'00'),':',FORMAT(CONVERT(int,("+arg+")%60),'00'))";
            //result = "IIF("+arg+" IS NULL, NULL, CONCAT(CASE WHEN "+daysIsParam+" THEN CONCAT(DATEPART(dy,"+arg+")-1,':') ELSE '' END, CONVERT(VARCHAR,"+arg+",108)))";
        } else if (includeDays) {
            result = "CONCAT(CONVERT(int,("+arg+")/86400),':',FORMAT(CONVERT(int,("+arg+")/3600%24),'00'),':',FORMAT(CONVERT(int,(("+arg+")/60)%60),'00'),':',FORMAT(CONVERT(int,("+arg+")%60),'00'))";
            //result = "IIF("+arg+" IS NULL, NULL, CONCAT(DATEPART(dy,"+arg+")-1,':',CONVERT(VARCHAR,"+arg+",108)))";
        } else {
            result = "CONCAT(FORMAT(CONVERT(int,"+arg+"/3600),'########00'),':',FORMAT(CONVERT(int,(("+arg+")/60)%60),'00'),':',FORMAT(CONVERT(int,("+arg+")%60),'00'))";
            //result = "CONVERT(VARCHAR,"+arg+",108)";
        }
                
        return "IIF("+arg+" IS NULL, NULL, "+result+")";
    }
    
    @Override
    default String sqlInitCap(boolean hasLocaleOverride) {
    	return "%s";  // Unsupported.  Requires user defined function
    }

    @Override
    default String sqlChr() {
    	return "NCHAR(ROUND(%s,0,1))";
    }
    
    @Override
    default String sqlAscii() {
    	// They didn't make this easy...
    	return "UNICODE(%s)"; 
    }
    
	@Override
	default String sqlExponent(String argument) {
        // tests showed double precision was only 2.5% faster than numeric (i.e. the rest of
        // the query processing machinery dominates, so go for max precision
		return "CAST(EXP(" + argument + ") AS DECIMAL(38,10))";
	}
	
	@Override
    default String sqlLogBaseE(String argument) {
        return String.format(sqlToNumber(),"LOG(" + argument + ")");
    }
	   
    @Override
    default String sqlLogBase10(String argument) {
        return String.format(sqlToNumber(),"LOG10(" + argument + ")");
    }
    
    @Override
    default String sqlMod(String number, String modulus) {
        return "(" + number + " % " + modulus + ")";
    }
    
	@Override
    default SQLPair getPowerSql( String[] args, String[] guards) {
        String sql = "CAST(POWER(" + args[0] + ", " + args[1] + ") AS DECIMAL(38,10))";
        String guard = SQLPair.generateGuard(guards, "ROUND(" + args[1] + ",0,1)<>" + args[1] +
	            " OR(" + args[0] + "<>0 AND LOG10(ABS(" + args[0] + "))*" + args[1] + ">38)");    	
        return new SQLPair(sql, guard);
    }
	
	// MSSqlServer doesn't support greatest or least until 2022.  So we do (a+b+ABS(a-b))/2
	@Override
	default String sqlGreatest(String arg1, String arg2) {
	    if ("0".equals(arg2)) {
	        // Simplify "ensure positive"
	        return "(" + arg1 +"+ABS(" + arg1 +  "))/2";
	    }
		return "(" + arg1 + "+" + arg2 + "+ABS(" + arg1 + "-" + arg2 + "))/2";
	}


	@Override
    default String sqlEnsurePositive(String argument) {
		// SQL Server doesn't have greatest.  So do (a+abs(a))/2
    	return "((" + argument + " + ABS(" + argument + "))/2)";
    }


	@Override
	default String sqlLpad(String str, String amount, String pad) {
		// We need to fill the left side with the start of the string, not the end.  
		// So if they pass in 'abc', the 'a' should be added, not the c.  So we need to do length
		// checks.  Even if it's just ' ', we need to return the LEFT of the string.
		if (pad != null) {
			return "LEFT(REPLICATE("+pad+","+amount+"),CASE WHEN "+amount+" <= LEN("+str+") THEN 0 ELSE "+amount+" - LEN("+str+") END) + LEFT("+str+","+amount+")";
		} else {
			return "LEFT(SPACE("+amount+"),CASE WHEN "+amount+" <= LEN("+str+") THEN 0 ELSE "+amount+" - LEN("+str+") END) + LEFT("+str+","+amount+")";
		}
	}


	// Rpad's simpler because we don't need to check the length of the string
	@Override
	default String sqlRpad(String str, String amount, String pad) {
		if (pad != null) {
			return "LEFT("+str+"+REPLICATE("+pad+","+amount+"/LEN("+pad+")),"+amount+")";
		} else {
			return "LEFT("+str+"+SPACE("+amount+"),"+amount+")";
		}
	}
	
	
	@Override
    default Object sqlMakeStringComparable(Object str, boolean forCompare) {
		if (forCompare) {
			return str + " COLLATE Latin1_General_100_BIN2";
		}
		return str + " COLLATE Latin1_General_CS_AS";
    }

	
    /**
     * @param scale the number of digits to the right of the radix
     * @return the SQL string to use to in TO_CHAR for the given scale.  This is used by 
     * {@link #getCurrencyFormat(String, String, boolean)}
     */
	@Override
    default StringBuilder getCurrencyMask(int scale) {
        StringBuilder mask = new StringBuilder(40).append("'###,###,###,###,##0");
        if (scale > 0) {
            mask.append('.');
            for (int i = 0; i < scale; i++) mask.append('0');
        }
        mask.append('\'');
        return mask;
    }
    

	@Override
	default void appendCurrencyFormat(StringBuilder sql, String isoCodeArg, String amountArg, CharSequence maskStr) {
        sql.append("CONCAT(").append(isoCodeArg).append(",' ',FORMAT(").append(amountArg).append(',').append(maskStr).append("))");
	}
        

}
