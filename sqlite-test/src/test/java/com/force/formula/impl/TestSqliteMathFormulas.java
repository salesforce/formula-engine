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
 * Contains non-extended math tests for formulatests.xml in spanner
 * @author stamm
 * @since 0.3
 */
@RunWith(AllTests.class)
public class TestSqliteMathFormulas extends FormulaSqliteTests {

	
    public TestSqliteMathFormulas(String owner) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super("SqliteMathFormulaTests");
    }

    public static TestSuite suite()
    {
        try {
            return new TestSqliteMathFormulas("no");
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
