/**
 * 
 */
package com.force.formula.sql;

import static org.junit.Assert.*;

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
		assertEquals(SQLPair.generateGuard(new String[0], "foo IS NULL"), SQLPair.generateGuard(new String[] {"foo IS NULL"}, null));
	} 
	
	@Test
	public void testSQLPair() {
		SQLPair pair1 = new SQLPair("bar IS NULL", "foo IS NULL");
		SQLPair pair2 = new SQLPair("bar IS NULL", null);
		assertNotEquals(pair1, pair2);
		assertNotEquals(pair1.hashCode(), pair2.hashCode());
		assertNotEquals(pair1.toString(), pair2.toString());
	}
}
