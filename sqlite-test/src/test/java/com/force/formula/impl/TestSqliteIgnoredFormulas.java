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
 * Contains ignored or bad tests for formulatests.xml for Spanner
 * @author stamm
 * @since 0.3
 */
@RunWith(AllTests.class)
@Ignore
public class TestSqliteIgnoredFormulas extends FormulaSqliteTests {

	
    public TestSqliteIgnoredFormulas(String owner) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super("SqliteIgnoredFormulaTests");
    }

    public static TestSuite suite()
    {
        try {
            return new TestSqliteIgnoredFormulas("no");
        } catch (ParserConfigurationException | SAXException | IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        return testCase.getTestLabels().contains("ignore") || testCase.getTestLabels().contains("badNashorn");
    }    
}
