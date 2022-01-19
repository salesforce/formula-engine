package com.force.formula;

/**
 * Info about the result of the formula
 *
 * @author wmacklem
 * @since 156
 */
public interface FormulaReturnType {

    /**
     * @return the name of the field,  if it's a custom formula field
     */
    default String getName() {
        return null;
    }

    /**
     * @return the DataType of this formula
     */
    FormulaDataType getDataType();
    
    /**
     * @return scale of the number, if the return type is a number
     */
    default int getScale() {
        return 32;
    }
    
    /**
     * @return the FieldOrColumnInfo backing this formula.  This may return null as some formulas are not applicable to fields or columns.
     */
    default FormulaSchema.FieldOrColumn getFieldOrColumnInfo() {
        return null;
    }
}
