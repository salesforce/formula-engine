/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;

import org.testcontainers.containers.MySQLContainer;

import com.force.formula.DisplayField;

/**
 * Mysql tester that uses a container. 
 * @author stamm
 */
public class MysqlContainerTester extends DbContainerTester<MySQLContainer<?>> {

	/**
	 * @throws IOException
	 */
	public MysqlContainerTester() throws IOException {
	}

	@Override
	protected MySQLContainer<?> constructDb() throws IOException {
		// Mysql, like 
		MySQLContainer<?> result = new MySQLContainer<>("mysql");
		result.withCommand("--collation-server=utf8mb4_bin",
						   "--character-set-server=utf8mb4");  // Formula engine assumes case sensitive.
		return result;
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
	protected String getNullSqlValue(DisplayField df) {
		return "NULL";
	}

	/**
	 * @return the type to use as as text type when testing (text in postgres, varchar in oracle)
	 */
	protected String getTextType() {
		return "varchar";
	}
	
	/**
	 * @param arg the string to convert to a timestamp
	 * @return a SQL string that will convert arg to a datetime
	 */
	protected String stringToDateTime(String arg) {
		return "TIMESTAMP("+arg+")";
	}
	
	/**
	 * @param arg the string to convert to a date
	 * @return a SQL string that will convert arg to a datetime
	 */
	protected String stringToDate(String arg) {
		return "STR_TO_DATE(" + arg + ",'%D-%m-%Y')";
	}
}
