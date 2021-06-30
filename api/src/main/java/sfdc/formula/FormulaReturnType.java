package sfdc.formula;

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
    String getName();

    /**
     * @return the DataType of this formula
     */
    FormulaDataType getDataType();
    
    /**
     * @return scale of the number, if the return type is a number
     */
    int getScale();
    
    /**
     * @return the FieldOrColumnInfo backing this formula.  This may return null as some formulas are not applicable to fields or columns.
     */
    FormulaSchema.FieldOrColumn getFieldOrColumnInfo();
}
