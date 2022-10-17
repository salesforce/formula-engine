/**
 * 
 */
package com.force.formula.impl.sql;

import java.lang.reflect.Type;
import java.util.Date;

import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaTime;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaDateUtil;

/**
 * Implementation of FormulaSqlHooks for Presto style databases (Amazon Athena, Trino, etc).
 * 
 * @author stamm
 * @since 0.3
 */
public interface FormulaPrestoHooks extends FormulaSqlHooks {
	@Override
	default boolean isPrestoStyle() {
		return true;
	}	
	
    /**
     * @return whether the string is a "number"
     */
    @Override
    default String sqlIsNumber() {
       return "REGEXP_LIKE(REGEXP_REPLACE(%s,'[0-9]+','0'),'^[+-]?(0|0\\.|\\.0|0\\.0)([Ee][+-]?0)?$')";
    }

    /**
     * @param argument the value to truncate
     * @return how to call truncate to drop all decimal places
     */
	@Override
    default String sqlTrunc(String argument) {
        return "CAST(TRUNCATE(" + argument + ") AS DECIMAL(38,18))";
    }
    
    
    /**
     * @param argument the value to truncate
     * @param scale the scale to truncate to
     * @return how to call truncate to drop to the given number of decimal places
     */
    @Override
    default String sqlTrunc(String argument, String scale) {
        return "CAST(TRUNCATE(CAST(" + argument + " AS DECIMAL(38,18)), " + scale + ") AS DECIMAL(38,18))";
    }
    
    @Override
    default SQLPair getPowerSql( String[] args, String[] guards) {
        String sql = "POWER(" + args[0] + ", " + args[1] + ")";
        String guard = SQLPair.generateGuard(guards, "TRUNCATE(" + args[1] + ")<>" + args[1] +
                    " OR(" + args[0] + "<>0 AND LOG10(ABS(" + args[0] + "))*" + args[1] + ">38)");     
        return new SQLPair(sql, guard);
    }
    
    /**
     * Formulas are usually numeric, but some functions, like round or trunc, require a cast to ::int in postgres
     * @param argument scale argument
     * @return the argument converted to an integer for rounding
     */
    @Override
    default String sqlRoundScaleArg(String argument) {
        return "try_cast(" + argument + " as integer)";
    }

    
    /**
     * @return the string to use for String.format to convert something to number generically, without a format
     */
    @Override
    default String sqlToNumber() {
        return "CAST(%s AS DECIMAL(38,18))";   // Override to specify your own default precision.
    }
    
    @Override
    default String sqlMakeDecimal(String argument) {
        return "CAST("+argument+" AS DECIMAL(38,18))";   // Override to specify your own default precision.
    }
    
    /**
     * @return the format for converting to a date value
     */
    @Override
    default String sqlToDateIso() {
        return "date_parse(%s, '%%Y-%%m-%%d')";
    }
    
    /**
     * @return the format for converting to a datetime value
     */
    @Override
    default String sqlToTimestampIso() {
        return "date_parse(%s, '%%Y-%%m-%%d %%H:%%i:%%s')";
    }
    
    /**
     * @return the format for String.format for converting from a datetime value to a string YYYY-MM-DD HH24:MI:SS
     */
    @Override
    default String sqlToCharTimestamp() {
        return "date_format(%s, '%%Y-%%m-%%d %%H:%%i:%%s')";
    }
    
    /**
     * @return the format for String.format for converting from a date value to a string YYYY-MM-DD
     */
    @Override
    default String sqlToCharDate() {
        return "date_format(%s, '%%Y-%%m-%%d')";
    }
    
    
    @Override
    default String sqlInstr2(String strArg, String substrArg) {
        return String.format("STRPOS(%s, %s)", strArg, substrArg);
    }
    
