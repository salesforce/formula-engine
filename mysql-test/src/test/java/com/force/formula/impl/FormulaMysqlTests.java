package com.force.formula.impl;
/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */


import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.force.formula.FormulaEngine;
import com.force.formula.impl.BaseCustomizableParserTest.FieldTestFormulaValidationHooks;
import com.force.formula.sql.MysqlContainerTester;

/**
 * Abstract class for testing formulas with oracle-xe
 * This *does* cross modules and looks in the filesystem.  Have impl generate a test jar file
 * if that is undesireable.
 * @author stamm
 * @since 0.2.0
 */
public abstract class FormulaMysqlTests extends FormulaGenericTests {

	// use a single DB with a docker container, and not three of them by sharing them.
	static DbTester SHARED_TESTER;
	static {
		try {
			SHARED_TESTER = new MysqlContainerTester();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
    public FormulaMysqlTests(String name) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super(name);
    }
    
	@Override
	protected BaseFormulaGenericTest createTestCase(boolean positive, String entity, FormulaTestCaseInfo testCase) {
		return new MysqlFormulaTest(testCase, testCase.getName(), positive, this);
	}

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        if (testCase.getTestLabels().contains("ignore")) return false;
        return true;
    }

    @Override
    protected void setUpTest(BaseFormulaGenericTest test) {
        FormulaEngine.setHooks(new MysqlFormulaValidationHooks());
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
	
	protected static class MysqlFormulaTest extends FormulaGenericTest {
		
		public MysqlFormulaTest(FormulaTestCaseInfo testCase, String name, boolean positive,
				FormulaGenericTests suite) {
			super(testCase, name, positive, suite);
		}

		@Override
		protected boolean shouldCompareSql() {
			// TODO: AddMonths in Oracle has different behavior from Postgres for leap days.
			// @see https://docs.oracle.com/cd/B19306_01/server.102/b14200/functions004.htm
			// "If date is the last day of the month or if the resulting month has fewer days than the 
			//      day component of date, then the result is the last day of the resulting month. "
			if ("testAddMonths".equals(getName()) ||
					"testAddMonthsDate".equals(getName())) {
				return false;
			}
			return super.shouldCompareSql();
		}
	}

    protected static class MysqlFormulaValidationHooks extends FieldTestFormulaValidationHooks {
        @Override
		public FormulaSqlHooks getSqlStyle() {
        	return FormulaSqlHooks.DefaultStyle.MYSQL;
		}
    }
    
}