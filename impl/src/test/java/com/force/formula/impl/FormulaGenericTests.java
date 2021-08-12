/*

 * Created on Mar 1, 2005
 * Copyright, 1999-2004, salesforce.com
 * All Rights Reserved Company Confidential
 */
package com.force.formula.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.force.formula.*;
import com.force.formula.commands.FormulaJsTestUtils;
import com.force.formula.impl.FormulaImpl;
import com.force.formula.impl.FormulaTestCaseInfo.CompareType;
import com.force.formula.impl.FormulaTestCaseInfo.EvaluationContext;

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
 * @since 140
 */
public abstract class FormulaGenericTests extends BaseFormulaGenericTests {

	private static String[] KEYS = new String[] {"formula", "javascript", "javascriptLp", "formulaNullAsNull", "javascriptNullAsNull", "javascriptLpNullAsNull"};

	public FormulaGenericTests(String name) throws FileNotFoundException, ParserConfigurationException, SAXException,
	IOException {
		super(name, "labels", true);
	}

	@Override
	protected boolean filterTests(FormulaTestCaseInfo testCase) {
		return true;
	}

	@Override
	protected BaseFormulaGenericTest createTestCase(boolean positive, String entity, FormulaTestCaseInfo testCase) {
		return new FormulaGenericTest(testCase, testCase.getName(), entity, positive, this);
	}
	@Override
	protected boolean createNegativeTests() {
		return true;
	}

	private static class FormulaGenericTest extends BaseFormulaGenericTest {
		public FormulaGenericTest(FormulaTestCaseInfo testCase, String name, String entity, boolean positive, FormulaGenericTests suite) {
			super(testCase, name, entity, positive, suite);
		}

		@Override
		protected String getDirectory() {
			return "target/FormulaFields";
		}

		@Override
		protected String getJsonDirectory() {
			return "../formula-js/tests/target/formulaJson";
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
			return KEYS;

		}

		@Override
		protected void getResultsViaMultiplePaths(Map<String, String> results, FieldDefinitionInfo fieldInfo,
				String entityRecId) throws Exception {
			Map<String,Object> entityObject = getData(entityRecId);
			if (getTestCaseInfo().evalForContext(EvaluationContext.Formula)) {
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
				jsMap.put("record", FormulaJsTestUtils.makeJSMap(record));

				FormulaTypeSpec type = nullAsNull ? MockFormulaType.JAVASCRIPT_NULLASNULL: MockFormulaType.JAVASCRIPT;
				RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(type, formulaContext, formulaSource);
				Formula formula = formulaInfo.getFormula();

				Object value = FormulaJsTestUtils.evaluateFormula(formula, formulaContext.getFormulaReturnType().getDataType(), formulaContext, jsMap);

				// The formula needs to be "processed" for display by doing the right scale
				value = FormulaImpl.formatResult(formulaContext, formulaContext.getFormulaReturnType(), value);
				return value == null ? null : String.valueOf(value);
			} catch (Throwable e) {
				return "Error: " + e.getMessage();
			}
		}

