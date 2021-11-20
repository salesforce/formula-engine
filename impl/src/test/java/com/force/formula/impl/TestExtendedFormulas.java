/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.xml.sax.SAXException;

import junit.framework.TestSuite;

/**
 * Contains non-extended tests for formulatests.xml
 * @author stamm
 * @since 0.2
 */
@RunWith(AllTests.class)
public class TestExtendedFormulas extends FormulaPostgreSQLTests {

    public TestExtendedFormulas(String owner) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super("ExtendedFormulaTests");
    }

    public static TestSuite suite()
    {
        try {
            return new TestExtendedFormulas("no");
        } catch (ParserConfigurationException | SAXException | IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        return testCase.getTestLabels().contains("extended");
    }
    
	@Override
	protected boolean ignoreJavascriptValueMismatchInAutobuilds(String testName) {
		if ("testDistance".equals(testName)) {
			// TODO: Implement distance functions in Javascript
			return true;
		}
		if ("testAddMonthsDate".equals(testName)) {
			// TODO: AddMonths in javascript doesn't handle leap year + daylight savings + last day of month.
			return true;
		}
		return false;
	}

}
