/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaFieldInfo.FormulaFieldType;
import com.force.formula.FormulaSchema.Entity;
import com.force.formula.FormulaSchema.Field;
import com.force.formula.util.*;
import com.google.common.collect.ImmutableSet;

/**
 * Sample class that contains similar functionality for interacting with different types of Java
 * objects.  T can be a complex object, or a 
 * 
 * @author stamm
 * @since 0.2
 */
public abstract class BaseObjectFormulaContext<T> extends BaseCompositeFormulaContext {
    T object;
    final EntityWithFields entity;
    final FormulaSchema.Field reference;
    private final Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Generate a formula context for the given object
     * @param defaultContext
     * @param topLevelFormulaType
     * @param object
     */
    protected BaseObjectFormulaContext(FormulaRuntimeContext defaultContext, EntityWithFields beanEntity, FormulaTypeSpec topLevelFormulaType, T object) {
        this(defaultContext, beanEntity, topLevelFormulaType, null, object);
    }

    // This will generate extra contexts for *all* foreign keys. Invoke filterFormulaContext if you don't want that.
    /**
     * Internal constructor of a formula context, that will work for metadata (i.e. before the bean is there)
     * @param defaultContext
     * @param beanEntity
     * @param topLevelFormulaType
     * @param reference the reference to this object from the defaultContext (if any)
     * @param object the object 
     */
    protected BaseObjectFormulaContext(FormulaRuntimeContext defaultContext, EntityWithFields beanEntity, final FormulaTypeSpec topLevelFormulaType, FormulaSchema.Field reference, T object) {
        super(defaultContext, topLevelFormulaType);
        addContextProvider("$System", (outerContext) -> new SystemFormulaContext(outerContext));
        this.object = object;
        this.entity = beanEntity;
        this.reference = reference;
        setProperty(FormulaContext.EXPAND_FORMULA_REFERENCES, true);  // Assume we need to expand formula references
        /*
        for (BaseObjectField<T> field : this.entity.getFields()) {
            Entity[] fks = field.getFormulaForeignKeyDomains();
            if (fks != null && fks.length == 1) {
                // Note, this will evalue 
                addContextProvider(field.getName(), (outerContext) -> 
                    new BeanFormulaContext(this, (BaseObjectEntity<T>)fks[0], topLevelFormulaType, field, object != null ? field.getRawValue(object, field.getName()) : null));
            }
        }
        */
    }
    
    final Object getObject() {
        return this.object;
    }
    
    EntityWithFields getEntity() {
        return this.entity;
    }

    @Override
    public String getName() {
        // This should return the path to get from here to the parent.
        return reference != null ? reference.getName() : getEntity().getName();
    }
    
    @Override
    public FormulaContext getParentContext() {
        return defaultContext instanceof BaseObjectFormulaContext ? defaultContext : null;
    }
    
    @Override
    protected Set<String> getGlobalVariablesSupportedOffline() {
        return ImmutableSet.of("$SYSTEM");
    }

    
    @Override
    public String getFullName(boolean useDurableName, FormulaContext relativeToContext) {
        return "";
    }
    
    @SuppressWarnings("unchecked")
    ObjectField<T> getField(String devName) {
        return (ObjectField<T>)entity.getField(devName.toLowerCase());                
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P> P getProperty(String name) {
        return (P)this.properties.get(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        this.properties.put(name, value);
    }



    @Override
    public ContextualFormulaFieldInfo lookup(String devName, boolean isDynamicRefBase)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        ObjectField<T> field = getField(devName);
        if (field != null) {
            return new BeanFormulaFieldInfoImpl(this, field, field.getFormulaSource());
        }
        return super.lookup(devName, isDynamicRefBase);
    }

    @Override
    public String getString(String fieldName, boolean useNative) throws InvalidFieldReferenceException, UnsupportedTypeException {
        ObjectField<T> field = getField(fieldName);
        if (field != null) {
            return (String) field.getRawValue(this.object, fieldName);
        } else {
            return super.getString(fieldName, useNative);
        }        
    }

    
    @Override
    public FormulaCurrencyData getCurrency(String fieldName)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        ObjectField<T> field = getField(fieldName);
        if (field != null) {
            Object value = field.getRawValue(this.object, fieldName);
            if (value == null)
                return null;
            if (value instanceof FormulaCurrencyData) { 
                return (FormulaCurrencyData) value;
            }
            // TODO: Check if it's a string with a currency value
            BigDecimal num = value instanceof BigDecimal? (BigDecimal)value:new BigDecimal(String.valueOf(value));
            return new MockCurrencyData(MockCurrencyData.EMPTY_ISO_CODE, num);
        } else {
            return super.getCurrency(fieldName);
        }    
    }

