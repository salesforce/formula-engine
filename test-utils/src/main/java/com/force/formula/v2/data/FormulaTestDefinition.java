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

    public String getTestName() {
        return testName;
    }

    public FormulaFieldDefinition getTestCaseFieldInfo() {
        return testCaseFieldInfo;
    }

    public List<FormulaFieldDefinition> getReferenceFields() {
        return referenceFields;
    }

    public List<String> getExecutionPaths() {
        return executionPaths;
    }

    public List<FormulaFieldDefinition> getAllFieldsToCreate(){
        return this.flattenedFieldList;
    }

    public List<FormulaTestData> getTestData() {
        return testData;
    }
}

