/**
 * 
 */
package com.force.formula.impl.sql;

import com.force.formula.impl.FormulaSqlHooks;

/**
 * Implementation of FormulaSqlHooks for Oracle-style DBs
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
    default String sqlToDate() {
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
    

    /**
     * @return the function that allows subtraction of two timestamps to get the microsecond/day difference.  This is
     * missing from mysql, but available in oracle.  This allows you to try and fix that.
     */
    @Override
    default String psqlSubtractTwoTimestamps() {
    	return "(-TIMESTAMPDIFF(SECOND,%s,%s)/86400)";
    	//return "((UNIX_TIMESTAMP(%s)-UNIX_TIMESTAMP(%s))/86400)";
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
     * @return the format to use for adding months.
     */
    @Override
    default String sqlAddMonths() {
		return "DATE_ADD(%s, INTERVAL TRUNCATE(%s,0) MONTH)";
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

	@Override
	default String sqlConvertDateTimeToDate(String dateTime, String userTimezone, String userTzOffset) {
		// TODO Auto-generated method stub
		return "DATE(CONVERT_TZ("+dateTime+",'UTC',"+userTzOffset+"))";
		
	}
}
