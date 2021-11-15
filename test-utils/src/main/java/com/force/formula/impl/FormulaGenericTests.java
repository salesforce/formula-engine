/*

 * Created on Mar 1, 2005
 * Copyright, 1999-2004, salesforce.com
 * All Rights Reserved Company Confidential
 */
package com.force.formula.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.*;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.force.formula.*;
import com.force.formula.commands.FormulaJsTestUtils;
import com.force.formula.impl.FormulaTestCaseInfo.CompareType;
import com.force.formula.impl.FormulaTestCaseInfo.DefaultEvaluationContext;
import com.force.formula.util.FormulaDateUtil;
import com.force.formula.util.FormulaI18nUtils;

/*
 * TODO: Add following Tests:
 * 1) To check LastActivityDate
 * 2) To check entity allowed field references
 * 3) Add test to validate CreatedDate
 */

/**
 * using the test case xml file and calls the api to create the fields (both
 * custom and formula), reads the data file and processes the data file to get
 * the resulting formula field via api, ui and bulk mode, as well as template version.
 *
 * @author syendluri, dchasman, aballard
 * @since 0.1.0
 */
public abstract class FormulaGenericTests extends BaseFormulaGenericTests {
    private static final Logger logger = Logger.getLogger("com.force.formula");

	private static String[] SQL_KEYS = new String[] {"formula", "sql", "javascript", "javascriptLp", "formulaNullAsNull", "sqlNullAsNull", "javascriptNullAsNull", "javascriptLpNullAsNull"};
	private static String[] SQL_NO_JS_KEYS = new String[] {"formula", "sql", "formulaNullAsNull", "sqlNullAsNull"};
	private static String[] KEYS = new String[] {"formula", "javascript", "javascriptLp", "formulaNullAsNull", "javascriptNullAsNull", "javascriptLpNullAsNull"};

	public FormulaGenericTests(String name) throws FileNotFoundException, ParserConfigurationException, SAXException,
	IOException {
		super(name, "labels", true);
	}
	
	
	/**
	 * @return whether javascript should be tested for these formulas.  This should be used in -impl, and by default,
	 * but is turned on in the db-test modules for speed (since it's duplicative)
	 */
	protected boolean shouldTestJavascript() {
		return true;
	}
	
	/**
	 * @return whether sql should be tested for these formulas.  It validates more of the functionality... but is... slower.
	 */
	protected boolean shouldTestSql() {
		return false;
	}
	
	private DbTester dbTester;
	// Simplify set of the db by creating one per suite.
	protected final DbTester getDbTester() throws IOException, SQLException {
		if (dbTester == null) {
			dbTester = constructDbTester();
		}
		return dbTester;
	}
	
	/**
	 * @return a new DbTester that will be closed at the end of thest
	 * @throws IOException if an exception occurred starting the DB
	 * @throws SQLException if an exception occurred starting the DB
	 */
	protected DbTester constructDbTester() throws IOException, SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean filterTests(FormulaTestCaseInfo testCase) {
		return true;
	}

	@Override
	protected BaseFormulaGenericTest createTestCase(boolean positive, String entity, FormulaTestCaseInfo testCase) {
		return new FormulaGenericTest(testCase, testCase.getName(), positive, this);
	}
	@Override
	protected boolean createNegativeTests() {
		return true;
	}

	/**
	 * 
	 * @param testName name of the test
	 * @return whether the javascript value mismatch should be in the gold files, or a general error.
	 */
	protected boolean ignoreJavascriptValueMismatchInAutobuilds(String testName) {
		return false;
	}
	
	protected static class FormulaGenericTest extends BaseFormulaGenericTest {
		public FormulaGenericTest(FormulaTestCaseInfo testCase, String name, boolean positive, FormulaGenericTests suite) {
			super(testCase, name, positive, suite);
		}

		@Override
		protected String getDirectory() {
			return "src/test/goldfiles/FormulaFields";
		}

		@Override
		protected String getJsonDirectory() {
			return "../formula-js/tests/target/formulaJson";
		}
		
		/**
		 * @return whether the DbTester should be used to evaluate sql
		 */
		protected boolean shouldTestSql() {
			return ((FormulaGenericTests)getSuite()).shouldTestSql();
		}

