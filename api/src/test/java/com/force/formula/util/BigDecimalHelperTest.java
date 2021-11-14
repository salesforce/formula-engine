package com.force.formula.util;

import java.math.*;

import com.force.formula.util.BigDecimalHelper.FormulaPowerException;

import junit.framework.TestCase;

/**
 * Validate BigDecimal 
 * @author stamm
 * @since 0.1.0
 */
public class BigDecimalHelperTest extends TestCase {

	public BigDecimalHelperTest(String name) {
		super(name);
	}
	
	public void testRound() throws Exception {
		assertEquals(new BigDecimal("2.35"), BigDecimalHelper.round(new BigDecimal("2.34568"), 2));
		assertEquals(new BigDecimal("2.35"), BigDecimalHelper.roundNumberToScale(2.34568d, 2));
		assertEquals(new BigDecimal("2.35"), BigDecimalHelper.roundNumberToScale(2.34568f, 2));
	}
	
	public void testRoundWithMode() throws Exception {
		assertEquals(new BigDecimal("2.35"), BigDecimalHelper.round(new BigDecimal("2.34568"), 2, RoundingMode.HALF_UP));
		assertEquals(new BigDecimal("2.34"), BigDecimalHelper.round(new BigDecimal("2.34568"), 2, RoundingMode.DOWN));
		assertEquals(new BigDecimal("2.35"), BigDecimalHelper.round(new BigDecimal("2.35"), 2, RoundingMode.UP));
	}

	public void testIsNumber() throws Exception {
		assertTrue(BigDecimalHelper.functionIsNumber("2.34568"));
		assertTrue(BigDecimalHelper.functionIsNumber("-2.34568"));
		assertTrue(BigDecimalHelper.functionIsNumber("+2.34568"));
		assertFalse(BigDecimalHelper.functionIsNumber("!2.34568"));
		assertFalse(BigDecimalHelper.functionIsNumber(""));
	}

	public void testExceedsNumericLimits() throws Exception {
		assertFalse(BigDecimalHelper.exceedsNumericLimits(new BigDecimal("2.34568"), 8, 2));
		assertTrue(BigDecimalHelper.exceedsNumericLimits(new BigDecimal(new BigInteger("1"),10000), 8, 2));
		assertTrue(BigDecimalHelper.exceedsNumericLimits(Double.POSITIVE_INFINITY, 1, 1));
		assertTrue(BigDecimalHelper.exceedsNumericLimits(Double.NaN, 1, 1));
	}

	public void testFormatBigDecimal() throws Exception {
		assertEquals("2.34568", BigDecimalHelper.formatBigDecimal(new BigDecimal("2.34568")));
		assertEquals("0", BigDecimalHelper.formatBigDecimal(new BigDecimal("0")));
		assertEquals("1000", BigDecimalHelper.formatBigDecimal(new BigDecimal(new BigInteger("1"),-3)));
		assertEquals("0.001", BigDecimalHelper.formatBigDecimal(new BigDecimal(new BigInteger("1"),3)));
	}
	
	
	public void testFormulaPower() throws Exception {
		assertEquals(new BigDecimal("4"), BigDecimalHelper.formulaPower(new BigDecimal("2"), new BigDecimal("2")));
		try {
			BigDecimalHelper.formulaPower(new BigDecimal("200"), new BigDecimal("100"));
			fail();
		} catch (FormulaPowerException ex) {
			assertEquals("Out of range argument to POWER() function", ex.getMessage());
		}
	}

	
}

