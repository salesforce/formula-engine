/**
 * 
 */
package com.force.formula.impl.sql;

import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaTime;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.util.FormulaDateUtil;

/**
 * Implementation of FormulaSqlHooks for H2
 * @author stamm
 * @since 0.3
 */
public interface FormulaH2Hooks extends FormulaSqlHooks {
    @Override
	default boolean isH2Style() {
		return true;
	}	
	
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for DateTime Value
     */
    @Override
    default String sqlDatetimeValueGuard() {
        return " NOT REGEXP_LIKE (%s, '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01]) ([01]?[0-9]|2[0-3]):[0-5]?\\d:[0-5]?\\d$')";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
    @Override
    default String sqlDateValueGuard() {
        return " NOT REGEXP_LIKE (%s, '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$')";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
    @Override
    default String sqlTimeValueGuard() {
        return " NOT REGEXP_LIKE (%s, '^([01]\\d|2[0-3]):[0-5][0-9]:[0-5][0-9]\\.[0-9][0-9][0-9]$')";
    }
    
    @Override
    default String sqlIsNumber() {
        return "REGEXP_LIKE(REGEXP_REPLACE(%s,'[0-9]+','0'),'^[+-]?(0|0\\.|\\.0|0\\.0)([Ee][+-]?0)?$')";
    }
    
    @Override
    default String sqlToNumber() {
        return "CAST(%s AS DECIMAL(38,18))";   // Override to specify your own default precision.
    }
    
    @Override
    default String sqlLastDayOfMonth() {
        return "EXTRACT(DAY FROM dateadd(day,-1,dateadd(month,1,date_trunc(month,%s))))";
    }
    
    @Override
    default String sqlToCharTimestamp() {
        return "FORMATDATETIME(%s, 'yyyy-MM-dd HH:mm:ss', 'en', 'UTC')";
    }

    @Override
    default String sqlToCharDate() {
        return "FORMATDATETIME(%s, 'yyyy-MM-dd HH:mm:ss', 'en', 'UTC')";
    }
    
    
    @Override
    default String sqlToCharTime() {
        return "FORMATDATETIME(%s, 'HH:mm:ss.SSS', 'en', 'UTC')";
    }
    
    @Override
    default String sqlToTimestampIso() {
        return "PARSEDATETIME(TRIM(%s), 'yyyy-M-d H:m:s', 'en', 'UTC')";
    }
    
    @Override
    default String sqlToDateIso() {
        return "PARSEDATETIME(%s, 'yyyy-M-d', 'en', 'UTC')";
    }
    
    @Override
    default String sqlConstructDate(String yearSql, String monthSql, String daySql) {
        return "PARSEDATETIME(" + yearSql + " || '-' || " + monthSql + " || '-' || " + daySql + ", 'yyyy-M-d', 'en', 'UTC')";
    }

    @Override
    default String sqlDateFromYearAndMonth(String yearValue, String monthValue) {
        return "PARSEDATETIME(" + yearValue + " || '-' || " + monthValue + " || '-01','yyyy-M-d','en','UTC')";
    }
    
    @Override
    default String sqlGetWeekday() {
        return "EXTRACT(DOW from %s)";
    }
    
    @Override
    default String sqlGetIsoWeek() {
        return "EXTRACT(ISO_WEEK from %s)";
    }
        
    @Override
    default String sqlGetIsoYear() {
        return "EXTRACT(ISO_YEAR from %s)";
    }
    
    @Override
    default String sqlGetDayOfYear() {
        return "EXTRACT(DAY_OF_YEAR from %s)";
    }
    
    @Override
    default String sqlGetEpoch(Type dateType) {
        return "EXTRACT(EPOCH from %s)";
    }

    
    @Override
    default String sqlChronoUnit(ChronoUnit field, Type dateType) {
        switch (field) {
        case HOURS:
            return "EXTRACT(HOUR from %s)";
        case MINUTES:
            return "EXTRACT(MINUTE from %s)";
        case SECONDS:
            return "EXTRACT(SECOND from %s)";
        case MILLIS:            
            return "EXTRACT(MILLISECOND from %s)";
        default:
        }
        return FormulaSqlHooks.super.sqlChronoUnit(field, dateType);
    }
    
    @Override
    default String sqlChr() {
        return "CHR(TRUNC(%s))";
    }
     
    @Override
    default String sqlAddMonths(String dateArg, Type dateArgType, String numMonths) {
        StringBuffer sb = new StringBuffer();
        sb.append(" (CASE");
        sb.append(" WHEN extract(day FROM (date_trunc(month, %s) + interval '1' month - interval '1' day)) = ");
        sb.append("      extract(day FROM (date_trunc(day, %s))) ");
        sb.append(" THEN interval '1' day");
        sb.append(" ELSE interval '0' day");
        sb.append(" END ) ");

        String dayAddition = String.format(sb.toString(), dateArg, dateArg);
        return String.format("(%s + " + dayAddition + " + (interval '1' month *TRUNC(%s))) - " + dayAddition, dateArg, numMonths);
    }
    
    
    // DateMath in H2 is very similar to TransactSQL
    @Override
    default String sqlSubtractTwoTimestamps(boolean inSeconds, Type dateType) {
        return inSeconds ? "(CAST(-DATEDIFF(MILLISECOND,%s,%s) AS DECIMAL(38,10))/1000)" 
                : "(CAST(-DATEDIFF(MILLISECOND,%s,%s) AS DECIMAL(38,10))/86400000)";  
    } 
    
    @Override
    default String sqlSubtractTwoTimes() {
        return "(CAST(-DATEDIFF(MILLISECOND,%s,%s) AS DECIMAL(38,10))/1000)";
    } 
    
    @Override
    default String sqlGetTimeInSeconds() {
        return "DATEDIFF(second, TIME '00:00:00', %s)";
    }
    
    // Same as TransactSQL
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
    
    @Override
    default String sqlParseTime(String stringExpr) {
        // TIME (3) so it won't lose milliseconds
        return String.format("CAST(PARSEDATETIME(%s,'HH:mm:ss[.SSS]','en','UTC') as TIME(3))", stringExpr);
    }
    
    @Override
    default String sqlExtractTimeFromDateTime(String dateTimeExpr) {
        return String.format("CAST(%s as TIME(3))", dateTimeExpr);
    }

    
    @Override
    default String sqlInitCap(boolean hasLocaleOverride) {
        return "%s";  // Unsupported.  Requires user defined function
    }

    // Oracle style formatting
    @Override
    default String sqlIntervalFromSeconds(Type dateType) {
        if(dateType == FormulaDateTime.class) {
            return "ROUND(ABS(%s))";
        }else {
            return "TRUNC(ABS(%s))";
        }
    }
   
    // Use expensive overcalculating date math.   Sorry, but oracle doesn't support formatting a duration
    // and the format it does use is difficult to work with if not including days
    // TODO: Use the interval output and SUBSTR what is needed
    @Override
    default String sqlIntervalToDurationString(String arg, boolean includeDays, String daysIsParam) {
        String result;
        if (daysIsParam != null) {
            result = "(CASE WHEN "+daysIsParam+" THEN TO_CHAR(TRUNC(("+arg+")/86400))||':'||TO_CHAR(MOD(TRUNC(("+arg+")/3600),24),'FM09') ELSE TO_CHAR(TRUNC(("+arg+")/3600),'FM99999909') END)||':'||TO_CHAR(MOD(TRUNC(("+arg+")/60),60),'FM09')||':'||TO_CHAR(MOD(TRUNC("+arg+"),60),'FM09')";
        } else if (includeDays) {
            result = "TO_CHAR(TRUNC(("+arg+")/86400))||':'||TO_CHAR(MOD(TRUNC(("+arg+")/3600),24),'FM09')||':'||TO_CHAR(MOD(TRUNC(("+arg+")/60),60),'FM09')||':'||TO_CHAR(MOD(TRUNC("+arg+"),60),'FM09')";
        } else {
            result = "TO_CHAR(TRUNC(("+arg+")/3600),'FM99999909')||':'||TO_CHAR(MOD(TRUNC(("+arg+")/60),60),'FM09')||':'||TO_CHAR(MOD(TRUNC("+arg+"),60),'FM09')";
        }
        return "NVL2("+arg+","+result+",NULL)";
    }
}
