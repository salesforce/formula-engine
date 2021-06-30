/*
 * Copyright, 2002, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */

package sfdc.formula;


import java.text.MessageFormat;

import com.google.common.collect.ImmutableMap;

/**
 * Content types of columns.  This classifies columns by their intended data contents.
 * From this classification, we can deduce the raw data type at the database level,
 * and we should be able to deduce the html elements to display and edit the column.
 */
public enum MockFormulaDataType implements FormulaDataType {
	TEXT("Text"),
    BOOLEAN("Boolean"),
    CURRENCY("Currency"),
    INTEGER("Integer"),
    DOUBLE("Double"),
    PERCENT("Percent"),
    DATETIME("DateTime"),
    TIMEONLY("TimeOnly"),
    DATEONLY("DateOnly"),
    STATICENUM("StaticEnum"),
    ENTITYID("EntityId"),
    ;
    //
    // Public methods
    //
    
    private final String name;
    MockFormulaDataType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getJavaName() {
        return name();
    }

    /**
     * @return the single char datatype stored in the database for the datatype of a custom field of this type
     */
    @Override
    public String getDatatype() {
        return name();
    }

    public char getDatatypeLetter() {
        return (char)('A' + ordinal());
    }

    /**
     * Localized display of column type
     */
    @Override
    public String getLabel() {
        return "null";
    }

    @Override
    public boolean isCustom() {
        return false;
    }

    /*
     * Broad categorizations of column types
     */

    @Override
    public boolean isTextOrEncrypted() {
        return this == TEXT;
    }

    @Override
    public boolean isEncrypted() {
    	return false;
    }

    @Override
    public boolean canBeEncryptedAtRest() {
        switch (this) {
        case TEXT:
        case DATETIME:
        case DATEONLY:
            return true;
        default:
            return false;
        }
    }

    @Override
    public boolean isSimpleText() {
        switch (this) {
        case TEXT:
            return true;
        default:
            return false;
        }
    }

    @Override
    public boolean isSimpleTextOrClob() {
        return isSimpleText();
    }

    @Override
    public boolean isText() {
        return (isSimpleTextOrClob());
    }

    @Override
    public boolean isPhone() {
    	return false;
    }

    @Override
    public boolean isPercent() {
        return this == PERCENT;
    }

    @Override
    public boolean isBoolean() {
        return this == BOOLEAN;
    }

    @Override
    public boolean isInteger() {
        return this == INTEGER;
    }

    @Override
    public boolean isDecimal() {
        return this == DOUBLE || this == PERCENT || this == CURRENCY;
    }

    @Override
    public boolean isNumber() {
        return isDecimal() || isInteger();
    }

    @Override
    public boolean isNumberOrDynamicEnum() {
        return isNumber();
    }

     @Override
    public boolean isDateTime() {
        return this == DATETIME;
    }

    @Override
    public boolean isDateOnly() {
        return this == DATEONLY;
    }

    @Override
    public boolean isDate() {
        return isDateTime() || isDateOnly();
    }

    @Override
    public boolean isTimeOnly() {
        return this == TIMEONLY;
    }

    @Override
    public boolean isCurrency() {
        return this == CURRENCY;
    }

    @Override
    public boolean isRaw() {
        return false;
    }

    @Override
    public boolean isLob() {
        return isBlob() || isClob();
    }

    @Override
    public boolean isBlob() {
        return false;
    }

    @Override
    public boolean isFfxBlob() {
        return false;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isMultiEnum() {
        return false;
    }

    /**
     * @return whether this column type represents a single column with a mapping from DB to displayed value
     */
    @Override
    public boolean isAnySingleEnum() {
        return this == STATICENUM;
    }

    /**
     * @return whether this column type represents a static enum
     */
    @Override
    public boolean isStaticEnum() {
        return this == STATICENUM;
    }

    /**
     * @return whether this column needs to be used with the isPickVal function in formulas
     */
    @Override
    public boolean isPickval(){
        return (isAnySingleEnum() && !isMultiEnum());
    }

    /**
     * @return whether the field has one column based on a dynamic picklist field, and no other columns
     */
    @Override
    public boolean isSingleDynamicPicklist() {
        return false;
    }

    @Override
    public boolean isAnyPerson() {
        return false;
    }

    /**
     * @return whether the field can be translated by toLabel() API call
     */
    @Override
    public boolean isTranslatable() {
        return isPickval();
    }

    /**
     * @return whether the field contains some dynamic enum column
     */
    @Override
    public boolean containsSomeDynamicEnum() {
        return false;
    }

    /**
     * @return whether the field has at least one column based on a dynamic picklist field
     */
    @Override
    public boolean containsSomeDynamicPicklist() {
        return containsSomeDynamicEnum() ;
    }

    @Override
    public boolean isSingleColumn() {
        return !isCompound();
    }

    @Override
    public boolean isSingleConcreteColumn() {
        // File is included here because, in File, The first column is the "real" column.
        // The rest are derived fields.
        return isSingleColumn() || isFile();
    }

    @Override
    public boolean isCompound() {
    	return false;
    }

    @Override
    public boolean isClob() {
    	return false;
    }

    public boolean isComplex() {
        return isCompound();
    }

    @Override
    public boolean isExternal() {
    	return false;
    }


    @Override
    public boolean isId() {
    	return this == ENTITYID;
    }

    @Override
    public boolean isIndexable() {
    	return false;
    }



    @Override
    public boolean canBeMarkedExternalId() {
    	return false;
    }

    @Override
    public boolean canBeMarkedUnique() {
    	return false;
    }

    @Override
    public boolean canBeMasked() {
    	return false;
    }

    /**g
     * @return whether the column has a specified length.
     * Certain column types like checkbox and number fields do not have a concept of length.
     * Number fields have precision and scale, and checkboxes have none of the three.
     */
    @Override
    public boolean hasLength() {
    	return false;
    }

    /**
     * @return whether both the precision and scale are defined (number columns)
     */
    @Override
    public boolean hasPrecisionAndScale() {
        return isNumber();
    }

    @Override
    public boolean hasPrecision() {
        return isNumber();
    }

    @Override
    public boolean hasScale() {
        return isNumber();
    }

    @Override
    public boolean canBeMarkedCaseInsensitive() {
        return this == TEXT;
    }

    @Override
    public boolean isLocation() {
        return false;
    }

    @Override
    public boolean isTextEnum() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * For easy debugging
     */
    @Override
    public String toString() {
        return "ColumnType." + name();
    }

    /**
     * Output the Oracle data type for this column type (for documentation)
     */
    @Override
    public String toOracleString(int length) {
        if (isInteger() || isDecimal())
            return "NUMBER";
        else if (isDate())
            return "DATE";
        else if (isId())
            return "CHAR(15)";
        else if (isBoolean())
            return "CHAR(1)";
        else if (isText())
            return MessageFormat.format("VARCHAR2({0})", length);
        else
            return toString();
    }
    
    private static final ImmutableMap<String, MockFormulaDataType> byCamelCaseName;
    static {
        ImmutableMap.Builder<String, MockFormulaDataType> builder = ImmutableMap.builder();
        for (MockFormulaDataType sdt : values()) {
            builder.put(sdt.getName(), sdt);
        }
        byCamelCaseName = builder.build();
    }

    /**
     * @return the standard data type with the specified camel-case name.
     */
    public static MockFormulaDataType fromCamelCaseName(String name) {
        return byCamelCaseName.get(name);
    }
}