    @Override
    default String sqlInstr3(String strArg, String substrArg, String startLocation) {
        return String.format("CASE WHEN COALESCE(STRPOS(SUBSTR(%s,CAST(%s AS BIGINT)),%s),0) > 0 THEN STRPOS(SUBSTR(%s,CAST(%s AS BIGINT)),%s) + %s - 1 ELSE 0 END", strArg, startLocation, substrArg, strArg, startLocation, substrArg, startLocation);
    }
    
    
    @Override
    default String sqlConcat(boolean withSpaces) {
        // Note: this is supported in trino, but not presto, so maintain compatibility with .277
        //return "CONCAT_WS(\"\",%s, %s)";  // CONCAT doesn't skip nulls
        
        // This doesn't work "right" in that null || null should be null and not ''. 
        return  "COALESCE(%s,'')||COALESCE(%s,'')";
    }

    @Override
    default String sqlLpad(String str, String amount, String pad) {
        // You must supply the pad in presto
        if (pad == null) {
            return "LPAD(" + str + ", " + amount + ", ' ')";            
        } else {
            return FormulaSqlHooks.super.sqlLpad(str, amount, pad);
        }
    }

    @Override
    default String sqlRpad(String str, String amount, String pad) {
        // You must supply the pad in presto
        if (pad == null) {
            return "RPAD(" + str + ", " + amount + ", ' ')";            
        } else {
            return FormulaSqlHooks.super.sqlRpad(str, amount, pad);
        }
    }
    
    @Override
    default String sqlRight(String stringArg, String countArg) {
        return "SUBSTR(" + stringArg + ", -GREATEST(CAST(" + countArg+ " AS INTEGER), 0))";
    }

    @Override
    default String sqlInitCap(boolean hasLocaleOverride) {
        return "(array_join((transform((split(lower(%s),' ')), x -> concat(upper(substr(x,1,1)),substr(x,2,length(x))))),' ',''))";
    }
    
    @Override
    default String sqlChr() {
        return "CHR(CAST (ROUND(%s) AS BIGINT))";
    }

    @Override
    default String sqlAscii() {
         return "CAST(CODEPOINT(CAST(SUBSTR(%s,1,1) AS VARCHAR(1))) AS DECIMAL(38,18))";
    }

    @Override
    default String sqlAddDaysToDate(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType, boolean isAddition) {
        if (lhsDataType == Date.class ) {
            // <date|timestamp> <+|-> <number>
            if (!isAddition) {
                return String.format("DATE_ADD('day', -CAST(%s AS BIGINT), %s)", rhsValue, lhsValue);
            } else {
                return String.format("DATE_ADD('day', CAST(%s AS BIGINT), %s)", rhsValue, lhsValue);
            }
        } else if (lhsDataType==FormulaDateTime.class) {
            if (!isAddition) {
                return String.format("DATE_ADD('second', -CAST(%s*86400 AS BIGINT), %s)", rhsValue, lhsValue);
            } else {
                return String.format("DATE_ADD('second', CAST(%s*86400 AS BIGINT), %s)", rhsValue, lhsValue);
            }
        } else if (rhsDataType == Date.class) {
            return String.format("DATE_ADD('day', CAST(%s AS BIGINT), %s)", lhsValue, rhsValue);
        } else {
            return String.format("DATE_ADD('second', CAST(%s*86400 AS BIGINT), %s)", lhsValue, rhsValue);
        }
     }
    
    @Override
    default String sqlAddMillisecondsToTime(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType,  boolean isAddition) {
        if (rhsDataType == FormulaTime.class) {
            return "(DATE_DIFF('millisecond'," + rhsValue + "," + lhsValue + ")+"+FormulaDateUtil.MILLISECONDSPERDAY+")%"+FormulaDateUtil.MILLISECONDSPERDAY;
        } else {            
            if (!isAddition) {
                return "CAST(DATE_ADD('millisecond',-CAST(" + rhsValue + " AS BIGINT),"+lhsValue+") AS TIME)";
            } else {
                return "CAST(DATE_ADD('millisecond',CAST(" + rhsValue + " AS BIGINT),"+lhsValue+") AS TIME)";
            }
        }
    }
    
    @Override
    default String getDateFromUnixTime() {
        return "FROM_UNIXTIME(%s)";
    }
    
    @Override
    default String sqlGetEpoch() {
        return "TO_UNIXTIME(%s)";
    }
    
