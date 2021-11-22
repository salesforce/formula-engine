package com.force.formula.impl;
/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */


import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Ignore;
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
@Ignore
public class TestMariaDBStandardFormulas extends FormulaMariaDBTests {

	
    public TestMariaDBStandardFormulas(String owner) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super("MariaDBStandardFormulaTests");
    }

    public static TestSuite suite()
    {
        try {
            return new TestMariaDBStandardFormulas("no");
        } catch (ParserConfigurationException | SAXException | IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        if (testCase.getTestLabels().contains("ignore")) return false;
        return testCase.getTestLabels().size() == 0 || testCase.getTestLabels().contains("basic");
    }    
}
