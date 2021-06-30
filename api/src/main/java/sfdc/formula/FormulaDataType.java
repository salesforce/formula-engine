/*
 * Copyright, 1999-2015, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package sfdc.formula;

/**
 * Content types of columns.  This classifies columns by their intended data contents.
 * From this classification, we can deduce the raw data type at the database level,
 * and we should be able to deduce the html elements to display and edit the column.
 * Pulled from the DataType interface in 200.
 *
 * @author stamm
 * @since 200
 */
public interface FormulaDataType {
    /*
     * Naming
     */

    /**
     * @return the name of this column type in mixed case
     */
    String getName();

    /**
     * @return the name of this column type in all-caps
     */
    String getJavaName();

    /**
     * @return the database representation of this type
     */
    String getDatatype();

    /**
     * Localized display of column type
     */
    String getLabel();

    /**
     * Output the Oracle data type for this column type (for documentation)
     */
    String toOracleString(int length);

    /*
     * Broad categorizations of column types
     */

    boolean isCustom();
    boolean isSimpleText();
    boolean isSimpleTextOrClob();
    boolean isText();
    boolean isTextOrEncrypted();

    /**
     * Returns true if the contents of fields of this type are *always* encrypted. This is true only for legacy
     * encrypted fields types (ENCRYPTEDTEXT and SFDCENCRYPTEDTEXT).
     *
     * Please try to avoid using this; where possible, use FieldInfo.getEncryption() instead.
     */
    boolean isEncrypted();

    /**
     * True if the contents of fields of this type *may* be encrypted using the newer platform encryption framework.
     * (AtRest == new).
     * <p/>
     * Generally speaking, encryption will happen for fields that
     * <ul>
     * <li>are of a DataType that canBeEncryptedAtRest</li>
     * <li>are themselves declared canBeEncryptedAtRest in udd.xml (for standard fields)</li>
     * <li>are in orgs that have the EncryptionAtRest feature enabled</li>
     * <li>have been configured by org admins to be encrypted</li>
     * </ul>
     */
    boolean canBeEncryptedAtRest();

    boolean isPhone();
    boolean isBoolean();
    boolean isInteger();
    boolean isDecimal();
    boolean isPercent();
    boolean isNumber();
    boolean isNumberOrDynamicEnum();//Dynamic Enum is treated as number in db
    boolean isCurrency();
    boolean isDateTime();
    boolean isDateOnly();
    boolean isDate();
    boolean isTimeOnly();
    boolean isId();
    boolean isRaw();
    boolean isClob();
    boolean isBlob();
    boolean isFfxBlob();
    boolean isLob();
    boolean isAnySingleEnum();
    boolean isStaticEnum();
    boolean isSingleDynamicPicklist();
    boolean containsSomeDynamicEnum();
    boolean containsSomeDynamicPicklist();
    boolean isMultiEnum();
    boolean isAnyPerson();
    boolean isLocation();  // Location or address
    boolean isTextEnum();  // Is TextEnum, i.e. an editable combo box.

    /**
     * @return true if the DataType always resides in single column with no derived fields,
     * and hence representing a single ColumnInfo/​ColumnCommon.
     * false returns include compound fields and BitVector (because BitVector may be one or more columns)
     */
    boolean isSingleColumn();
    /**
     * @return true if the DataType is stored in a single concrete column in the db but
     * may include multiple derived columns (e.g. File)
     */
    boolean isSingleConcreteColumn();
    /**
     * @return true if the DataType always has multiple columns (including derived columns)
     * It doesn't include BitVectors since they may be stored in a single column
     */
    boolean isCompound();
    boolean isFile();
    /** @return true if it is a link or Id to an external object */
    boolean isExternal();


    /*
     * Schema properties
     */

    /**
     * @return whether the column has a specified length.
     * Certain column types like checkbox and number fields do not have a concept of length.
     * Number fields have precision and scale, and checkboxes have none of the three.
     */
    boolean hasLength();

    /**
     * @return whether both the precision and scale are defined (number columns)
     */
    boolean hasPrecisionAndScale();

    boolean hasPrecision();

    boolean hasScale();



    boolean canBeMarkedExternalId();

    /**
     * @return whether custom fields of this type can be marked unique on most customizable objects. NB that this doesn't
     * include ENTITYID or ENUMORID fields, despite the fact that custom fields of those types can be marked unique on
     * customizable manageable entities.
     */
    boolean canBeMarkedUnique();

    boolean canBeMarkedCaseInsensitive();

    boolean canBeMasked();
    
    /**
     * @return whether or not you should test for IdTraits.EMPTY_KEY for values.  In order to prevent outer joins,
     * and the performance issues associated with it, there are some "empty_key" rows with empty values to prevent
     * the perf issues.  It's not a big deal these days.
     */
    default boolean canBeEmptyKeyForNullInDb() {
        return isId();
    }

    /**
     * @return whether this column needs to be used with the isPickVal function in formulas
     */
    boolean isPickval();
    
    /*
     * Functional properties
     */

    /**
     * @return whether the field can be translated by toLabel() API call
     */
    boolean isTranslatable();

    /**
     * @return supports custom index
     */
    boolean isIndexable();
}