		/**
		 * @return whether the javascript should be used.  Turn off in the 
		 * sql tests modules
		 */
		protected boolean shouldTestJavascript() {
			return ((FormulaGenericTests)getSuite()).shouldTestJavascript();
		}
		
		/**
		 * @return whether for a give test, the values from the SL engine should be compared
		 */
		protected boolean shouldCompareSql() {
			return shouldTestSql() && !getTestCaseInfo().ignoreSql();
		}
		
		@Override
		protected boolean ignoreJavascriptValueMismatchInAutobuilds() {
			return ((FormulaGenericTests)getSuite()).ignoreJavascriptValueMismatchInAutobuilds(getName());
		}

		@Override
		protected void createFail(FormulaTestCaseInfo testCase, FieldDefinitionInfo fieldInfo) throws Exception {
		}

		@Override
		protected void createTestFormulas(FieldDefinitionInfo fieldInfo, String entityRecId) throws Exception {
			/*
            cfIdNull = null;
            cfId = createCustomField(fieldInfo, false); // we need this to create the template from currently
            if (getTestCaseInfo().evalForContext(EvaluationContext.Formula)) {
                cfIdNull = createFormulaFieldNullAsNull(fieldInfo);
            }
            // First time we use this fieldInfo, create its merge field template also.
            if (fieldInfo.getTemplate() == null && getTestCaseInfo().evalForContext(EvaluationContext.Template)) {
                fieldInfo.setTemplate(getTemplateSource(entityRecId, fieldInfo.getDevName()));
            }
			 */
			// (getTestCaseInfo().evalForContext(EvaluationContext.Workflow))
			// cfId is not null. Use this field as a mock field for workflow formula. No need to do anything to create workflow formula testcase setup.
		}

		@Override
		protected boolean cleanupTest() throws Exception {
			return false;
		}

		@Override
		protected String[] getKeyNames() {
			return shouldTestSql() ? (shouldTestJavascript() ? SQL_KEYS : SQL_NO_JS_KEYS) : KEYS;

		}

		@Override
		protected void getResultsViaMultiplePaths(Map<String, String> results, FieldDefinitionInfo fieldInfo,
				String entityRecId) throws Exception {
			Map<String,Object> entityObject = getData(entityRecId);
			if (getTestCaseInfo().evalForContext(DefaultEvaluationContext.Formula)) {
				getFormulaValues(results, fieldInfo, entityObject, false);
				getFormulaValues(results, fieldInfo, entityObject, true);
			}
		}

		private String evaluateJavascript(FormulaRuntimeContext formulaContext, Map<String, Object> entityObject, String formulaSource, boolean nullAsNull) {
			try {
				// Establish the context for the entity object
				Map<String,Object> record = entityObject != null ? new HashMap<String,Object>(entityObject) : new HashMap<String,Object>();
				// TODO: LDT - Add Profile and RecordType contexts
				Map<String,Object> jsMap = new HashMap<>();
				jsMap.put("record", FormulaJsTestUtils.get().makeJSMap(record));

				FormulaTypeSpec type = nullAsNull ? MockFormulaType.JAVASCRIPT_NULLASNULL: MockFormulaType.JAVASCRIPT;
				RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(type, formulaContext, formulaSource);
				Formula formula = formulaInfo.getFormula();

				Object value = FormulaJsTestUtils.get().evaluateFormula(formula, formulaContext.getFormulaReturnType().getDataType(), formulaContext, jsMap);

				// The formula needs to be "processed" for display by doing the right scale
				value = FormulaI18nUtils.formatResult(formulaContext, formulaContext.getFormulaReturnType(), value);
				return value == null ? null : String.valueOf(value);
			} catch (Throwable e) {
				logger.log(Level.FINER, "Error in javascript", e);
				return "Error: " + e.getMessage();
				
			}
		}

		/**
		 * Evaluate the formula using a Sql Engine provided in the implementation of {@link FormulaGenericTests#getDbTester()}
		 * @param formulaContext the context to evaluate
		 * @param entityObject the set of values to use to replace references in the formula
		 * @param formulaSource the original formula source
		 * @param nullAsNull should null be treated as null or as blank
		 * @return the result of evaluating the 
		 */
		private String evaluateSql(FormulaRuntimeContext formulaContext, Map<String, Object> entityObject, String formulaSource, boolean nullAsNull) {
			try {
				return ((FormulaGenericTests)getSuite()).getDbTester().evaluateSql(formulaContext, entityObject, formulaSource, nullAsNull);
			} catch (Throwable e) {
				logger.log(Level.FINER, "Error in sql", e);
				return "Error: " + e.getMessage();
			}
		}
		
