/**
 * 
 */
package com.force.formula.sql.mysql;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

import org.testcontainers.containers.MariaDBContainer;

import com.force.formula.FormulaEngine;

/**
 * Mysql tester that uses a container. 
 * @author stamm
 */
public class MariaDBContainerTester extends MySQLStyleContainerTester<MariaDBContainer<?>> {

	/**
	 * @throws IOException
	 */
	public MariaDBContainerTester() throws IOException {
	}

	@Override
	protected MariaDBContainer<?> constructDb() throws IOException {
		// Mysql, like 
		MariaDBContainer<?> result = new MariaDBContainer<>("mariadb:10.8");
		result.withCommand("--collation-server=utf8mb4_bin",
						   "--character-set-server=utf8mb4");  // Formula engine assumes case sensitive.
		return result;
	}
	
	@Override
	public String getDbTypeName() {
		return "mariadb";
	}
	
	// MariaDB driver uses time object.
    @Override
    protected String formatDbTimeResult(ResultSet rset) throws SQLException {
        Time time = rset.getTime(1);
        if (time == null)
            return null;
        return FormulaEngine.getHooks().constructTime(time.getTime()).toString();
    }
}
