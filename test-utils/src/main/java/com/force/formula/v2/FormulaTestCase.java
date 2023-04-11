
package com.force.formula.v2;

import com.force.formula.FormulaTestBase;
import com.force.formula.impl.MapFormulaContext;
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

public class FormulaTestCase extends FormulaTestBase {

    private final FormulaTestDefinition testCase;
    private final MapFormulaContext.MapEntity testEntity;
    private final FormulaXMLTestSuite testSuite;

    public FormulaTestCase(FormulaTestDefinition testCase, FormulaXMLTestSuite testSuite) {
        super(testCase.getTestName());
        this.testCase = testCase;
        this.testEntity = createTestEntity(testCase);
        this.testSuite = testSuite;
    }

    private MapFormulaContext.MapEntity createTestEntity(FormulaTestDefinition testCase){
        //In opensource, fields are created like following and are added to a testEntity which is composed of these fields
        //Creating MapFieldInfo is equivalent to creating custom fields in core.
        List<FormulaFieldDefinition> fieldsToCreate = testCase.getAllFieldsToCreate();
        Map<String, MapFormulaContext.MapFieldInfo> fields = new HashMap<>();
        for (FormulaFieldDefinition fieldInfo : fieldsToCreate) {
            fields.put(fieldInfo.getFieldName(),
                    new MapFormulaContext.MapFieldInfo(fieldInfo.getFieldName(), fieldInfo.getDataType(), fieldInfo.getFormula(), 1));
        }
        return new MapFormulaContext.MapEntity("testEntity", fields.values());
    }

    @Override
    public void runTest(){
        runTestCase();
        compareGoldFileOutput();
    }

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
                            testData.getExpectedOutput().get(executionPath),
                            output);
                }
            }
        }
    }

    private void compareGoldFileOutput(){
        File existingGoldFile = new File(this.testSuite.getGoldFileDirectoryPath() + "/" + this.testCase.getTestName()+ ".xml");
        existingGoldFile.getParentFile().mkdirs();

        byte[] results = generateGoldFileContents().getBytes();
        boolean isTestPassed = true;
        StringBuilder testFailureMessage = new StringBuilder();

        try{
            if (existingGoldFile.exists()) {
                isTestPassed = compareXmlResults(existingGoldFile, results, testFailureMessage);
            }
            //Write the new contents to the existing gold file
            Files.write(results, existingGoldFile);
        }catch (IOException ex){
            fail("Exception occured while comparing gold file output"+ ex.getMessage());
        }
        assertTrue(testFailureMessage.toString(), isTestPassed);
    }

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

    protected boolean compareXmlResults(File f, byte[] xml, StringBuilder testFailureMsg) throws IOException {
        byte[] contents = Files.asByteSource(f).read();

        if (!Arrays.equals(contents, xml)) {
            testFailureMsg.append("\n\nTESTCASE: " + this.testCase.getTestName() +
                    " Changes identified in generated SQL and Javascript.  Check gold file.\n");
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
        List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(this.testCase.getTestName(), this.testCase.getTestName()+".results", before, patch, 1);
        for (int i = 2; i < Math.min(20, unifiedDiff.size()); i++) {
            testFailureMsg.append(unifiedDiff.get(i)).append('\n');
        }
    }

}
