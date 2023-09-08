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
 * Contains non-extended math tests for formulatests.xml.
 * @author stamm
 * @since 0.1.11
 */
@RunWith(AllTests.class)
@Ignore //All XML defined tests are executed using FormulaPostgresXMLTests
public class TestMathFormulas extends FormulaPostgreSQLTests {

	
    public TestMathFormulas(String owner) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super("MathFormulaTests");
    }

    public static TestSuite suite()
    {
        try {
            return new TestMathFormulas("no");
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
