package com.force.formula.v2.data;

import org.apache.commons.lang3.Validate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Each testcase will have a list of FormulaTestData. This provides the input and expected output values for each execution path
 * like formula, javascript, sql, etc.
 */
public class FormulaTestData {
    /**
     * This field contains the input values for each reference field defined in the testcase.
     */
    private final Map<String,Object> input;
    /**
     * This field contains the mapping of <execution path, expected output> for the above input. Each execution path can
     * have a different value.
     */
    private final Map<String,String> expectedOutput;

    public FormulaTestData(String input, List<String> outputs, List<String> executionPaths, List<FormulaFieldDefinition> referenceFields) {

        List<FormulaFieldDefinition> inputFields = getAllFieldsForInputData(referenceFields);
        String[] inputArray = input.trim().split(",");

        Validate.isTrue(inputArray.length==inputFields.size(),"Each field needs to have a " +
                "corresponding input value defined. Input Fields Size: " + inputFields.size()+" , Input Values Size: "
                + inputArray.length);
        Validate.isTrue(executionPaths.size()==outputs.size(),"Each executionPath needs to have" +
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

    public Map<String, Object> getInput() {
        return input;
    }

    public Map<String, String> getExpectedOutput() {
        return Collections.unmodifiableMap(expectedOutput);
    }

    private List<FormulaFieldDefinition> getAllFieldsForInputData(List<FormulaFieldDefinition> nestedFields){
        List<FormulaFieldDefinition> flattenedList = flattenFieldList(nestedFields);
        return flattenedList.stream()
                .filter(fdi -> (fdi.getFormula()==null || fdi.getFormula()==""))
                .collect(Collectors.toList());
    }

    private List<FormulaFieldDefinition> flattenFieldList(List<FormulaFieldDefinition> nestedFields) {
        List<FormulaFieldDefinition> flattenedList = new LinkedList<>();
        for (FormulaFieldDefinition field : nestedFields) {
            if (field.getReferenceFields()!=null && !field.getReferenceFields().isEmpty())
                flattenedList.addAll(flattenFieldList(field.getReferenceFields()));
            flattenedList.add(field);
        }
        return flattenedList;
    }
}
