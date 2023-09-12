package com.force.formula.impl;

import com.force.formula.DbTester;
import com.force.formula.FormulaEngine;
import com.force.formula.impl.sql.FormulaDefaultSqlStyle;
import com.force.formula.sql.OracleContainerTester;
import com.force.formula.v2.FormulaXMLTestSuite;
import com.force.formula.v2.IFormulaTestCaseFilter;
import com.force.formula.v2.IFormulaTestDefinitionParser;
import com.force.formula.v2.data.FormulaTestDefinition;
import com.force.formula.v2.impl.FormulaTestDefinitionFileParser;
import junit.framework.TestSuite;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * A test suite that uses test xml file - formulaTestV2.xml to generate and run those tests using oracle database
 */
@RunWith(AllTests.class)
public class FormulaOracleXMLTests extends FormulaXMLTestSuite {

    // use a single DB with a docker container, and not three of them by sharing them.
    static DbTester SHARED_TESTER;
    static {
        try {
            SHARED_TESTER = new OracleContainerTester();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public FormulaOracleXMLTests(List<String> testDefinitionAbsoluteFilePaths, IFormulaTestDefinitionParser fileParser, IFormulaTestCaseFilter testCaseFilter, String goldFileDirectory) {
        super("FormulaOracleXMLTests", testDefinitionAbsoluteFilePaths, fileParser, testCaseFilter, goldFileDirectory);
    }

    /**
     * Creates a test suite by providing
     *      test xml file paths,
     *      setting database specific hooks for oracle,
     *      setting timezone for testing purposes,
     *      supplying a parse to parse test xml file,
     *      supplying a filter to filter out some test scenarios,
     *      and a directory path for oracle specific gold files.
     *
     * @return a test suite created from the given test xml file
     */
    public static TestSuite suite() {
        List<String> xmlFiles = new ArrayList<>();
        xmlFiles.add("com/force/formula/impl/formulaTestV2.xml");
        //The following hook has Oracle as the database hook
        FormulaEngine.setHooks(new OracleFormulaValidationHooks());
        FormulaEngine.setFactory(BaseFieldReferenceTest.TEST_FACTORY);
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        IFormulaTestDefinitionParser<FormulaTestDefinition> parser = new FormulaTestDefinitionFileParser();
        String goldFileDirectory = "src/test/goldfiles/FormulaFields/v2/oracle";
        return new FormulaOracleXMLTests(xmlFiles, parser, null, goldFileDirectory);
    }

    /**
     * Creates a oracle database tester
     * @return a oracle database tester
     * @throws IOException if there is an IO issue while creating database tester
     */
    @Override
    protected DbTester constructDbTester() throws IOException {
        return SHARED_TESTER;
    }

    protected static class OracleFormulaValidationHooks extends BaseCustomizableParserTest.FieldTestFormulaValidationHooks {
        @Override
        public FormulaSqlHooks getSqlStyle() {
            return FormulaDefaultSqlStyle.ORACLE;
        }
    }
}
