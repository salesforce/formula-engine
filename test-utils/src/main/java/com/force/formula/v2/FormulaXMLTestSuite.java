package com.force.formula.v2;

import com.force.formula.DbTester;
import com.force.formula.v2.data.FormulaTestDefinition;
import junit.framework.TestSuite;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The FormulaXMLTestSuite class represents a TestSuite for executing formula XML tests.
 * This abstract class extends the JUnit TestSuite class and provides a base implementation
 * for creating test cases based on provided XML test definition files.
 */
public abstract class FormulaXMLTestSuite extends TestSuite {

    private static final Logger logger = Logger.getLogger(FormulaXMLTestSuite.class.getName());

    private DbTester dbTester;

    private final String goldFileDirectoryPath;

    /**
     * Creates a new FormulaXMLTestSuite with the given test suite name, list of test definition
     * file paths, test definition file parser, test case filter, and gold file directory path.
     *
     * The constructor parses the test definition XML files into test case definitions, filters certain
     * test cases based on the provided filter, and creates test cases for each remaining test case
     * definition, then adds them to the test suite.
     *
     * @param testSuiteName The name of the test suite.
     * @param testDefinitionAbsoluteFilePaths The file paths of the test definition XML files.
     * @param fileParser The parser for parsing the test definition XML files.
     * @param testCaseFilter The filter for filtering certain test cases.
     * @param goldFileDirectoryPath The gold file directory path for storing intermediate state of tests.
     * @throws IllegalArgumentException if the test definition absolute file paths are null or empty,
     * or if the file parser is null.
     */
    public FormulaXMLTestSuite(String testSuiteName, List<String> testDefinitionAbsoluteFilePaths,
                               IFormulaTestDefinitionParser fileParser,
                               IFormulaTestCaseFilter testCaseFilter,
                               String goldFileDirectoryPath) {
        super(testSuiteName);

        Validate.notEmpty(testDefinitionAbsoluteFilePaths, "testDefinitionAbsoluteFilePaths cannot be null or empty");
        Validate.notNull(fileParser, "fileParser cannot be null");

        //Parse test definition xml files into test case infos
        List<FormulaTestDefinition> testDefinitions = fileParser.parse(testDefinitionAbsoluteFilePaths);

        //filter certain test cases that you don't want to execute based on the provided filter
        List<FormulaTestDefinition> filteredTestCaseInfos = testDefinitions;
        if(testCaseFilter!=null){
            filteredTestCaseInfos = testCaseFilter.filter(testDefinitions);
        }

        //create test cases and add them to test suite
        for(FormulaTestDefinition testDefinition : filteredTestCaseInfos){
            FormulaTestCase formulaTestCase = new FormulaTestCase(testDefinition, this);
            addTest(formulaTestCase);
        }

        //Gold file to store intermediate state of the tests to help in debugging like SQL & JS generated
        this.goldFileDirectoryPath = goldFileDirectoryPath;
    }

    /**
     * Gets the DbTester instance for the test suite.
     *
     * @return DbTester instance for the class.
     */
    public DbTester getDbTester(){
        try{
            if (dbTester == null) {
                dbTester = constructDbTester();
            }
            return dbTester;
        }catch (Throwable ex){
            logger.log(Level.FINER, "Error creating DbTester" + ex.getMessage());
            return null;
        }
    }

    /**
     * Abstract method to create the DbTester instance to be used for test cases which are part of this test suite.
     *
     * @return an instance of DbTester
     * @throws IOException if an exception occurred while constructing the db
     */
    protected abstract DbTester constructDbTester() throws IOException;

    /**
     * Get the directory path where the goldfiles for this test suite will be created
     *
     * @return goldfile directory path as string
     */
    public String getGoldFileDirectoryPath() {
        return goldFileDirectoryPath;
    }
}
