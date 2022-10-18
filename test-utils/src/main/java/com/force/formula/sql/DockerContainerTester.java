/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.testcontainers.containers.GenericContainer;

/**
 * an abstract class for testing DBs using the org.testcontainer container
 * framework.  For speed and simplicity, it reuses the connection
 * throughout the entire use of the tester, so it doesn't create multiple
 * underlying databases.
 * 
 * @author stamm
 */
public abstract class DockerContainerTester<C extends GenericContainer<?>> extends AbstractDbTester {

	private C theDb = null;
	private Connection conn = null;

	public DockerContainerTester() throws IOException {
	}

	/**
	 * @return the DB to construct
	 * @throws IOException if an exception occurred while constructing the db
	 */
	protected abstract C constructDb() throws IOException;
	
	final synchronized C getDb() throws IOException {
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
	protected Connection getConnection() throws SQLException, IOException {
		if (this.conn == null) {
			C db = getDb();
			this.conn = constructConnection(db);
		}
		return this.conn;
	}
	
	/**
	 * Construct a connection.
	 * @param db the GenericContainer with the DB
	 * @return a connection that connects to the generic container
	 */
	protected abstract Connection constructConnection(C db)  throws SQLException, IOException;

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
