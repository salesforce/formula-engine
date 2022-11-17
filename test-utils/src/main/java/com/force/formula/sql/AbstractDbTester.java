/**
 * 
 */
package com.force.formula.sql;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import com.force.formula.DisplayField;
import com.force.formula.Formula;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaException;
import com.force.formula.FormulaFieldReferenceInfo;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.FormulaSchema.Entity;
import com.force.formula.FormulaTime;
import com.force.formula.FormulaTypeSpec;
import com.force.formula.MockFormulaDataType;
import com.force.formula.MockFormulaType;
import com.force.formula.RuntimeFormulaInfo;
import com.force.formula.impl.BaseFormulaGenericTests.DbTester;
import com.force.formula.impl.MapFormulaContext;
import com.force.formula.util.FormulaDateUtil;
import com.force.formula.util.FormulaI18nUtils;
import com.force.formula.util.FormulaTextUtil;

/**
 * A default implementation of DbTester of formulas that simplifies some stuff around
 * creating the test case SQL statements.
 * 
 * Instead of writing rows to the database, it puts all of the "passed in
 * values" in a subquery with alias "c", casting the null values to the right types.
 * 
 * @author stamm
 * @since 0.2
 */
public abstract class AbstractDbTester implements DbTester {

	public AbstractDbTester() throws IOException {
	}
	
	/**
	 * @return the connection to use to call into the sql engine
	 * @throws SQLException
	 * @throws IOException
	 */
	protected abstract Connection getConnection() throws SQLException, IOException;
	
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
	 * @param arg the string to convert to a date
	 * @return a SQL string that will convert arg to a datetime
	 */
	protected String stringToDate(String arg) {
		return "TO_DATE(" + arg + ",'DD-MM-YYYY')";
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
			return stringToDate("'" + FormulaDateUtil.formatDateToSql(((java.util.Date) value)) + "'");
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
			return getBigDecimalLiteral(bd, df);
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
	 * @param context the formulaContext
	 * @param columnSql the SQL returned from the formula for the value.
	 * @param formula the formula whose return type to check
	 */
	protected String fixSqlFormat(FormulaContext context, String columnSql, Formula formula) {
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
			break;
		case TIMEONLY:
			return getTimeFormat(columnSql);
		default:
		}		
		return columnSql;
	}

	/**
	 * @param columnSql the expression that returns a time value 
	 * @return the value of the time to return
	 */
	protected String getTimeFormat(String columnSql) {
		return columnSql;
	}
	
	/**
	 * Get the bind value to use.  Overridable if types need to be coerced
	 * @param df the field being bound
	 * @param value the value of the field
	 * @return the parameter, which must contain just one questionmark
	 */
	protected String getParamPlaceholder(DisplayField df, Object value) {
		return "?";
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
		sub.append(" FROM (SELECT 1 as fake");
		if (values != null) {
			for (DisplayField df : formulaContext.getDisplayFields(mfc.getEntity())) {
				Object value = values.get(df.getFormulaFieldInfo().getName());
				String sqlValue;

				if (useBinds()) {
					sqlValue = getParamPlaceholder(df, value);
				} else {
					if (value == null) {
						sqlValue = getNullSqlValue(df);
					} else {
						sqlValue = getSqlLiteralValue(df, value);
					}
				}
				sub.append(", " + sqlValue).append(" as ").append(df.getFormulaFieldInfo().getDbColumn(null, null));
			}
		}
		sub.append(getFromDual());
		sub.append(") c");
		return sub;
	}
	
	
	/**
	 * @return true if we should use binding variables instead of literals. 
	 */
	protected boolean useBinds() {
		return false;
	}
	
	/**
	 * Set the bind variables for the prepared statement for the tgiven values.  Needs to match makeRowValueSubquery
	 * binding
	 * @param pstmt the prepared statement to bind
	 * @param formulaContext the MapFormulaContext used to lookup the value
	 * @param values the Map of all the values
	 * @throws SQLException if something happens whilst binding
	 */
	protected void setVariables(PreparedStatement pstmt, FormulaRuntimeContext formulaContext, Map<String, Object> values) throws SQLException {
		if (useBinds()) {
			MapFormulaContext mfc = (MapFormulaContext) formulaContext;
			if (values != null) {
				int i = 1;
				for (DisplayField df : formulaContext.getDisplayFields(mfc.getEntity())) {
					Object value = values.get(df.getFormulaFieldInfo().getName());
					int sqlType = getSqlType((MockFormulaDataType)df.getFormulaFieldInfo().getDataType());
					if (value == null) {
						pstmt.setNull(i, sqlType);
					} else {
						bindSqlValue(pstmt, df, value, i);
					}
					i++;
				}
			}
		}
	}
	
	/**
	 * Bind the sql value for the display field.
	 * @param pstmt the prepared statement to bind
	 * @param df the display field
	 * @param value the value from the MapEntityObject
	 * @param position the numeric position to bind
	 */
	protected void bindSqlValue(PreparedStatement pstmt, DisplayField df, Object value, int position) throws SQLException {
		switch ((MockFormulaDataType) df.getFormulaFieldInfo().getDataType()) {
		case DATETIME:
			pstmt.setTimestamp(position, new Timestamp(((java.util.Date) value).getTime()));
			break;
		case DATEONLY:
			pstmt.setDate(position, new Date(((java.util.Date) value).getTime()));
			break;
		case TIMEONLY:
			pstmt.setTime(position, new Time(((FormulaTime)value).getTimeInMillis()));
			break;
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
			bindBigDecimal(pstmt, df, bd, position);
			break;
		case BOOLEAN:
			pstmt.setString(position, Boolean.TRUE.equals(value) ? "1" : "0");
			break;
		default:
			pstmt.setString(position, String.valueOf(value));
		}
	}

