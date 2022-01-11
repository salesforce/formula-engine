/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.testcontainers.containers.MySQLContainer;

import com.force.formula.DisplayField;
import com.force.formula.FormulaEngine;

/**
 * Mysql tester that uses a container. 
 * @author stamm
 */
public class MySQLContainerTester extends DbContainerTester<MySQLContainer<?>> {

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
	protected boolean useBinds() {
		// The sql driver doesn't like control characters in literals it seems.
		return true;
	}

	/**
	 * @return the value to use as a null value for the display field.
	 * @param df the display field
	 */
	@Override
	protected String getNullSqlValue(DisplayField df) {
		return "NULL";
	}

	/**
	 * @return the type to use as as text type when testing (text in postgres, varchar in oracle)
	 */
	@Override
	protected String getTextType() {
		return "varchar";
	}
	
	/**
	 * @param arg the string to convert to a timestamp
	 * @return a SQL string that will convert arg to a datetime
	 */
	@Override
	protected String stringToDateTime(String arg) {
		return "TIMESTAMP("+arg+")";
	}
	
	/**
	 * @param arg the string to convert to a date
	 * @return a SQL string that will convert arg to a datetime
	 */
	@Override
	protected String stringToDate(String arg) {
		return "STR_TO_DATE(" + arg + ",'%D-%m-%Y')";
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
