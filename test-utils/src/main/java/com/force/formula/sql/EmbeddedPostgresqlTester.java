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

import com.force.formula.*;
import com.force.formula.FormulaSchema.Entity;
import com.force.formula.impl.BaseFormulaGenericTests.DbTester;
import com.force.formula.impl.MapFormulaContext;
import com.force.formula.util.*;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * a DbTester of formulas that uses embedded postgres for validation.  Use this
 * if you don't want to deal with docker.
 * 
 * Instead of writing rows to the database, it puts all of the "passed in
 * values" in a subquery with alias "c", casting the null values to the right types.
 * 
 * @author stamm
 */
public class EmbeddedPostgresqlTester extends DbTester {

	private EmbeddedPostgres pg = null;

	public EmbeddedPostgresqlTester() throws IOException {
	}

	/**
	 * @return an EmbeddedPostgres to use
	 * @throws IOException if an exception occurred while constructing the postgres db
	 */
	protected EmbeddedPostgres constructPostgres() throws IOException {
		return EmbeddedPostgres.builder().start();
	}
	
	final EmbeddedPostgres getPostgres() throws IOException {
		if (pg == null) {
			pg = constructPostgres();
		}
		return this.pg;
	}
	
	/**
	 * @return the value to use as a null value for the display field.
	 * @param df the display field
	 */
	protected String getNullSqlValue(DisplayField df) {
		switch ((MockFormulaDataType) df.getFormulaFieldInfo().getDataType()) {
		case DATETIME:
			return "CAST (NULL AS timestamp)";
		case DATEONLY:
			return "CAST (NULL AS date)";
		case CURRENCY:
		case INTEGER:
		case PERCENT:
		case DOUBLE:
			return "CAST (NULL AS numeric)";
		case TEXT:
			return "CAST (NULL AS text)";			
		default:
			return "NULL";
		}
	}
	
	/**
	 * @return the value to use as a literal sql value for the display field.
	 * @param df the display field
	 * @param value the value from the MapEntityObject
	 */
	protected String getSqlLiteralValue(DisplayField df, Object value) {
		switch ((MockFormulaDataType) df.getFormulaFieldInfo().getDataType()) {
		case DATETIME:
			return "'" + FormulaDateUtil.formatDatetimeToISO8601((java.util.Date) value)
					+ "'::timestamp";
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
		sub.append(") c");
		return sub;
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
			return "(" + columnSql + ")::timestamp";
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
	public String evaluateSql(String testName, FormulaRuntimeContext formulaContext, Object entityObject, String formulaSource,
			boolean nullAsNull) throws SQLException, FormulaException, IOException {
		@SuppressWarnings("unchecked")
		Map<String, Object> values = (Map<String, Object>) entityObject;

		CharSequence subQuery = makeRowValueSubquery(formulaContext, values);

		FormulaTableRegistry registry = new MockFormulaTableRegistry();
		EmbeddedPostgres epg = getPostgres();
		if (epg != null) {
			try (Connection conn = epg.getDatabase("postgres", "postgres").getConnection()) {
				FormulaTypeSpec type = nullAsNull ? MockFormulaType.NULLASNULL : MockFormulaType.DEFAULT;
				RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(type, formulaContext, formulaSource);
				FormulaWithSql formula = (FormulaWithSql) formulaInfo.getFormula();
				String column = formula.toSQL(registry);
				column = fixSqlFormat(column, formula);

				try {
					// System.out.println("SELECT " + column + subQuery.toString());  // For debugging
					try (PreparedStatement pstmt = conn.prepareStatement("SELECT " + column + subQuery.toString())) {
						try (ResultSet rset = pstmt.executeQuery()) {
							if (rset.next()) {
								return formatDbResult(rset, formulaContext, formula);
							}
						}
					}
				} catch (SQLException e) {
					throw e; // Useful for a breakpoint
				}
			}
		}
		return null;
	}

	@Override
	public void close() throws Exception {
		// It autocloses.
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
