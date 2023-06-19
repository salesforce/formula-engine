/*
 * Created on Mar 1, 2005
 * Copyright, 1999-2021, salesforce.com
 * All Rights Reserved Company Confidential
 */
package com.force.formula.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.xml.sax.SAXException;

import com.force.formula.Formula;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaEngineHooks;
import com.force.formula.FormulaException;
import com.force.formula.FormulaFactory;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.FormulaTestBase;
import com.force.formula.FormulaTypeSpec;
import com.force.formula.MockFormulaType;
import com.force.formula.RuntimeFormulaInfo;
import com.force.formula.impl.FormulaTestCaseInfo.CompareType;
import com.force.formula.impl.FormulaTestCaseInfo.WhyIgnoreSql;
import com.force.formula.impl.MapFormulaContext.MapEntity;
import com.force.formula.impl.MapFormulaContext.MapFieldInfo;
import com.force.formula.sql.FormulaWithSql;
import com.force.formula.util.FormulaTextUtil;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import junit.framework.TestSuite;

/**
 * Using the test case xml file and calls the api to create the fields (both
 * custom and formula), reads the data file and processes the data file to get
 * the resulting formula field via various methods.
 *
 * This is base code that is shared by various different applications of the formula engine.
 *
 * todo ValidationGenericTests should also use this base class
 *
 * @author syendluri, dchasman, aballard
 * @since 0.1.0
 */
abstract public class BaseFormulaGenericTests extends TestSuite {

	// parameter testLabelsAttribute allows different xml attribute names to be used to specify ftest labels so that subclasses
	// build from this can run in different basic/standard/extended etc suites.
	// swapResultTypes specifies whether for tests that specify swapping types, the results should be varied as well as the parameter types.
	public BaseFormulaGenericTests(String owner, String testLabelsAttribute, boolean swapResultTypes) throws FileNotFoundException, ParserConfigurationException, SAXException,
	IOException {
		super(owner);
		createTestCases(this, getTestCaseLocation(), "Positive Tests", true, owner, testLabelsAttribute, swapResultTypes);
	}
	
