/**
 * 
 */
package com.force.formula;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author stamm
 * 0.1.23
 */

public class FormulaPropertiesTest {

	@Test(expected = IllegalArgumentException.class)
	public void testMaxSqlSize() {
		FormulaProperties props = new FormulaProperties();
		props.setMaxSqlSize(500000);
	}

	public void testFormulaProps() {
		FormulaProperties props = new FormulaProperties();
		props.setMaxSqlSize(15000);
		Assert.assertEquals(15000, props.getMaxSqlSize());
	}

}
