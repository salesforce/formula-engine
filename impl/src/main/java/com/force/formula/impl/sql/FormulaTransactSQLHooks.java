/**
 * 
 */
package com.force.formula.impl.sql;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;

import com.force.formula.FormulaDateTime;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.sql.SQLPair;

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
    default String sqlToDate() {
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
  
    /**
     * @return the string to use for String.format to convert something to number generically, without a format
     */
    @Override
    default String sqlToNumber() {
		return "CAST(%s AS DECIMAL(38,18))";   // Override to specify your own default precision.
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
		return String.format("CHARINDEX(%s, %s, %s COLLATE Latin1_General_CS_AS)", substrArg, strArg, startLocation);
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
	

    /**
     * @return the function that allows subtraction of two timestamps to get the microsecond/day difference.  This is
     * missing from mysql, but available in oracle.  This allows you to try and fix that.
     */
    @Override
    default String sqlSubtractTwoTimestamps() {
    	// If you are older than sqlserver 16, you'll need to use DATEDIFF and deal with the errors
    	return "(CAST(-DATEDIFF_BIG(SECOND,%s,%s) AS DECIMAL(38,10))/86400)";  
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
	/**
     * @return the format to use for adding months.
     */
	@Override
    default String sqlAddMonths(String dateArg, String numMonths) {
    	return String.format("DATEADD(MONTH, %s, %s)", numMonths, dateArg);
    }
	
	

	@Override
	default String sqlExponent(String argument) {
        // tests showed double precision was only 2.5% faster than numeric (i.e. the rest of
        // the query processing machinery dominates, so go for max precision
		return "CAST(EXP(" + argument + ") AS DECIMAL(38,10))";
	}
    
	@Override
    default SQLPair getPowerSql( String[] args, String[] guards) {
        String sql = "CAST(POWER(" + args[0] + ", " + args[1] + ") AS DECIMAL(38,10))";
        String guard = SQLPair.generateGuard(guards, "ROUND(" + args[1] + ",0,1)<>" + args[1] +
	            " OR(" + args[0] + "<>0 AND LOG10(ABS(" + args[0] + "))*" + args[1] + ">38)");    	
        return new SQLPair(sql, guard);
    }
	
	@Override
    default String sqlEnsurePositive(String argument) {
		// SQL Server doesn't have greatest.
    	return "(" + argument + " + ABS(" + argument + ")/2";
    }

}
