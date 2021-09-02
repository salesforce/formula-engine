/**
 * 
 */
package com.force.formula.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author stamm
 *
 */
public class SQLPairTest {

	@Test
	public void testGenerateGuard() {
		assertNull(SQLPair.generateGuard(new String[0], null));
		assertEquals("foo IS NULL", SQLPair.generateGuard(new String[0], "foo IS NULL"));
		assertEquals("foo IS NULL", SQLPair.generateGuard(new String[] {"foo IS NULL"}, null));
		assertEquals("bar IS NULL OR foo IS NULL", SQLPair.generateGuard(new String[] {"bar IS NULL"}, "foo IS NULL"));
	}
}
