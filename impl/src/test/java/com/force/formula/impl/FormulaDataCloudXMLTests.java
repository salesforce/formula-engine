package com.force.formula.impl;

import com.force.formula.DbTester;
import com.force.formula.FormulaEngine;
import com.force.formula.impl.sql.FormulaDefaultSqlStyle;
import com.force.formula.sql.DataCloudTester;
import com.force.formula.v2.FormulaTestCase;
import com.force.formula.v2.FormulaXMLTestSuite;
import com.force.formula.v2.IFormulaTestCaseFilter;
import com.force.formula.v2.IFormulaTestDefinitionParser;
import com.force.formula.v2.data.FormulaTestDefinition;
import com.force.formula.v2.impl.FormulaTestDefinitionFileParser;
import com.force.formula.impl.FormulaSqlHooks;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * A test suite that uses test xml file - formulaTestV2.xml to generate and run those tests
 * against Salesforce DataCloud Hyper DB using the DataCloud JDBC driver.
 */
@RunWith(AllTests.class)
public class FormulaDataCloudXMLTests extends FormulaXMLTestSuite {

    public FormulaDataCloudXMLTests(List<String> testDefinitionAbsoluteFilePaths, IFormulaTestDefinitionParser fileParser, IFormulaTestCaseFilter testCaseFilter, String goldFileDirectory) {
        super("FormulaDataCloudXMLTests", testDefinitionAbsoluteFilePaths, fileParser, testCaseFilter, goldFileDirectory);
    }

    /**
     * Creates a test suite by providing
     *      test xml file paths,
     *      setting database specific hooks for postgres (DataCloud uses postgres-compatible SQL),
     *      setting timezone for testing purposes,
     *      supplying a parser to parse test xml file,
     *      supplying a filter to filter out some test scenarios,
     *      and a directory path for datacloud specific gold files.
     *
     * @return a test suite created from the given test xml file
     */
    public static TestSuite suite() {
        List<String> xmlFiles = new ArrayList<>();
        xmlFiles.add("com/force/formula/impl/formulaTestV2.xml");
        FormulaEngine.setHooks(new BaseCustomizableParserTest.FieldTestFormulaValidationHooks() {
            @Override
            public FormulaSqlHooks getSqlStyle() {
                return FormulaDefaultSqlStyle.DATACLOUD;
            }
        });
        FormulaEngine.setFactory(BaseFieldReferenceTest.TEST_FACTORY);
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        IFormulaTestDefinitionParser<FormulaTestDefinition> parser = new FormulaTestDefinitionFileParser();
        String goldFileDirectory = "src/test/goldfiles/FormulaFields/v2/datacloud";
        IFormulaTestCaseFilter<FormulaTestDefinition> filter = new DataCloudTestCaseFilter();
        return new FormulaDataCloudXMLTests(xmlFiles, parser, filter, goldFileDirectory);
    }

    /**
     * Use DataCloudFormulaTestCase which normalizes numeric values for SQL path comparison.
     */
    @Override
    protected FormulaTestCase createTestCase(FormulaTestDefinition testDefinition) {
        return new DataCloudFormulaTestCase(testDefinition, this);
    }

    /**
     * Creates a DataCloud database tester
     * @return a DataCloud database tester
     * @throws IOException if there is an IO issue while creating database tester
     */
    @Override
    protected DbTester constructDbTester() throws IOException {
        return new DataCloudTester();
    }
}