/**
 * 
 */
package com.force.formula;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

/**
 * @author stamm
 *
 */

public class FormulaDateTimeTest {

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void testWrapper() {
		Date d = Calendar.getInstance().getTime();
		FormulaDateTime fdt = new FormulaDateTime(d);
		assertEquals(d, fdt.getDate());
		assertEquals(d.getTime(), fdt.getDate().getTime());
		assertFalse(d.equals(fdt));
		assertFalse(fdt.equals(d));
		assertEquals(0,  new FormulaDateTime(d).compareTo(fdt));
		assertEquals(d.toString(),  fdt.toString());

		assertEquals(d,  FormulaDateTime.unwrap(fdt));
		assertEquals(d,  FormulaDateTime.unwrap(d));

		assertEquals("null",  new FormulaDateTime(null).toString());

	}
	

}
