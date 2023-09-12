package com.force.formula.impl;

import com.force.formula.DbTester;
import com.force.formula.FormulaEngine;
import com.force.formula.sql.EmbeddedPostgresTester;
import com.force.formula.sql.PostgreSQLContainerTester;
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
 * A test suite that uses test xml file - formulaTestV2.xml to generate and run those tests using postgres database
 */
@RunWith(AllTests.class)
public class FormulaPostgresXMLTests extends FormulaXMLTestSuite {
    // If true, use docker for testing the postgres DB with testcontainers.org.  Otherwise, use embedded
    // The embedded one is quicker and easier to manage.  Only difference as of Nov 2021 is an error message
    // change in testIfNullNullIf
    public static final boolean USE_DOCKER_FOR_DB = false;

    public FormulaPostgresXMLTests(List<String> testDefinitionAbsoluteFilePaths, IFormulaTestDefinitionParser fileParser, IFormulaTestCaseFilter testCaseFilter, String goldFileDirectory) {
        super("FormulaPostgresXMLTests", testDefinitionAbsoluteFilePaths, fileParser, testCaseFilter, goldFileDirectory);
    }

    /**
     * Creates a test suite by providing
     *      test xml file paths,
     *      setting database specific hooks for postgres,
     *      setting timezone for testing purposes,
     *      supplying a parse to parse test xml file,
     *      supplying a filter to filter out some test scenarios,
     *      and a directory path for postgres specific gold files.
     *
     * @return a test suite created from the given test xml file
     */
    public static TestSuite suite() {
        List<String> xmlFiles = new ArrayList<>();
        xmlFiles.add("com/force/formula/impl/formulaTestV2.xml");
        //The following hook has Postgres as the database hook
        FormulaEngine.setHooks(new BaseCustomizableParserTest.FieldTestFormulaValidationHooks());
        FormulaEngine.setFactory(BaseFieldReferenceTest.TEST_FACTORY);
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        IFormulaTestDefinitionParser<FormulaTestDefinition> parser = new FormulaTestDefinitionFileParser();
        String goldFileDirectory = "src/test/goldfiles/FormulaFields/v2/postgres";
        return new FormulaPostgresXMLTests(xmlFiles, parser, null, goldFileDirectory);
    }

    /**
     * Creates a postgres database tester
     * @return a postgres database tester
     * @throws IOException if there is an IO issue while creating database tester
     */
    @Override
    protected DbTester constructDbTester() throws IOException {
        if (USE_DOCKER_FOR_DB) {
            return new PostgreSQLContainerTester();
        } else {
            return new EmbeddedPostgresTester();
        }
    }
}
