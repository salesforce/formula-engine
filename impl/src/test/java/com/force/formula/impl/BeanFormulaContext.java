package com.force.formula.impl;

import java.beans.*;
import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.force.formula.*;
import com.force.formula.FormulaSchema.Entity;
import com.google.common.cache.*;
import com.google.common.collect.ImmutableMap;

/**
 * Example of a formula context that uses Java Beans to access its content.
 * Note: this is just for testing, and to provide an example, that's it.
 * 
 * @author stamm
 * @since 0.2
 */
public class BeanFormulaContext extends BaseObjectFormulaContext<Object> {
    static final LoadingCache<Class<?>, BeanEntity> BEAN_CACHE = CacheBuilder.newBuilder().build(
            CacheLoader.from((c)->BeanEntity.fromClass(c)));

    
    /**
     * Generate a formula context using JavaBeans
     * @param defaultContext
     * @param topLevelFormulaType
     * @param bean
     */
    public BeanFormulaContext(FormulaRuntimeContext defaultContext, FormulaTypeSpec topLevelFormulaType, Object bean) {
        this(defaultContext, getEntityInfo(bean), topLevelFormulaType, null, bean);
    }

    // This will generate extra contexts for *all* foreign keys. Invoke filterFormulaContext if you don't want that.
    /**
     * Internal constructor of a formula context, that will work for metadata (i.e. before the bean is there)
     * @param defaultContext
     * @param beanEntity
     * @param topLevelFormulaType
     * @param bean
     */
    BeanFormulaContext(FormulaRuntimeContext defaultContext, BeanEntity beanEntity, final FormulaTypeSpec topLevelFormulaType, BeanField reference, Object bean) {
        super(defaultContext, beanEntity, topLevelFormulaType, reference, bean);
        for (BeanField field : getEntity().getFields()) {
            Entity[] fks = field.getFormulaForeignKeyDomains();
            if (fks != null && fks.length == 1) {
                // Note, this will evalue 
                addContextProvider(field.getName(), (outerContext) -> 
                    new BeanFormulaContext(this, (BeanEntity)fks[0], topLevelFormulaType, field, bean != null ? field.getRawValue(bean, field.getName()) : null));
            }
        }
    }

    static BeanEntity getEntityInfo(Object o) {
        if (o == null) return null;
        return BEAN_CACHE.getUnchecked(o.getClass());
    }
        
    @Override
    BeanEntity getEntity() {
        return (BeanEntity) super.getEntity();
    }

    @Override
    public String getFullName(boolean useDurableName, FormulaContext relativeToContext) {
        return "";
        /*
        BeanFormulaContext context = this;

        List<String> contextNames = new ArrayList<>();
        while (context.getParentContext() != null && context != relativeToContext) {
            BeanFormulaContext parentContext = (BeanFormulaContext) context.getParentContext();
            contextNames.add(parentContext.getName());
            context = parentContext;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = contextNames.size() - 1; i >= 0; i--) {
            sb.append(contextNames.get(i));
            if (i != 0)
                sb.append(".");
        }

        return sb.toString();
        */
    }

    @Override
    public FormulaReturnType getFormulaReturnType() {
        // This should return the field that defines the formula, but since we're using this "generically", we'll return a random field.
        // This may cause issues if it actually checks the return type.
        //return new ContextualFormulaFieldInfoImpl(this, entity.getFields().iterator().next());
        return super.getFormulaReturnType();
    }

    /**
     * Example Entity based on entity beans
     */
    static class BeanEntity implements EntityWithFields {
        private final BeanInfo bean;
        private final Map<String,BeanField> fields;

        /**
         * Construct from a class (you should use BEAN_CACAHE for this
         */
        static BeanEntity fromClass(Class<?> c) {
            try {
                return new BeanEntity(Introspector.getBeanInfo(c));
            } catch (IntrospectionException x) {
                throw new RuntimeException(x);
            }
        }
        
