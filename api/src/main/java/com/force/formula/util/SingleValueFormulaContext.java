/**
 * 
 */
package com.force.formula.util;

import java.math.BigDecimal;
import java.util.Date;

import com.force.formula.ContextualFormulaFieldInfo;
import com.force.formula.DisplayField;
import com.force.formula.FormulaCurrencyData;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaGeolocation;
import com.force.formula.FormulaReturnType;
import com.force.formula.FormulaSchema;
import com.force.formula.FormulaTime;
import com.force.formula.FormulaTypeSpec;
import com.force.formula.InvalidFieldReferenceException;
import com.force.formula.UnsupportedTypeException;

/**
 * Class for implementing a Formula Context with a single value, normally called "Value."
 * This is intended to be used for writing validation formulas of a single value like 
 * <tt>Value &gt; 0</tt> 
 *
 * @author stamm
 * @since 0.2.6
 */
public class SingleValueFormulaContext<T> extends BaseRootFormulaContext {
    public static final String VALUE_NAME = "Value";
    
    private final FormulaDataType dataType;
    private final T value;
    
    /**
     * @param topLevelFormulaType the top level formula type
     * @param returnType the type to return for this formula
     * @param dataType type type of the single value
     * @param value the single value to return
     */
    public SingleValueFormulaContext(FormulaTypeSpec topLevelFormulaType, FormulaReturnType returnType, FormulaDataType dataType, T value) {
        super(topLevelFormulaType, returnType);
        this.dataType = dataType;
        this.value = value;
    }
    
    /**
     * @return the name of the field reference for this value
     */
    public String getValueName() {
        return VALUE_NAME;
    }
    
    public FormulaDataType getValueType() {
        return this.dataType;
    }

    public T getValue() {
         return this.value;
    }

    @Override
    public FormulaDateTime getDateTime(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.equalsIgnoreCase(getValueName())) {
            return (FormulaDateTime) getValue();
        } else {
            return super.getDateTime(fieldName);
        }
    }

    @Override
    public BigDecimal getNumber(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.equalsIgnoreCase(getValueName())) {
            return (BigDecimal) getValue();
        } else {
            return super.getNumber(fieldName);
        }
    }

    @Override
    public String getString(String fieldName, boolean useNative)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.equalsIgnoreCase(getValueName())) {
            return (String) getValue();
        } else {
            return super.getString(fieldName, useNative);
        }
    }

    @Override
    public Date getDate(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.equalsIgnoreCase(getValueName())) {
            return (Date) getValue();
        } else {
            return super.getDate(fieldName);
        }
    }

    @Override
    public FormulaTime getTime(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.equalsIgnoreCase(getValueName())) {
            return (FormulaTime) getValue();
        } else {
            return super.getTime(fieldName);
        }
    }

    @Override
    public FormulaCurrencyData getCurrency(String fieldName)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.equalsIgnoreCase(getValueName())) {
            return (FormulaCurrencyData) getValue();
        } else {
            return super.getCurrency(fieldName);
        }
    }

    @Override
    public Object getObject(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.equalsIgnoreCase(getValueName())) {
            return getValue();
        } else {
            return super.getObject(fieldName);
        }
    }

    @Override
    public String getMaskedString(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.equalsIgnoreCase(getValueName())) {
            return (String) getValue();
        } else {
            return super.getMaskedString(fieldName);
        }
    }

    @Override
    public FormulaGeolocation getLocation(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.equalsIgnoreCase(getValueName())) {
            return (FormulaGeolocation) getValue();
        } else {
            return super.getLocation(fieldName);
        }
    }

    @Override
    public DisplayField[] getDisplayFields(FormulaSchema.Entity entityInfo) {
        return new DisplayField[] {
             new DisplayField(getValueName(), getValueName(), new SingleValueFormulaFieldInfo(getValueName(), this.getValueType()))
        };
    }

    @Override
    public ContextualFormulaFieldInfo lookup(String name, boolean isDynamicRefBase) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (getValueName().equalsIgnoreCase(name)) {
            return new SingleValueFormulaFieldInfo(getValueName(), this.getValueType());
        } else {
            throw new InvalidFieldReferenceException(name, "Unknown field");
        }
    }

    @Override
    public String toDurableName(String name) throws InvalidFieldReferenceException, UnsupportedTypeException {
        return name;
    }

    @Override
    public String toJavascriptName(String name) throws InvalidFieldReferenceException {
        if (getValueName().equals(name)) {
            return getValueName();
        }
        return name;
    }
    
    @Override
    public String fromDurableName(String reference) throws InvalidFieldReferenceException, UnsupportedTypeException {
        return reference;
    }
    
    class SingleValueFormulaFieldInfo extends FormulaFieldInfoImpl {

        public SingleValueFormulaFieldInfo(String name, FormulaDataType columnType) {
            super(name, name, name);
            this.columnType = columnType;
        }

        @Override
        public FormulaDataType getDataType() {
            return columnType;
        }

        @Override
        public String getDbColumn(String standardTablAlias, String customTableAlias) {
            throw new UnsupportedOperationException();
        }

        private final FormulaDataType columnType;
    }
}
