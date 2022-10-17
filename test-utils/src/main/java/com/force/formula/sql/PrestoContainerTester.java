/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.testcontainers.containers.PrestoContainer;

/**
 * Presto tester that uses a container.  
 * @author stamm
 */
@SuppressWarnings("deprecation") // Presto is replaced with Trino, but Athena is based on Presto for now.
public class PrestoContainerTester extends DbContainerTester<PrestoContainer<?>> {

	/**
	 * @throws IOException
	 */
	public PrestoContainerTester() throws IOException {
	}

	@Override
	public String getDbTypeName() {
		return "presto";
	}

	@Override
	protected PrestoContainer<?> constructDb() throws IOException {
		return new PrestoContainer<>("ghcr.io/trinodb/presto");
	}

	@Override
    protected synchronized Connection getConnection() throws SQLException, IOException {
	    Connection conn = super.getConnection();
	    /*
	    try (PreparedStatement stmt = conn.prepareStatement("SELECT ?")) {
	        stmt.setInt(1, 1);
	        try (ResultSet rs = stmt.executeQuery()) {
	            rs.next();
	            int i = rs.getInt(1);
	            System.out.println(i);
	        }
	    }
	    */
	    return conn;
    }
	
	   
    @Override
    protected boolean useBinds() {
        return false;
    }
    
    @Override
    protected String getDecimalType() {
        return "decimal(38,18)";
    }
	
	/**
     * Don't close the connection per statement, only at the end of the test.
     */
    @Override
    protected void closeConnectionPerStmt(Connection conn) throws SQLException {
        //conn.close();
    }

}
