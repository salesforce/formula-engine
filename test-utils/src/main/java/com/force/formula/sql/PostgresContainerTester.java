/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Postgres tester that uses a container. 
 * @author stamm
 */
public class PostgresContainerTester extends DbContainerTester<PostgreSQLContainer<?>> {

	/**
	 * @throws IOException
	 */
	public PostgresContainerTester() throws IOException {
	}

	@Override
	protected PostgreSQLContainer<?> constructDb() throws IOException {
		return new PostgreSQLContainer<>("postgres");
	}
	
	@Override
	protected String getDecimalType() {
		return "numeric";
	}
	
	@Override
	protected String getTextType() {
		return "text";
	}
	
	@Override
	protected String getTimestampType() {
		return "timestamp";
	}
	
	@Override
	protected String stringToDateTime(String arg) {
		return arg + "::timestamp";
	}	
	
	@Override
	protected String convertToDateTime(String arg) {
		return arg + "::timestamp";
	}	
}