    @Override
    public BigDecimal getNumber(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        ObjectField<T> field = getField(fieldName);
        if (field != null) {
            Number value = (Number) field.getRawValue(this.object, fieldName);
            if (value == null)
                return null;
            return (value instanceof BigDecimal)? (BigDecimal)value :new BigDecimal(String.valueOf(value));
        } else {
            return super.getNumber(fieldName);
        }    
    }

    @Override
    public FormulaDateTime getDateTime(String fieldName)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        ObjectField<T> field = getField(fieldName);
        if (field != null) {
            Date d = (Date) field.getRawValue(this.object, fieldName);
            return (d == null) ? null : new FormulaDateTime(d);
        } else {
            return super.getDateTime(fieldName);
        }  
    }

    @Override
    public Date getDate(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        ObjectField<T> field = getField(fieldName);
        if (field != null) {
            return (Date) field.getRawValue(this.object, fieldName);
        } else {
            return super.getDate(fieldName);
        }  
    }

    @Override
    public Boolean getBoolean(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        ObjectField<T> field = getField(fieldName);
        if (field != null) {
            Object value = field.getRawValue(this.object, fieldName);
            return Boolean.TRUE.equals(value) || "1".equals(value);
        } else {
            return super.getBoolean(fieldName);
        }  
    }

    @Override
    public Object getObject(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        ObjectField<T> field = getField(fieldName);
        if (field != null) {
            return field.getRawValue(this.object, fieldName);
        } else {
            return super.getObject(fieldName);
        }  
    }

    @Override
    public DisplayField getDisplayField(FormulaSchema.ApiElement aei) {
        if (aei == null) return null;
        try {
            BaseObjectField<?> fieldInfo = (BaseObjectField<?>) aei.getFieldInfo();
            return new DisplayField(getName(), getName(), lookup(fieldInfo.getName()));
        } catch (FormulaException x) {
            return null;
            // Field is not visible, NOOP
        }
    }

    
    @Override
    public DisplayField[] getDisplayFields(Entity entityInfo) {
        List<DisplayField> fields = new LinkedList<DisplayField>();
        for (FormulaSchema.Field fieldInfo : ((EntityWithFields)entityInfo).getFields()) {
            DisplayField displayField = getDisplayField(fieldInfo);
            if (displayField != null) {
                fields.add(displayField);
            }
        }
        return fields.toArray(new DisplayField[0]);
    }


    /*
    @Override
    public FormulaReturnType getFormulaReturnType() {
        // This should return the field that defines the formula, but since we're using this "generically", we'll return a random field.
        // This may cause issues if it actually checks the return type.
        //return new ContextualFormulaFieldInfoImpl(this, entity.getFields().iterator().next());
        return super.getFormulaReturnType();
    }
    */

    @Override
    public String getCurrencyIsoCode() throws FormulaException {
        return MockCurrencyData.EMPTY_ISO_CODE;
    }

    @Override
    public String toDurableName(String name) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (BaseCompositeFormulaContext.isGlobalContextFieldReference(name) || name.indexOf('.')>0)
            return super.toDurableName(name);
        String fullName = getFullName(true, null);
        StringBuilder sb = new StringBuilder(fullName);
        if (fullName.length() > 0 )
            sb.append(".");
        FormulaSchema.FieldOrColumn fieldOrColumnInfo = entity.getField(name);
        sb.append(fieldOrColumnInfo.getName());
        return sb.toString();
    }

    @Override
    public String fromDurableName(Connection conn, String reference) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (BaseCompositeFormulaContext.isGlobalContextFieldReference(reference))
            return super.fromDurableName(conn, reference);

        int lastDot = reference.lastIndexOf(".");
        if (lastDot < 0) {
            // Decode?
            return reference;
        }
        FormulaRuntimeContext context = lastDot >= 0 ? getContextFromReference(reference) : getDefaultContext();

        String name = reference.substring(lastDot + 1);
        String decodedReference = context.fromDurableName(conn, name);

        return (context == getDefaultContext()) ? decodedReference : String.format("%s.%s", context.getFullName(false, getDefaultContext()), decodedReference);
    }

    // Support all javascript functions, even the "non-offline" ones.
    @Override
    public boolean isFunctionSupportedOffline(FormulaCommandType command, int numberOfArguments) {
        return true;
    }
    
    /**
     * Represents an entity where you can get the field information easily.
     * Not part of FormulaSchema.Entity, because it doesn't respect any kind of field level security
     * @author stamm
     */
    interface EntityWithFields extends FormulaSchema.Entity {
        /**
         * @return the fields that are available on this entity.
         */
        Collection<? extends FormulaSchema.Field> getFields();
        /**
         * Note: this function is intended to be case insensitive
         * @param devName the given name
         * @return the field associated with this name
         */
        FormulaSchema.Field getField(String devName);
    }

    /**
     * Represents a field that can extract a field from a given object
     * @author stamm
     */
    interface ObjectField<T> extends FormulaSchema.Field {
        /**
         * For the given field devName for the given object, return the raw value.
         */
        Object getRawValue(T object, String devName) throws InvalidFieldReferenceException;
        /**
         * @return the formula source for this entity if it is calculated.  Mostly for testing.
         */
        String getFormulaSource();
    }


    /**
     * Extample Field based on BeanInfo's method descriptor
     *
     * @author stamm
     */
    static abstract class BaseObjectField<T> implements ObjectField<T> {
        private final FormulaSchema.Entity entity;
        private final String name;
        private final FormulaDataType dataType;
        private final String formulaSource;
        private final Entity[] foreignKeys;
        public BaseObjectField(FormulaSchema.Entity entity, String name, FormulaDataType dataType, String formulaSource, Entity[] foreignKeys) {
            this.entity = entity;
            this.name = name;
            this.dataType = dataType;
            this.formulaSource = formulaSource;
            this.foreignKeys = foreignKeys;
        }
        
        @Override
        public FormulaDataType getDataType() {
            return this.dataType;
        }

        @Override
        public Field getFieldInfo() {
            return this;
        }

        @Override
        public Entity getEntityInfo() {
            return this.entity;
        }

        @Override
        public boolean isColumnInfo() {
            return false;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Entity[] getFormulaForeignKeyDomains() {
            return foreignKeys;
        }

        @Override
        public boolean isCalculated() {
            return formulaSource != null;
        }
        
        @Override
        public String getFormulaSource() {
            return this.formulaSource;
        }

        @Override
        public FormulaFieldType getFieldType() {
            return null;
        }

        @Override
        public String getForeignKeyRelationshipName() {
            return getName();
        }
        @Override
        public String toString() {
            return "MapField[" + getEntityInfo().getName() + "." + this.getName() + "]";
        }
    }
    
    protected static class BeanFormulaFieldInfoImpl extends ContextualFormulaFieldInfoImpl {

		public BeanFormulaFieldInfoImpl(FormulaContext context, ObjectField<?> field, String formulaSource) {
			super(context, field, formulaSource);
		}

		public BeanFormulaFieldInfoImpl(FormulaContext context, ObjectField<?> field) {
			super(context, field);
		}

		@Override
		public String getDbColumn(String standardTablAlias, String customTableAlias) {
			return standardTablAlias + "." + getFieldOrColumnInfo().getName();
		}
    }
}
