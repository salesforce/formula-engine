/*
 * Copyright, 2002, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */

package com.force.formula;


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
    LIST("List"),
    MAP("Map")
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



    @Override
    public boolean isClob() {
    	return false;
    }

    @Override
    public boolean isId() {
    	return this == ENTITYID;
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
    
    private static final ImmutableMap<String, MockFormulaDataType> byCamelCaseName;
    static {
        ImmutableMap.Builder<String, MockFormulaDataType> builder = ImmutableMap.builder();
        for (MockFormulaDataType sdt : values()) {
            builder.put(sdt.getName(), sdt);
        }
        byCamelCaseName = builder.build();
    }

    /**
     * @param name the DataType in CamelCase
     * @return the standard data type with the specified camel-case name.
     */
    public static MockFormulaDataType fromCamelCaseName(String name) {
        return byCamelCaseName.get(name);
    }
}
