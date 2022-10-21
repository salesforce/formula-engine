package com.force.formula.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.force.formula.FormulaDataType;
import com.force.formula.MockFormulaDataType;
import com.force.formula.impl.FormulaTestCaseInfo.WhyIgnoreSql;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Describe your class here.
 *
 * @author Srikanth L Yendluri, Adrienne Dimayuga
 * @since 0.1.0
 */
public class FormulaTestUtils {

    public static final int MAX_FIELD_SIZE = 3;
    public static final int MAX_NAME_LENGTH = 40;
    public static final String NEW_TYPE = "new";
    static final String[] FORMULA_CONSTANTS = { "true", "false", "null" };
    public static final String NEGATIVE_TEST_CASE_TYPE = "negative";
    private static final int DEFAULT_SCALE = 0;
    private static final int DEFAULT_PRECISION = 18;


    public enum UPDATE_FLAGS {
        CREATE, UPDATE, NOCREATEUPDATE
    }

    public FormulaTestUtils() {
        super();
    }
    
    protected FormulaTestCaseInfo constructFormulaTestCaseInfo(String tcName, String testLabels, String accuracyIssue, FieldDefinitionInfo tcFormulaFieldInfo,
            List<FieldDefinitionInfo> referenceFields, String owner, String compareType, String evalContexts,  String compareTemplate,
            Map<String,FormulaTestCaseInfo.WhyIgnoreSql> whyIgnoreSql, String whyIgnoreJs, boolean multipleResultTypes, Element testCaseElement) {
        return new FormulaTestCaseInfo(this, tcName, testLabels, accuracyIssue, tcFormulaFieldInfo, referenceFields, owner, compareType, 
                evalContexts, compareTemplate, whyIgnoreSql, whyIgnoreJs, multipleResultTypes);
    }

    /*
     * Gets the test cases from the xml file (formula-testcases.xml).
     */
    public List<FormulaTestCaseInfo> getTestCases(String xmlFileName, Predicate<FormulaTestCaseInfo> filter, String owner,
            String testLabelsAttribute, boolean swapResultTypes)
        throws ParserConfigurationException, SAXException, IOException, FileNotFoundException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document tcDoc = builder.parse(new InputSource(FormulaTestUtils.class.getResourceAsStream(xmlFileName)));
        
