/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * an abstract class for testing DBs using the org.testcontainer db
 * framework.  For speed and simplicity, it reuses the connection
 * throughout the entire use of the tester, so it doesn't create multiple
 * underlying databases.
 * 
 * @author stamm
 * @param <DB> the DB container
 */
public abstract class DbContainerTester<DB extends JdbcDatabaseContainer<?>> extends DockerContainerTester<DB> {

	public DbContainerTester() throws IOException {
	}


	
	@Override
    protected Connection constructConnection(DB db) throws SQLException, IOException {
	    return DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword());
	}

}
