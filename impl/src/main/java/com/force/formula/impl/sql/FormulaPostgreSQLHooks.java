/**
 * 
 */
package com.force.formula.impl.sql;

import java.lang.reflect.Type;
import java.util.Date;

import com.force.formula.FormulaDateTime;
import com.force.formula.impl.FormulaSqlHooks;

/**
 * Implementation of FormulaSqlHooks for Oracle-style DBs
 * @author stamm
 * @since 0.2
 */
public interface FormulaPostgreSQLHooks extends FormulaSqlHooks {
	@Override
	default boolean isPostgresStyle() {
		return true;
	}	

	// Handle plsql regexp differences (where oracle needs regexp_like and postgres wants ~ or similar to)
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for DateTime Value
     */
	@Override
    default String sqlDatetimeValueGuard() {
	   return " NOT %s ~ '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01]) ([01]?[0-9]|2[0-3]):[0-5]?\\d:[0-5]?\\d$' ";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
	@Override
    default String sqlDateValueGuard() {
 	   return " NOT %s ~ '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$' ";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
	@Override
    default String sqlTimeValueGuard() {
  	   return " NOT %s ~ '^([01]\\d|2[0-3]):[0-5][0-9]:[0-5][0-9]\\.[0-9][0-9][0-9]$' ";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
	@Override
    default String sqlIsNumber() {
  	   return "REGEXP_REPLACE(%s,'[0-9]+','0','g') ~ '^[+-]?(0|0\\.|\\.0|0\\.0)([Ee][+-]?0)?$'";
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
    default String sqlToDate() {
		return "CAST(%s AS DATE)";
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
    
    /**
     * @return the string to use for TO_TIMESTAMP to get seconds.microseconds.  It's a subtle difference
     */
	@Override
    default String sqlSecsAndMsecs() {
		return "SSSS.MS";
    }
    
    /**
     * @return the string to use for TO_TIMESTAMP to get seconds.microseconds.  It's a subtle difference
     */
	@Override
    default String sqlHMSAndMsecs() {
		return "HH24:mi:ss.MS";
    }
    
    
    /**
     * @return the string to use for TO_TIMESTAMP to get seconds.  It's a subtle difference
     */
	@Override
    default String sqlSecsInDay() {
		return "SSSS";
    }
    

    /**
     * @return the function that allows subtraction of two timestamps to get the microsecond/day difference.  This is
     * missing from psql, but available in oracle.  This allows you to try and fix that.
     */
	@Override
    default String sqlSubtractTwoTimestamps() {
        return "((EXTRACT(EPOCH FROM %s)-EXTRACT(EPOCH FROM %s))::numeric/86400)";
    } 
	
	@Override
    default String sqlAddDaysToDate(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType, boolean isAddition) {
        if (lhsDataType == Date.class || lhsDataType==FormulaDateTime.class) {
            // <date|timestamp> <+|-> <number>
            return String.format("(%s%spg_catalog.make_interval(0,0,0,0,0,0,%s*86400))::timestamp(0)", lhsValue, isAddition ? "+" : "-", rhsValue);
        } else {
            // <number> + <date|timestamp>
            return String.format("(pg_catalog.make_interval(0,0,0,0,0,0,%s*86400)%s%s)::timestamp(0)", lhsValue, isAddition ? "+" : "-", rhsValue);
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
        return "RIGHT(" + stringArg + ", GREATEST(" + countArg+ ", 0)::integer)";
    }

    /**
     * @return the the current milliseconds of the day suitable.  You may want to use ::time instead, so overridable
     */
	@Override
    default String sqlTimeNow() {
        return "EXTRACT(EPOCH FROM AGE(NOW()::timestamp, DATE_TRUNC('day', NOW()::timestamp)))::BIGINT::NUMERIC";
    }
    
    /**
     * @return the format to use for adding months.
     */
	@Override
    default String sqlAddMonths(String dateArg, String numMonths) {
		return String.format("(%s+'1 day'::interval+('1 month'::interval*TRUNC(%s)))-'1 day'::interval", dateArg, numMonths);
    }
    
    
    /**
     * @return the format for converting to a datetime value
     */
	@Override
    default String sqlToTimestampIso() {
		return "TO_TIMESTAMP(%s, 'YYYY-MM-DD HH24:MI:SS')";
    }
    
    /**
     * @return the function to use for getting the last day of the month of a date.
     */
	@Override
    default String sqlLastDayOfMonth() {
		return "EXTRACT(DAY FROM (date_trunc('month',%s)+ interval '1 month -1 day')::timestamp(0))::numeric";
    }

    
    /**
     * @return the format for converting to a datetime value
     * @param withSpaces whether spaces should be used around the "||" for compatibility
     */
	@Override
    default String sqlConcat(boolean withSpaces) {
		// Formula engine generally allows || with nulls, as it's less confusing
		return "CONCAT(%s, %s)";
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
     * In oracle and mysql, it's INSTR.  Use binary for INSTR to keep it case sensitive
     * @param strArg the value of the string to search in
     * @param substrArg the value of the substring to search
     * @param startLocation the value of the start location to use as the offset (1-indexed)
     */
	@Override
    default String sqlInstr3(String strArg, String substrArg, String startLocation) {
		// This is unfortunate, as it has to reevaluate the subexpression twice in order to return 0 for not found.
		// If your postgresql has a better version of this, please use it instead.
		return String.format("CASE WHEN COALESCE(STRPOS(SUBSTR(%s,%s::integer),%s),0) > 0 THEN STRPOS(SUBSTR(%s,%s::integer),%s) + %s - 1 ELSE 0 END", strArg, startLocation, substrArg, strArg, startLocation, substrArg, startLocation);
    }
    
    
    /**
     * Formulas are usually numeric, but some functions, like round or trunc, require a cast to ::int in postgres
     * @param argument scale argument
     * @return the argument converted to an integer for rounding
     */
    @Override
    default String sqlRoundScaleArg(String argument) {
		return argument + "::integer";
    }

    /**
     * Formulas are usually numeric, but some functions, like round or trunc, return an integer that may
     * cause type cast issues
     * @param argument argument that's in an integer
     * @return the argument converted from an integer to numeric for use
     */
    @Override
    default String sqlMakeDecimal(String argument) {
		return argument + "::numeric";
    }

	@Override
	default String sqlConvertDateTimeToDate(String dateTime, String userTimezone, String userTzOffset) {
		return "DATE_TRUNC('DAY', ("+dateTime+" AT TIME ZONE 'UTC') AT TIME ZONE '"+userTimezone+"')::timestamp";
	}

	@Override
	default String sqlExponent(String argument) {
        // tests showed double precision was only 2.5% faster than numeric (i.e. the rest of
        // the query processing machinery dominates, so go for max precision
		return "EXP(" + argument + "::numeric(40,20))";
	}
    
}