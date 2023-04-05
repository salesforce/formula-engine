package com.force.formula.v2;

import com.force.formula.DbTester;
import com.force.formula.v2.data.FormulaTestDefinition;
import junit.framework.TestSuite;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class FormulaXMLTestSuite extends TestSuite {

    private static final Logger logger = Logger.getLogger(FormulaXMLTestSuite.class.getName());

    private DbTester dbTester;

    private final String goldFileDirectoryPath;

    public FormulaXMLTestSuite(String testSuiteName, List<String> testDefinitionAbsoluteFilePaths,
                               IFormulaTestDefinitionParser fileParser,
                               IFormulaTestCaseFilter testCaseFilter,
                               String goldFileDirectoryPath) {
        super(testSuiteName);

        Validate.notEmpty(testDefinitionAbsoluteFilePaths, "testDefinitionAbsoluteFilePaths cannot be null or empty");
        Validate.notNull(fileParser, "fileParser cannot be null");

        //Parse test definition xml files into test case infos
        List<FormulaTestDefinition> testCaseInfos = fileParser.parse(testDefinitionAbsoluteFilePaths);

        //filter certain test cases that you don't want to execute based on the provided filter
        List<FormulaTestDefinition> filteredTestCaseInfos = testCaseInfos;
        if(testCaseFilter!=null){
            filteredTestCaseInfos = testCaseFilter.filter(testCaseInfos);
        }

        //create test cases and add them to test suite
        for(FormulaTestDefinition testCaseInfo : filteredTestCaseInfos){
            FormulaTestCase formulaTestCase = new FormulaTestCase(testCaseInfo, this);
            addTest(formulaTestCase);
        }

        //Gold file to store intermediate state of the tests to help in debugging like SQL & JS generated
        this.goldFileDirectoryPath = goldFileDirectoryPath;
    }

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

    protected abstract DbTester constructDbTester() throws IOException;

    public String getGoldFileDirectoryPath() {
        return goldFileDirectoryPath;
    }
}
