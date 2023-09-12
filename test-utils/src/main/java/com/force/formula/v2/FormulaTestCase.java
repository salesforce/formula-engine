
package com.force.formula.v2;

import com.force.formula.FormulaTestBase;
import com.force.formula.impl.MapFormulaContext;
import com.force.formula.util.FormulaTextUtil;
import com.force.formula.v2.data.FormulaFieldDefinition;
import com.force.formula.v2.data.FormulaTestData;
import com.force.formula.v2.data.FormulaTestDefinition;
import com.force.formula.v2.impl.ExecutionPaths;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Each test definition corresponds to a formula testcase which will be run by the test framework. Each formula testcase
 * is part of some test suite that provides the testcase a database tester, sets appropriate properties and paths.
 *
 * Every testcase executes the formula for each defined execution path and then compares its output to the expected
 * output. It also generates a gold file containing sql and javascript code that is compared with the earlier gold file
 * output to show differences if any.
 *
 */
public class FormulaTestCase extends FormulaTestBase {

    private final FormulaTestDefinition testCase;
    private final MapFormulaContext.MapEntity testEntity;
    private final FormulaXMLTestSuite testSuite;

    /**
     * A constructor to create an instance of formula test case from a formula test definition
     *
     * @param testCase formula test definition
     * @param testSuite formula test suite which will contain this formula test case
     */
    public FormulaTestCase(FormulaTestDefinition testCase, FormulaXMLTestSuite testSuite) {
        super(testCase.getTestName());
        this.testCase = testCase;
        this.testEntity = createTestEntity(testCase);
        this.testSuite = testSuite;
    }

    /**
     * Creates a test entity from the provided formula test definition to be used for evaluating formula output
     *
     * @param testCase formula test definition
     * @return a test entity from the provided formula test definition to be used for evaluating formula output
     */
    private MapFormulaContext.MapEntity createTestEntity(FormulaTestDefinition testCase){
        //In opensource, fields are created like following and are added to a testEntity which is composed of these fields
        //Creating MapFieldInfo is equivalent to creating custom fields in core.
        List<FormulaFieldDefinition> fieldsToCreate = testCase.getAllFieldsToCreate();
        Map<String, MapFormulaContext.MapFieldInfo> fields = new HashMap<>();
        for (FormulaFieldDefinition fieldInfo : fieldsToCreate) {
            fields.put(fieldInfo.getFieldName(),
                    new MapFormulaContext.MapFieldInfo(fieldInfo.getFieldName(), fieldInfo.getDataType(), fieldInfo.getFormula(), fieldInfo.getScale()));
        }
        return new MapFormulaContext.MapEntity("testEntity", fields.values());
    }

    /**
     * Its the method that gets executed by the test framework to run this test case
     */
    @Override
    public void runTest(){
        compareGoldFileOutput();
        runTestCase();
    }

    /**
     * Iterates over all execution paths defined for this test case for each of the test data and compare against
     * expected outputs
     */
    private void runTestCase(){
        for(String executionPath: this.testCase.getExecutionPaths()){
            if(ExecutionPaths.get(executionPath)!=null){
                for(FormulaTestData testData: this.testCase.getTestData()){
                    String output = ExecutionPaths.get(executionPath)
                            .execute(this.testCase.getTestCaseFieldInfo().getFormula(),
                                    this.testCase.getTestCaseFieldInfo().getDataType(),
                                    testData.getInput(),
                                    this.testEntity,
                                    this.testSuite.getDbTester());
                    assertEquals(this.testCase.getTestName() + " failed for execution path: " + executionPath
                                + " and for testData: " + testData,
                            FormulaTextUtil.escapeToXml(testData.getExpectedOutput().get(executionPath)),
                            FormulaTextUtil.escapeToXml(output).trim());
                }
            }
        }
    }

    /**
     * Compares the new gold files with the old ones to show differences if any
     */
    private void compareGoldFileOutput(){
        File existingGoldFile = new File(this.testSuite.getGoldFileDirectoryPath() + "/" + this.testCase.getTestName()+ ".xml");
        existingGoldFile.getParentFile().mkdirs();

        byte[] newGoldFileOutput = generateGoldFileContents().getBytes();
        boolean isTestPassed = true;
        StringBuilder testFailureMessage = new StringBuilder();

        try{
            if (existingGoldFile.exists()) {
                isTestPassed = compareXmlResults(existingGoldFile, newGoldFileOutput, testFailureMessage);
            }
            //Write the new contents to the existing gold file
            Files.write(newGoldFileOutput, existingGoldFile);
        }catch (IOException ex){
            fail("Exception occured while comparing gold file output"+ ex.getMessage());
        }
        assertTrue(testFailureMessage.toString(), isTestPassed);
    }

    /**
     * Generates the gold file output for this test case
     *
     * @return a gold file output for this test case
     */
    private String generateGoldFileContents(){
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<testCase name=\"" + this.testCase.getTestName() + "\">\n");
        List<String> executionPaths = this.testCase.getExecutionPaths();
        Collections.sort(executionPaths);
        for(String executionPath: executionPaths){
            if(ExecutionPaths.get(executionPath)!=null) {
                sb.append(ExecutionPaths.get(executionPath).generateGoldFileOutput(this.testCase.getTestCaseFieldInfo().getFormula(),
                        this.testCase.getTestCaseFieldInfo().getDataType(),
                        this.testEntity));
            }
        }
        sb.append("</testCase>\n");
        return sb.toString();
    }

    /**
     * Compares an existing gold file contents with the newly generates gold file output
     *
     * @param existingGoldFile existing gold file
     * @param newGoldFileOutput new generates gold file output
     * @param testFailureMsg string builder to be used to append any failure messages that happen while comparing
     * @return true if there is no mismatch between existing gold file and new generates gold file output, else false
     * @throws IOException if there is an issue while reading the byte inputs
     */
    protected boolean compareXmlResults(File existingGoldFile, byte[] newGoldFileOutput, StringBuilder testFailureMsg) throws IOException {
        byte[] existingGoldFileContents = Files.asByteSource(existingGoldFile).read();

        if (!Arrays.equals(existingGoldFileContents, newGoldFileOutput)) {
            testFailureMsg.append("\n\nTESTCASE: " + this.testCase.getTestName() +
                    " Changes identified in generated SQL and Javascript.  Check gold file.\n");
            appendDiffSnippet(existingGoldFileContents, newGoldFileOutput, testFailureMsg);
            return false;
        } else {
            return true;
        }

    }

    /**
     * Append a diff snippet to go into the error log to make debugging easier if running remotely
     *
     * @param existingGoldFileContents the contents of the goldfile
     * @param newGoldFileOutput the contents of the new gold file output of the run
     * @param testFailureMsg the failure message to append the diff to
     * @throws IOException if there is an issue while reading the byte inputs
     */
    protected void appendDiffSnippet(byte[] existingGoldFileContents, byte[] newGoldFileOutput, StringBuilder testFailureMsg) throws IOException {
        List<String> before = CharSource.wrap(new String(existingGoldFileContents, Charsets.UTF_8)).lines().collect(Collectors.toList());
        List<String> after = CharSource.wrap(new String(newGoldFileOutput, Charsets.UTF_8)).lines().collect(Collectors.toList());

        com.github.difflib.patch.Patch<String> patch = DiffUtils.diff(before, after);
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(this.testCase.getTestName(), this.testCase.getTestName()+".results", before, patch, 1);
        for (int i = 2; i < Math.min(20, unifiedDiff.size()); i++) {
            testFailureMsg.append(unifiedDiff.get(i)).append('\n');
        }
    }

}
