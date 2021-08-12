/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.force.formula.*;
import com.force.formula.FormulaSchema.Entity;
import com.google.common.collect.ImmutableSet;

/**
 * A formula context that works on a Map (instead of a Bean/POJO), but with a "complex" schema
 * associated with it.
 * 
 * This is used in test code, but can be used for whatever 
 *
 * @author stamm
 * @since 0.2.0
 */
public class MapFormulaContext extends BaseObjectFormulaContext<Map<String,?>> {
 
    /**
     * Generate a formula context using JavaBeans
     * @param defaultContext
     * @param topLevelFormulaType
     * @param bean
     */
    public MapFormulaContext(FormulaRuntimeContext defaultContext, MapEntity mapEntity, FormulaTypeSpec topLevelFormulaType, Map<String,?> bean) {
        this(defaultContext, mapEntity, topLevelFormulaType, null, bean);
    }

    // This will generate extra contexts for *all* foreign keys. Invoke filterFormulaContext if you don't want that.
    /**
     * Internal constructor of a formula context, that will work for metadata (i.e. before the bean is there)
     * @param defaultContext
     * @param mapEntity
     * @param topLevelFormulaType
     * @param bean
     */
    @SuppressWarnings("unchecked")
    MapFormulaContext(FormulaRuntimeContext defaultContext, MapEntity mapEntity, final FormulaTypeSpec topLevelFormulaType, FormulaSchema.Field reference, Map<String,?> map) {
        super(defaultContext, mapEntity, topLevelFormulaType, reference, map);
        for (FormulaSchema.Field field : this.entity.getFields()) {
            Entity[] fks = field.getFormulaForeignKeyDomains();
            if (fks != null && fks.length == 1) {
                try {
                    Map<String,?> subMap = map != null ? (Map<String,?>)((BaseObjectField<Map<String,?>>)field).getRawValue(map, field.getName()) : null;
                    addContextProvider(field.getName(), (outerContext) -> new MapFormulaContext(this, (MapEntity)fks[0], topLevelFormulaType, field, subMap));
                } catch (InvalidFieldReferenceException x) {
                    throw new AssertionError();
                }
            }
        }
    }
    
    @Override
    MapEntity getEntity() {
        return (MapEntity) super.getEntity();
    }

    @Override
    public String getName() {
        // This should return the path to get from here to the parent.
        return reference != null ? reference.getName() : getEntity().getName();
    }
    
    @Override
    public FormulaContext getParentContext() {
        return defaultContext instanceof MapFormulaContext ? defaultContext : null;
    }
    
    @Override
    protected Set<String> getGlobalVariablesSupportedOffline() {
        return ImmutableSet.of("$SYSTEM");
    }

    
    @Override
    public String getFullName(boolean useDurableName, FormulaContext relativeToContext) {
        return "";
    }


    @Override
    public FormulaReturnType getFormulaReturnType() {
        // This should return the field that defines the formula, but since we're using this "generically", we'll return a random field.
        // This may cause issues if it actually checks the return type.
        //return new ContextualFormulaFieldInfoImpl(this, entity.getFields().iterator().next());
        return super.getFormulaReturnType();
    }

    public static class MapFieldInfo {
        final String name;
        final FormulaDataType dataType;
        final String formulaSource;

        public MapFieldInfo(String name, FormulaDataType dataType) {
            this(name, dataType, null);
        }
        
        public MapFieldInfo(String name, FormulaDataType dataType, String formulaSource) {
            this.name = name;
            this.dataType = dataType;
            this.formulaSource = !FormulaTextUtil.isNullEmptyOrWhitespace(formulaSource) ? formulaSource : null ;
        }
        
    }
    

    /**
     * Example Entity based on entity beans
     */
    static class MapEntity implements EntityWithFields {
        private final String name;
        private final Map<String,MapField> fields;
        
        private static Function<MapField,String> TO_LOWER = (b)->b.getName().toLowerCase();
        
        MapEntity(String name, Collection<MapFieldInfo> infos) {
            this.name = name;
            this.fields = infos.stream().map((a)->new MapField(this, a.name, a.dataType, a.formulaSource, null)).collect(Collectors.toMap(TO_LOWER, (c)->c));
        }
        @Override
        public String getName() {
            return name;
        }
        @Override
        public String getLabel() {
            return name;
        }
        @Override
        public Collection<MapField> getFields() {
            return fields.values();
        }
        @Override
        public MapField getField(String devName) {
            if (devName == null) return null;
            return fields.get(devName.toLowerCase());
        }
        @Override
        public String toString() {
            return "MapEntity [" + this.getName() + "]";
        }
    }
    
    
    /**
     * Field based on BeanInfo's method descriptor
     *
     * @author stamm
     */
    static class MapField extends BaseObjectFormulaContext.BaseObjectField<Map<String,?>> {
        public MapField(MapEntity entity, String name, FormulaDataType dataType, String formulaSource, Entity[] foreignKeys) {
            super(entity, name, dataType, formulaSource, foreignKeys);
        }
        
        @Override
        public Object getRawValue(Map<String,?> bean, String devName) throws InvalidFieldReferenceException {
            if (bean == null) return null;
            return bean.get(devName);
        }

    }
}
