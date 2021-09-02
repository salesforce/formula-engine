/**
 * 
 */
package com.force.formula.sql;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.force.i18n.settings.SettingsSectionNotFoundException;

/**
 * @author stamm
 *
 */
public class InvalidFormulaTest {

	@Test(expected = SettingsSectionNotFoundException.class)
	public void testEvaluate() throws Exception {
		new InvalidFormula(new SettingsSectionNotFoundException("foo")).evaluate(null);
	}

	@Test(expected = SettingsSectionNotFoundException.class)
	public void testEvaluateRaw() throws Exception {
		new InvalidFormula(new SettingsSectionNotFoundException("foo")).evaluateRaw(null);
	}
	
	@Test(expected = SettingsSectionNotFoundException.class)
	public void testBulkProcessing() throws Exception {
		new InvalidFormula(new SettingsSectionNotFoundException("foo")).bulkProcessingBeforeEvaluation(null);
	}

	@Test(expected = SettingsSectionNotFoundException.class)
	public void testCompareTo() throws Exception {
		new InvalidFormula(new SettingsSectionNotFoundException("foo")).compareTo(null);
	}
	
	public void testNonThrowingMethods() throws Exception {
		FormulaWithSql f = new InvalidFormula(new SettingsSectionNotFoundException("foo"));
		assertEquals("null", f.toJavascript());
		assertEquals("null", f.getJavascriptRaw());
		assertEquals("NULL", f.getSQLRaw());
		assertEquals("NULL", f.toSQL(null));
		assertEquals("NULL", f.toSQLError(null));
		assertEquals(f, f);
	}
	

}
