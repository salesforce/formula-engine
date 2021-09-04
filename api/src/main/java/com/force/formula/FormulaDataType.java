/*
 * Copyright, 1999-2015, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula;

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
    default String getDatatype() {
        return getJavaName();
    }

    /**
     * Localized display of column type
     */
    String getLabel();

    /*
     * Broad categorizations of column types
     */
    
    default boolean isCustom() {
        return false;
    }
    // Is simple text that will be <4000 chars
    boolean isSimpleText();

    default boolean isSimpleTextOrClob() {
        return isSimpleText() || isClob();
    }
    // Things that can be considered text, but also picklists, like comboboxes or currency iso codes
    default boolean isText() {
        return isSimpleText();
    }
    /**
     * @return
     */
    default boolean isTextOrEncrypted() {
        return isText();
    }

    /**
     * Returns true if the contents of fields of this type are *always* encrypted. This is true only for legacy
     * encrypted fields types (ENCRYPTEDTEXT).
     *
     * Please try to avoid using this; where possible, use FieldInfo.getEncryption() instead.
     */
    default boolean isEncrypted() {
        return false;
    }

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
    default boolean canBeEncryptedAtRest() {
        return false;
    }

    boolean isBoolean();
    boolean isInteger();
    boolean isDecimal();
    boolean isPercent();
    default boolean isNumber() {
        return isInteger() || isDecimal() || isPercent();        
    }
    default boolean isCurrency()  {
        return false;
    }
    boolean isDateTime();
    boolean isDateOnly();
    default boolean isDate() {
        return isDateTime() || isDateOnly();
    }
    default boolean isTimeOnly() {
        return false;
    }
    default boolean isId() {
        return false;
    }
    default boolean isRaw() {
        return false;
    }
    default boolean isClob() {
        return false;
    }
    default boolean isBlob() {
        return false;
    }
    default boolean isLob() {
        return isClob() || isBlob();
    }
    /**
     * @return if this is a picklist value that can be only from a set of choices,
     */
    default boolean isAnySingleEnum() {
        return isStaticEnum() || isMultiEnum();
    }
    default boolean isStaticEnum() {
        return false;
    }
    default boolean isSingleDynamicPicklist() {
        return false;
    }

    default boolean isMultiEnum() {
        return false;
    }
    default boolean isAnyPerson() {
        return false;
    }
    default boolean isLocation() {  // Location or address
        return false;
    }
    default boolean isTextEnum() { // Is TextEnum, i.e. an editable combo box.
        return false;
    }

    /*
     * Schema properties
     */
    
    /**
     * @return whether or not you should test for IdTraits.EMPTY_KEY for values.  In order to prevent outer joins,
     * and the performance issues associated with it, there are some "empty_key" rows with empty values to prevent
     * the perf issues.  It's not a big deal these days, but it is for salesforce internally.
     */
    default boolean canBeEmptyKeyForNullInDb() {
        return isId();
    }

    /**
     * @return whether this column needs to be used with the isPickVal function in formulas
     */
    default boolean isPickval() {
        return false;
    }
    

}