        return processTestCases(tcDoc, xmlFileName, filter, owner, testLabelsAttribute, swapResultTypes);
    }

    public List<FormulaTestCaseInfo> processTestCases(Document tcDoc, String xmlFileName, Predicate<FormulaTestCaseInfo> filter, String owner,
            String testLabelsAttribute, boolean swapResultTypes) throws IOException, ParserConfigurationException, SAXException {
        List<FormulaTestCaseInfo> tcResult = new LinkedList<FormulaTestCaseInfo>();

        // Get any included files 
        NodeList tcList = tcDoc.getElementsByTagName("include");
        for (int tcCount = 0; tcCount < tcList.getLength(); tcCount++) {
            Node tempNode = tcList.item(tcCount);
            if (!(tempNode instanceof Element))
                continue;
            Element testCase = (Element)tempNode;
            String href = testCase.getAttribute("href");
            // When using JAR files, you can't do "/../", so you have to replace the file.
            if (!href.startsWith("/")) {
                href = xmlFileName.substring(0,xmlFileName.lastIndexOf('/')) + "/" + href;
            }
            List<FormulaTestCaseInfo> testCases = getTestCases( href, filter, owner, testLabelsAttribute, swapResultTypes);
            if (testCases != null) {
                tcResult.addAll(testCases);
            }
        }
        
        tcList = tcDoc.getElementsByTagName("testcase");
        // create a field def info for each test case
        for (int tcCount = 0; tcCount < tcList.getLength(); tcCount++) {
            Node tempNode = tcList.item(tcCount);
            if (!(tempNode instanceof Element))
                continue;
            Element testCase = (Element)tempNode;

            boolean tcNegative = "true".equals(testCase.getAttribute("negative"));
            String tcName = testCase.getAttribute("name");
            String tcDevName = testCase.getAttribute("devName");
            if (tcDevName.length() >= MAX_NAME_LENGTH - 3) { // leave room for suffix for null-as-null version
                tcDevName = tcDevName.substring(0, MAX_NAME_LENGTH - 3);
            }
            String tcLabelName = testCase.getAttribute("labelName");
            String dataType = testCase.getAttribute("dataType");
            FormulaDataType tcDataType = getDataType(dataType);
            
            String code = testCase.getAttribute("code");
            boolean tcSwapArgs = "true".equals(testCase.getAttribute("swap"));
            boolean tcSwapTypes = "true".equals(testCase.getAttribute("swapTypes"));
            // Caller specifies attribute name that defines the applicable test labels so we can
            // have different basic/standard/extended etc division for different usages.
            // Default to the standard attribute "labels"
            String testLabels = testCase.getAttribute(testLabelsAttribute);
            if (testLabels.length() == 0 && !"labels".equals(testLabelsAttribute)) {
                testLabels = testCase.getAttribute("labels");
            }
            String compareType = testCase.getAttribute("compareType");
            String eval = testCase.getAttribute("eval");
            String compareTemplate = testCase.getAttribute("compareTemplate");
            String domain = testCase.getAttribute("domain");
            String whyIgnoreSqlStr = testCase.getAttribute("whyIgnoreSql");
            String whyIgnoreJs = testCase.getAttribute("whyIgnoreJs");

            String scale = testCase.getAttribute("scale");
            int tcScale = (scale.length() > 0) ? Integer.parseInt(scale) : DEFAULT_SCALE;

            String precision = testCase.getAttribute("precision");
            int tcPrecision = (precision.length() > 0) ? Integer.parseInt(precision) : DEFAULT_PRECISION;
            String dataFile = testCase.getAttribute("dataFile");
            // If its null, it will set null otherwise it will set encoding scheme
            String encoding = testCase.getAttribute("encoding");  
            String accuracyIssue = testCase.getAttribute("accuracyIssue");

            // construct the field definition for test case
            FieldDefinitionInfo tcFormulaFieldInfo = new FieldDefinitionInfo(null, tcDataType, tcDevName,
                tcLabelName, tcPrecision, tcScale, 0, domain, code);
            List<FieldDefinitionInfo> referenceFields = null;
            Map<String,WhyIgnoreSql> whyIgnoreSql;
            // Get whyIgnoreSql which could be an element, or an attribute for compatibility reasons
 
            
            // create an array list of reference fields and qa test cases
            if (testCase.hasChildNodes()) {
                NodeList children = testCase.getChildNodes();

                referenceFields = new ArrayList<FieldDefinitionInfo>(extractFieldsDefintions(children, null));
                whyIgnoreSql = extractWhyIgnoreSql(children, whyIgnoreSqlStr);
            } else {
                whyIgnoreSql = extractWhyIgnoreSql(null, whyIgnoreSqlStr);
            }

            FormulaTestCaseInfo tcInfo = constructFormulaTestCaseInfo(tcName, testLabels, accuracyIssue, tcFormulaFieldInfo,
                referenceFields, owner, compareType, eval, compareTemplate, whyIgnoreSql, whyIgnoreJs, swapResultTypes, testCase);

            if (filter != null && !filter.test(tcInfo)) {
                continue;
            }
            
            if (tcNegative) {
                String errorCode = testCase.getAttribute("errorcode");
                String errorMsg = testCase.getAttribute("errmsg");
                tcInfo.setNegative(tcNegative, errorCode, errorMsg);
            }

            tcInfo.setSwapArgs(tcSwapArgs);
            tcInfo.setSwapTypes(tcSwapTypes);
            tcInfo.setDataFileName(dataFile);
            if (encoding != null && !encoding.equals("")) {
                tcInfo.setEncoding(encoding);
            }
            tcResult.add(tcInfo);
        }
        // System.out.println("Total Test Cases : " + tcResult.size() + "\n");

        return tcResult.size() > 0 ? tcResult : null;
    }

    // TODO: SY - get rid of this clugy stuff, and implement a generic one
    public static List<List<String>> getSwapSets(int noOfArgs) {
        List<List<String>> swapSets = new ArrayList<List<String>>();

        if (noOfArgs > 1) {
            String[][] combinations = { { "number", "number", "number" }, { "number", "number", "currency" },
                { "number", "number", "percent" }, { "number", "currency", "currency" },
                { "number", "currency", "number" }, { "number", "currency", "percent" },
                { "number", "percent", "number" }, { "number", "percent", "percent" },
                { "number", "percent", "currency" }, { "percent", "currency", "currency" },
                { "percent", "currency", "percent" }, { "percent", "currency", "number" },
                { "percent", "percent", "number" }, { "percent", "percent", "currency" },
                { "percent", "percent", "percent" }, { "currency", "currency", "number" },
                { "currency", "currency", "percent" }, { "currency", "currency", "currency" } };

            for (String[] type : combinations) {
                List<String> myArgTypes = Arrays.asList(type);
                swapSets.add(myArgTypes);
            }
        } else {
            String[][] combinations = { { "number", "number" }, { "number", "currency" }, { "number", "percent" },
                { "currency", "currency" }, { "currency", "number" }, { "currency", "percent" },
                { "percent", "number" }, { "percent", "percent" }, { "percent", "currency" } };
            for (String[] type : combinations) {
                List<String> myArgTypes = Arrays.asList(type);
                swapSets.add(myArgTypes);
            }
        }
        return swapSets;
    }

    public static List<List<String>> getDataFromFile(String fileName) throws IOException, FileNotFoundException {
        return getDataFromFile(FormulaTestUtils.class, "/com/force/formula/impl/data/", fileName);
    }
    
    public static List<List<String>> getDataFromFile(Class<?> testClass, String dir, String fileName) throws IOException, FileNotFoundException {

        List<List<String>> dataSet = new ArrayList<List<String>>();
        BufferedReader input = new BufferedReader(new InputStreamReader(testClass.getResourceAsStream(dir
            + fileName), "UTF-8"));
        String temp;
        while ((temp = input.readLine()) != null) {
            temp = temp.trim();
            if (temp.length() == 0 || temp.charAt(0) == '#') {
                continue;
            }
            if (temp.charAt(0) == '`') {  // Use backtick as a "use quoting and escaping" for json/complex strings
                dataSet.add(splitSimpleWithQuoteAndEsc(temp.substring(1), ','));
            } else {
                dataSet.add(Arrays.asList(temp.split(",", -1)));
            }
        }

        input.close();
        return dataSet;
    }

    public static String getCFDeveloperName(String fieldName) {
        return fieldName + "__c";
    }

    public List<String> cleanLiteralTypes(List<String> fieldNames) {
        List<String> cFieldNames = new ArrayList<String>(fieldNames);
        cFieldNames.removeAll(Arrays.asList(FormulaTestUtils.FORMULA_CONSTANTS));
        return cFieldNames;
    }

    public void updateDataFile(String dataFileName, String formula) throws IOException, FileNotFoundException {
        throw new UnsupportedOperationException() ;
        /*
        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(TestContext.getFuncTestFile("/config/formula/")
            + dataFileName, true), "UTF-8"));
        output.append("\n#" + formula);
        output.close();
        */
    }

    public List<String> getFormulaNames(String xmlFileName, String retriveForEntity)
        throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document formulaFieldsDocument = documentBuilder.parse(new File(xmlFileName));

        List<String> result = new ArrayList<String>();
        NodeList entities = formulaFieldsDocument.getElementsByTagName("entity");

        for (int nEntity = 0; nEntity < entities.getLength(); nEntity++) {
            Element entity = (Element)entities.item(nEntity);

            String entityName = entity.getAttribute("enumId");
            if (retriveForEntity != null && !entityName.trim().equals(retriveForEntity))
                continue;

            NodeList formulas = entity.getElementsByTagName("field");
            String labelName = null;
            try {
                for (int nFormula = 0; nFormula < formulas.getLength(); nFormula++) {
                    Element formula = (Element)formulas.item(nFormula);

                    String devName = formula.getAttribute("devName");
                    result.add(devName.trim());
                    NodeList childNodes = formula.getElementsByTagName("DependantField");
                    for (int nChild = 0; nChild < childNodes.getLength(); nChild++) {
                        Element child = (Element)childNodes.item(nChild);
                        String c_devName = child.getAttribute("devName");
                        String isFormula = child.getAttribute("code");

                        if (isFormula != "")
                            result.add(c_devName.trim());
                    }

                }
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to Parse " + xmlFileName + " for Formula: " + labelName + "\n"
                    + e.getMessage(), e);
            }
        }
        return result;
    }

    public List<FieldDefinitionInfo> getFieldDefintions(String xmlFileName, String retriveForEntity)
        throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document fieldsDocument = documentBuilder.parse(new File(xmlFileName));

        List<FieldDefinitionInfo> result = new LinkedList<FieldDefinitionInfo>();
        NodeList entities = fieldsDocument.getElementsByTagName("entity");

        for (int nEntity = 0; nEntity < entities.getLength(); nEntity++) {
            Element entity = (Element)entities.item(nEntity);

            String entityName = entity.getAttribute("enumId");
            if (retriveForEntity != null && !entityName.trim().equals(retriveForEntity))
                continue;

            NodeList entityFieldList = entity.getChildNodes();
            result.addAll(extractFieldsDefintions(entityFieldList, entityName));
        }
        return result;

    }
    
    private static Set<String> TEXT_TYPE = ImmutableSet.of("email", "url", "phone", "textarea");
    private static Set<String> DOUBLE_TYPE = ImmutableSet.of("number");
    private static Set<String> CURRENCY_TYPE = ImmutableSet.of("currency");
    private static Set<String> PERCENT_TYPE = ImmutableSet.of("percent");
    
    public FormulaDataType getDataType(String dataTypeName) {
        if (TEXT_TYPE.contains(dataTypeName)) return MockFormulaDataType.TEXT;
        if (DOUBLE_TYPE.contains(dataTypeName)) return MockFormulaDataType.DOUBLE;
        if (CURRENCY_TYPE.contains(dataTypeName)) return MockFormulaDataType.CURRENCY;
        if (PERCENT_TYPE.contains(dataTypeName)) return MockFormulaDataType.PERCENT;
        FormulaDataType dataType = MockFormulaDataType.fromCamelCaseName(dataTypeName);
        if (dataType == null) {
            throw new IllegalArgumentException("Couldn't figure out type " + dataTypeName);
        }
        return dataType;
    }
        
    protected FieldDefinitionInfo constructFieldDefinitionInfo(String entityName, FormulaDataType returnType, String devName, String labelName, Element defElement) {
        return new FieldDefinitionInfo(entityName, returnType, devName, labelName);
    }
    
    private List<FieldDefinitionInfo> extractFieldsDefintions(NodeList fieldList, String entityName)
        throws IOException, FileNotFoundException {
        List<FieldDefinitionInfo> fieldDefinitions = new LinkedList<FieldDefinitionInfo>();
        for (int nField = 0; nField < fieldList.getLength(); nField++) {

            Node tempNode = fieldList.item(nField);
            if (!(tempNode instanceof Element) || !"referencefield".equals(tempNode.getNodeName()))
                continue;
            Element field = (Element)tempNode;
            String devName = field.getAttribute("devName");
            String labelName = field.getAttribute("labelName");
            String dataTypeName = field.getAttribute("dataType");
            FormulaDataType dataType = getDataType(dataTypeName);
            String precision = field.getAttribute("precision");
            String scale = field.getAttribute("scale");
            String length = field.getAttribute("length");
            String formulaCode = field.getAttribute("code");
            boolean isStandard = "true".equalsIgnoreCase(field.getAttribute("standardfield"));

            FieldDefinitionInfo fieldDefinition = constructFieldDefinitionInfo(entityName, dataType, devName, labelName, field);

            fieldDefinition.setIsStandard(isStandard);

            fieldDefinition.setReturnType(dataType);
            
            if (precision != "") {
                fieldDefinition.setPrecision(Integer.parseInt(precision));
            }

            if (scale != "") {
                fieldDefinition.setScale(Integer.parseInt(scale));
            }
            if (length != "") {
                fieldDefinition.setLength(Integer.parseInt(length));
            }

            if (dataType.isPickval()) {
                for (List<String> value : getDataFromFile(field.getAttribute("picklistdatafile").trim())) {
                    fieldDefinition.appendPickListValue(value.get(0));
                }
            }
            if (formulaCode != "") {
                fieldDefinition.setFormula(formulaCode);
            }

            if (field.hasChildNodes()) {
                NodeList childNodes = field.getChildNodes();
                fieldDefinition.setRefFields(extractFieldsDefintions(childNodes, entityName));
            }
            fieldDefinitions.add(fieldDefinition);
        }
        return fieldDefinitions;
    }

    // WhyIgnoreSql as an element was added in 0.3 because a single string for all the DBs was unwieldy. 
    private Map<String, WhyIgnoreSql> extractWhyIgnoreSql(NodeList fieldList, String str) {
        ImmutableMap.Builder<String, WhyIgnoreSql> builder = null;
        // Parse the child node list if available
        if (fieldList != null) {
            for (int nField = 0; nField < fieldList.getLength(); nField++) {
    
                Node tempNode = fieldList.item(nField);
                if (!(tempNode instanceof Element) || !"whyIgnoreSql".equalsIgnoreCase(tempNode.getNodeName())) continue;
                Element field = (Element)tempNode;
                String dbStr = field.getAttribute("db");
                String reason = field.getAttribute("reason");
                String failures = field.getAttribute("numFailures");
                String unimplementedStr = field.getAttribute("unimplemented");
                if (reason == null || reason.length() == 0) {
                    reason = field.getTextContent();
                }
                int numFailures = failures != null && failures.length() > 0 ? Integer.parseInt(failures) : 0;
                boolean unimplemented = "true".equals(unimplementedStr);
                if (builder == null) {
                    builder = ImmutableMap.builder();
                }
                for (String db : Splitter.on(',').split(dbStr)) {
                    builder.put(db, new WhyIgnoreSql(db, reason, numFailures, unimplemented));
                }
            }
        }
        // Parse the attribute string
        if (str != null && str.length() > 0) {
            if (builder == null) {
                builder = ImmutableMap.builder();
            }
            final ImmutableMap.Builder<String, WhyIgnoreSql> b = builder;  // Appease lambda
            Splitter.on(',').withKeyValueSeparator(':').split(str).forEach((k,v)->b.put(k, new WhyIgnoreSql(k,v,-1,false)));
        }
        return builder != null ? builder.build() : Collections.emptyMap();
    }



    
    /**
     * Splits the given string str using the given delimiter and returns the result as a string list. Double-quotes and
     * escaped characters using backslash as the escape character are honored. If str is null, then null is returned.<br>
     * <br>
     * The returned string list is an ArrayList that is constructed using four as the ArrayList's initial size. If you
     * expect to have more than four elements more than just on the rare occasion, then please consider using another
     * splitSimpleWithQuoteAndEsc overload that lets you pass in the expected size.
     *
     * @param str
     *            The string to split
     * @param delimiter
     *            The delimiter to split the string using
     * @param expectedSize
     *            The expected number of elements in the output list. If you don't know, or if it could be arbitrarily
     *            large, and if you will only access the returned list sequentially with an iterator, then use 0 to tell
     *            this method to use a LinkedList
     * @return String list or, if str was null, then null
     */
    public static List<String> splitSimpleWithQuoteAndEsc(String str, char delimiter) {
        return splitSimpleWithQuoteAndEsc(str, delimiter, 4, false);
    }

    /**
     * Splits the given string str using the given delimiter and returns the result as a string list. Double-quotes and
     * escaped characters using backslash as the escape character are honored. If str is null, then null is returned.<br>
     * <br>
     * The returned string list is an ArrayList that is constructed using the given expected size as the ArrayList's
     * initial size. If you are not aware of the expected size, then use 0, which will cause this method to use a
     * LinkedList instead of an ArrayList.
     *
     * @param str
     *            The string to split
     * @param delimiter
     *            The delimiter to split the string using
     * @param expectedSize
     *            The expected number of elements in the output list. If you don't know, or if it could be arbitrarily
     *            large, and if you will only access the returned list sequentially with an iterator, then use 0 to tell
     *            this method to use a LinkedList
     * @return String list or, if str was null, then null
     */
    public static List<String> splitSimpleWithQuoteAndEsc(String str, char delimiter, int expectedSize) {
        return splitSimpleWithQuoteAndEsc(str, delimiter, expectedSize, false);
    }

    /**
     * Splits the given string str using the given delimiter, trims each element, and returns the result as a string
     * list. Double-quotes and escaped characters using backslash as the escape character are honored. If str is null,
     * then null is returned.<br>
     * <br>
     * The returned string list is an ArrayList that is constructed using the given expected size as the ArrayList's
     * initial size. If you are not aware of the expected size, then use 0, which will cause this method to use a
     * LinkedList instead of an ArrayList.
     *
     * @param str
     *            The string to split
     * @param delimiter
     *            The delimiter to split the string using
     * @param expectedSize
     *            The expected number of elements in the output list. If you don't know, or if it could be arbitrarily
     *            large, and if you will only access the returned list sequentially with an iterator, then use 0 to tell
     *            this method to use a LinkedList
     * @return String list or, if str was null, then null
     */
    public static List<String> splitSimpleAndTrimWithQuoteAndEsc(String str, char delimiter, int expectedSize) {
        return splitSimpleWithQuoteAndEsc(str, delimiter, expectedSize, true);
    }

    /**
     * Splits the given string str using the given delimiter and returns the result as a string list. Double-quotes and
     * escaped characters using backslash as the escape character are honored. If str is null, then null is returned.<br>
     * <br>
     * The returned string list is an ArrayList that is constructed using the given expected size as the ArrayList's
     * initial size. If you are not aware of the expected size, then use 0, which will cause this method to use a
     * LinkedList instead of an ArrayList.
     *
     * @param str
     *            The string to split
     * @param delimiter
     *            The delimiter to split the string using
     * @param expectedSize
     *            The expected number of elements in the output list. If you don't know, or if it could be arbitrarily
     *            large, and if you will only access the returned list sequentially with an iterator, then use 0 to tell
     *            this method to use a LinkedList
     * @param trim
     *            When true, each element is trimmed before being added to the returned string list
     * @return String list or, if str was null, then null
     */
    private static List<String> splitSimpleWithQuoteAndEsc(String str, char delimiter, int expectedSize, boolean trim) {
        if (str == null) {
            return null;
        }
        List<String> result = (expectedSize == 0)? new LinkedList<String>(): new ArrayList<String>(expectedSize);

        StringBuilder cur = new StringBuilder(25);
        boolean inQuote = false;
        boolean inEscape = false;

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (inEscape) { // escaped character
                switch (ch) {
                    case 'n':
                        cur.append('\n');
                        break;
                    case 'r':
                        cur.append('\r');
                        break;
                    case 't':
                        cur.append('\t');
                        break;
                    default:
                        cur.append(ch);
                }
                inEscape = false;
            } else if (ch == '\\') { // Escaping
                inEscape = true;
            } else if (ch == '\"') { // Quoting
                inQuote = !inQuote;
            } else if (!inQuote && ch == delimiter) { // A delimiter
                if (trim) {
                    result.add(cur.toString().trim());
                } else {
                    result.add(cur.toString());
                }
                cur.setLength(0);
            } else { // normal
                cur.append(ch);
            }
        }

        if (inEscape || inQuote) {
            throw new IllegalArgumentException("illegal string for splitting: " + str);
        }

        if (trim) {
            result.add(cur.toString().trim());
        } else {
            result.add(cur.toString());
        }

        return result;
    }
}
