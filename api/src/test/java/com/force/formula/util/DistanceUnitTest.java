/**
 * 
 */
package com.force.formula.util;

import junit.framework.TestCase;

/**
 * @author stamm
 */
public class DistanceUnitTest extends TestCase {


	public DistanceUnitTest(String name) {
		super(name);
	}

	public void testMiles() {
		DistanceUnit miles = DistanceUnit.getUnitByName("MI");
		assertEquals("mi", miles.getAbbreviation());
		assertEquals(7917.522, miles.getEarthMeanDiameter());
		assertEquals(12436.814474917783, miles.getMaxDistanceOnEarth());
	}

	public void testKilometers() {
		DistanceUnit kms = DistanceUnit.getUnitByName("KM");
		assertEquals("km", kms.getAbbreviation());
		assertEquals(12742.018, kms.getEarthMeanDiameter());
		assertEquals(20015.115070354455, kms.getMaxDistanceOnEarth());
	}

}