		private void getFormulaValues(Map<String, String> results, FieldDefinitionInfo fieldInfo, Map<String,Object> entityObject, boolean nullAsNull) throws Exception {
			String keySuffix = nullAsNull ? "NullAsNull" : "";
			String valueViaJavascript = null;
			String valueViaJavascriptLp = null;  // Low precision
			String valueViaFormula = null;


			if (fieldInfo.getFormula() != null) {
				FormulaTypeSpec type = nullAsNull ? MockFormulaType.JAVASCRIPT_NULLASNULL: MockFormulaType.JAVASCRIPT;
				FormulaRuntimeContext formulaContext =  new MapFormulaContext(super.setupMockContext(fieldInfo.getReturnType()), mapEntity, type, entityObject);
				RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(type, formulaContext, fieldInfo.getFormula());
				Formula formula = formulaInfo.getFormula();
				Object result = null;
				try {
					result = formula.evaluate(formulaContext);
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

				formulaContext.setProperty(FormulaContext.HIGHPRECISION_JS, true);
				valueViaJavascript = evaluateJavascript(formulaContext, entityObject, fieldInfo.getFormula(), nullAsNull);
				formulaContext.setProperty(FormulaContext.HIGHPRECISION_JS, false);
				valueViaJavascriptLp = evaluateJavascript(formulaContext, entityObject, fieldInfo.getFormula(), nullAsNull);
			}

			// Expand any hyperlink markers.... to be consistent with API
			//valueViaEntityObject = CalculatedFieldTextElement.handleHyperlink(valueViaEntityObject, false, false);
			//valueViaFormula = CalculatedFieldTextElement.handleHyperlink(valueViaFormula, false, false);
			results.put("formula" + keySuffix , valueViaFormula);
			results.put("javascript" + keySuffix , valueViaJavascript);
			results.put("javascriptLp" + keySuffix , valueViaJavascriptLp);
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
					results.get("formula"), results.get("javascript"), results.get("javascriptLp"), false);
			if (mismatchMessage != null) {
				return mismatchMessage;
			}
			mismatchMessage = verifyMultiplePath(fieldInfo, results.get("formulaNullAsNull"), results.get("javascriptNullAsNull"), results.get("javascriptLpNullAsNull"), true);
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
				FieldDefinitionInfo fieldInfo, String viaFormula, String viaJavascript, String viaJavascriptLp, boolean nullIsNull) {

			final CompareType compareType = getTestCaseInfo().getCompareType();

			// If any of them generated a result with an error message, don't compare.

			if (hasErrorMessage(viaFormula)) {
				return null;
			}
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


			// If one is null, they all should be (except template should be "")
			if (viaFormula == null) {
				// Since we didn't check javascript earlier, we need to make sure it is also null.
				if (viaJavascript == null) {
					if (viaJavascriptLp == null) {
						return null;
					} else {
						return "If one is null, they all should be null. viaFormula " + viaFormula + " viaJavascriptLp " + viaJavascript;
					}
				} else {
					return "If one is null, they all should be null. viaFormula " + viaFormula + " viaJavascript " + viaJavascript;
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
					boolean compareOK;
					String mismatchMessage = null;
					if (!getTestCaseInfo().getAccuracyIssue().ignoreHighPrecision()) {  // If there's an accuracy issue with decimal, don't bother
						BigDecimal viaJavascriptDec = viaJavascript != null ? new BigDecimal(viaJavascript, mc).setScale(scale, RoundingMode.HALF_UP) : BigDecimal.ZERO; // We can get here through nullAsNull
						if (compareType == CompareType.Approximate) {
							BigDecimal delta = BigDecimal.ONE.movePointLeft(scale);
							compareOK = viaFormulaDec.subtract(viaJavascriptDec).abs().compareTo(delta) <= 0;
							if (! compareOK) {
								StringBuilder errorMsg = new StringBuilder(128).append("viaFormula ").append(viaFormulaDec.toEngineeringString())
										.append(" is not within range ").append(delta.toEngineeringString())
										.append(" of viaJavascript ").append(viaJavascriptDec.toEngineeringString());
								mismatchMessage = errorMsg.toString();
							}
						} else {
							compareOK = viaFormulaDec.equals(viaJavascriptDec);
							if (! compareOK) {
								mismatchMessage = getGenericJavascriptValueEqualityFailMessage(viaFormulaDec.toEngineeringString(), viaJavascriptDec.toEngineeringString());
							}
						}
					}

					// Always compare approximately.  But allow "NeedHighPrecision" to be ignored
					if (!getTestCaseInfo().getAccuracyIssue().ignoreLowPrecision()) {
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
				return null;

			} else if (compareType == CompareType.DateTime) {
				// W-3358047; See FormulaGenericTests - Account - testOriginTime
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

				return null;

			} else {
				//  Default comparison gauntlet
				if (! viaFormula.equals(viaJavascript) && !getTestCaseInfo().getAccuracyIssue().ignoreHighPrecision()) {
					return getGenericJavascriptValueEqualityFailMessage(viaFormula,  viaJavascript);
				}
				if (! viaFormula.equals(viaJavascriptLp) && !getTestCaseInfo().getAccuracyIssue().ignoreLowPrecision()) {
					return getGenericJavascriptLpValueEqualityFailMessage(viaFormula,  viaJavascriptLp);
				}
				return null;
			}
		}
	}

}
