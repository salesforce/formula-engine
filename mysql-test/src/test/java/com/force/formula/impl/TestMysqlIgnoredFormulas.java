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
 * Contains non-extended tests for formulatests.xml in Mysql
 * @author stamm
 * @since 0.2
 */
@RunWith(AllTests.class)
// @Ignore
public class TestMysqlIgnoredFormulas extends FormulaMySQLTests {

    public TestMysqlIgnoredFormulas(String owner) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super("MysqlIgnoredFormulaTests");
    }

    public static TestSuite suite()
    {
        try {
            return new TestMysqlIgnoredFormulas("no");
        } catch (ParserConfigurationException | SAXException | IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        return testCase.getTestLabels().contains("ignore");
    }
}
