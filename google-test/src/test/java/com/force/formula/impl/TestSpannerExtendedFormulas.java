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
 * Contains non-extended tests for formulatests.xml in spanner
 * @author stamm
 * @since 0.3
 */
@RunWith(AllTests.class)
public class TestSpannerExtendedFormulas extends FormulaSpannerTests {

    public TestSpannerExtendedFormulas(String owner) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super("SpannerExtendedFormulaTests");
    }

    public static TestSuite suite()
    {
        try {
            return new TestSpannerExtendedFormulas("no");
        } catch (ParserConfigurationException | SAXException | IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        return testCase.getTestLabels().contains("extended");
    }
}
