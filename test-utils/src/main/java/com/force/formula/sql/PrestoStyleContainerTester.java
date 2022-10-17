/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.force.formula.DisplayField;
import com.force.formula.FormulaEngine;

/**
 * Presto-style tester that uses a container (either deprecated presto, or newer trino)
 * @author stamm
 */
public abstract class PrestoStyleContainerTester<DB extends JdbcDatabaseContainer<?>> extends DbContainerTester<DB> {

	/**
	 * @throws IOException
	 */
	public PrestoStyleContainerTester() throws IOException {
	}

    @Override
    protected boolean useBinds() {
        return false;   // Trino/Presto have a bug in setBigDecimal which calls toString instead of toPlainString
        // and the DB does not parse scientific notation in decimal literals
    }
    
    @Override
    protected String getDecimalType() {
        return "decimal(38,18)";
    }
    
    /**
     * @param arg the string to convert to a timestamp
     * @return a SQL string that will convert arg to a datetime
     */
    @Override
    protected String stringToDateTime(String arg) {
        return "DATE_PARSE(" + arg + ",'%Y-%m-%dT%H:%i:%sZ')";
    }
    
    /**
     * @param arg the string to convert to a date
     * @return a SQL string that will convert arg to a datetime
     */
    @Override
    protected String stringToDate(String arg) {
        return "DATE_PARSE(" + arg + ",'%d-%m-%Y')";
    }
    
	/**
     * Don't close the connection per statement, only at the end of the test.
     */
    @Override
    protected void closeConnectionPerStmt(Connection conn) throws SQLException {
        //conn.close();
    }
    
    @Override
    public String getSqlExceptionMessage(Throwable e) {
        // Strip off the UUID after Query failed.
        String message = e.getMessage();
        int queryFailedLoc = message.indexOf("Query failed (#");
        if (queryFailedLoc >= 0) {
           message = message.substring(message.indexOf(':', queryFailedLoc)+2);
        }
        return message;
    }

    @Override
    protected String formatDbTimeResult(ResultSet rset) throws SQLException {
        Time time = rset.getTime(1);
        if (time == null)
            return null;
        return FormulaEngine.getHooks().constructTime(time.getTime()).toString();
    }

    @Override
    protected String getBigDecimalLiteral(BigDecimal bd, DisplayField df) {
        // Use explicit DECIMALs because we can't bind correctly below.
        return "DECIMAL '" + bd.toPlainString() + "'";
    }
    
    /*
    @Override
    protected void bindBigDecimal(PreparedStatement pstmt, DisplayField df, BigDecimal bd, int position) throws SQLException {
        // The Trino driver uses BigDecimal.toString(), but doesn't accept exponent notation.  So we have to bump up the scale.
        super.bindBigDecimal(pstmt, df, bd, position);
    }
    */
}