		protected void getFormulaValues(Map<String, String> results, FieldDefinitionInfo fieldInfo, Map<String,Object> entityObject, boolean nullAsNull) throws Exception {
			String keySuffix = nullAsNull ? "NullAsNull" : "";
			String valueViaJavascript = null;
			String valueViaJavascriptLp = null;  // Low precision
			String valueViaFormula = null;
			String valueViaSql = null;


			if (fieldInfo.getFormula() != null) {
				FormulaTypeSpec type = nullAsNull ? MockFormulaType.JAVASCRIPT_NULLASNULL: MockFormulaType.JAVASCRIPT;
				FormulaRuntimeContext formulaContext =  new MapFormulaContext(super.setupMockContext(fieldInfo.getReturnType()), mapEntity, type, entityObject);
				RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(type, formulaContext, fieldInfo.getFormula());
				Formula formula = formulaInfo.getFormula();
				try {
					Object result = formula.evaluate(formulaContext);
					if (result != null) {
						// Note this gets more of a "raw value" than the access via the entity object.  We can get the same
						// (or closer) value by converting to/from Double or by setting scale to 1 if it is currently 0.
						// but for now, leave it as is.  Gold file will show different, but numerically equal value.
						valueViaFormula = (result instanceof BigDecimal) ? ((BigDecimal)result).toPlainString()
								: result.toString();
					} else if (fieldInfo.getReturnType().isBoolean()) {
						// Oracle/Formula null values in formulas are resolved to null since they are always guarded with
						// NVLs.  In java BooleanField doesn't support tristate, so it resolves to false.
						valueViaFormula = Boolean.FALSE.toString();
					}
				} catch (Throwable e) {
					valueViaFormula = "Error: " + e.getClass().getName();
				}

				if (shouldTestJavascript()) {
					formulaContext.setProperty(FormulaContext.HIGHPRECISION_JS, true);
					valueViaJavascript = evaluateJavascript(formulaContext, entityObject, fieldInfo.getFormula(), nullAsNull);
					formulaContext.setProperty(FormulaContext.HIGHPRECISION_JS, false);
					valueViaJavascriptLp = evaluateJavascript(formulaContext, entityObject, fieldInfo.getFormula(), nullAsNull);
				}
				
				if (shouldTestSql()) {
					valueViaSql = evaluateSql(formulaContext, entityObject, fieldInfo.getFormula(), nullAsNull);
				}
			}

			results.put("formula" + keySuffix , valueViaFormula);
			results.put("javascript" + keySuffix , valueViaJavascript);
			results.put("javascriptLp" + keySuffix , valueViaJavascriptLp);
			results.put("sql" + keySuffix , valueViaSql);
		}

		/**         * Helper method to print a consistent error message about a mismatch between formula and
		 * javascript evaluated values
		 */
		private String getGenericJavascriptValueEqualityFailMessage(String viaFormula, String viaJavascript) {
			StringBuilder errorMsg = new StringBuilder(128).append("viaFormula ")
					.append(viaFormula).append(" does not equal viaJavascript ").append(viaJavascript);
			return errorMsg.toString();
		}

		private String getGenericJavascriptLpValueEqualityFailMessage(String viaFormula, String viaJavascript) {
			StringBuilder errorMsg = new StringBuilder(128).append("viaFormula ")
					.append(viaFormula).append(" does not equal viaJavascriptLp ").append(viaJavascript);
			return errorMsg.toString();
		}


		@Override
		String verifyResults(FieldDefinitionInfo fieldInfo, Map<String, String> results) {
			String mismatchMessage = verifyMultiplePath(fieldInfo,
					results.get("formula"), results.get("sql"), results.get("javascript"), results.get("javascriptLp"), false);
			if (mismatchMessage != null) {
				return mismatchMessage;
			}
			mismatchMessage = verifyMultiplePath(fieldInfo, results.get("formulaNullAsNull"), results.get("sqlNullAsNull"), results.get("javascriptNullAsNull"), results.get("javascriptLpNullAsNull"), true);
			return mismatchMessage;
		}