	/**
     * @return the literal to use in the statement for a big decimal.
     * Sometimes needs to override the scale/precision, or handle bugs
     * @param df the field being bound
     * @param bd the BigDecimal with its scale set
     */
    protected String getBigDecimalLiteral(BigDecimal bd, DisplayField df) {
        return bd.toPlainString();
    }
	
	/**
	 * Binding a big decimal sometimes needs to override the scale/precision, or handle bugs
	 * @param pstmt the statement to bind
	 * @param df the field being bound
	 * @param bd the BigDecimal with its scale set
	 * @param position the position in the statement.
	 */
	protected void bindBigDecimal(PreparedStatement pstmt, DisplayField df, BigDecimal bd, int position) throws SQLException {
        pstmt.setBigDecimal(position, bd);
	}
	
	/**
	 * @return the string that will be "from dual" in oracle in the subselect
	 */
    protected String getFromDual()  {
    	return "";
    }
    
    protected int getSqlType(MockFormulaDataType dataType) {
    	switch (dataType) {
    	case DATEONLY:
    		return Types.DATE;
    	case DATETIME:
    		return Types.TIMESTAMP;
    	case TIMEONLY:
    		return Types.TIME;
    	case CURRENCY:
    	case DOUBLE:
    	case INTEGER:
    	case PERCENT:
    		return Types.DECIMAL;
    	default:
    	}
    	return Types.VARCHAR;
    }
    
	/**
	 * Format the result from the database, which will be in column 1 of the result set, as a string suitable
	 * for comparing with Java and Javascript evaluation
	 * @param rset the result set already nexted to the first row, with the result in column 1
	 * @param formulaContext the formula contex used to evaluate
	 * @param formula the formula that generated the sql (with the result passed through {@link #fixSqlFormat(FormulaContext, String, Formula)}
	 * @return a String formatted, or null if the object is null
	 * @throws SQLException if there was a SQL exception
	 */
	protected String formatDbResult(ResultSet rset, FormulaRuntimeContext formulaContext, Formula formula) throws SQLException {
		MockFormulaDataType returnType = (MockFormulaDataType) formula.getDataType();
		switch (returnType) {
		case DATEONLY:
		case DATETIME:
		    try {
    			Timestamp d = rset.getTimestamp(1);
    			if (d == null)
    				return null;
    			return d.toString();
		    } catch (IllegalArgumentException | SQLException ex) {
		        // If it isn't a timestamp, see if it works as a date...
		        Date d = rset.getDate(1);
                if (d == null)
                    return null;
                return new Timestamp(d.getTime()).toString();
		    }
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
			return number.stripTrailingZeros().toPlainString(); // Strip trailing zeros because that's driver specific.
		case TIMEONLY:
			return formatDbTimeResult(rset);
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
	 * 
	 * @param rset the value to retrieve from column 1
	 * @return the value of the time formatted
	 * @throws SQLException
	 */
	protected String formatDbTimeResult(ResultSet rset) throws SQLException {
		BigDecimal millis = rset.getBigDecimal(1);
		if (millis == null)
			return null;
		return FormulaEngine.getHooks().constructTime(millis).toString();
	}
	
	/**
	 * Evaluate the formula using the embedded engine.
	 */
	@Override
	public String evaluateSql(String testName, FormulaRuntimeContext formulaContext, Object entityObject, String formulaSource,
			boolean nullAsNull) throws SQLException, FormulaException, IOException {
		@SuppressWarnings("unchecked")
		Map<String, Object> values = (Map<String, Object>) entityObject;

		CharSequence subQuery = makeRowValueSubquery(formulaContext, values);

		FormulaTableRegistry registry = new MockFormulaTableRegistry();
		FormulaTypeSpec type = nullAsNull ? MockFormulaType.NULLASNULL : MockFormulaType.DEFAULT;
		RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(type, formulaContext, formulaSource);
		FormulaWithSql formula = (FormulaWithSql) formulaInfo.getFormula();
		String column = formula.toSQL(registry);
		column = fixSqlFormat(formulaContext, column, formula);

		String sql = "SELECT " + column + subQuery.toString();
		try {
			Connection conn = getConnection();
			try {
				//System.out.println(testName + ": " + sql);  // For debugging
				try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
					setVariables(pstmt, formulaContext, values);
					try (ResultSet rset = pstmt.executeQuery()) {
						if (rset.next()) {
							return formatDbResult(rset, formulaContext, formula);
						}
					}
				}
			} finally {
				closeConnectionPerStmt(conn);
			}
		} catch (SQLException e) {
			// System.out.println(testName + ": " + sql);  // For debugging
			throw e; // Useful for a breakpoint
		}
		return null;
	}

	/**
	 * Close the connection after performing one statement.  Used in embedded, but not
	 * anywhere else
	 * @param conn the connection to close
	 * @throws SQLException
	 */
	protected void closeConnectionPerStmt(Connection conn) throws SQLException {
		conn.close();
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