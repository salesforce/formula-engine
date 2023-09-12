/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import junit.framework.TestSuite;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Contains non-extended math tests for formulatests.xml in oracle
 * @author stamm
 * @since 0.2
 */
@RunWith(AllTests.class)
@Ignore //All XML defined tests are executed using FormulaOracleXMLTests
public class TestOracleMathFormulas extends FormulaOracleTests {

	
    public TestOracleMathFormulas(String owner) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super("OracleMathFormulaTests");
    }

    public static TestSuite suite()
    {
        try {
            return new TestOracleMathFormulas("no");
        } catch (ParserConfigurationException | SAXException | IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        if (testCase.getTestLabels().contains("ignore")) return false;
        return testCase.getTestLabels().contains("math");
    }    
}
