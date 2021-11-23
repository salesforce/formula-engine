/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;

import org.testcontainers.containers.MSSQLServerContainer;

/**
 * MSSqlServer tester that uses a container. 
 * @author stamm
 */
public class MSSQLServerContainerTester extends DbContainerTester<MSSQLServerContainer<?>> {

	/**
	 * @throws IOException
	 */
	public MSSQLServerContainerTester() throws IOException {
	}

	@Override
	protected MSSQLServerContainer<?> constructDb() throws IOException {
		MSSQLServerContainer<?> result = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server");
		result.acceptLicense();		
		return result;
	}
	
	@Override
	public String getDbTypeName() {
		return "mssql";
	}

	@Override
	protected String getDecimalType() {
		return "decimal(38,18)";
	}
	
	// See https://docs.microsoft.com/en-us/sql/t-sql/functions/cast-and-convert-transact-sql?view=sql-server-ver15
	// for the magic numbers
	@Override
	protected String stringToDateTime(String arg) {
		return "CONVERT(DATETIME,"+arg+",127)";
	}

	@Override
	protected String stringToDate(String arg) {
		return "CONVERT(DATE," + arg + ",105)";
	}

	@Override
	protected boolean useBinds() {
		// Using bind with Mysql seems to be off.
		return false;
	}
	
}
