/**
 * 
 */
package com.force.formula.impl;

import java.lang.reflect.Type;
import java.util.*;

import com.force.formula.sql.FormulaSqlStyle;
import com.force.formula.sql.SQLPair;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * A set of SQL hooks used in standard functionality to provide differences in SQL implementations.
 * 
 * @author stamm
 * @since 0.1.0
 */
public interface FormulaSqlHooks extends FormulaSqlStyle {
	
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
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
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
     * @return the string to use for String.format to convert something to date generically, without a specified
     */
    default String sqlToDate() {
		return "CAST(%s AS DATE)";
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
		return "CAST(%s AS CHAR)";
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
     * @return the function to use for conversion to Date generically.
     */
    default String sqlNullToDate() {
    	return String.format(sqlToDate(), "NULL");
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
     * @return the function that allows subtraction of two timestamps to get the microsecond/day difference.  This is
     * missing from psql, but available in oracle.  This allows you to try and fix that.
     */
    default String sqlSubtractTwoTimestamps() {
    	throw new UnsupportedOperationException();
    } 

    /**
     * Format the sql for adding a given number of days (fractionally) to a date
     * @param lhsValue the left hand side (must be date or datetime if subtraction)
     * @param rhsValue the right hand side (will be number if subtraction)
     * @param isAddition whet
     * @return a SQL expression for adding the given number of days to the date
     */
    default String sqlAddDaysToDate(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType,  boolean isAddition) {
    	return "(" + lhsValue + (isAddition ? "+" : "-") + rhsValue + ")";
    }
    
    
    
    /**
     * Function right can be... complicated, especially in Oracle
     * @param stringArg the sql for the string value
     * @param countArg the sql for the number of chars
     * @return the SQL for generating RIGHT()
     */
    default String sqlRight(String stringArg, String countArg) {
        return "RIGHT(" + stringArg + ", GREATEST(" + countArg+ ", 0))";
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
     */
    default String sqlAddMonths(String dateArg, String numMonths) {
    	throw new UnsupportedOperationException();
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
     * @return the function to use for getting the last day of the month of a date.
     */
    default String sqlLastDayOfMonth() {
    	throw new UnsupportedOperationException();
    }

    
    /**
     * @return the format for converting to a datetime value
     * @param withSpaces whether spaces should be used around the "||" for compatibility
     */
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
        sql.append("CONCAT(").append(isoCodeArg).append(",' ',TO_CHAR(").append(amountArg).append(',').append(maskStr).append("))");
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
     * @param argument the value to truncate
     * @return how to call exponent on the function, 
     */
    default String sqlExponent(String argument) {
    	return "EXP(" + argument + ")";
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
}
