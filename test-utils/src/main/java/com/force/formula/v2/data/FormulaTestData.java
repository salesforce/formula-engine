package com.force.formula.v2.data;

import com.force.formula.v2.Utils;
import org.apache.commons.lang3.Validate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Each testcase will have a list of FormulaTestData. This provides the input and expected output values for each execution path
 * like formula, javascript, sql, etc.
 */
public class FormulaTestData {
    /**
     * This field contains the input values for each reference field defined in the testcase. It is a map of
     * (reference field name, test input field value)
     */
    private final Map<String,Object> input;
    /**
     * This field contains the mapping of (execution path, expected output) for the above input. Each execution path will
     * have a different value.
     */
    private final Map<String,String> expectedOutput;

    /**
     * Creates a FormulaTestData object for each test data as defined in the test xml file
     *
     * @param input input string as defined in the test xml file containing comma separated values for each reference field
     *              defined in order of flattened reference fields order
     * @param outputs ordered list of string expected output values for each execution path
     * @param executionPaths ordered list of execution paths as defined in the test xml file
     * @param referenceFields ordered list of reference fields as defined in the test xml file
     */
    public FormulaTestData(String input, List<String> outputs, List<String> executionPaths, List<FormulaFieldDefinition> referenceFields) {

        List<FormulaFieldDefinition> inputFields = getAllFieldsForInputData(referenceFields);
        String[] inputArray = input.trim().split(",");

        Validate.isTrue((inputFields.size()==0)||((inputFields.size()>0) && (inputArray.length==inputFields.size())),"Each field needs to have a " +
                "corresponding input value defined. Input Fields Size: " + inputFields.size()+" , Input Values Size: "
                + inputArray.length);
        Validate.isTrue(executionPaths.size()==outputs.size(),"Each executionPath needs to have " +
                "an expectedOutput in order in which the execution paths are indexed or defined in the testcase");

        Map<String, Object> inputMap = new HashMap<>();
        for(int i=0;i<inputFields.size();i++){
            if(inputArray[i]!=null && inputArray[i].length()>0){
                inputMap.put(inputFields.get(i).getFieldName(),inputFields.get(i).createObjectWithGivenValue(inputArray[i]));
            }else{
                inputMap.put(inputFields.get(i).getFieldName(),null);
            }
        }
        this.input = Collections.unmodifiableMap(inputMap);

        Map<String,String> expectedOutput = new HashMap<>();
        for(int j=0;j<executionPaths.size();j++){
            expectedOutput.put(executionPaths.get(j),outputs.get(j));
        }
        this.expectedOutput = Collections.unmodifiableMap(expectedOutput);
    }

    /**
     * Gets the test input value defined for each reference field
     * @return a map of (reference field name, test input field value)
     */
    public Map<String, Object> getInput() {
        return input;
    }

    /**
     * Gets the expected output value for each execution path
     * @return a map of (execution path, expected output value)
     */
    public Map<String, String> getExpectedOutput() {
        return Collections.unmodifiableMap(expectedOutput);
    }

    //Flattens the nested reference fields and remove the formula fields to provide an order for the test input values
    private List<FormulaFieldDefinition> getAllFieldsForInputData(List<FormulaFieldDefinition> nestedFields){
        List<FormulaFieldDefinition> flattenedList = Utils.flattenFieldList(nestedFields);
        return flattenedList.stream()
                .filter(fdi -> (fdi.getFormula()==null || fdi.getFormula()==""))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "FormulaTestData{" +
                "input=" + input +
                ", expectedOutput=" + expectedOutput +
                '}';
    }
}
