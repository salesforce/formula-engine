/**
 * 
 */
package com.force.formula.sql.google;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.testcontainers.containers.GenericContainer;

import com.force.formula.FormulaEngine;
import com.force.formula.sql.DockerContainerTester;

/**
 * Spanner tester that uses a google container with the latest emulator 
 * @author stamm
 * @since 0.3
 */
public abstract class GoogleContainerTester<C extends GenericContainer<?>> extends DockerContainerTester<C> {

	/**
	 * @throws IOException
	 */
	public GoogleContainerTester() throws IOException {
	}

	@Override
	public String getDbTypeName() {
		return "spanner";
	}
	
	@Override
    protected boolean useBinds() {
        return true;  // Use bind, otherwise \q fails, but you can only specify 9 digits of precision... Oh well.
    }
	
    /**
     * @return the type to use as as text type when testing (text in postgres, varchar in oracle)
     */
    @Override
    protected String getTextType() {
        return "string";
    }
    
    @Override
    protected String getDecimalType() {
        return "numeric";
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
        return "PARSE_DATE('%d-%m-%Y'," + arg + ")";
    }
    
    @Override
    protected String formatDbTimeResult(ResultSet rset) throws SQLException {
        BigDecimal millis = rset.getBigDecimal(1);
        if (millis == null)
            return null;
        return FormulaEngine.getHooks().constructTime(millis).toString();
    }
    
}
