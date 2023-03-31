package com.force.formula.v2.impl;

import com.force.formula.FormulaDataType;
import com.force.formula.v2.IFormulaTestDefinitionParser;
import com.force.formula.v2.Utils;
import com.force.formula.v2.data.FormulaFieldDefinition;
import com.force.formula.v2.data.FormulaTestData;
import com.force.formula.v2.data.FormulaTestDefinition;
import com.force.formula.v2.exception.FormulaFileParseException;
import org.apache.commons.lang3.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class FormulaTestDefinitionFileParser implements IFormulaTestDefinitionParser<FormulaTestDefinition> {

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

    private List<FormulaTestDefinition> parse(String absoluteFilePath){
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document document;

        try{
            File file = new File(absoluteFilePath);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(file);
        }catch (ParserConfigurationException| SAXException | IOException ex){
            throw new FormulaFileParseException(ex);
        }

        return parseTestCaseDefinitions(document);
    }

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
            List<String> executionPaths = Arrays.asList(testCase.getAttribute("executionPaths").trim().split("\\s*,\\s*"));
            List<FormulaFieldDefinition> referenceFields = extractReferenceFields(testCase);
            List<FormulaTestData> testData = extractTestData(testCase, executionPaths, referenceFields);

            //Validations before creating a testCaseFieldInfo, rest all validations are taken care of in constructors
            Validate.notEmpty(formula, "formula in the testcase definition cannot be null or empty");
            FormulaDataType formulaDataType = Utils.getDataType(dataType);

            testCaseInfos.add(new FormulaTestDefinition(
                    testName,
                    new FormulaFieldDefinition(formulaFieldName, formulaDataType, formula, referenceFields),
                    referenceFields,
                    executionPaths,
                    testData));
        }
        return testCaseInfos.size()>0?testCaseInfos:null;
    }

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
                List<FormulaFieldDefinition> innerReferenceFields = extractReferenceFields(field);
                FormulaDataType formulaDataType = Utils.getDataType(dataType);
                referenceFields.add(new FormulaFieldDefinition(fieldName, formulaDataType, formula, innerReferenceFields));
            }
        }
        //we don't want to return empty lists to keep things simple and uniform
        return referenceFields.size()>0?referenceFields:null;
    }

    private List<FormulaTestData> extractTestData(Element element, List<String> executionPaths, List<FormulaFieldDefinition> referenceFields){
        List<FormulaTestData> testDataList = new LinkedList<>();
        if(element.hasChildNodes()){
            NodeList children = element.getChildNodes();
            for(int i=0;i<children.getLength();i++){
                Node tempNode = children.item(i);
                if(!(tempNode instanceof Element) || !"testData".equals(tempNode.getNodeName())){
                    continue;
                }
                Element field = (Element)tempNode;
                String input = field.getAttribute("input").trim();
                List<String> outputs = Arrays.asList(field.getAttribute("expectedOutput")
                        .trim().split("\\s*,\\s*"));
                testDataList.add(new FormulaTestData(input, outputs, executionPaths, referenceFields));
            }
        }
        //we don't want to return empty lists to keep things simple and uniform
        return testDataList.size()>0?testDataList:null;
    }

}