    /**
     * @return the function to use for getting the last day of the month of a date.
     */
    @Override
    default String sqlLastDayOfMonth() {
        return "day(LAST_DAY_OF_MONTH(%s))";
    }
    
    @Override
    default String sqlGetDayOfYear() {
        return "DAY_OF_YEAR(%s)";
    }
    
    @Override
    default String sqlGetIsoWeek() {
        return "CAST(DATE_FORMAT(%s,'%%v') AS DECIMAL(38,18))";
    }
    
    @Override
    default String sqlGetIsoYear() {
        return "CAST(DATE_FORMAT(%s,'%%x') AS DECIMAL(38,18))";
    }
    
    @Override
    default String sqlGetTimeInSeconds() {
        return "DATE_DIFF('second', TIME '00:00:00', %s)";
    }
    
    @Override
    default String sqlAddMonths(String dateArg, String numMonths) {
        StringBuffer sb = new StringBuffer();
        sb.append(" (CASE");
        sb.append(" WHEN day(date_trunc('month', %s) + interval '1' month - interval '1' day) = ");
        sb.append("      day(date_trunc('day', %s)) ");
        sb.append(" THEN interval '1' day");
        sb.append(" ELSE interval '0' day");
        sb.append(" END ) ");

        String dayAddition = String.format(sb.toString(), dateArg, dateArg);
        return String.format("(%s + " + dayAddition + " + ((interval '1' month)*TRUNCATE(%s))) - " + dayAddition, dateArg, numMonths);
    }
    
    @Override
    default StringBuilder getCurrencyMask(int scale) {
        return new StringBuilder(6).append("'%,.").append(Integer.toString(scale)).append("f'");
    }

    @Override
    default void appendCurrencyFormat(StringBuilder sql, String isoCodeArg, String amountArg, CharSequence maskStr) {
        sql.append("CONCAT_WS('',").append(isoCodeArg).append(",' ',FORMAT(").append(maskStr).append(',').append(amountArg).append("))");
    }

    // Presto doesn't have any interval or time formatting functions.
    @Override
    default String sqlIntervalToDurationString(String arg, boolean includeDays, String daysIsParam) {
        String result;
        if (daysIsParam != null) {
            result = "(CASE WHEN "+daysIsParam+" THEN CONCAT(FORMAT('%.0f',TRUNCATE("+arg+"/86400)),':',DATE_FORMAT(from_unixtime("+arg+"%86400),'%H:%i:%s')) ELSE CONCAT(FORMAT('%02.0f',TRUNCATE("+arg+"/3600)),':',DATE_FORMAT(from_unixtime("+arg+"),'%i:%s')) END)";
        } else if (includeDays) {
            result = "CONCAT(FORMAT('%.0f',TRUNCATE("+arg+"/86400)),':',DATE_FORMAT(from_unixtime("+arg+"%86400),'%H:%i:%s'))";
        } else {
            result = "CONCAT(FORMAT('%02.0f',TRUNCATE("+arg+"/3600)),':',DATE_FORMAT(from_unixtime("+arg+"),'%i:%s'))";
        }
        return result;
    }
    
    @Override
    default String sqlSubtractTwoTimestamps(boolean inSeconds) {
        return inSeconds ? "CAST(-DATE_DIFF('SECOND',%s,%s) AS DECIMAL(38,10))" 
                : "(CAST(-DATE_DIFF('SECOND',%s,%s) AS DECIMAL(38,10))/86400)";  
    } 
    
    @Override
    default String sqlSubtractTwoTimes() {
        return "CAST(DATE_DIFF('second',%s,%s) AS DECIMAL(38,10))";
    } 

    /**
     * Intervals in presto aren't helpful for formatting, so don't use them.
     */
    @Override
    default String sqlIntervalFromSeconds() {
        return "TRUNCATE(ABS(%s),0)";
    }

    @Override
    default String sqlConvertPercent(String argument) {
        return "(" + argument + " / (DECIMAL '100.00000000'))";  // Presto needs more decimal places.
    }

}
