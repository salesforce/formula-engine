/**
 * 
 */
package com.force.formula;

/**
 * The field reference holds both fieldOrColumnId and optionally can specify which domain to use.
 * @author stamm
 * @since 0.0.1
 */
public interface FormulaFieldReferenceInfo {

    /**
     * @return the field or column associated with the field reference.  Probably a foreign key
     */
    public FormulaSchema.FieldOrColumn getFieldOrColumn();

    /**
     * @return The domain to use when multiple domains are available.
     */
    public FormulaSchema.Entity getForeignKeyDomain();
}
