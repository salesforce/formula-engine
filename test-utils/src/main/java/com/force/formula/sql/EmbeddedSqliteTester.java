/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
        return true;
    }
	
	@Override
	protected Connection getConnection() throws SQLException, IOException {
        return DriverManager.getConnection("jdbc:sqlite:target/sample.db");
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
        return "CAST("+arg+" AS TIMESTAMP)";
    }
    
    /**
     * @param arg the string to convert to a date
     * @return a SQL string that will convert arg to a datetime
     */
    @Override
    protected String stringToDate(String arg) {
        return "CAST("+arg+" AS DATE)";
    }

	@Override
	public void close() throws Exception {
		// It autocloses.
	}

}
