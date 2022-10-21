/**
 * 
 */
package com.force.formula.sql.mysql;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.testcontainers.containers.MySQLContainer;

import com.force.formula.FormulaEngine;

/**
 * Mysql tester that uses a container. 
 * @author stamm
 */
public class MySQLContainerTester extends MySQLStyleContainerTester<MySQLContainer<?>> {

	/**
	 * @throws IOException
	 */
	public MySQLContainerTester() throws IOException {
	}

	@Override
	protected MySQLContainer<?> constructDb() throws IOException {
		// Mysql, like 
		MySQLContainer<?> result = new MySQLContainer<>("mysql");

		// @see https://github.com/testcontainers/testcontainers-java/issues/914
		result.withConfigurationOverride(null); // If /tmp isn't visible to docker, this hangs
		result.withCommand("--collation-server=utf8mb4_bin",
						   "--character-set-server=utf8mb4");  // Formula engine assumes case sensitive.
		return result;
	}
	
	@Override
	public String getDbTypeName() {
		return "mysql";
	}
	
    @Override
    protected String getTimeFormat(String columnSql) {
        return "UNIX_TIMESTAMP("+columnSql+")%86400";
    }
    
    @Override
    protected String formatDbTimeResult(ResultSet rset) throws SQLException {
        BigDecimal millis = rset.getBigDecimal(1);
        if (millis == null)
            return null;
        millis = millis.movePointRight(3);  // It's micro in mysql.
        return FormulaEngine.getHooks().constructTime(millis).toString();
    }
	
}
