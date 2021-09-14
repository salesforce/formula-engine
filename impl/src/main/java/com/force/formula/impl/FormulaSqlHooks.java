/**
 * 
 */
package com.force.formula.impl;

import com.force.formula.sql.FormulaSqlStyle;

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
    	   return " NOT %s ~ '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01]) ([01]?[0-9]|2[0-3]):[0-5]\\d:[0-5]\\d$' ";
    	}
    	return " NOT REGEXP_LIKE (%s, '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01]) ([01]?[0-9]|2[0-3]):[0-5]\\d:[0-5]\\d$')"
    			+ "/*  Adding some comments to keep the same length for this guard as it was before improving to more robust guard */";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
    default String sqlDateValueGuard() {
    	if (isPostgresStyle()) {
     	   return " NOT %s ~ '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$' ";
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
        return " NOT REGEXP_LIKE (%s, '^([01]\\d|2[0-3]):[0-5][0-9]:[0-5][0-9]\\.[0-9][0-9][0-9]$') /*comments to keep size */ ";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
    default String sqlIsNumber() {
    	if (isPostgresStyle()) {
      	   return "REGEXP_REPLACE(%s,'[0-9]+','0','g') ~ '^[+-]?(0|0\\.|\\.0|0\\.0)([Ee][+-]?0)?$'";
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
    	if (isPostgresStyle()) {
    		return "COALESCE";
    	}
        return "NVL";
    }
    
    /**
     * @return the string to use for String.format to convert something to date generically, without a specified
     */
    default String sqlToDate() {
    	if (isPostgresStyle()) {
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
        return "TO_NUMBER(%s)";
    }

    /**
     * @return the string to use for String.format to convert something to text generically, without a format
     */
    default String sqlToChar() {
    	if (isPostgresStyle()) {
    		return "CAST(%s AS TEXT)";
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
        return "SSSSS.F3";
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
     */
    default String sqlUpperCaseWithLocaleFormat(boolean hasLocaleOverride) {
    	if (isOracleStyle()) {
	        if (hasLocaleOverride) {
	            return "NLS_UPPER(%s,CASE WHEN SUBSTR(%s,1,2) = 'tr' THEN 'NLS_SORT=xturkish' ELSE 'NLS_SORT=xwest_european' END)";
	        } else {
	            return "NLS_UPPER(%s,'NLS_SORT=xwest_european')";
	        }
    	}
    	// return "sfdc_util.icu_upper(%s,%s)";
    	// You could do UPPER(%s COLLATE %s), but that doesn't work in general.
    	return "UPPER(%s)";
    }

    /**
     * PSQL doesn't include a locale-specific lower function.  This allows the override of that function 
     * if you have one locally, like icu_transform
     * @param hasLocaleOverride if the locale override should be used.  If not, the second format argument will be 'en'
     */
    default String sqlLowerCaseWithLocaleFormat(boolean hasLocaleOverride) {
    	if (isOracleStyle()) {
            if (hasLocaleOverride) {
                return "NLS_LOWER(%s,CASE WHEN SUBSTR(%s,1,2) = 'tr' THEN 'NLS_SORT=xturkish' ELSE 'NLS_SORT=xwest_european' END)";
            } else {
                return "NLS_LOWER(%s,'NLS_SORT=xwest_european')";
            }
    	}
        // return "sfdc_util.icu_lower(%s,%s)";
        return "LOWER(%s)";
    }

    /**
     * @return the function that allows subtraction of two timestamps to get the microsecond/day difference.  This is
     * missing from psql, but available in oracle.  This allows you to try and fix that.
     */
    default String psqlSubtractTwoTimestamps() {
        // return "sfdc_util.ts_minus_ts(%s,%s)";
        return "(EXTRACT(EPOCH FROM %s)-EXTRACT(EPOCH FROM %s))";
    } 
    
    
    /**
     * Function right can be... complicated, especially i
     * @param stringArg
     * @param countArg
     * @return
     */
    default String sqlRight(String stringArg, String countArg) {
        // Oracle allows {n,m} where m < 0 and treats it as {0,0} but Postgres will throw an error, and
        // this actually comes up in formula tests. The Postgres would work on both, but it makes the sql longer
        // so that is not allowed for Oracle
    	//return "REGEXP_SUBSTR("+stringArg+",'.{0,'||NVL2("+countArg+",CASE WHEN "+countArg+"<0 THEN 0 ELSE "+countArg+" END,0)||'}$',1,1,'n')"; 
    	if (isOracleStyle()) {
            return "REGEXP_SUBSTR(%s,'.{0,'||NVL(%s,0)||'}$',1,1,'n')";
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
        // return "SFDC_DATE()";
        return "NOW()";
    }
    
    /**
     * @return the the current milliseconds of the day suitable.  You may want to use ::time instead, so overridable
     */
    default String sqlTimeNow() {
    	if (isOracleStyle()) {
    		return "TO_NUMBER(TO_CHAR(SYSTIMESTAMP, '"+sqlSecsAndMsecs()+"'))*1000";    		
    	}
        // return "EXTRACT(EPOCH FROM AGE(SFDC_TIMESTAMP(), DATE_TRUNC('day', SFDC_TIMESTAMP())))::BIGINT::NUMERIC";
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
    		return "%s + '1 month'::interval*%s";
    	}
    	throw new UnsupportedOperationException();
    }
    
    
    /**
     * @return the format for converting to a datetime value
     */
    default String sqlToTimestampIso() {
    	if (isPostgresStyle()) {
    		return "TO_TIMESTAMP(%s, 'YYYY-MM-DD HH24:MI:SS')";
    	}
		return "TO_DATE(%s, 'YYYY-MM-DD HH24:MI:SS')";
    }
    
    /**
     * @return the format for converting to a datetime value
     */
    default String sqlToDateIso() {
		return "TO_DATE(%s, 'YYYY-MM-DD')";
    }
     
    /**
     * @return the format for converting to a datetime value
     * @param whether spaces should be used around the "||" for compatibility
     */
    default String sqlConcat(boolean withSpaces) {
    	if (isPostgresStyle()) {
    		// Formula engine generally allows || with nulls, as it's less confusing
    		return "CONCAT(%s, %s)";
    	}
		return withSpaces ? "%s || %s" : "%s||%s";
    }
    
    
    
    /**
     * Default implementation of SqlStyles.  If you want to override any of the sql  functions here, override
     * the 
     * @author stamm
     */
	public static enum DefaultStyle implements FormulaSqlHooks {
		POSTGRES,
		ORACLE;

		@Override
		public boolean isPostgresStyle() {
			return this == POSTGRES;
		}

		@Override
		public boolean isOracleStyle() {
			return this == ORACLE;
		}
	}
}
