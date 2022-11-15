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
 * Contains non-extended math tests for formulatests.xml in H2
 * @author stamm
 * @since 0.3
 */
@RunWith(AllTests.class)
public class TestH2MathFormulas extends FormulaH2Tests {

	
    public TestH2MathFormulas(String owner) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super("H2MathFormulaTests");
    }

    public static TestSuite suite()
    {
        try {
            return new TestH2MathFormulas("no");
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
