package sfdc.formula;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public interface FormulaFieldInfo extends FormulaReturnType {

    /**
     * @return Opaque internal identifier that should only be used to refer to a field and not interpreted in any way outside the internals of the FFI implementation (Memento design pattern)
     */
    Object getId();

    String getLabel();

    // From Doug: This has to do with templating support - basically we needed to preserve the auto formating behavior of merge fields when they
    // are directly referenced in an {!} expression but use the type specific fields when refs are used in an expression. See MergeFieldFormulaFieldInfo.
    FormulaDataType getTemplateDataType();

    boolean isRuntimeType();  // used for VF expressions where type cannot be known until runtime. getDataType()/getTemplateDataType should not be used.

    boolean isCustom();

    String getDbColumn(String standardTablAlias, String customTableAlias);

    FormulaPicklistInfo getEnumInfo();

    /**
     * @return 15char ids or Entity.Field combos for standard field/columns
     */
    String[] getExternalizedIds();

    /**
     * @return the entities that this field may refer to if this field is a
     * foreign key, with an option to allow fields that are not readable by the
     * current user.
     */
    FormulaSchema.Entity[] getFormulaForeignKeyDomains();
    
    /**
     * Represents any "special" fields, without comment on the underlying data store.  For example, if you want to treat the primary key 
     * "specially", you have no way to distinguish it from any other ID.
     *
     * @author stamm
     * @since 0.2
     */
    public interface FormulaFieldType {
        /**
         * @return <code>true</code> if this is a primary key field type
         */
        boolean isPrimaryKey();

        /**
         * @return the ColumnType of any field of this FieldType.  This ColumnType cannot be overriden.
         */
        FormulaDataType getDefaultColumnType();
    }
}
