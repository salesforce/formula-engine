/**
 * 
 */
package com.force.formula.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import com.force.formula.*;
import com.force.formula.FormulaSchema.Entity;
import com.force.formula.impl.BaseFormulaGenericTests.DbTester;
import com.force.formula.sql.*;
import com.force.formula.util.*;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * a DbTester of formulas that uses embedded postgres for validation.
 * 
 * Instead of writing rows to the database, it puts all of the "passed in
 * values" in a subquery with alias "c", casting the null values to .
 * 
 * @author stamm
 */
public class EmbeddedPostgresqlTester extends DbTester {

	private final EmbeddedPostgres pg;

	/**
	 * @throws IOException
	 * 
	 */
	public EmbeddedPostgresqlTester() throws IOException {
		this.pg = EmbeddedPostgres.builder().start();
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
	CharSequence makeRowValueSubquery(FormulaRuntimeContext formulaContext, Map<String, Object> values) {
		MapFormulaContext mfc = (MapFormulaContext) formulaContext;

		// Construct subquery
		StringBuilder sub = new StringBuilder();
		sub.append(" FROM (SELECT 1");
		if (values != null) {
			for (DisplayField df : formulaContext.getDisplayFields(mfc.getEntity())) {
				Object value = values.get(df.getFormulaFieldInfo().getName());
				String sqlValue = null;

				if (value == null) {
					switch ((MockFormulaDataType) df.getFormulaFieldInfo().getDataType()) {
					case DATETIME:
						sqlValue = "CAST (NULL AS timestamp)";
						break;
					case DATEONLY:
						sqlValue = "CAST (NULL AS date)";
						break;
					case CURRENCY:
					case INTEGER:
					case PERCENT:
					case DOUBLE:
						sqlValue = "CAST (NULL AS numeric)";
						break;
					default:
						sqlValue = "NULL";
					}
				} else {
					switch ((MockFormulaDataType) df.getFormulaFieldInfo().getDataType()) {
					case DATETIME:
						sqlValue = "'" + FormulaDateUtil.formatDatetimeToISO8601((java.util.Date) value)
								+ "'::timestamp";
						break;
					case DATEONLY:
						sqlValue = "TO_DATE('" + FormulaDateUtil.formatDateToSql(((java.util.Date) value))
								+ "','DD-MM-YYYY')";
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
						sqlValue = bd.toPlainString();
						break;
					case TEXT:
						// Replace singlequote
						sqlValue = "'" + FormulaTextUtil.replaceSimple(String.valueOf(value), "'", "''") + "'";
						break;
					default:
						sqlValue = String.valueOf(value);
					}
				}
				sub.append(", " + sqlValue).append(" as ").append(df.getFormulaFieldInfo().getDbColumn(null, null));
			}
		}
		sub.append(") c");
		return sub;
	}

	/**
	 * Evaluate the formula using the embedded postgres engine.
	 */
	@Override
	public String evaluateSql(FormulaRuntimeContext formulaContext, Object entityObject, String formulaSource,
			boolean nullAsNull) throws SQLException, FormulaException {
		@SuppressWarnings("unchecked")
		Map<String, Object> values = (Map<String, Object>) entityObject;

		CharSequence subQuery = makeRowValueSubquery(formulaContext, values);

		FormulaTableRegistry registry = new MockFormulaTableRegistry();
		if (pg != null) {
			try (Connection conn = pg.getDatabase("postgres", "postgres").getConnection()) {
				FormulaTypeSpec type = nullAsNull ? MockFormulaType.NULLASNULL : MockFormulaType.DEFAULT;
				RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(type, formulaContext, formulaSource);
				FormulaWithSql formula = (FormulaWithSql) formulaInfo.getFormula();
				String column = formula.toSQL(registry);
				MockFormulaDataType returnType = (MockFormulaDataType) formula.getDataType();
				// Convert types on return so they look right.
				switch (returnType) {
				case DATETIME:
					column = "(" + column + ")::timestamp";
					break;
				case PERCENT:
					column = "(" + column + ")/100"; // The format below moves the decimal point over
					break;
				case BOOLEAN:
					column = FormulaImpl.massageSqlForType(formulaContext.getFormulaReturnType(), column);
					break;
				default:
				}
				try {
					try (PreparedStatement pstmt = conn.prepareStatement("SELECT " + column + subQuery.toString())) {
						try (ResultSet rset = pstmt.executeQuery()) {
							if (rset.next()) {
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
									return FormulaValidationHooks.get().constructTime(millis).toString();
								default:
								}
								String result = rset.getString(1);
								if (result == null || result.length() == 0) {
									return null;
								}
								return result;
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
