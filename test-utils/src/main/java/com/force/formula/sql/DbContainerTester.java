/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.List;
import java.util.Map;

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.force.formula.*;
import com.force.formula.FormulaSchema.Entity;
import com.force.formula.impl.BaseFormulaGenericTests.DbTester;
import com.force.formula.impl.MapFormulaContext;
import com.force.formula.util.*;

/**
 * an abstract class for testing DBs using the org.testcontainer db
 * framework
 * 
 * @author stamm
 */
public abstract class DbContainerTester<DB extends JdbcDatabaseContainer<?>> extends DbTester {

	private DB pg = null;
	private Connection conn = null;

	public DbContainerTester() throws IOException {
	}

	/**
	 * @return the DB to construct
	 * @throws IOException if an exception occurred while constructing the db
	 */
	protected abstract DB constructDb() throws IOException;
	
	final synchronized DB getDb() throws IOException {
		if (pg == null) {
			pg = constructDb();
			pg.start();
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
				pg.stop();}));
		}
			
		return this.pg;
	}
	

	
	
	protected synchronized Connection getConnection() throws SQLException, IOException {
		if (this.conn == null) {
			DB db = getDb();
			this.conn = DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword());
		}
		return this.conn;
	}

	
	/**
	 * @return the value to use as a null value for the display field.
	 * @param df the display field
	 */
	protected String getNullSqlValue(DisplayField df) {
		switch ((MockFormulaDataType) df.getFormulaFieldInfo().getDataType()) {
		case DATETIME:
			return "CAST (NULL AS "+getTimestampType()+")";
		case DATEONLY:
			return "CAST (NULL AS date)";
		case CURRENCY:
		case INTEGER:
		case PERCENT:
		case DOUBLE:
			return "CAST (NULL AS "+getDecimalType()+")";
		case TEXT:
			return "CAST (NULL AS "+getTextType()+")";			
		default:
			return "NULL";
		}
	}
	
	/**
	 * @return the type to use as a decimal type when testing (numeric in postgres, number in oracle)
	 */
	protected String getDecimalType() {
		return "number";
	}
	
	/**
	 * @return the type to use as as text type when testing (text in postgres, varchar in oracle)
	 */
	protected String getTextType() {
		return "varchar";
	}
	
	
	/**
	 * @return the type to use as as datetime type when testing (text in postgres, varchar in oracle)
	 */
	protected String getTimestampType() {
		return "date";
	}

	/**
	 * @param arg the string to convert to a timestamp
	 * @return a SQL string that will convert arg to a datetime
	 */
	protected String stringToDateTime(String arg) {
		return "TO_DATE("+arg+", 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')";
	}
	

	/**
	 * @param arg the selected column that may or may not be a timestsamp
	 * @return a SQL string that will convert arg to a datetime
	 */
	protected String convertToDateTime(String arg) {
		return arg;
	}

	/**
	 * @return the value to use as a literal sql value for the display field.
	 * @param df the display field
	 * @param value the value from the MapEntityObject
	 */
	protected String getSqlLiteralValue(DisplayField df, Object value) {
		switch ((MockFormulaDataType) df.getFormulaFieldInfo().getDataType()) {
		case DATETIME:
			return stringToDateTime("'" + FormulaDateUtil.formatDatetimeToISO8601((java.util.Date) value) + "'");
		case DATEONLY:
			return "TO_DATE('" + FormulaDateUtil.formatDateToSql(((java.util.Date) value))
					+ "','DD-MM-YYYY')";
		case PERCENT:
		case CURRENCY:
		case DOUBLE:
			// Make sure we use the right scale for numbers, as postgres will treat them as
			// integers
			BigDecimal bd = new BigDecimal(String.valueOf(value));
			int newScale = df.getFormulaFieldInfo().getScale();
			if (newScale > bd.scale()) {
				bd = bd.setScale(newScale, RoundingMode.HALF_DOWN);
			}
			return bd.toPlainString();
		case TEXT:
			// Replace singlequote
			return "'" + FormulaTextUtil.replaceSimple(String.valueOf(value), "'", "''") + "'";
		case BOOLEAN:
			return Boolean.TRUE.equals(value) ? "'1'" : "'0'";
		default:
			return String.valueOf(value);
		}
	}
	
	/**
	 * @return the sql formatted correctly based on the data type.  Mostly for making sure
	 * percent is divided by 100 and the datetime is the right timestamp
	 * @param columnSql the SQL returned from the formula for the value.
	 * @param formula the formula whose return type to check
	 */
	protected String fixSqlFormat(String columnSql, Formula formula) {
		MockFormulaDataType returnType = (MockFormulaDataType) formula.getDataType();
		switch (returnType) {
		case DATETIME:
			return convertToDateTime("(" + columnSql + ")");
		case PERCENT:
			return "(" + columnSql + ")/100"; // The format below moves the decimal point over
		case BOOLEAN:
			if (!columnSql.endsWith(" THEN '1' ELSE '0' END")) {  // TODO: This shouldn't work.
				return "CASE WHEN " + columnSql + " THEN '1' ELSE '0' END";
			}
		default:
		}		
		return columnSql;
	}
	
	/**
	 * Make a FROM clause with a subquery with alias "c" that contains all the
	 * values as columns easily substituted in the formula below
	 * 
	 * @param formulaContext the formula context to use to retreive the display
	 *                       fields
	 * @param values         the values to mock
	 * @return a subquery to evaluate field references
	 */
	protected CharSequence makeRowValueSubquery(FormulaRuntimeContext formulaContext, Map<String, Object> values) {
		MapFormulaContext mfc = (MapFormulaContext) formulaContext;

		// Construct subquery
		StringBuilder sub = new StringBuilder();
		sub.append(" FROM (SELECT 1");
		if (values != null) {
			for (DisplayField df : formulaContext.getDisplayFields(mfc.getEntity())) {
				Object value = values.get(df.getFormulaFieldInfo().getName());
				String sqlValue;

				if (value == null) {
					sqlValue = getNullSqlValue(df);
				} else {
					sqlValue = getSqlLiteralValue(df, value);
				}
				sub.append(", " + sqlValue).append(" as ").append(df.getFormulaFieldInfo().getDbColumn(null, null));
			}
		}
		sub.append(getFromDual());
		sub.append(") c");
		return sub;
	}

	/**
	 * @return the string that will be "from dual" in oracle in the subselect
	 */
    protected String getFromDual()  {
    	return "";
    }
    
	
	/**
	 * Format the result from the database, which will be in column 1 of the result set, as a string suitable
	 * for comparing with Java and Javascript evaluation
	 * @param rset the result set already nexted to the first row, with the result in column 1
	 * @param formulaContext the formula contex used to evaluate
	 * @param formula the formula that generated the sql (with the result passed through {@link #fixSqlFormat(String, Formula)}
	 * @return a String formatted, or null if the object is null
	 * @throws SQLException if there was a SQL exception
	 */
	protected String formatDbResult(ResultSet rset, FormulaRuntimeContext formulaContext, Formula formula) throws SQLException {
		MockFormulaDataType returnType = (MockFormulaDataType) formula.getDataType();
		switch (returnType) {
		case DATEONLY:
		case DATETIME:
			Timestamp d = rset.getTimestamp(1);
			if (d == null)
				return null;
			return d.toString();
		case CURRENCY:
		case PERCENT:
			BigDecimal bigDecimal = rset.getBigDecimal(1);
			if (bigDecimal == null)
				return null;
			// Currency and Percent have special formatting.
			return String.valueOf(FormulaI18nUtils.formatResult(formulaContext, formulaContext.getFormulaReturnType(), bigDecimal));
		case INTEGER:
		case DOUBLE:
			BigDecimal number = rset.getBigDecimal(1);
			if (number == null)
				return null;
			return number.toPlainString();
		case TIMEONLY:
			BigDecimal millis = rset.getBigDecimal(1);
			if (millis == null)
				return null;
			return FormulaEngine.getHooks().constructTime(millis).toString();
		case BOOLEAN:
			String result = rset.getString(1);
			return "1".equals(result) ? "true" : "false";
		default:
		}	
		String result = rset.getString(1);
		if (result == null || result.length() == 0) {
			return null;
		}
		return result;
	}
	
	/**
	 * Evaluate the formula using the embedded postgres engine.
	 */
	@Override
	public String evaluateSql(FormulaRuntimeContext formulaContext, Object entityObject, String formulaSource,
			boolean nullAsNull) throws SQLException, FormulaException, IOException {
		@SuppressWarnings("unchecked")
		Map<String, Object> values = (Map<String, Object>) entityObject;

		CharSequence subQuery = makeRowValueSubquery(formulaContext, values);

		FormulaTableRegistry registry = new MockFormulaTableRegistry();
		DB epg = getDb();
		if (epg != null) {
			FormulaTypeSpec type = nullAsNull ? MockFormulaType.NULLASNULL : MockFormulaType.DEFAULT;
			RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(type, formulaContext, formulaSource);
			FormulaWithSql formula = (FormulaWithSql) formulaInfo.getFormula();
			String column = formula.toSQL(registry);
			column = fixSqlFormat(column, formula);

			try {
				//System.out.println("SELECT " + column + subQuery.toString());  // For debugging

				Connection conn = getConnection();
				try (PreparedStatement pstmt = conn.prepareStatement("SELECT " + column + subQuery.toString())) {
					try (ResultSet rset = pstmt.executeQuery()) {
						if (rset.next()) {
							return formatDbResult(rset, formulaContext, formula);
						}
					}
				}
			} catch (SQLException e) {
				//System.out.println("SELECT " + column + subQuery.toString());  // For debugging
				throw e; // Useful for a breakpoint
			}
		}
		return null;
	}

	@Override
	public void close() throws Exception {
		if (this.conn != null) {
			this.conn.close();
			this.conn = null;
		}
	}

	/**
	 * A mock formula table registry where everything is on table alias "c".
	 * 
	 * @author stamm
	 */
	static class MockFormulaTableRegistry implements FormulaTableRegistry {

		@Override
		public TableIdentifier getLogicalSqlTable(List<? extends FormulaFieldReferenceInfo> fieldPath,
				boolean isCustomTable, boolean isCurrency) {
			return new TableIdentifier() {
				@Override
				public Entity getEntityInfo() {
					return null;
				}

				@Override
				public String getAlias() {
					return "c";
				}
			};
		}

		@Override
		public TableSet getTableAliases(List<? extends FormulaFieldReferenceInfo> fieldPath,
				TableSet currentTableAliases) {
			return new TableSet("c", "c", "c");
		}
	}

}