	protected FormulaTestUtils getTestUtils() {
	    return new FormulaTestUtils();
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

	
	/**
	 * @return the location of the xml that contains the data
	 */
	protected String getTestCaseLocation() {
	    return "/com/force/formula/impl/formulatests.xml";
	}
	
	protected String getDataDir() {
        return "/com/force/formula/impl/data/";
	}

	private void createTestCases(TestSuite parentRootSuite, String xmlTestDefFile, String rootNodeName,
			boolean positive, String owner, String testLabelsAttribute, boolean swapResultTypes)
					throws ParserConfigurationException, SAXException, IOException, FileNotFoundException {

		// Set the timezone to GMT.  This may mask issues, but keeping track of GMT vs non-GMT when doing date comparisons is nigh impossible
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

		TestSuite rootSuite = new TestSuite(rootNodeName); // When there are
		parentRootSuite.addTest(rootSuite);

		Set<String> uniqueTestNames = new HashSet<String>();
		Map<String, String> uniqueDataNames = new HashMap<String, String>();

		// Get testcases to run, for each entity.
		TestSuite entitySuite = null;
		List<FormulaTestCaseInfo> testCases = getTestUtils().getTestCases(xmlTestDefFile, null, owner, testLabelsAttribute, swapResultTypes);
		if (testCases != null) {
			for (FormulaTestCaseInfo testCase : testCases) {

				String aTestName = testCase.getName();
				String mixDataName = testCase.getDataFileName();
				String lowDataNam = null;
				if (mixDataName != null && ! mixDataName.isEmpty()) {
					lowDataNam = mixDataName.toLowerCase();
				}
				// Make sure all test cases are uniquely named in a case-insensitive sense:
				boolean wasUnique = uniqueTestNames.add(aTestName.toLowerCase());
				Assert.assertTrue("Test name " + aTestName + " is not unique-case-insensitive.", wasUnique);
				if (lowDataNam != null) {
					// Make sure there is not a dataFile with the same name but different case:
					if (uniqueDataNames.containsKey(lowDataNam)) {
						String originalMix = uniqueDataNames.get(lowDataNam);
						Assert.assertEquals("Test name " + aTestName + " has conflicting DataFileName " + mixDataName + ". ",
								originalMix, mixDataName);
					} else {
						uniqueDataNames.put(lowDataNam, mixDataName);
					}
				}

				// Give the subclasses a chance to filter out any that are not applicable
				if (filterTests(testCase)) {
					BaseFormulaGenericTest genericTest = createTestCase(positive, null, testCase);
					if (entitySuite == null) {
						entitySuite = new TestSuite();
						rootSuite.addTest(entitySuite);
					}
					entitySuite.addTest(genericTest);
				}
			}
		}
	}

	abstract protected boolean filterTests(FormulaTestCaseInfo testCase);
	abstract protected boolean createNegativeTests();
	abstract protected BaseFormulaGenericTest createTestCase(boolean positive, String entity, FormulaTestCaseInfo testCase);

	/**
	 * Allow the test to override the hooks and factory.
	 * @param test the test to override
	 */
	abstract protected void setUpTest(BaseFormulaGenericTest test);
	
	/**
	 * Keep track of failures in SQL so changed in behavior can be managed and not just ignored
	 * @author stamm
	 * @since 0.3
	 */
    protected static class TestCaseStats {
        protected int numberExecuted = 0;
        /**
         * The number of differences seen between Javascript and Java
         */
        protected int numberJsDifferences = 0;
        /**
         * The number of differences seen between SQL and Java
         */
        protected int numberSqlDifferences = 0;
    }
	
	abstract protected static class BaseFormulaGenericTest extends FormulaTestBase {
		MapEntity mapEntity;
		Map<String,Map<String,Object>> objects = new HashMap<>();
		private final BaseFormulaGenericTests suite;


		public BaseFormulaGenericTest(FormulaTestCaseInfo testCase, String name, boolean positive, BaseFormulaGenericTests suite) {
			super(name);
			this.testCase = testCase;
			this.positive = positive;
			this.suite = suite;
		}

		FormulaTestCaseInfo getTestCaseInfo() {
			return testCase;
		}
		
		protected BaseFormulaGenericTests getSuite() {
			return this.suite;
		}

		@Override
		public void runTest() throws Throwable {
			testFormula();
		}

		/**
		 * This test is designed to test all the basic functionality of formula
		 * fields (about 1000+ tests). It parses tests from xml file. The XML file
		 * has following format: <pre>
		 * {@code 
		 * <entity name="account"> <testcase negative=true
		 * name="" code="customnum1__c + customnum2__c" devname="" ......>
		 * <referencefield devname="customnum1" datatype="" ...../> .... </testcase>
		 * </entity>
		 * }
		 * </pre>
		 *
		 * Each testcase is represented by an instance of FormulaTestCaseInfo, and
		 * all the referencefields are stored in a list of FieldDefinitionInfo in
		 * FormulaTestCaseInfo.
		 *
		 * getRunnables() method in FormulaTestCaseInfo returns one or more
		 * instances of FormulaTestRunnable depending on how each testcase is
		 * defined (if swapargs and swapTypes are set, then multiple testRunnables
		 * are generated).
		 *
		 * getListOfFieldsToCreate() will return a list of referencefields
		 * (including the sub formula fields) generated in the same sequence as they
		 * need to be created (i.e regular custom fields first, then formula fields)
		 *
		 * Each FormulaTestRunnable has the testcase Formula Field stored as
		 * FieldDefintionInfo, and the required dependent fields (excluding the
		 * child formula fields) as list of FieldDefinitionInfo.
		 *
		 * This is the final step where testcase formula field will be created, and
		 * then data is read from test data file (multiple data sets), and each
		 * dataset is set for all the reference fields used inside the formula, then
		 * the formula is accessed to get the evaluated value. The retrieved values
		 * are written to results stream. Once all the datasets for all the
		 * FormulaTestRunnables are tried, the results stream is compared with gold
		 * file, to determine any failures.
		 * @throws Exception if an error occurs
		 */
		public void testFormula() throws Exception {
			// Initialize field Maps if not already done or if dataManager is used
			// by another test, and hanging old Data in Maps.

			// Used to capture testfailures, and will be printed at the end of all
			// test runs.
			StringBuilder testFailureMsg = new StringBuilder();

			FormulaEngineHooks oldHooks = FormulaEngine.getHooks();
			FormulaFactory oldFactory = FormulaEngine.getFactory();
			try {
			    suite.setUpTest(this);

				if (doTestSetup(testCase, testFailureMsg)) {
					if (positive) {
						runTestCase(testCase, null, testFailureMsg);
					} else {
						runTestCaseWithFail(testCase, testFailureMsg);
					}
				}

				if (testFailureMsg.length() > 0) {
					fail("\n" + testFailureMsg.toString());
				}
			} finally {
				FormulaEngine.setHooks(oldHooks);
				FormulaEngine.setFactory(oldFactory);
			}
		}



		// Setup for one of the testcases from the definition file.  This creates any
		// custom fields that the test defines, (but not the formula field that will contain the test code).
		private boolean doTestSetup(FormulaTestCaseInfo testCase, StringBuilder testFailureMsg) throws Exception {
			boolean testPass = true;
			String fieldName = null;
			try {
				List<FieldDefinitionInfo> referenceFields = testCase.getFieldsToCreate();

				Map<String,MapFieldInfo> fields = new HashMap<>();

				for (FieldDefinitionInfo fieldInfo : referenceFields) {
					if (fieldInfo.isStandard()) {
						throw new AssertionError("Fix test for " + fieldInfo.getDevName());
					}
					String name = fieldInfo.getDevName() + "__c";
					fields.put(name, new MapFieldInfo(name, fieldInfo.getReturnType(), fieldInfo.getFormula(), fieldInfo.getScale()));  // Use force.com naming

				}
				this.mapEntity = new MapEntity("test", fields.values());
			} catch (Throwable e) {
				testPass = false;
				testFailureMsg.append("\n\nTESTCASE: " + testCase.getName() + " Failed. Field: " + fieldName
						+ " creation failed.\n");
				testFailureMsg.append("\n" + getStackTrace(e));
			}

			return testPass;
		}

		// Run a "negative test case".... one that is expected to fail to create the formula field.
		private boolean runTestCaseWithFail(FormulaTestCaseInfo testCase, StringBuilder testFailureMsg) throws Exception {

			boolean testPass = true;

			List<FormulaTestRunnable> instances = testCase.getRunnables(suite.getTestUtils());
			for (FormulaTestRunnable instance : instances) {
				try {
					FieldDefinitionInfo fieldInfo = instance.getTcFieldInfo();
					createFail(testCase, fieldInfo);
				} catch (Throwable e) {
					testPass = false;
					testFailureMsg.append("\n\nTESTCASE: " + testCase.getName() + " Failed.\n");
					testFailureMsg.append("\n" + getStackTrace(e));
				}
			}

			return testPass;
		}

		// Run a test case (one formula, multiple instances, multiple evaluation methods, against multiple inputs
		private boolean runTestCase(FormulaTestCaseInfo testCase, String entityRecId, StringBuilder testFailureMsg)
				throws Exception {
			// get the testdata required for the test case into 2 dimensional List..
			List<List<String>> testData = new ArrayList<List<String>>();

			if (testCase.getDataFileName() != null) {
				try {
					testData = FormulaTestUtils.getDataFromFile(suite.getClass(), suite.getDataDir(), testCase.getDataFileName());
				} catch (IOException e) {
					testFailureMsg.append("\n\nError: Data File for testCase: " + testCase.getName() + " is missing\n");
					testFailureMsg.append("\n" + getStackTrace(e));
					return false;
				}
			}

			// init the streams required to store the test results.
			ByteArrayOutputStream xml = new ByteArrayOutputStream();
			PrintStream xmlOut = new PrintStream(xml, false, "UTF-8");
			xmlOut.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			if (testCase.getCompareType() == CompareType.None) {
				xmlOut.println("<!-- Note: Results for different evaluation methods not compared for this test due to compareType=\"none\" . Check results carefully -->");
			}
			xmlOut.println("<testCase name=\"" + testCase.getName() + "\">");

			JestDataModel jestDataModel = new JestDataModel();
			jestDataModel.testSuiteName = testCase.getName();

			String firstValueMismatchMessage = null;
			List<FormulaTestRunnable> instances = testCase.getRunnables(suite.getTestUtils());
			boolean testPassed = true;

			for (FormulaTestRunnable instance : instances) {
				try {
					FieldDefinitionInfo fieldInfo = instance.getTcFieldInfo();

					xmlOut.print("   <testInstance formula=\"" + FormulaTextUtil.escapeToXml(fieldInfo.getFormula()) + "\"");
					xmlOut.print(" returntype=\"" + fieldInfo.getReturnType().getName() + "\"");
					jestDataModel.formula = fieldInfo.getFormula();
					jestDataModel.returnType = fieldInfo.getReturnType().getName();

					if (fieldInfo.isNumeric()) {
						xmlOut.print(" precision=\"" + fieldInfo.getPrecision() + "\" scale=\"" + fieldInfo.getScale() + "\"");
						jestDataModel.precision = fieldInfo.getPrecision();
						jestDataModel.scale = fieldInfo.getScale();
					}
					xmlOut.println(">");

					printChildFormulaDetails(testCase, xmlOut);

					String valueMismatchM = runTestInstance(instance, testData, entityRecId, testFailureMsg, xmlOut, jestDataModel);
					if (valueMismatchM != null && firstValueMismatchMessage == null) {
						firstValueMismatchMessage = valueMismatchM;
					}
				} catch (Throwable e) {
					testFailureMsg.append("\n\nTESTCASE: " + testCase.getName() + " failed during runTestCase.\n");
					testFailureMsg.append("\n" + getStackTrace(e));
					testPassed = false;
				}
				xmlOut.println("   </testInstance>");
			}
			xmlOut.println("</testCase>");
			xmlOut.flush();
			xmlOut.close();

			try {
				File f = new File(getDirectory() + "/" + testCase.getName() + ".xml");
				f.getParentFile().mkdirs();
				f.setWritable(true);
				
				byte[] results = xml.toByteArray();
				if (f.exists()) {
					testPassed = compareXmlResults(f, results, testFailureMsg);
				}				
				Files.write(results, f);

				Gson gson = new GsonBuilder().setPrettyPrinting()
								.disableHtmlEscaping().serializeNulls().create();
				File jsonFile = new File(getJsonDirectory() + "/" + testCase.getName() + ".json");
				jsonFile.getParentFile().mkdirs();
				jsonFile.setWritable(true);
				FileWriter fileWriter = new FileWriter(jsonFile);
				gson.toJson(jestDataModel, fileWriter);
				fileWriter.flush();
				fileWriter.close();

			} catch (Throwable afe) {
				testFailureMsg.append("\n\nTESTCASE: " + testCase.getName() + " has failed\n");
				testFailureMsg.append("\n" + getStackTrace(afe));
				testPassed = false;
			}


			// If any test case resulted in mismatched results, fail now.
			if (testPassed && firstValueMismatchMessage != null) {
				testFailureMsg.append("\n\nTESTCASE: " + testCase.getName() +
						" results from different methods do not match.  Check gold file. First mismatch is:\n" +
						firstValueMismatchMessage + "\n");
				testPassed = false;
			}
			return testPassed;
		}
		
		protected boolean compareXmlResults(File f, byte[] xml, StringBuilder testFailureMsg) throws IOException {
			byte[] contents = Files.asByteSource(f).read();

			if (!Arrays.equals(contents, xml)) {
				testFailureMsg.append("\n\nTESTCASE: " + testCase.getName() +
						" results changed from expected results.  Check gold file.\n");
				appendDiffSnippet(contents, xml, testFailureMsg);
				return false;
			} else {
				return true;
			}

		}
		
		/**
		 * Append a diff snippet to go into the error log to make debugging easier if running remotely
		 * @param goldfile the contents of the goldfile
		 * @param results the contents of the result of the run
		 * @param testFailureMsg the failure message to append the diff to.
		 */
		protected void appendDiffSnippet(byte[] goldfile, byte[] results, StringBuilder testFailureMsg) throws IOException {
			List<String> before = CharSource.wrap(new String(goldfile, Charsets.UTF_8)).lines().collect(Collectors.toList());
			List<String> after = CharSource.wrap(new String(results, Charsets.UTF_8)).lines().collect(Collectors.toList());
			
			com.github.difflib.patch.Patch<String> patch = DiffUtils.diff(before, after);
			List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(testCase.getName(), testCase.getName()+".results", before, patch, 1);
			for (int i = 2; i < Math.min(20, unifiedDiff.size()); i++) {
				testFailureMsg.append(unifiedDiff.get(i)).append('\n');
			}
			
		}
		
		
		/**
		 * Print the evaluation expressions at the beginning of the goldfile.
		 * @param fieldInfo the formula field
		 * @param xmlOut the output
		 * @param jestDataModel the Jest Datamodel
		 * @throws Exception if a problem occurs
		 */
		protected void outputEvalutionExpressions(FieldDefinitionInfo fieldInfo, PrintStream xmlOut, JestDataModel jestDataModel) throws Exception {
			if (this.suite.shouldTestSql()) {
				printOutputSql(fieldInfo, xmlOut, true);
				printOutputSql(fieldInfo, xmlOut, false);
			}
			if (this.suite.shouldTestJavascript()) {
				printOutputJavascript(fieldInfo, xmlOut, true, false, jestDataModel);
				printOutputJavascript(fieldInfo, xmlOut, true, true, jestDataModel);
				printOutputJavascript(fieldInfo, xmlOut, false, false, jestDataModel);
				printOutputJavascript(fieldInfo, xmlOut, false, true, jestDataModel);
			}
		}

		/**
		 * Run one instance of the test case.  Separate instances are created for swapped operands and
		 * different numeric type combinations.  Each instance runs multiple evaluation methods
		 * against multiple input cases.
		 *
		 * Returns a non-null message if any data row resulted in different results for different paths.
		 * Any other error will throw back to caller and terminate this test case.
		 * @param instance the test case
		 * @param testData the data for the test
		 * @param entityRecId the ID of the entity if storing in a DB row
		 * @param testFailureMsg the buffer for adding an error message
		 * @param xmlOut the xml output
		 * @param jestDataModel the Jest Datamodel
		 * @return an error message if there is any 
		 * @throws Exception if an error occurred
		 */
		protected String runTestInstance(FormulaTestRunnable instance, List<List<String>> testData, String entityRecId,
				StringBuilder testFailureMsg, PrintStream xmlOut, JestDataModel jestDataModel) throws Exception {

			String firstTestFailMessage = null;
			FieldDefinitionInfo fieldInfo = instance.getTcFieldInfo();
			TestCaseStats stats = new TestCaseStats();
			try {
				createTestFormulas(fieldInfo, entityRecId);

				outputEvalutionExpressions(fieldInfo, xmlOut, jestDataModel);

				List<FieldDefinitionInfo> fieldList = instance.getFieldNames();
				if (fieldList.size() > 0) {
					for (List<String> testDataRow : testData) {
						if (instance.isSwapped()) {
							Collections.reverse(testDataRow);
						}

						setDataForRec(fieldList, testDataRow, entityRecId, jestDataModel);
						String failMessage = getTestCaseResults(testDataRow, fieldInfo, entityRecId, xmlOut, jestDataModel, stats);
						if (failMessage != null && firstTestFailMessage == null) {
							firstTestFailMessage = failMessage;
						}

						if (instance.isSwapped()) {
							Collections.reverse(testDataRow);
						}
					}
				} else {
					firstTestFailMessage = getTestCaseResults(null, fieldInfo, entityRecId, xmlOut, jestDataModel, stats);
				}

			} finally {
				cleanupTest();
			}
			if (firstTestFailMessage == null) {
			    firstTestFailMessage = validateTestCaseStats(instance, stats, xmlOut);
			}
			
			return firstTestFailMessage;
		}

		/**
		 * Allow validation of testCaseStats after running all of the requests (like how many failures in JS/SQL there should be
		 * @param instance the current running test
		 * @param stats the current stats for the test
		 * @param xmlOut where to output the erro
		 * @return the error if the test case stats do not correspond with the expected outcome
		 */
		protected String validateTestCaseStats(FormulaTestRunnable instance, TestCaseStats stats, PrintStream xmlOut) {
		    return null;
		}
        
		
		/**
		 * Run all the versions of the test for one data row, verify results, and write to printstream.
		 * @return  null if the results match, otherwise an error message explaining the failure.
		 */
		private String getTestCaseResults(
				List<String> testDataRow, FieldDefinitionInfo fieldInfo, String entityRecId, PrintStream out, JestDataModel jestDataModel, TestCaseStats stats)
						throws Exception {

			Map<String, String> results = new HashMap<>();
			getResultsViaMultiplePaths(results, fieldInfo, entityRecId);
			String mismatchMessage = (getTestCaseInfo().getCompareType() == CompareType.None) ?
					null : verifyResults(fieldInfo, results, stats);

			out.println("      <result>");
			if (mismatchMessage != null) {
				out.println("      <!-- Test Case results don't match: " + mismatchMessage + " -->");
			}
			out.println("      <inputvalues>"
					+ FormulaTextUtil.escapeToXml(testDataRow != null ? testDataRow.toString() : "No data") + "</inputvalues>");

			// To avoid depending on map order, or breaking all existing gold files by sorting, we ask the test generator for the keynames
			// in the expected order.
			for (String key: getKeyNames()) {

				String resultForKey = results.getOrDefault(key, "not-tested");
				out.println(String.format("         <%s>%s</%s>",
						key, FormulaTextUtil.escapeToXml(resultForKey), key));

				if("formula".equals(key) && !jestDataModel.tests.isEmpty()) {
					int index = jestDataModel.tests.size() > 0 ? jestDataModel.tests.size() - 1: 0;
					jestDataModel.tests.get(index).output = resultForKey;
				}

			}
			out.println("      </result>");

			// Write out all test case failures to the gold file, but skip Javascript failures if
			// it's registered to be ignored
			if (getSuite().shouldTestJavascript() && ignoreJavascriptValueMismatchInAutobuilds() && mismatchMessage != null &&
					(mismatchMessage.contains("Javascript had an error when no other did:") ||
							(!mismatchMessage.contains("viaEntity") && !mismatchMessage.contains("viaApi") && !mismatchMessage.contains("viaTemplate")
							        && !mismatchMessage.contains("viaSql")
							        && !mismatchMessage.contains("SQL had an error")) ||
							(mismatchMessage.contains("If one is null, they all should be null")))) {
				return null;
			}
			
			return mismatchMessage;
		}

		/**
		 * Get formula sizes for both SQL and guard and print them in the results file.
		 *
		 * These formula sizes are evaluated for Oracle and therefore different for
		 * Postgres DB. Please talk to Declarative App Builder team for further assistance.
		 */
		private void printOutputJavascript(FieldDefinitionInfo fieldInfo, PrintStream xmlOut, boolean highPrec, boolean nullAsNull, JestDataModel jestDataModel) throws Exception {
			FormulaTypeSpec type = nullAsNull ? MockFormulaType.JAVASCRIPT_NULLASNULL: MockFormulaType.JAVASCRIPT;
			FormulaRuntimeContext formulaContext =  new MapFormulaContext(super.setupMockContext(fieldInfo.getReturnType()), mapEntity, type, null);
			formulaContext.setProperty(FormulaContext.HIGHPRECISION_JS, highPrec);
			RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(type, formulaContext, fieldInfo.getFormula());
			Formula formula = formulaInfo.getFormula();

			xmlOut.print("    <JsOutput highPrec=\""+highPrec+"\" nullAsNull=\""+nullAsNull+"\">");
			xmlOut.print(FormulaTextUtil.escapeToXml(formula.toJavascript()));
			xmlOut.println("</JsOutput>");

			JSCode jscode = new JSCode();
			jscode.isHighPrecision = highPrec;
			jscode.isNullAsNull = nullAsNull;
			jscode.javascript = formula.toJavascript();
			jestDataModel.jsCodes.add(jscode);
		}
		

		/**
		 * Get formula sizes for both SQL and guard and print them in the results file.
		 *
		 * These formula sizes are evaluated for Oracle and therefore different for
		 * Postgres DB. Please talk to Declarative App Builder team for further assistance.
		 */
		private void printOutputSql(FieldDefinitionInfo fieldInfo, PrintStream xmlOut, boolean nullAsNull) throws Exception {
			FormulaTypeSpec type = nullAsNull ? MockFormulaType.DEFAULT: MockFormulaType.NULLASNULL;
			FormulaRuntimeContext formulaContext =  new MapFormulaContext(super.setupMockContext(fieldInfo.getReturnType()), mapEntity, type, null);
			RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(type, formulaContext, fieldInfo.getFormula());
			FormulaWithSql formula = (FormulaWithSql) formulaInfo.getFormula();

			xmlOut.println("    <SqlOutput nullAsNull=\""+nullAsNull+"\">");
			xmlOut.print("       <Sql>");
			xmlOut.print(FormulaTextUtil.escapeToXml(formula.getSQLRaw()));
			xmlOut.println("</Sql>");
			xmlOut.print("       <Guard>");
			xmlOut.print(FormulaTextUtil.escapeToXml(formula.getGuard()));
			xmlOut.println("</Guard>");
			xmlOut.println("    </SqlOutput>");
		}


		// This should return if it correctly fails to create the formula and
		// throw exception if it creates it, or fails for some unexpected reason.
		abstract protected void createFail(FormulaTestCaseInfo testCase, FieldDefinitionInfo fieldInfo) throws Exception;

		// Get the directory where we will store the gold files.
		abstract protected String getDirectory();

		// Get the directory where we will store JEST test files.
		abstract protected String getJsonDirectory();

		// Create whatever formula fields etc are needed for this test case.
		abstract protected void createTestFormulas(FieldDefinitionInfo fieldInfo, String entityRecId) throws Exception;

		// Cleanup whatever was created by above.
		abstract protected boolean cleanupTest() throws Exception;

		// Does the actual evaluations via all required methods, and returns them in supplied map, where key is a name describing the method
		abstract protected void getResultsViaMultiplePaths(Map<String, String> results, FieldDefinitionInfo fieldInfo, String entityRecId) throws Exception;

		// Should we ignore SQL differences for this change
		abstract protected WhyIgnoreSql getWhyIgnoreSql();

		
		/**
		 * Do verifications as defined by the compareContexts attribute.
		 * @return  null if verification passed, otherwise a String explaining the failure.
		 */
		abstract String verifyResults(FieldDefinitionInfo fieldInfo, Map<String, String> results, TestCaseStats stats) throws Exception;

		// Returns the keys in the results map in the order to put them in the gold file
		abstract protected String[] getKeyNames();

		/**
		 * @return true to log a mismatch in the generated Javascript value, but don't produce a test failure in the autobuilds.
		 * Autobuilds are integration tests.
		 * Necessary so that we can iteratively close gaps in the generated Javascript formula string.
		 */
		protected boolean ignoreJavascriptValueMismatchInAutobuilds() {
			return false;
		}

		private String getStackTrace(Throwable e) {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			PrintStream out = new PrintStream(bout);
			e.printStackTrace(out);
			out.flush();
			String trace = bout.toString();
			out.close();
			return trace.substring(0, Math.min(2300, trace.length()));
		}

		private void printChildFormulaDetails(FormulaTestCaseInfo testCase, PrintStream xmlOut) {
			for (FieldDefinitionInfo fieldInfo : testCase.getReferenceFields()) {
				if (fieldInfo.isFormula()) {
					xmlOut.print("   <" + fieldInfo.getDevName());
					xmlOut.print(" dataType=\"" + fieldInfo.getReturnType() + "\"");
					xmlOut.print(" formula=\"" + FormulaTextUtil.escapeToXml(fieldInfo.getFormula()) + "\"");
					xmlOut.println("/>");
				}
			}
		}

		/**
		 *
		 * @param fieldDataType
		 * @param data -
		 *            a string value of data to be converted to object.
		 */
		private Object getDataObject(FormulaDataType fieldDataType, String data) throws Exception {
			if (fieldDataType.isNumber()) {
				if ("NULL".equals(data)) return null;
				return new BigDecimal(data.trim());
			} else if (fieldDataType.isDate()) {
				return getDateObject(data.trim(), fieldDataType);
			} else if (fieldDataType.isBoolean()) {
				return Boolean.valueOf(data.trim());
			} else
				return data;
		}

		/*
		 * Creates a Date Object from a String value of format YYYY:MM:DD:hh:mm:ss
		 */
		public Date getDateObject(String dateString, FormulaDataType fieldDataType) {
			myCal.clear();

			if (dateString == null || dateString.length() == 0) return myCal.getTime();

			StringTokenizer stDate = new StringTokenizer(dateString, ":");
			int year = stDate.hasMoreTokens() ? Integer.parseInt(stDate.nextToken()) : 2004;
			int month = stDate.hasMoreTokens() ? Integer.parseInt(stDate.nextToken()) - 1 : 0;
			int dayOfMonth = stDate.hasMoreTokens() ? Integer.parseInt(stDate.nextToken()) : 1;
			int hourOfDay = stDate.hasMoreTokens() ? Integer.parseInt(stDate.nextToken()) : 0;
			int minutes = stDate.hasMoreTokens() ? Integer.parseInt(stDate.nextToken()) : 0;
			int seconds = stDate.hasMoreTokens() ? Integer.parseInt(stDate.nextToken()) : 0;
			TimeZone timeZone = stDate.hasMoreTokens() ? TimeZone.getTimeZone(stDate.nextToken()) : null;
			if (timeZone == null) {
				timeZone = TimeZone.getDefault();
			}
			myCal.setTimeZone(timeZone);
			if (fieldDataType.isDateOnly()) {
				// remove the time part:
				myCal.set(year, month, dayOfMonth, 0, 0, 0);
			} else {
				myCal.set(year, month, dayOfMonth, hourOfDay, minutes, seconds);
			}

			return myCal.getTime();
		}


		private void setDataForRec(List<FieldDefinitionInfo> fieldList, List<String> testDataRow, 
				String entityRecId2, JestDataModel jestDataModel) throws Exception {

			Map<String, Object> fieldData = new HashMap<String, Object>();
			TestData testData = new TestData();

			for (int i = 0; i < fieldList.size(); i++) {
				String fieldName = fieldList.get(i).getReferenceName();
				if (testDataRow.get(i) != null && testDataRow.get(i).length() > 0) {
					fieldData.put(fieldName, getDataObject(fieldList.get(i).getReturnType(),
							testDataRow.get(i)));
					testData.inputs.put(fieldName, testDataRow.get(i));
				} else {
					fieldData.put(fieldName, null);
					testData.inputs.put(fieldName, null);
				}
			}
			jestDataModel.tests.add(testData);

			if (fieldData.size() > 0) {
				// Version > 156 will fail with STRING_TOO_LONG error
				objects.put(entityRecId2, fieldData);
			}
		}
		

		// Adjust numeric results so gold file doesn't differ between machine architectures.
		// This is used for template and visualforce evaluations which don't know about the precision specified
		// in the test file.
		protected String fixNumericString(FieldDefinitionInfo fieldInfo, String result) {
			if (result != null && result.length() != 0) {
				final CompareType compareType = getTestCaseInfo().getCompareType();
				if (compareType == CompareType.Number || compareType == CompareType.Approximate) {
					try {
						BigDecimal round = new BigDecimal(result.replace(",", ""));
						// JVM java.lang.Math.* method results vary slightly between architectures and versions.
						// This round to the precision requested in the test and (hopefully) also rounds these differences away.
						round = round.round(new MathContext(fieldInfo.getPrecision(), RoundingMode.HALF_UP));
						result = round.toPlainString();
					} catch (Exception x) {
                        // ignore, just return the original result unaltered
					    return result;
					}
				}
			}
			return result;
		}

		protected Map<String,Object> getData(String id) {
			return objects.get(id);
		}

		private final FormulaTestCaseInfo testCase;
		private final boolean positive;
		private static final Calendar myCal = Calendar.getInstance();

	}

	/**
	 * @param  formulaEvalResult  The String to be checked for one of several kinds of error message.
	 * @return  Ideally, this should return true for any input that is not a valid formula evaluation result.
	 */
	protected static boolean hasErrorMessage(String formulaEvalResult) {
		if (formulaEvalResult == null) {
			return false;
		}
		if (formulaEvalResult.length() == 0) {
			return false;
		}
		if (formulaEvalResult.contains(errorResultKey)) {
			return true;
		}
		if (formulaEvalResult.contains(otherErrorKey)) {
			return true;
		}
		if (formulaEvalResult.contains(notValidForKey)) {
			return true;
		}
		if (formulaEvalResult.contains(notValidNumKey)) {
			return true;
		}
		if (formulaEvalResult.contains(outOfRangeKey)) {
			return true;
		}
		if (formulaEvalResult.contains(apexPDevEx)) {
			return true;
		}
		if (formulaEvalResult.contains(javascriptEx)) {
			return true;
		}
		return false;
	}

	//  Example:  "Error: java.lang.ArithmeticException"
	protected static final String errorResultKey = "Error:";
	//  Example:  "Arithmetic error: Division undefined"
	private static final String otherErrorKey = " error:";
	//  Example:  "The value 'whatever' is not valid for operator '>'"
	private static final String notValidForKey = "is not valid for";
	//  Example:  "The value 'abc' is not a valid number"
	private static final String notValidNumKey = "is not a valid number";
	//  Examples:  "Out of range argument to POWER() function"
	//             "Month or Day out of range in DATE() function"
	private static final String outOfRangeKey = "ut of range ";
	//  Not 100% sure this should be shown to our customers, but currently it is:
	private static final String apexPDevEx = "ApexPagesDeveloperException";
	//  Not 100% sure this should be shown to our customers, but currently it is:
	private static final String javascriptEx = "javax.script.ScriptException";


	public static class FormulaTestRunnable {

		private FieldDefinitionInfo tcFieldInfo;
		// remove
		private List<FieldDefinitionInfo> referenceFields = new LinkedList<FieldDefinitionInfo>(); //only needs devname and return type

		private String testCaseName;
		private boolean isSwapped;
		private String error;

		public FormulaTestRunnable(FieldDefinitionInfo formulaFieldInfo, String testCaseName) {
			super();

			this.testCaseName = testCaseName;
			this.tcFieldInfo = formulaFieldInfo;
			this.isSwapped = Boolean.FALSE;
			this.error = null;
		}

		public FormulaTestRunnable() {
			this(null, null);
		}

		public FormulaTestRunnable(FieldDefinitionInfo formulaFieldInfo, String testCaseName,
				List<FieldDefinitionInfo> referenceFields) {
			this(formulaFieldInfo, testCaseName);
			this.referenceFields = referenceFields;
		}

		public FieldDefinitionInfo getTcFieldInfo() {
			return this.tcFieldInfo;
		}

		public void setTcFieldInfo(FieldDefinitionInfo tcFieldInfo) {
			this.tcFieldInfo = tcFieldInfo;
		}

		public String getTestCaseName() {
			return this.testCaseName;
		}

		// remove
		public List<FieldDefinitionInfo> getFieldNames() {
			return this.referenceFields;
		}

		public void setFieldNames(List<FieldDefinitionInfo> referenceFields) {
			this.referenceFields = referenceFields;
		}

		public void addReferenceField(FieldDefinitionInfo fieldInfo) {
			this.referenceFields.add(fieldInfo);
		}

		public boolean isSwapped() {
			return isSwapped;
		}

		public void setIsSwapped(boolean swapped) {
			this.isSwapped = swapped;
		}

		public String getError() {
			return this.error;
		}

		public void setError(String error) {
			this.error = error;
		}

        @Override
        public String toString() {
            return "FormulaTestRunnable [name" + testCaseName + ",field=" + tcFieldInfo + "]";
        }
    }

	/**
	 * Allows pluggable testing of function evaluation through sql.  If you want fancier stuff, you can
	 * override the whole class.
	 * @since 0.1.0
	 */ 
	public interface DbTester extends AutoCloseable {
		 String getDbTypeName();
		
		/**
		 * Evaluate the sql for the formula using sql
		 * @param formulaContext the formula context
		 * @param entityObject an object representing the values to use for the particular row.  You may want to
		 * insert this row into the DB when doing the valuation to make the DB substitution easier
		 * @param formulaSource the source of the formula
		 * @param nullAsNull whether null is treated as null or as blank/0
		 * @return the result of evaluating the formula using a sql engine
		 * @throws IOException if there is an IO issue with the sql engine
		 * @throws SQLException if there is an issue evaluating the sql
		 * @throws FormulaException if there is an issue evaluating the formula
		 */
		String evaluateSql(String name, FormulaRuntimeContext formulaContext, Object entityObject, String formulaSource, boolean nullAsNull) throws IOException, SQLException, FormulaException;

		/**
		 * Some JDBC drivers include a UUID in all exceptions, so this allows you to "remove" that.
		 * @param e the exception thrown during evaluation
		 * @return the message to use in GoldFiles for the exception.
		 * @throws ArithmeticException if you want to treat this exception as a NULL instead of an error.  Suitable
		 * if there is an architectural difference when running on different platforms (Linux vs Mac)
		 */
		default String getSqlExceptionMessage(Throwable e) {
		    return e.getMessage();
		}
		
		// Support close, if necessary.
		@Override
		default void close() throws Exception {
		}
		
		
	}

}
