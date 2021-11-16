/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.xml.sax.SAXException;

import junit.framework.TestSuite;

/**
 * Contains non-extended tests for formulatests.xml in oracle
 * @author stamm
 * @since 0.2
 */
@RunWith(AllTests.class)
public class TestOracleExtendedFormulas extends FormulaOracleTests {

    public TestOracleExtendedFormulas(String owner) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super("OracleExtendedFormulaTests");
    }

    public static TestSuite suite()
    {
        try {
            return new TestOracleExtendedFormulas("no");
        } catch (ParserConfigurationException | SAXException | IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        return testCase.getTestLabels().contains("extended");
    }
}
