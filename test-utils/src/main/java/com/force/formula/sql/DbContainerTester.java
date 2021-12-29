/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.sql.*;

import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * an abstract class for testing DBs using the org.testcontainer db
 * framework.  For speed and simplicity, it reuses the connection
 * throughout the entire use of the tester, so it doesn't create multiple
 * underlying databases.
 * 
 * @author stamm
 */
public abstract class DbContainerTester<DB extends JdbcDatabaseContainer<?>> extends AbstractDbTester {

	private DB theDb = null;
	private Connection conn = null;

	public DbContainerTester() throws IOException {
	}

	/**
	 * @return the DB to construct
	 * @throws IOException if an exception occurred while constructing the db
	 */
	protected abstract DB constructDb() throws IOException;
	
	final synchronized DB getDb() throws IOException {
		if (theDb == null) {
			theDb = constructDb();
			theDb.start();
			// Add a thread to kill the docker container.
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					if (conn != null) {
						conn.close();
					}
				} catch (SQLException ex) {
					conn = null;
					// Ignore this issue.
				}
				theDb.stop();}));
		}
			
		return this.theDb;
	}
	
	@Override
	protected synchronized Connection getConnection() throws SQLException, IOException {
		if (this.conn == null) {
			DB db = getDb();
			this.conn = DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword());
		}
		return this.conn;
	}

	/**
	 * Don't close the connection per statement, only at the end of the test.
	 */
	@Override
	protected void closeConnectionPerStmt(Connection conn) throws SQLException {
		// Do nothing.
	}

	
	@Override
	public void close() throws Exception {
		if (this.conn != null) {
			this.conn.close();
			this.conn = null;
		}
	}


}
