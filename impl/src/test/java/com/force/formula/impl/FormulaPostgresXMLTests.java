package com.force.formula.impl;

import com.force.formula.DbTester;
import com.force.formula.FormulaEngine;
import com.force.formula.sql.EmbeddedPostgresTester;
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

@RunWith(AllTests.class)
public class FormulaPostgresXMLTests extends FormulaXMLTestSuite {

    public FormulaPostgresXMLTests(List<String> testDefinitionAbsoluteFilePaths, IFormulaTestDefinitionParser fileParser, IFormulaTestCaseFilter testCaseFilter, String goldFileDirectory) {
        super("FormulaPostgresXMLTests", testDefinitionAbsoluteFilePaths, fileParser, testCaseFilter, goldFileDirectory);
    }

    /**
     * As all the tests under this suite should run with same configurations, having a beforeTest() and afterTest() at
     * suite level makes sense.
     * @return
     */
    public static TestSuite suite() {
        List<String> xmlFiles = new ArrayList<>();
        xmlFiles.add("src/test/resources/com/force/formula/impl/formulaTestV2.xml");
        //The following hook has Postgres as the database hook
        FormulaEngine.setHooks(new BaseCustomizableParserTest.FieldTestFormulaValidationHooks());
        FormulaEngine.setFactory(BaseFieldReferenceTest.TEST_FACTORY);
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        IFormulaTestDefinitionParser<FormulaTestDefinition> parser = new FormulaTestDefinitionFileParser();
        String goldFileDirectory = "src/test/goldfiles/FormulaFields/v2/postgres";
        return new FormulaPostgresXMLTests(xmlFiles, parser, null, goldFileDirectory);
    }

    @Override
    protected DbTester constructDbTester() throws IOException {
        return new EmbeddedPostgresTester();
    }
}
