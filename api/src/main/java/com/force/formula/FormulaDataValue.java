/**
 * 
 */
package com.force.formula;

/**
 * A value that represents more than one scalar value, or a formula-specific implementation
 * of a scalar value that could be treated nullable.  Often it is only part of a type
 * which could be null, like the Location inside an address, or another type that allows
 * null as a value.
 * 
 * Common uses are Location & Currency (which both represent two or more scalar values)
 *
 * @author stamm(shansma)
 * @since 0.0.3 (132)
 */
public interface FormulaDataValue {

    /**
     * Returns true if the field is logically empty -- null or empty string or 
     * other such value. Some data types may never return true for this.
     */
	default boolean isEmpty() {
		return false;
	}

    /**
     * Returns the value of this FieldData object. 
     * Some FieldObjects return a basic Java class (e.g., String) that we need
     * to wrap. This allows you to get the actual value. For non-basic classes,
     * the implementation of this will just say "<code>return this</code>".
     */
    default Object getNativeValue() {
    	return this;
    }
}
