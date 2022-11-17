/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

import com.force.formula.DisplayField;
import com.force.formula.FormulaEngine;
import com.force.formula.MockFormulaDataType;

/**
 * a DbTester of formulas that uses embedded h2 for validation.  
 * 
 * @author stamm
 * @since 0.3.6
 */
public class EmbeddedH2Tester extends AbstractDbTester {

	public EmbeddedH2Tester() throws IOException {
	}

	@Override
	public String getDbTypeName() {
		return "h2";
	}

	@Override
    protected boolean useBinds() {
	    // Naked binds in H2 need to be cast, and they are quite slow, and they don't seem to bind BigDecimals right.
        return true;
    }
	
	@Override
	protected Connection getConnection() throws SQLException, IOException {
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:db1");
        return conn;
	}
	
	@Override
	protected String getDecimalType() {
		return "numeric(38,18)";
	}
	
	@Override
	protected String getTimestampType() {
		return "timestamp";
	}
	
	@Override
    protected String stringToDateTime(String arg) {
        return "PARSEDATETIME("+arg+", 'yyyy-MM-dd''T''HH:mm:ss''Z''', 'en', 'UTC')";
    }

    @Override
    protected String stringToDate(String arg) {
        return "PARSEDATETIME(" + arg + ",'dd-MM-yyyy', 'en', 'UTC')";
    }
    
    @Override
    protected String formatDbTimeResult(ResultSet rset) throws SQLException {
        Time time = rset.getTime(1);
        if (time == null)
            return null;
        return FormulaEngine.getHooks().constructTime(time.getTime()).toString();
    }

    // See https://github.com/h2database/h2database/issues/1383
    @Override
    protected String getParamPlaceholder(DisplayField df, Object value) {
        // Naked binds in H2 need to be cast.
        switch ((MockFormulaDataType) df.getFormulaFieldInfo().getDataType()) {
        case DATETIME:
            return "CAST (? AS "+getTimestampType()+")";
        case DATEONLY:
            return "CAST (? AS date)";
        case CURRENCY:
        case INTEGER:
        case PERCENT:
        case DOUBLE:
            return "CAST (? AS "+getDecimalType()+")";
        case TEXT:
        default:
            return "CAST (? AS "+getTextType()+")";          
        }
    }
}
