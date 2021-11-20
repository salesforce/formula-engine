package com.force.formula.impl;
/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */


import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.xml.sax.SAXException;

import junit.framework.TestSuite;

/**
 * Contains non-extended tests for formulatests.xml in MariaDB
 * @author stamm
 * @since 0.2
 */
@RunWith(AllTests.class)
public class TestMariaDBExtendedFormulas extends FormulaMariaDBTests {

    public TestMariaDBExtendedFormulas(String owner) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super("MariaDBExtendedFormulaTests");
    }

    public static TestSuite suite()
    {
        try {
            return new TestMariaDBExtendedFormulas("no");
        } catch (ParserConfigurationException | SAXException | IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        return testCase.getTestLabels().contains("extended");
    }
}
