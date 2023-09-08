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
 * Contains non-extended tests for formulatests.xml
 * @author stamm
 * @since 0.2
 */
@RunWith(AllTests.class)
@Ignore //All XML defined tests are executed using FormulaPostgresXMLTests
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
		if ("testFormatCurrency".equals(testName)) {
			// TODO: The negative sign shows up in front of the code in javascript.  
			// "-USD 100.00" instead of "USD -100.00"
			return true;
		}

		if ("testInitCap".equals(testName)) {
			// TODO: Initcap function doesn't handle decomposed dotted I correctly.
			return true;
		}
		return false;
	}

}
