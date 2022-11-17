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
import com.force.formula.impl.sql.FormulaDefaultSqlStyle;
import com.force.formula.sql.EmbeddedH2Tester;

/**
 * Abstract class for testing formulas with embedded H2
 * @author stamm
 * @since 0.3.0
 */
public abstract class FormulaH2Tests extends FormulaGenericTests {
    public FormulaH2Tests(String name) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super(name);
    }
    
	@Override
	protected BaseFormulaGenericTest createTestCase(boolean positive, String entity, FormulaTestCaseInfo testCase) {
		return new H2FormulaTest(testCase, testCase.getName(), positive, this);
	}

    @Override
    protected boolean filterTests(FormulaTestCaseInfo testCase) {
        if (testCase.getTestLabels().contains("ignore")) return false;
        return true;
    }

    @Override
    protected void setUpTest(BaseFormulaGenericTest test) {
        FormulaEngine.setHooks(new H2FormulaValidationHooks());
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
		return new EmbeddedH2Tester();
	}
	
	protected static class H2FormulaTest extends FormulaGenericTest {
		
		public H2FormulaTest(FormulaTestCaseInfo testCase, String name, boolean positive,
				FormulaGenericTests suite) {
			super(testCase, name, positive, suite);
		}
	}

    protected static class H2FormulaValidationHooks extends FieldTestFormulaValidationHooks {
        @Override
		public FormulaSqlHooks getSqlStyle() {
            return FormulaDefaultSqlStyle.H2;
            
		}
    }
    
}