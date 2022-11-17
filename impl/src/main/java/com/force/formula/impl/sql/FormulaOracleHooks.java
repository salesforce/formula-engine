/**
 * 
 */
package com.force.formula.impl.sql;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import com.force.formula.FormulaDateTime;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.impl.FormulaValidationHooks;
import com.force.formula.util.FormulaI18nUtils;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * Implementation of FormulaSqlHooks for Oracle-style DBs
 * @author stamm
 * @since 0.2
 */
public interface FormulaOracleHooks extends FormulaSqlHooks {
	@Override
	default boolean isOracleStyle() {
		return true;
	}	

	// Handle plsql regexp differences (where oracle needs regexp_like and postgres wants ~ or similar to)
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for DateTime Value
     */
	@Override
    default String sqlDatetimeValueGuard() {
    	return " NOT REGEXP_LIKE (%s, '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01]) ([01]?[0-9]|2[0-3]):[0-5]?\\d:[0-5]?\\d$')"
    			+ "/* Adding some comments to keep the same length for this guard as it was before improving to more robust guard*/";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
	@Override
    default String sqlDateValueGuard() {
    	return " NOT REGEXP_LIKE (%s, '^\\d{4}-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$') /*comments to keep size */ ";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
	@Override
    default String sqlTimeValueGuard() {
        return " NOT REGEXP_LIKE (%s, '^([01]\\d|2[0-3]):[0-5][0-9]:[0-5][0-9]\\.[0-9][0-9][0-9]$') /*comments to keep size */ ";
    }
    
    /**
     * @return how to do "not regexp_like", where the %s is used to represent the value to guard against for a date Value
     */
	@Override
	default String sqlIsNumber() {
        return "REGEXP_LIKE(REGEXP_REPLACE(%s,'[0-9]+','0'),'^[+-]?(0|0\\.|\\.0|0\\.0)([Ee][+-]?0)?$')";
    }
    
    /**
     * @return the function to use for NVL.  In postgres, it's usually coalesce, but in oracle, you want NVL.
     */
	@Override
	default String sqlNvl() {
        return "NVL";
    }
    
    /**
     * @return the string to use for String.format to convert something to date generically, without a specified
     */
	@Override
    default String sqlToDate(Type dateType) {
        return "TO_DATE(%s)";
    }

    /**
     * @return the string to use for String.format to convert something to number generically, without a format
     */
	@Override
    default String sqlToNumber() {

        return "TO_NUMBER(%s)";
    }

    /**
     * @return the string to use for String.format to convert something to text generically, without a format
     */
	@Override
    default String sqlToChar() {
        return "TO_CHAR(%s)";
    }
    
    /**
     * @return the string to use for TO_TIMESTAMP to get seconds.microseconds.  It's a subtle difference
     */
	@Override
    default String sqlSecsAndMsecs() {
        return "SSSSS.FF3";
    }
    
    /**
     * @return the string to use for TO_TIMESTAMP to get seconds.microseconds.  It's a subtle difference
     */
	@Override
    default String sqlHMSAndMsecs() {
        return "HH24:mi:ss.FF3";
    }
    
    
    /**
     * @return the string to use for TO_TIMESTAMP to get seconds.  It's a subtle difference
     */
	@Override
    default String sqlSecsInDay() {
        return "SSSSS"; // Oracle needs 5 Ss
    }
	
    /**
     * PSQL doesn't include a locale-specific upper function.  This allows the override of that function
     * if you have installed one locally, like icu_transform.
     * @param hasLocaleOverride if the locale override should be used.  If not, the second format argument will be 'en'
     * @return the sql expression to use for uppercase with a locale
     */
	@Override
    default String sqlUpperCaseWithLocaleFormat(boolean hasLocaleOverride) {
        if (hasLocaleOverride) {
            return "NLS_UPPER(%s,CASE WHEN SUBSTR(%s,1,2) = 'tr' THEN 'NLS_SORT=xturkish' ELSE 'NLS_SORT=xwest_european' END)";
        } else {
            return "NLS_UPPER(%s,'NLS_SORT=xwest_european')";
        }
    }

    /**
     * PSQL doesn't include a locale-specific lower function.  This allows the override of that function 
     * if you have one locally, like icu_transform
     * @param hasLocaleOverride if the locale override should be used.  If not, the second format argument will be 'en'
     * @return the sql expression to use for lowercase with a locale
     */
	@Override
    default String sqlLowerCaseWithLocaleFormat(boolean hasLocaleOverride) {
        if (hasLocaleOverride) {
            return "NLS_LOWER(%s,CASE WHEN SUBSTR(%s,1,2) = 'tr' THEN 'NLS_SORT=xturkish' ELSE 'NLS_SORT=xwest_european' END)";
        } else {
            return "NLS_LOWER(%s,'NLS_SORT=xwest_european')";
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
        // Oracle allows {n,m} where m < 0 and treats it as {0,0} but Postgres will throw an error, and
        // this actually comes up in formula tests. The Postgres would work on both, but it makes the sql longer
        // so that is not allowed for Oracle
    	//return "REGEXP_SUBSTR("+stringArg+",'.{0,'||NVL2("+countArg+",CASE WHEN "+countArg+"<0 THEN 0 ELSE "+countArg+" END,0)||'}$',1,1,'n')"; 
        return "REGEXP_SUBSTR("+stringArg+",'.{0,'||NVL("+countArg+",0)||'}$',1,1,'n')";
    }
    
    /**
     * @return the the date.  Overridable in case you want to change the timezone functionality with ::timestamp.
     */
	@Override
    default String sqlNow() {
		return "SYSDATE";
    }
    
    /**
     * @return the the current milliseconds of the day suitable.  You may want to use ::time instead, so overridable
     */
	@Override
    default String sqlTimeNow() {
		return "TO_NUMBER(TO_CHAR(SYSTIMESTAMP, '"+sqlSecsAndMsecs()+"'))*1000";    		
    }
    
    /**
     * @return the format to use for adding months.
     */
	@Override
    default String sqlAddMonths(String dateArg, Type dateArgType, String numMonths) {
		return String.format("ADD_MONTHS(%s, %s)", dateArg, numMonths);
    }
    
    /**
     * @return the function to use for getting the last day of the month of a date.
     */
	@Override
    default String sqlLastDayOfMonth() {
		return "TO_CHAR(LAST_DAY(%s),'DD')";
    }
	
	@Override
    default String sqlInitCap(boolean hasLocaleOverride) {
        if (hasLocaleOverride) {
            return "NLS_INITCAP(%s,CASE WHEN SUBSTR(%s,1,2) = 'nl' THEN 'NLS_SORT=xdutch' ELSE 'NLS_SORT=xwest_european' END)";
        } else {
            return "NLS_INITCAP(%s)";
        }
    }
	
    /**
     * Intervals in oracle aren't helpful, so don't use them.
     */
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
	
	@Override
    default String sqlChr() {
    	return "CHR(%s USING NCHAR_CS)";
    }
    
	@Override
    default String sqlAscii() {
    	return "ASCII(UNISTR(%s))";
    }
    
    /**
     * @return the format for converting to a datetime value
     * @param withSpaces whether spaces should be used around the "||" for compatibility
     */
	@Override
    default String sqlConcat(boolean withSpaces) {
		return withSpaces ? "%s || %s" : "%s||%s";
    }
    
    @Override
	default String sqlConvertDateTimeToDate(String dateTime, String userTimezone, String userTzOffset) {
    	if (FormulaValidationHooks.get().canUseDatevalueFixedForDST()) {
            // Oracle org with the DatevalueFixForDSTEnabled preference on
            return "TRUNC(CAST((FROM_TZ(CAST("+dateTime+" AS TIMESTAMP), 'UTC') AT TIME ZONE '"+userTimezone+"') AS DATE))";
        } else {
            // Oracle org with the DatevalueFixForDSTEnabled preference off
        	return "TRUNC("+dateTime+" + ("+userTzOffset+"/24.0))";
        }
  }

	/**
     * @param scale the number of digits to the right of the radix
     * @return the SQL string to use to in TO_CHAR for the given scale.  This is used by 
     * {@link #getCurrencyFormat(String, String, boolean)}
     */
	@Override
    default StringBuilder getCurrencyMask(int scale) {
        StringBuilder mask = new StringBuilder(40).append("'FM9G999G999G999G999G999G990");
        if (scale > 0) {
            mask.append('D');
            for (int i = 0; i < scale; i++) mask.append('0');
        }
        mask.append('\'');
        return mask;
    }
    
    @Override
	default void appendCurrencyFormat(StringBuilder sql, String isoCodeArg, String amountArg, CharSequence maskStr) {

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
        StringBuilder nlsParam = new StringBuilder(40).append("'NLS_NUMERIC_CHARACTERS=''").append(decimalSeparator);  // there are no weird DecimalSeparator characters
        if (groupingSeparator == '\'')
            nlsParam.append("''");  // the single quote character needs to be doubled up
        else if (groupingSeparator == '\u00A0')
            nlsParam.append(' ');  // oracle doesn't like the unicode NBSP character
        else
            nlsParam.append(groupingSeparator);
        nlsParam.append("'''");
        sql.append(isoCodeArg).append("||' '||TO_CHAR(").append(amountArg).append(',').append(maskStr).append(',').append(nlsParam).append(')');

    }

	/**
     * @return the SQL string to use to format currency.
     * @param isoCodeArg the argument with the 3 character (ISO 4217) currency code 
     * @param amountArg the argument with the numeric value of the currency
     * @param canAmountBeNull whether the argument can be null (should this be in a coalesce statement)
     */
	@Override
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

        // Put everything together
        StringBuilder sql = new StringBuilder(800);
        if (canAmountBeNull) {
            // Handle null amount
            sql.append("NVL2(").append(amountArg).append(',');
        }

        appendCurrencyFormat(sql, isoCodeArg, amountArg, maskStr);

        if (canAmountBeNull) {
            // Handle null amount
            sql.append(",NULL)");
        }
        return sql.toString();
    }

	
}
