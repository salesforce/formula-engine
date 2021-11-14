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

import com.force.formula.FormulaEngine;
import com.force.formula.impl.BaseCustomizableParserTest.FieldTestFormulaValidationHooks;
import com.force.formula.sql.EmbeddedPostgresqlTester;

import junit.framework.TestSuite;

/**
 * Contains non-extended math tests for formulatests.xml.
 * @author stamm
 * @since 0.1.11
 */
@RunWith(AllTests.class)
public class TestMathFormulas extends FormulaGenericTests {

	
    public TestMathFormulas(String owner) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super("MathFormulaTests");
    }

    public static TestSuite suite()
    {
        try {
            return new TestMathFormulas("no");
        } catch (ParserConfigurationException | SAXException | IOException x) {
            throw new RuntimeException(x);
        }
    }

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        if (testCase.getTestLabels().contains("ignore")) return false;
        return testCase.getTestLabels().contains("math");
    }

    @Override
    protected void setUpTest(BaseFormulaGenericTest test) {
        FormulaEngine.setHooks(new FieldTestFormulaValidationHooks());
        FormulaEngine.setFactory(BaseFieldReferenceTest.TEST_FACTORY);
    }

	@Override
	protected boolean shouldTestSql() {
		return true;
	}

	@Override
	protected DbTester constructDbTester() throws IOException {
		return new EmbeddedPostgresqlTester();
	}

	@Override
	protected boolean ignoreJavascriptValueMismatchInAutobuilds(String testName) {
		if ("testISNUMBER".equals(testName)) {
			// The javascript isNumber for ", " returns true, because javascript
			return true;
		}
		if ("testVALUE".equals(testName)) {
			// The javascript value for "+1" returns error,  because javascript
			return true;
		}
		return false;
	}

    
}
