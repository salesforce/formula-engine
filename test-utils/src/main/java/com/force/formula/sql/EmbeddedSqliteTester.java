/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.force.formula.DisplayField;
import com.force.formula.Formula;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.MockFormulaDataType;
import com.force.formula.util.FormulaDateUtil;

/**
 * a DbTester of formulas that uses embedded sqlite for validation.  
 * 
 * Since the sqlite-jdbc driver contains everything to run sqlite 
 * this is relatively straightforward
 * 
 * @author stamm
 * @since 0.3.0
 */
public class EmbeddedSqliteTester extends AbstractDbTester {

	public EmbeddedSqliteTester() throws IOException {
	}

	@Override
	public String getDbTypeName() {
		return "sqlite3";
	}

	@Override
    protected boolean useBinds() {
	    // Don't use binds because by default, Dates are bound using numbers
	    // which seems to confuse things.  You can change the binding in the
	    // SqlConnection 
	    // TODO: Support either numbers of strings for dates
        return false;
    }
	
	@Override
	protected Connection getConnection() throws SQLException, IOException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        //((SQLiteConnection)conn).getConnectionConfig().setDateClass("TEXT");
        return conn;
	}
	
	@Override
	protected String getDecimalType() {
		return "numeric";
	}
	
	@Override
	protected String getTextType() {
		return "text";
	}
	
	@Override
	protected String getTimestampType() {
		return "timestamp";
	}
	
    /**
     * @param arg the string to convert to a timestamp
     * @return a SQL string that will convert arg to a datetime
     */
    @Override
    protected String stringToDateTime(String arg) {
        return arg;
    }
    
    /**
     * @param arg the string to convert to a date
     * @return a SQL string that will convert arg to a datetime
     */
    @Override
    protected String stringToDate(String arg) {
        return arg;
    }

	@Override
	public void close() throws Exception {
		// It autocloses.
	}
	
	// Date and DateTimes are suggestions in sqlite.
	@Override
    protected String formatDbResult(ResultSet rset, FormulaRuntimeContext formulaContext, Formula formula) throws SQLException {
        MockFormulaDataType returnType = (MockFormulaDataType) formula.getDataType();
        switch (returnType) {
        case DATEONLY:
            // Note Date's can be numbers or strings in sqlite.  We're assuming strings
            try {
                Date d = rset.getDate(1);
                if (d == null)
                    return null;
                return new Timestamp(d.getTime()).toString();
            } catch (SQLException ex) {
                if ("Error parsing date".equals(ex.getMessage())) {
                    String str = rset.getString(1);
                    try {
                        Date d = DATE_FORMATTER.get().parse(str);
                        return new Timestamp(d.getTime()).toString();
                    } catch (ParseException e) {
                        throw ex;
                    }
                    
                }
            }
        case DATETIME:
            try {
                Timestamp ts = rset.getTimestamp(1);
                if (ts == null)
                    return null;
                return ts.toString();
            } catch (SQLException ex) {
                if ("Error parsing time stamp".equals(ex.getMessage())) {
                    Date d = rset.getDate(1);
                    return new Timestamp(d.getTime()).toString();
                }
                throw ex;
            }
        case INTEGER:
        case DOUBLE:
            BigDecimal number = rset.getBigDecimal(1);
            if (number == null)
                return null;
            // Mac and Linux have different scales...  Sigh..
            if (number.scale() > 12) {
                number = number.setScale(12, RoundingMode.HALF_DOWN);
            }
            return number.stripTrailingZeros().toPlainString(); // Strip trailing zeros because that's driver specific.

        default:
        }
        
        return super.formatDbResult(rset, formulaContext, formula);
    }

	// In Sqlite, time is an illusion.
    @Override
    protected String formatDbTimeResult(ResultSet rset) throws SQLException {
        String str = rset.getString(1);
        return str;
    }

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMATTER = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

    // Pass in literal values as YYYY-MM-DD, not what oracle wants.
    @Override
    protected String getSqlLiteralValue(DisplayField df, Object value) {
        switch ((MockFormulaDataType) df.getFormulaFieldInfo().getDataType()) {
        case DATEONLY:
            return stringToDate("'" + DATE_FORMATTER.get().format((Date) value) + "'");
        case DATETIME:
            return stringToDate("'" + FormulaDateUtil.formatDatetimeToSqlLiteral((Date) value) + "'");
        default:
            
        }
        return super.getSqlLiteralValue(df, value);
    }
}