        public BeanEntity(BeanInfo bean) {
            this.bean = bean;
            ImmutableMap.Builder<String,BeanField> b = ImmutableMap.builder();
            for (PropertyDescriptor method : bean.getPropertyDescriptors()) {
                if ("class".equals(method.getName())) continue;
                b.put(method.getName().toLowerCase(), BeanField.construct(this, method)); // TreeMap instead?
            }
            fields = b.build();
        }

        @Override
        public String getName() {
            return bean.getBeanDescriptor().getName();
        }

        @Override
        public String getLabel() {
            return bean.getBeanDescriptor().getDisplayName();
        }

        @Override
        public Collection<BeanField> getFields() {
            return fields.values();
        }
        
        @Override
        public BeanField getField(String devName) {
            if (devName == null) return null;
            return fields.get(devName.toLowerCase());
        }
        @Override
        public String toString() {
            return "BeanEntity [" + this.getName() + "]";
        }
    }
    
    /**
     * Field based on BeanInfo's method descriptor
     *
     * @author stamm
     * @since 212
     */
    static class BeanField extends BaseObjectField<Object> {
        private final FeatureDescriptor desc;

        public static BeanField construct(BeanEntity entity, FeatureDescriptor desc) {
            Method method = getMethod(desc);
            Class<?> returnType = method.getReturnType();
            FormulaDataType dataType = getTypeFromJava(returnType, method);
            
            String formulaSource;
            BeanFormulaType annotation = method.getAnnotation(BeanFormulaType.class);
            if (annotation != null && annotation.formulaSource().length() > 0) {
                formulaSource = annotation.formulaSource();
            } else {
                formulaSource = null;
            }
            int scale = annotation != null ? annotation.scale() : 0;

            Entity[] fks = null;
            if (dataType == MockFormulaDataType.ENTITYID) {
                try {
                    fks = new Entity[] {BEAN_CACHE.get(returnType)};
                } catch (ExecutionException x) {
                }
            }
            return new BeanField(entity, desc, dataType, formulaSource, fks, scale);
        }
        
        BeanField(Entity entity, FeatureDescriptor desc, FormulaDataType dataType, String formulaSource,
                Entity[] foreignKeys, int scale) {
            super(entity, desc.getName(), dataType, formulaSource, foreignKeys, scale);
            this.desc = desc;
        }
        
        static Method getMethod(FeatureDescriptor desc) {
            if (desc instanceof MethodDescriptor) {
                return ((MethodDescriptor)desc).getMethod();
            } else {
                return ((PropertyDescriptor)desc).getReadMethod();
            }
        }
    
        
        @Override
        public Object getRawValue(Object bean, String devName) throws InvalidFieldReferenceException {
            if (bean == null) return null;
            try {
                return getMethod(this.desc).invoke(bean);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException x) {
                throw new MissingFieldValueException(devName);
            }
        }

        @Override
		public int getScale() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
        public String toString() {
            return "BeanField[" + getEntityInfo().getName() + "." + this.getName() + "]";
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BeanFormulaType {
        MockFormulaDataType value();
        String formulaSource() default "";
        int scale() default 0;
    }
    
    static FormulaDataType getTypeFromJava(Class<?> c, Method m) {
        BeanFormulaType annotation = m.getAnnotation(BeanFormulaType.class);
        if (annotation != null && annotation.value() != null) {
            return annotation.value();
        }
        
        if (c == String.class) {
            return MockFormulaDataType.TEXT;
        } else if (c == Integer.class || c == Integer.TYPE) {
            return MockFormulaDataType.INTEGER;
        } else if (c == Date.class) {
            return MockFormulaDataType.DATEONLY;
        } else if (c == FormulaDateTime.class) {
            return MockFormulaDataType.DATETIME;
        } else if (c == Boolean.class || c == Boolean.TYPE) {
            return MockFormulaDataType.BOOLEAN;
        } else if (Number.class.isAssignableFrom(c)) {
            return MockFormulaDataType.DOUBLE;
        } else {
            return MockFormulaDataType.ENTITYID;
        }
    }
}
