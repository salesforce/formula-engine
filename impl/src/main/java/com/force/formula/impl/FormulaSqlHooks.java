/**
 * 
 */
package com.force.formula.impl;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import com.force.formula.sql.FormulaSqlStyle;
import com.force.formula.util.FormulaI18nUtils;
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
    	if (isPostgresStyle()) {
    	   return " NOT %s ~ '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01]) ([01]?[0-9]|2[0-3]):[0-5]?\\d:[0-5]?\\d$' ";
    	}
    	if (isMysqlStyle()) {
       	   return "1=0"; // mysql returns null on error
     	}
    	return " NOT REGEXP_LIKE (%s, '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01]) ([01]?[0-9]|2[0-3]):[0-5]?\\d:[0-5]?\\d$')"
    			+ "/* Adding some comments to keep the same length for this guard as it was before improving to more robust one   */";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
    default String sqlDateValueGuard() {
    	if (isPostgresStyle()) {
     	   return " NOT %s ~ '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$' ";
     	}
    	if (isMysqlStyle()) {
      	   return "1=0"; // mysql returns null on error
      	}
    	return " NOT REGEXP_LIKE (%s, '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$') /*comments to keep size */ ";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
    default String sqlTimeValueGuard() {
    	if (isPostgresStyle()) {
      	   return " NOT %s ~ '^([01]\\d|2[0-3]):[0-5][0-9]:[0-5][0-9]\\.[0-9][0-9][0-9]$' ";
      	}
    	if (isMysqlStyle()) {
       	   return "1=0"; // mysql returns null on error
       	}
        return " NOT REGEXP_LIKE (%s, '^([01]\\d|2[0-3]):[0-5][0-9]:[0-5][0-9]\\.[0-9][0-9][0-9]$') /*comments to keep size */ ";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
    default String sqlIsNumber() {
    	if (isPostgresStyle()) {
      	   return "REGEXP_REPLACE(%s,'[0-9]+','0','g') ~ '^[+-]?(0|0\\.|\\.0|0\\.0)([Ee][+-]?0)?$'";
      	}
    	if (isMysqlStyle()) {
       	   return "REGEXP_REPLACE(%s,'[0-9]+','0') REGEXP '^[+-]?(0|0\\.|\\.0|0\\.0)([Ee][+-]?0)?$'";
       	}
        /*
         * Make the matching as efficient as possible (regex's are sloooowww).
         * 1. condense sequences of digits to one 0
         * 2. then list all valid cases
         * This is _much_ faster than doing the matching in one regex as it limits backtracking to at most one character.
         */
        return "REGEXP_LIKE(REGEXP_REPLACE(%s,'[0-9]+','0'),'^[+-]?(0|0\\.|\\.0|0\\.0)([Ee][+-]?0)?$')";
    }
    
    /**
     * @return the function to use for NVL.  In postgres, it's usually coalesce, but in oracle, you want NVL.
     */
    default String sqlNvl() {
    	if (isPostgresStyle() || isMysqlStyle()) {
    		return "COALESCE";
    	}
        return "NVL";
    }
    
    /**
     * @return the string to use for String.format to convert something to date generically, without a specified
     */
    default String sqlToDate() {
    	if (isPostgresStyle() || isMysqlStyle()) {
    		return "CAST(%s AS DATE)";
    	}
        return "TO_DATE(%s)";
    }

    /**
     * @return the string to use for String.format to convert something to number generically, without a format
     */
    default String sqlToNumber() {
    	if (isPostgresStyle()) {
    		return "CAST(%s AS NUMERIC)";
    	}
    	if (isMysqlStyle()) {
    		return "CAST(%s AS DOUBLE)";   // Specify your own default precision.
    	}
        return "TO_NUMBER(%s)";
    }

    /**
     * @return the string to use for String.format to convert something to text generically, without a format
     */
    default String sqlToChar() {
    	if (isPostgresStyle()) {
    		return "CAST(%s AS TEXT)";
    	}
    	if (isMysqlStyle()) {
    		return "CONVERT(%s,CHAR)";
    	}
        return "TO_CHAR(%s)";
    }
    
    /**
     * @return the string to use for TO_TIMESTAMP to get seconds.microseconds.  It's a subtle difference
     */
    default String sqlSecsAndMsecs() {
    	if (isPostgresStyle()) {
    		return "SSSS.MS";
    	}
        return "SSSSS.FF3";
    }
    
    /**
     * @return the string to use for TO_TIMESTAMP to get seconds.microseconds.  It's a subtle difference
     */
    default String sqlHMSAndMsecs() {
    	if (isPostgresStyle()) {
    		return "HH24:mi:ss.MS";
    	}
        return "HH24:mi:ss.FF3";
    }
    
    
    /**
     * @return the string to use for TO_TIMESTAMP to get seconds.  It's a subtle difference
     */
    default String sqlSecsInDay() {
    	if (isPostgresStyle()) {
    		return "SSSS";
    	}
        return "SSSSS"; // Oracle needs 5 Ss
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
    	if (isOracleStyle()) {
	        if (hasLocaleOverride) {
	            return "NLS_UPPER(%s,CASE WHEN SUBSTR(%s,1,2) = 'tr' THEN 'NLS_SORT=xturkish' ELSE 'NLS_SORT=xwest_european' END)";
	        } else {
	            return "NLS_UPPER(%s,'NLS_SORT=xwest_european')";
	        }
    	}
    	// You could do UPPER(%s COLLATE %s), but that doesn't work in general.
    	return "UPPER(%s)";
    }

    /**
     * PSQL doesn't include a locale-specific lower function.  This allows the override of that function 
     * if you have one locally, like icu_transform
     * @param hasLocaleOverride if the locale override should be used.  If not, the second format argument will be 'en'
     * @return the sql expression to use for lowercase with a locale
     */
    default String sqlLowerCaseWithLocaleFormat(boolean hasLocaleOverride) {
    	if (isOracleStyle()) {
            if (hasLocaleOverride) {
                return "NLS_LOWER(%s,CASE WHEN SUBSTR(%s,1,2) = 'tr' THEN 'NLS_SORT=xturkish' ELSE 'NLS_SORT=xwest_european' END)";
            } else {
                return "NLS_LOWER(%s,'NLS_SORT=xwest_european')";
            }
    	}
        return "LOWER(%s)";
    }

    /**
     * @return the function that allows subtraction of two timestamps to get the microsecond/day difference.  This is
     * missing from psql, but available in oracle.  This allows you to try and fix that.
     */
    default String psqlSubtractTwoTimestamps() {
    	if (isMysqlStyle()) {
    		return "((UNIX_TIMESTAMP(%s)-UNIX_TIMESTAMP(%s))/86400)";
    	}
        return "((EXTRACT(EPOCH FROM %s)-EXTRACT(EPOCH FROM %s))::numeric/86400)";
    } 
    
    
    /**
     * Function right can be... complicated, especially in Oracle
     * @param stringArg the sql for the string value
     * @param countArg the sql for the number of chars
     * @return the SQL for generating RIGHT()
     */
    default String sqlRight(String stringArg, String countArg) {
        // Oracle allows {n,m} where m < 0 and treats it as {0,0} but Postgres will throw an error, and
        // this actually comes up in formula tests. The Postgres would work on both, but it makes the sql longer
        // so that is not allowed for Oracle
    	//return "REGEXP_SUBSTR("+stringArg+",'.{0,'||NVL2("+countArg+",CASE WHEN "+countArg+"<0 THEN 0 ELSE "+countArg+" END,0)||'}$',1,1,'n')"; 
    	if (isOracleStyle()) {
            return "REGEXP_SUBSTR("+stringArg+",'.{0,'||NVL("+countArg+",0)||'}$',1,1,'n')";
    	}
    	if (isMysqlStyle()) {
            return "RIGHT(" + stringArg + ", GREATEST(" + countArg+ ", 0))";
    	}
        return "RIGHT(" + stringArg + ", GREATEST(" + countArg+ ", 0)::integer)";
    }
    
    /**
     * @return the the date.  Overridable in case you want to change the timezone functionality with ::timestamp.
     */
    default String sqlNow() {
    	if (isOracleStyle()) {
    		return "SYSDATE";
    	}
        return "NOW()";
    }
    
    /**
     * @return the the current milliseconds of the day suitable.  You may want to use ::time instead, so overridable
     */
    default String sqlTimeNow() {
    	if (isOracleStyle()) {
    		return "TO_NUMBER(TO_CHAR(SYSTIMESTAMP, '"+sqlSecsAndMsecs()+"'))*1000";    		
    	}
    	if (isMysqlStyle()) {
            return "CAST(UNIX_TIMESTAMP(CURTIME(3)) * 1000 AS unsigned)";
    	}
        return "EXTRACT(EPOCH FROM AGE(NOW()::timestamp, DATE_TRUNC('day', NOW()::timestamp)))::BIGINT::NUMERIC";
    }
    
    /**
     * @return the format to use for adding months.
     */
    default String sqlAddMonths() {
    	if (isOracleStyle()) {
    		return "ADD_MONTHS(%s, %s)";
    	}
    	if (isPostgresStyle()) {
    		return "(%s+'1 day'::interval+('1 month'::interval*TRUNC(%s)))-'1 day'::interval";
    	}
    	if (isMysqlStyle()) {
    		return "DATE_ADD(%s, INTERVAL %s MONTH)";

    	}
    	throw new UnsupportedOperationException();
    }
    
    
    /**
     * @return the format for converting to a datetime value
     */
    default String sqlToTimestampIso() {
    	if (isMysqlStyle()) {
    		return "TIMESTAMP(%s)";
    	}
    	if (isPostgresStyle()) {
    		return "TO_TIMESTAMP(%s, 'YYYY-MM-DD HH24:MI:SS')";
    	}
		return "TO_DATE(%s, 'YYYY-MM-DD HH24:MI:SS')";
    }
    
    /**
     * @return the format for converting to a datetime value
     */
    default String sqlToDateIso() {
    	if (isMysqlStyle()) {
    		return "STR_TO_DATE(%s, '%%Y-%%m-%%d')";
    	}
		return "TO_DATE(%s, 'YYYY-MM-DD')";
    }
    
    
    /**
     * @return the function to use for getting the last day of the month of a date.
     */
    default String sqlLastDayOfMonth() {
    	if (isPostgresStyle()) {
    		return "EXTRACT(DAY FROM (date_trunc('month',%s)+ interval '1 month -1 day')::timestamp(0))::numeric";
    	}
    	if (isMysqlStyle()) {
    		return "LAST_DAY(%s)";
    	}
		return "TO_CHAR(LAST_DAY(%s),'DD')";
    }

    
    /**
     * @return the format for converting to a datetime value
     * @param withSpaces whether spaces should be used around the "||" for compatibility
     */
    default String sqlConcat(boolean withSpaces) {
    	if (isPostgresStyle()) {
    		// Formula engine generally allows || with nulls, as it's less confusing
    		return "CONCAT(%s, %s)";
    	}
    	if (isMysqlStyle()) {
    		return "CONCAT_WS(\"\",%s, %s)";  // CONCAT doesn't skip nulls
    	}
		return withSpaces ? "%s || %s" : "%s||%s";
    }

    /**
     * @return the formula for finding a substring in a string and returning the "position" 1-indexed.
     * In oracle and mysql, it's INSTR.
     * @param strArg the value of the string to search in
     * @param substrArg the value of the substring to search
     */
    default String sqlInstr2(String strArg, String substrArg) {
    	if (isPostgresStyle()) {
    		return String.format("STRPOS(%s, %s)", strArg, substrArg);
    	}
    	if (isMysqlStyle()) {
    		return String.format("INSTR(binary %s, %s)", strArg, substrArg);
    	}
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
    	if (isPostgresStyle()) {
    		// This is unfortunate, as it has to reevaluate the subexpression twice in order to return 0 for not found.
    		// If your postgresql has a better version of this, please use it instead.
    		return String.format("CASE WHEN COALESCE(STRPOS(SUBSTR(%s,%s::integer),%s),0) > 0 THEN STRPOS(SUBSTR(%s,%s::integer),%s) + %s - 1 ELSE 0 END", strArg, startLocation, substrArg, strArg, startLocation, substrArg, startLocation);
    	}
    	if (isMysqlStyle()) {
    		return String.format("CASE WHEN COALESCE(INSTR(binary SUBSTR(%s,%s),%s),0) > 0 THEN INSTR(binary SUBSTR(%s,%s),%s) + %s - 1 ELSE 0 END", strArg, startLocation, substrArg, strArg, startLocation, substrArg, startLocation);
    	}
		return String.format("INSTR(%s, %s, %s)", strArg, substrArg, startLocation);
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
    	
        if (isOracleStyle()) {
	        // Get the decimal point and thousands separator characters
	        NumberFormat nf = FormulaI18nUtils.getLocalizer().getNumberFormat();
	        
	        char groupingSeparator = ','; // Default to comma.
	        char decimalSeparator = '.'; // Default to dot.
	        
	        if (nf instanceof DecimalFormat) {
	            groupingSeparator = ((DecimalFormat)nf).getDecimalFormatSymbols().getGroupingSeparator();
	            decimalSeparator = ((DecimalFormat)nf).getDecimalFormatSymbols().getDecimalSeparator();
	        } 
	        // Add this if using ICU number formatting.
	        /*
	        else if (nf instanceof NumberFormatICU) {
	            com.ibm.icu.text.NumberFormat nf_icu = ((NumberFormatICU)nf).unwrap();
	            if (nf_icu instanceof com.ibm.icu.text.DecimalFormat) {
	                groupingSeparator = ((com.ibm.icu.text.DecimalFormat)nf_icu).getDecimalFormatSymbols().getGroupingSeparator();
	                decimalSeparator = ((com.ibm.icu.text.DecimalFormat)nf_icu).getDecimalFormatSymbols().getDecimalSeparator();
	            }
	        }
	        */
	        
	        // Oracle format
	        StringBuilder nlsParam = new StringBuilder(40).append("'NLS_NUMERIC_CHARACTERS=''").append(decimalSeparator);  // there are no weird DecimalSeparator characters
	        if (groupingSeparator == '\'')
	            nlsParam.append("''");  // the single quote character needs to be doubled up
	        else if (groupingSeparator == '\u00A0')
	            nlsParam.append(' ');  // oracle doesn't like the unicode NBSP character
	        else
	            nlsParam.append(groupingSeparator);
	        nlsParam.append("'''");
	
	        // Put everything together
	        StringBuilder sql = new StringBuilder(800);
	        if (canAmountBeNull) {
	            // Handle null amount
	            sql.append("NVL2(").append(amountArg).append(',');
	        }
	
	        sql.append(isoCodeArg).append("||' '||TO_CHAR(").append(amountArg).append(',').append(maskStr).append(',').append(nlsParam).append(')');

	
	        if (canAmountBeNull) {
	            // Handle null amount
	            sql.append(",NULL)");
	        }
	        return sql.toString();
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
        
        /*
        // Alternative implementation where single currency instances get formatted with currency sign
        UddOrgInfo orgInfo = ...;  // This has the information about the current instance
        DecimalFormatSymbols orgDfs = new DecimalFormatSymbols(orgInfo.getCurrencyLocale());
        if (!orgInfo.isMultiCurrencyEnabled()) { 
            // For single-currency orgs, prefix depends on whether isocode is the same as the org currency.
            sql.append("CASE WHEN ").append(isoCodeArg).append("<>'").append(orgDfs.getInternationalCurrencySymbol()).append("'THEN ");
            // Prefix is isocode, followed by a space, followed by an '-' sign when number is negative (just like multi-currency orgs).
            sql.append(isoCodeArg).append("||' '||");
            sql.append("CASE WHEN ").append(amountArg).append("<0THEN'-'ELSE NULL END");
            sql.append(" ELSE ");
            // Prefix is an '(' sign when number is negative, followed by the currency sign.
            sql.append("CASE WHEN ").append(amountArg).append("<0THEN'('ELSE NULL END");
            sql.append("||'").append(orgDfs.getCurrencySymbol()).append('\'');
            sql.append(" END||");
        } else {
            // For multi-currency orgs, prefix is isocode, followed by a space, followed by an optional '-' sign when number is negative
            sql.append(isoCodeArg).append("||' '||");
            sql.append("CASE WHEN ").append(amountArg).append("<0THEN'-'ELSE NULL END||");
        }
        sql.append("TO_CHAR(ABS(").append(amountArg).append("),").append(maskStr).append(',').append(nlsParam).append(")");
        if (!orgInfo.isMultiCurrencyEnabled()) {
            // For single-currency orgs, again check if isocode is the same as the org currency.
            // If showing currency sign, suffix is a ')' sign when number is negative.
            sql.append("||CASE WHEN ").append(isoCodeArg).append("<>'").append(orgDfs.getInternationalCurrencySymbol()).append("'AND ")
               .append(amountArg).append("<0THEN')'ELSE NULL END");
        }
        */
    }
    
    /**
     * Formulas are usually numeric, but some functions, like round or trunc, require a cast to ::int in postgres
     * @param argument scale argument
     * @return the argument converted to an integer for rounding
     */
    default String sqlRoundScaleArg(String argument) {
    	if (isPostgresStyle()) {
    		return argument + "::integer";
    	}
    	return argument;
    }

    /**
     * Formulas are usually numeric, but some functions, like round or trunc, return an integer that may
     * cause type cast issues
     * @param argument argument that's in an integer
     * @return the argument converted from an integer to numeric for use
     */
    default String sqlMakeDecimal(String argument) {
    	if (isPostgresStyle()) {
    		return argument + "::numeric";
    	}
    	return argument;
    }
    
    /**
     * Default implementation of SqlStyles.  If you want to override any of the sql  functions here, override
     * the 
     * @author stamm
     */
	public static enum DefaultStyle implements FormulaSqlHooks {
		POSTGRES,
		ORACLE,
		MYSQL;

		@Override
		public boolean isPostgresStyle() {
			return this == POSTGRES;
		}

		@Override
		public boolean isOracleStyle() {
			return this == ORACLE;
		}

		@Override
		public boolean isMysqlStyle() {
			return this == MYSQL;
		}	
	}
}
