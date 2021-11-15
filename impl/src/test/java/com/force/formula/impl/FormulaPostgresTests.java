/**
 * 
 */
package com.force.formula.impl;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.force.formula.FormulaEngine;
import com.force.formula.impl.BaseCustomizableParserTest.FieldTestFormulaValidationHooks;
import com.force.formula.sql.EmbeddedPostgresqlTester;
import com.force.formula.sql.PostgresContainerTester;

/**
 * Abstract class for testing formulas with javascript and postgres
 * @author stamm
 * @since 0.1.11
 */
public abstract class FormulaPostgresTests extends FormulaGenericTests {
	// If true, use docker for testing the postgres DB with testcontainers.org.  Otherwise, use embedded
	// The embedded one is quicker and easier to manage.
	public static final boolean USE_DOCKER_FOR_DB = false;
	
    public FormulaPostgresTests(String name) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        super(name);
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
		if (USE_DOCKER_FOR_DB) {
			return new PostgresContainerTester();
		} else {	
			return new EmbeddedPostgresqlTester();
		}
	}
}
