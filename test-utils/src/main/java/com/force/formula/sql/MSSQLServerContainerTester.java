/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

import org.testcontainers.containers.MSSQLServerContainer;

import com.force.formula.DisplayField;
import com.force.formula.FormulaEngine;
import com.force.formula.MockFormulaDataType;
import com.force.formula.util.FormulaTextUtil;
import com.google.common.base.CharMatcher;

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
		MSSQLServerContainer<?> result = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2017-latest");
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
	protected String getSqlLiteralValue(DisplayField df, Object value) {
		if (df.getFormulaFieldInfo().getDataType() == MockFormulaDataType.TEXT) {
			String strValue = String.valueOf(value);
			if (CharMatcher.ascii().matchesAllOf(strValue)) {
				return super.getSqlLiteralValue(df, strValue);
			}
			// With any non-ascii character, use NVARCHAR literals when doing binds
			return "N'" + FormulaTextUtil.replaceSimple(String.valueOf(value), "'", "''") + "'";
		}
		return super.getSqlLiteralValue(df, value);
	}

	@Override
	protected boolean useBinds() {
		// MSSQL completely misunderstands decimal scale when passed in and that makes the date math wrong 
		return false;
	}
	
	@Override
	protected String formatDbTimeResult(ResultSet rset) throws SQLException {
		Time time = rset.getTime(1);
		if (time == null)
			return null;
		return FormulaEngine.getHooks().constructTime(time.getTime()).toString();
	}
}