		Calendar getFromDateString(String str) throws ParseException {
			SimpleDateFormat javaDf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
			Date viaFormulaDate = javaDf.parse(str);
			Calendar c = Calendar.getInstance();
			c.setTime(viaFormulaDate);
			return c;
		}

		//  Determine if all paths resulted in same value (modulo various expected differences!)
		//  returning null means success.
		private String verifyMultiplePath(
				FieldDefinitionInfo fieldInfo, String viaFormula, String viaSql, String viaJavascript, String viaJavascriptLp, boolean nullIsNull) {

			final CompareType compareType = getTestCaseInfo().getCompareType();

			// If any of them generated a result with an error message, don't compare.

			if (hasErrorMessage(viaFormula)) {
				return null;
			}
            if (hasErrorMessage(viaSql)) {
            	if (shouldCompareSql()) {
            		return "SQL had an error that didn't affect Java: " + viaSql; 
            	} else {
            		return null;  // It's an error, but just leave it.
            	}
            }
            if (shouldTestJavascript()) {
				if (hasErrorMessage(viaJavascript) && !getTestCaseInfo().getAccuracyIssue().ignoreHighPrecision()) {
					if (nullIsNull) return null;
					if (!getTestCaseInfo().getAccuracyIssue().ignoreHighPrecision()) {
						StringBuilder errorMsg = new StringBuilder(128).append("Javascript had an error when no other did: should be ").append(viaFormula)
								.append(" but was ").append(viaJavascript);
						return errorMsg.toString();
					}
				}
				if (hasErrorMessage(viaJavascriptLp)) {
					if (nullIsNull) return null;
					if (!getTestCaseInfo().getAccuracyIssue().ignoreLowPrecision()) {
						StringBuilder errorMsg = new StringBuilder(128).append("JavascriptLp had an error when no other did: should be ").append(viaFormula)
								.append(" but was ").append(viaJavascript);
						return errorMsg.toString();
					}
				}
            }


			// If one is null, they all should be (except template should be "")
			if (viaFormula == null || (shouldCompareSql() && viaSql == null)) {
                String badNullMessage = "If one is null, they all should be null. viaFormula " + viaFormula + " viaSql " + viaSql;
                
                if (shouldCompareSql() && (viaFormula != null || viaSql != null)) {
                    return badNullMessage;
                }
                
                // Since we didn't check javascript earlier, we need to make sure it is also null.
                if (shouldTestJavascript()) {
					if (viaJavascript == null) {
						if (viaJavascriptLp == null) {
							return null;
						} else {
							return "If one is null, they all should be null. viaFormula " + viaFormula + " viaJavascriptLp " + viaJavascript;
						}
					} else {
						return "If one is null, they all should be null. viaFormula " + viaFormula + " viaJavascript " + viaJavascript;
					}
                } else {
                	return null;
                }
			}

			// So we know all non-null for the rest... (except template maybe)
			if (compareType == CompareType.Number || compareType == CompareType.Approximate) {
				try {
					// Currency may have currency code for via-entity evaluation
					String entityValue = viaFormula;
					if (fieldInfo.getReturnType().isCurrency()) {
						int sepPos = entityValue.indexOf(" ");
						if (sepPos > 0) {
							entityValue = entityValue.substring(sepPos + 1);
						}
					}
					final int scale = fieldInfo.getScale();
					MathContext mc = new MathContext(fieldInfo.getPrecision(), RoundingMode.HALF_UP);
					// NOTE: Core's tests don't do RoundingMode HALF_UP because it ends up being filtered through saving to the DB.
					BigDecimal viaFormulaDec = new BigDecimal(entityValue, mc).setScale(scale, RoundingMode.HALF_UP);
					BigDecimal viaSqlDec = shouldCompareSql() ? new BigDecimal(viaSql, mc).setScale(scale, RoundingMode.HALF_UP) : null; 
					boolean compareOK = true;  // In case ignoreSql and ignoreJavascript is on
					String mismatchMessage = null;
					if (!getTestCaseInfo().getAccuracyIssue().ignoreHighPrecision()) {  // If there's an accuracy issue with decimal, don't bother
						BigDecimal viaJavascriptDec = viaJavascript != null ? new BigDecimal(viaJavascript, mc).setScale(scale, RoundingMode.HALF_UP) : BigDecimal.ZERO; // We can get here through nullAsNull
						if (compareType == CompareType.Approximate) {
							BigDecimal delta = BigDecimal.ONE.movePointLeft(scale);
			                if (shouldCompareSql()) {
								compareOK = viaFormulaDec.subtract(viaSqlDec).abs().compareTo(delta) <= 0
										&& (!shouldTestJavascript() || viaFormulaDec.subtract(viaJavascriptDec).abs().compareTo(delta) <= 0);
			                } else if (shouldTestJavascript()) {
			                	compareOK = viaFormulaDec.subtract(viaJavascriptDec).abs().compareTo(delta) <= 0;
			                }
							if (! compareOK) {
								if (shouldCompareSql() && viaFormulaDec.subtract(viaSqlDec).abs().compareTo(delta) > 0) {
	                                mismatchMessage = "viaFormula " + viaFormulaDec.toEngineeringString() +
	                                                  " is not within range " + delta.toEngineeringString() +
	                                                  " of viaSql " + viaSqlDec.toEngineeringString();
	                            } else {
	                            	StringBuilder errorMsg = new StringBuilder(128).append("viaFormula ").append(viaFormulaDec.toEngineeringString())
											.append(" is not within range ").append(delta.toEngineeringString())
											.append(" of viaJavascript ").append(viaJavascriptDec.toEngineeringString());
									mismatchMessage = errorMsg.toString();
	                            }
							}
						} else {
							compareOK = true;
							if (shouldTestJavascript()) {
								compareOK &= viaFormulaDec.equals(viaJavascriptDec);
							}
							if (shouldCompareSql()) {
								compareOK &= viaFormulaDec.equals(viaSqlDec);
							}
							if (! compareOK) {
								if (shouldCompareSql() && ! viaFormulaDec.equals(viaSqlDec)) {
	                                mismatchMessage = "viaFormula " + viaFormulaDec.toEngineeringString() +
	                                                  " does not equal viaSql " + viaSqlDec.toEngineeringString();
	                            } else {
	                            	mismatchMessage = getGenericJavascriptValueEqualityFailMessage(viaFormulaDec.toEngineeringString(), viaJavascriptDec.toEngineeringString());
	                            
	                            }
							}
						}
					}

					// Always compare approximately.  But allow "NeedHighPrecision" to be ignored
					if (!getTestCaseInfo().getAccuracyIssue().ignoreLowPrecision() && shouldTestJavascript()) {
						BigDecimal viaJavascriptLpDec = viaJavascriptLp != null ? new BigDecimal(viaJavascriptLp, mc).setScale(scale, RoundingMode.HALF_UP) : BigDecimal.ZERO; // We can get here through nullAsNull
						BigDecimal delta = BigDecimal.ONE.movePointLeft(scale);
						compareOK = viaFormulaDec.subtract(viaJavascriptLpDec).abs().compareTo(delta) <= 0;
						if (! compareOK) {
							StringBuilder errorMsg = new StringBuilder(128).append("viaFormula ").append(viaFormulaDec.toEngineeringString())
									.append(" is not within range ").append(delta.toEngineeringString())
									.append(" of viaJavascriptLp ").append(viaJavascriptLpDec.toEngineeringString());
							mismatchMessage = errorMsg.toString();
						}
					}

					return mismatchMessage;

				} catch (NumberFormatException e) {
					// treat format error as failure
					return "NumberFormatException for one of viaFormula " + viaFormula + ", viaJavascript " + viaJavascript + " . ";
				}
			} else if (compareType  == CompareType.Date) {
				// API version has format yyyy-mm-dd or 2005-01-03T23:32:00.000Z.  Others are Mon Jan 03 00:00:00 GMT 2005.
				try {
					if (shouldCompareSql()) {
	                    SimpleDateFormat sqlDf = new SimpleDateFormat("yyyy-MM-dd");
	                    Date viaSqlDate = sqlDf.parse(viaSql);
	                    // Unfortunately date arithmetic in pointwise versions tracks a time also, so it
	                    // doesn't match the api version.  This is a historical mistake we can't correct
	                    // without impact on existing code.
	                    // So adjust the time part to 0 before comparing.
	                    Calendar c = getFromDateString(viaFormula);
	                    FormulaDateUtil.toMidnight(c);
	                    Date viaFormulaDate = c.getTime();
	                    boolean compareOK =  viaSqlDate.equals(viaFormulaDate);
	                    if (! compareOK) {
	                        return "viaFormula " + viaFormulaDate + " (" + viaFormula +
	                                          ") does not equal viaSql " + viaSqlDate + "(" + viaSql + ")";
	                    }
					}
				} catch (ParseException e) {
					return "Date ParseException for one of viaFormula " + viaFormula + ", viaSql " + viaSql + " .";
				}
				if (shouldTestJavascript()) {
					try {
	                    if (! viaFormula.equals(viaJavascript)) {
							Calendar c = getFromDateString(viaFormula);
							if (c.get(Calendar.YEAR) > 1776) {
								return getGenericJavascriptValueEqualityFailMessage(viaFormula, viaJavascript);
							}
						}
						if (! viaFormula.equals(viaJavascriptLp)) {
							Calendar c = getFromDateString(viaFormula);
							if (c.get(Calendar.YEAR) > 1776) {
								return getGenericJavascriptLpValueEqualityFailMessage(viaFormula, viaJavascriptLp);
							}
						}
					} catch (ParseException e) {
						return "Date ParseException for one of viaFormula " + viaFormula + ", viaJavaScript " + viaJavascript + " .";
					}
				}
				return null;

			} else if (compareType == CompareType.DateTime) {
				if (shouldCompareSql()) {
					try {
	                    SimpleDateFormat sqlDf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	                    Date viaSqlDate = sqlDf.parse(viaSql);
	                    SimpleDateFormat javaDf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
	                    Date viaFormulaDate = javaDf.parse(viaFormula);
	                    String mismatchMessage = null;
	                    boolean compareOK = viaSqlDate.getTime() == viaFormulaDate.getTime();
	                    if (! compareOK) {
	                        mismatchMessage = "viaFormula " + viaFormulaDate.getTime() + " (" + viaFormula +
	                                          ") does not equal viaApi " + viaSqlDate.getTime() + "(" + viaSql + ")";
	                    }
	                    return mismatchMessage;
					} catch (ParseException e) {
						return "Date ParseException for one of viaFormula " + viaFormula + ", viaSql " + viaSql + " .";
					}
				}

				
				// W-3358047; See FormulaGenericTests - Account - testOriginTime
				if (shouldTestJavascript()) {
					try {
						if (! viaFormula.equals(viaJavascript)) {
							Calendar c = getFromDateString(viaFormula);
							if (c.get(Calendar.YEAR) > 1776) {
								return getGenericJavascriptValueEqualityFailMessage(viaFormula, viaJavascript);
							}
						}
						if (! viaFormula.equals(viaJavascriptLp)) {
							Calendar c = getFromDateString(viaFormula);
							if (c.get(Calendar.YEAR) > 1776) {
								return getGenericJavascriptLpValueEqualityFailMessage(viaFormula, viaJavascriptLp);
							}
						}
						
					} catch (ParseException e) {
						return "Date ParseException for one of viaFormula " + viaFormula + ", viaJavaScript " + viaJavascript + " .";
					}
				}
				return null;
			} else {
				// Default comparison gauntlet
				if (shouldCompareSql() && !viaFormula.equals(viaSql)) {
					return "viaFormula " + viaFormula + " does not equal viaSql " + viaSql;
				}
				if (shouldTestJavascript()) {
					if (! viaFormula.equals(viaJavascript) && !getTestCaseInfo().getAccuracyIssue().ignoreHighPrecision()) {
						return getGenericJavascriptValueEqualityFailMessage(viaFormula,  viaJavascript);
					}
					if (! viaFormula.equals(viaJavascriptLp) && !getTestCaseInfo().getAccuracyIssue().ignoreLowPrecision()) {
						return getGenericJavascriptLpValueEqualityFailMessage(viaFormula,  viaJavascriptLp);
					}
				}
				return null;
			}
		}
	}


}
