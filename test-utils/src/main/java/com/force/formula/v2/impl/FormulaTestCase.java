
package com.force.formula.v2.impl;

import com.force.formula.FormulaTestBase;
import com.force.formula.impl.MapFormulaContext;
import com.force.formula.v2.FormulaXMLTestSuite;
import com.force.formula.v2.data.FormulaFieldDefinition;
import com.force.formula.v2.data.FormulaTestData;
import com.force.formula.v2.data.FormulaTestDefinition;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        for(FormulaTestData testData: this.testCase.getTestData()){
            for(String executionPath: this.testCase.getExecutionPaths()){
                if(ExecutionPaths.get(executionPath)!=null){
                    String output = ExecutionPaths.get(executionPath)
                            .execute(this.testCase.getTestCaseFieldInfo().getFormula(),
                                    this.testCase.getTestCaseFieldInfo().getDataType(),
                                    testData.getInput(),
                                    this.testEntity,
                                    this.testSuite.getDbTester());
                    assertEquals(this.testCase.getTestName() + " failed for execution path " + executionPath,
                            testData.getExpectedOutput().get(executionPath),
                            output);
                }
            }
        }
    }

    private void compareGoldFileOutput(){
        getGeneratedSqlAndJs();
     /*   try {
            File existingGoldFile = new File(this.testSuite.getGoldFileDirectoryPath() + "/" + this.testCase.getTestName()+ ".xml");
            existingGoldFile.getParentFile().mkdirs();

            byte[] results = getGeneratedSqlAndJs().getBytes();
            if (existingGoldFile.exists()) {
                testPassed = compareXmlResults(f, results, testFailureMsg);
            }
            //Write the new contents to the existing gold file
            Files.write(results, existingGoldFile);

        } catch (Throwable afe) {

            //assert failure
        } */
    }

    private String getGeneratedSqlAndJs(){
      /*  for(String executionPath: this.testCase.getExecutionPaths()){
            if(ExecutionPaths.get(executionPath)!=null) {

            }
        }*/
        return null;
    }

}
