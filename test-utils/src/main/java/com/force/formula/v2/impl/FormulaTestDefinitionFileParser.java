package com.force.formula.v2.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.force.formula.FormulaDataType;
import com.force.formula.v2.IFormulaTestDefinitionParser;
import com.force.formula.v2.Utils;
import com.force.formula.v2.data.FormulaFieldDefinition;
import com.force.formula.v2.data.FormulaTestData;
import com.force.formula.v2.data.FormulaTestDefinition;
import com.force.formula.v2.exception.FormulaFileParseException;

public class FormulaTestDefinitionFileParser implements IFormulaTestDefinitionParser<FormulaTestDefinition> {

    /**
     * A method that parses a list of test xml files to create formula test definitions to be used for testing
     *
     * @param absoluteFilePaths a list of file paths for test xml files that needs to be parsed
     * @return a list of formula test definitions
     * @throws FormulaFileParseException if there is an issue reading and parsing the provided file paths
     */
    @Override
    public List<FormulaTestDefinition> parse(List<String> absoluteFilePaths) throws FormulaFileParseException {
        if (absoluteFilePaths != null) {
            return absoluteFilePaths.stream()
                    .map(l -> parse(l))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * A method that parses a test xml file to create formula test definitions to be used for testing
     *
     * @param absoluteFilePath a file path for test xml file that needs to be parsed
     * @return a list of formula test definitions obtained from the given test xml file
     */
    private List<FormulaTestDefinition> parse(String absoluteFilePath){
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document document;

        try{
            File file = new File(this.getClass().getClassLoader().getResource(absoluteFilePath).getFile());
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(file);
        }catch (ParserConfigurationException| SAXException | IOException ex){
            throw new FormulaFileParseException(ex);
        }

        return parseTestCaseDefinitions(document);
    }

    /**
     * A method that parses a test xml document node to create formula test definitions to be used for testing
     *
     * @param document a test xml document node that needs to be parsed
     * @return a list of formula test definitions obtained from the given test xml document node
     */
    private List<FormulaTestDefinition> parseTestCaseDefinitions(Document document){
        List<FormulaTestDefinition> testCaseInfos = new LinkedList<FormulaTestDefinition>();

        //Get the list of all testcases from the xml file
        NodeList tcList = document.getElementsByTagName("testcase");

        //Create a FormulaTestCaseInfoV2 for each testcase
        for (int tcCount = 0; tcCount < tcList.getLength(); tcCount++) {
            Node tempNode = tcList.item(tcCount);
            if (!(tempNode instanceof Element))
                continue;
            Element testCase = (Element)tempNode;
            String testName = testCase.getAttribute("testName");
            String formulaFieldName = testCase.getAttribute("fieldName");
            String dataType = testCase.getAttribute("dataType");
            String formula = testCase.getAttribute("formula").trim();
            String scaleValue = testCase.getAttribute("scale");
            int scale = 2;
            if(!StringUtils.isEmpty(scaleValue)){
                scale = Integer.parseInt(scaleValue);
            }
            List<String> executionPaths = Arrays.asList(testCase.getAttribute("executionPaths").trim().split("\\s*,\\s*"));
            List<FormulaFieldDefinition> referenceFields = extractReferenceFields(testCase);
            List<FormulaTestData> testData = extractTestData(testCase, executionPaths, referenceFields);

            //Validations before creating a testCaseFieldInfo, rest all validations are taken care of in constructors
            Validate.notEmpty(formula, "formula in the testcase definition cannot be null or empty");
            FormulaDataType formulaDataType = Utils.getDataType(dataType);

            testCaseInfos.add(new FormulaTestDefinition(
                    testName,
                    new FormulaFieldDefinition(formulaFieldName, formulaDataType, formula, referenceFields, scale),
                    referenceFields,
                    executionPaths,
                    testData));
        }
        return testCaseInfos.size()>0?testCaseInfos:null;
    }

    /**
     * Extracts a list of reference fields for a formula in the order they are defined in the test xml file
     *
     * @param element a test xml element node that needs to be parsed to get reference fields for the formula
     * @return an ordered list of reference fields in the sequence they are defined in test xml file
     */
    private List<FormulaFieldDefinition> extractReferenceFields(Element element){
        List<FormulaFieldDefinition> referenceFields = new LinkedList<>();
        if(element.hasChildNodes()){
            NodeList children = element.getChildNodes();
            for (int i=0; i<children.getLength(); i++) {
                Node tempNode = children.item(i);
                if (!(tempNode instanceof Element) || !"referenceField".equals(tempNode.getNodeName())) {
                    continue;
                }
                Element field = (Element) tempNode;
                String fieldName = field.getAttribute("fieldName");
                String dataType = field.getAttribute("dataType");
                String formula = field.getAttribute("formula").trim();
                String scaleValue = field.getAttribute("scale");
                int scale = 2;
                if(!StringUtils.isEmpty(scaleValue)){
                    scale = Integer.parseInt(scaleValue);
                }
                List<FormulaFieldDefinition> innerReferenceFields = extractReferenceFields(field);
                FormulaDataType formulaDataType = Utils.getDataType(dataType);
                referenceFields.add(new FormulaFieldDefinition(fieldName, formulaDataType, formula, innerReferenceFields, scale));
            }
        }
        //we don't want to return empty lists to keep things simple and uniform
        return referenceFields.size()>0?referenceFields:null;
    }

    /**
     * Extracts a list of test data to be used for testing the formula
     *
     * @param element a test xml element node that needs to be parsed to test data to be used for testing the formula
     * @param executionPaths an ordered list of execution paths that are defined in the test xml file
     * @param referenceFields an ordered list of reference fields that are defined in the test xml file
     * @return a list of test data against which the formula will be executed
     */
    private List<FormulaTestData> extractTestData(Element element, List<String> executionPaths, List<FormulaFieldDefinition> referenceFields){
        List<FormulaTestData> testDataList = new LinkedList<>();
        String testCase = element.getAttribute("testName");
        if(element.hasChildNodes()){
            NodeList children = element.getChildNodes();
            for(int i=0;i<children.getLength();i++){
                Node tempNode = children.item(i);
                if(!(tempNode instanceof Element) || !"testData".equals(tempNode.getNodeName())){
                    continue;
                }
                Element field = (Element)tempNode;
                String input = field.getAttribute("input").trim();
                List<String> outputs = Arrays.stream(field.getAttribute("expectedOutput").split("(?<!\\\\),"))
                        .map(s -> s.replaceAll("\\\\,", ","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                testDataList.add(new FormulaTestData(testCase, input, outputs, executionPaths, referenceFields));
            }
        }
        //we don't want to return empty lists to keep things simple and uniform
        return testDataList.size()>0?testDataList:null;
    }

}
