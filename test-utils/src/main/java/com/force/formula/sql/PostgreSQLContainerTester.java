/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * PostgreSQL tester that uses a container. 
 * @author stamm
 */
public class PostgreSQLContainerTester extends DbContainerTester<PostgreSQLContainer<?>> {

	/**
	 * @throws IOException
	 */
	public PostgreSQLContainerTester() throws IOException {
	}

	@Override
	public String getDbTypeName() {
		return "postgres";
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
