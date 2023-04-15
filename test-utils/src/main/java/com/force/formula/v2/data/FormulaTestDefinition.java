package com.force.formula.v2.data;

import com.force.formula.v2.Utils;
import org.apache.commons.lang3.Validate;

import java.util.List;

/**
 * This class is used to hold all the testcase information that is defined in the testcase xml definition.
 */
public class FormulaTestDefinition {

    /**
     * A unique name for the test. Required Parameter.
     */
    private final String testName;

    /**
     * This field holds the formula field info which needs to be tested. Required Parameter.
     */
    private final FormulaFieldDefinition testCaseFieldInfo;

    /**
     * This is the list of all reference fields needed in the formula field which is getting tested.
     */
    private final List<FormulaFieldDefinition> referenceFields;

    /**
     * This is the list of all execution paths that you need to get tested. E.g. formula, javascript, sql, etc.
     * Required Parameter.
     */
    private final List<String> executionPaths;

    /**
     * This is the data that will be used for testing. It contains input values and expected output values for each
     * execution path. Required Parameter.
     */
    private final List<FormulaTestData> testData;

    /**
     * This is a derived field that contain the ordered list of all dependent custom fields
     */
    private final List<FormulaFieldDefinition> flattenedFieldList;

    /**
     * Creates an object of FormulaTestDefinition using the test definition provided in the test xml file
     *
     * @param testName a unique test name as defined in the test xml file
     * @param testCaseFieldInfo formula field definition for which the test needs to be executed
     * @param referenceFields ordered list of reference fields on which the formula of the formula field depends
     * @param executionPaths ordered list of execution paths for which the test needs to be executed
     * @param testData a collection of multiple test inputs for which the test needs to be executed
     */
    public FormulaTestDefinition(String testName, FormulaFieldDefinition testCaseFieldInfo, List<FormulaFieldDefinition> referenceFields, List<String> executionPaths, List<FormulaTestData> testData) {
        Validate.notEmpty(testName,"testName is a required parameter and  cannot be null or empty");
        Validate.notNull(testCaseFieldInfo, "testCaseFieldInfo is a required parameter and cannot be null");
        Validate.notEmpty(executionPaths, "executionPaths is a required parameter and cannot be null or empty");
        Validate.notEmpty(testData, "testData is a required parameter and cannot be null or empty");
        this.testName = testName;
        this.testCaseFieldInfo = testCaseFieldInfo;
        this.referenceFields = referenceFields;
        this.executionPaths = executionPaths;
        this.testData = testData;
        this.flattenedFieldList = Utils.flattenFieldList(referenceFields);
    }

    /**
     * Gets the test name for this FormulaTestDefinition
     * @return the test name for the FormulaTestDefinition
     */
    public String getTestName() {
        return testName;
    }

    /**
     * Gets the formula field definition for which the test needs to be executed
     * @return the formula field definition
     */
    public FormulaFieldDefinition getTestCaseFieldInfo() {
        return testCaseFieldInfo;
    }

    /**
     * Gets the ordered list of reference fields on which the formula depends
     * @return the ordered list of reference fields used in the formula
     */
    public List<FormulaFieldDefinition> getReferenceFields() {
        return referenceFields;
    }

    /**
     * Gets the ordered list of execution paths as defined in the test xml file
     * @return the ordered list of execution paths as defined in the test xml file
     */
    public List<String> getExecutionPaths() {
        return executionPaths;
    }

    /**
     * Gets the flattened ordered list of all the reference fields on which the formula depends to create them for testing
     * @return an ordered list of reference fields that needs to be created to execute the test
     */
    public List<FormulaFieldDefinition> getAllFieldsToCreate(){
        return this.flattenedFieldList;
    }

    /**
     * Gets the test data for which the output will be generated and compared against
     * @return a list of test input using which the test will be executed
     */
    public List<FormulaTestData> getTestData() {
        return testData;
    }
}

