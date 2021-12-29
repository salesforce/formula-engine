/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.force.formula.FormulaEngine;
import com.force.formula.impl.BaseCustomizableParserTest.FieldTestFormulaValidationHooks;
import com.force.formula.impl.sql.FormulaDefaultSqlStyle;
import com.force.formula.sql.MSSQLServerContainerTester;

/**
 * Abstract class for testing formulas with SQLServer
 * This *does* cross modules and looks in the filesystem.  Have impl generate a test jar file
 * if that is undesireable.
 * @author stamm
 * @since 0.2.0
 */
public abstract class FormulaMsSqlServerTests extends FormulaGenericTests {

	// use a single DB with a docker container, and not three of them by sharing them.
	static DbTester SHARED_TESTER;
	static {
		try {
			SHARED_TESTER = new MSSQLServerContainerTester();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
	
    public FormulaMsSqlServerTests(String name) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super(name);
    }
    
	@Override
	protected BaseFormulaGenericTest createTestCase(boolean positive, String entity, FormulaTestCaseInfo testCase) {
		return new MSSQLFormulaTest(testCase, testCase.getName(), positive, this);
	}

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        if (testCase.getTestLabels().contains("ignore")) return false;
        return true;
    }

    @Override
    protected void setUpTest(BaseFormulaGenericTest test) {
        FormulaEngine.setHooks(new MSSQLFormulaValidationHooks());
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
	
	protected static class MSSQLFormulaTest extends FormulaGenericTest {
		
		public MSSQLFormulaTest(FormulaTestCaseInfo testCase, String name, boolean positive,
				FormulaGenericTests suite) {
			super(testCase, name, positive, suite);
		}
		
		@Override
		protected String getSqlErrorMessageResult(String errorViaSql, String viaFormula) {
			// SqlServer can only handle 38 digits of precision, and because of the use of 
			// floats for many of the functions like POWER and EXP, we get exceptions with overflow
			// that we choose to accept.  So ignore these errors if the BigDecimal is giant
			if (errorViaSql.startsWith("Error: Arithmetic overflow error")) {
				// If it's a date, SqlServer doesn't like values beyond 2049.
				if (viaFormula != null && viaFormula.contains(" GMT ")) {
					return null;  // Ignore the error for big numbers
				}
				try {
					BigDecimal bd = new BigDecimal(viaFormula);
					if (bd.precision() > 20) {
						return null;  // Ignore the error for big numbers
					}
				} catch (NumberFormatException ex) {
					return super.getSqlErrorMessageResult(errorViaSql, viaFormula);
				}
			}
			return super.getSqlErrorMessageResult(errorViaSql, viaFormula);
		}
	}

    protected static class MSSQLFormulaValidationHooks extends FieldTestFormulaValidationHooks {
        @Override
		public FormulaSqlHooks getSqlStyle() {
        	return FormulaDefaultSqlStyle.TSQL;
		}
    }
    
}