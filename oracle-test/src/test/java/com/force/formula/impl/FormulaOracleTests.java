/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.force.formula.FormulaEngine;
import com.force.formula.impl.BaseCustomizableParserTest.FieldTestFormulaValidationHooks;
import com.force.formula.sql.OracleContainerTester;

/**
 * Abstract class for testing formulas with oracle-xe
 * This *does* cross modules and looks in the filesystem.  Have impl generate a test jar file
 * if that is undesireable.
 * @author stamm
 * @since 0.2.0
 */
public abstract class FormulaOracleTests extends FormulaGenericTests {

	// use a single DB with a docker container, and not three of them by sharing them.
	static DbTester SHARED_TESTER;
	static {
		try {
			SHARED_TESTER = new OracleContainerTester();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
    public FormulaOracleTests(String name) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super(name);
    }
    
	@Override
	protected BaseFormulaGenericTest createTestCase(boolean positive, String entity, FormulaTestCaseInfo testCase) {
		return new OracleFormulaTest(testCase, testCase.getName(), positive, this);
	}

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        if (testCase.getTestLabels().contains("ignore")) return false;
        return true;
    }

    @Override
    protected void setUpTest(BaseFormulaGenericTest test) {
        FormulaEngine.setHooks(new OracleFormulaValidationHooks());
        FormulaEngine.setFactory(BaseFieldReferenceTest.TEST_FACTORY);
    }

	@Override
	protected boolean shouldTestSql() {
		return true;
	}

	@Override
	protected boolean shouldTestJavascript() {
		return false; // don't duplicate effort
	}

	@Override
	protected DbTester constructDbTester() throws IOException {
		return SHARED_TESTER;
	}
	
	protected static class OracleFormulaTest extends FormulaGenericTest {
		
		public OracleFormulaTest(FormulaTestCaseInfo testCase, String name, boolean positive,
				FormulaGenericTests suite) {
			super(testCase, name, positive, suite);
		}
	}

    protected static class OracleFormulaValidationHooks extends FieldTestFormulaValidationHooks {
        @Override
		public FormulaSqlHooks getSqlStyle() {
        	return FormulaSqlHooks.DefaultStyle.ORACLE;
		}
    }
    
}