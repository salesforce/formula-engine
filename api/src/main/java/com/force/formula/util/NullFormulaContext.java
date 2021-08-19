/*
 * Copyright, 2006, salesforce.com
 */

package com.force.formula.util;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaSchema.ApiElement;
import com.force.formula.FormulaSchema.Entity;

/**
 * Implementation of FormulaRuntimeContext that has default implementations for everyting.
 * Useful if you only return strings or one other type of value.
 * @author dchasman
 * @since 144
 */
public abstract class NullFormulaContext implements FormulaRuntimeContext {

    protected final GlobalFormulaProperties globalProperties;
    
    protected NullFormulaContext(FormulaContext outerContext){
        this.globalProperties = outerContext == null ? null : outerContext.getGlobalProperties();
    }
    
    protected NullFormulaContext(GlobalFormulaProperties globalProperties){
        this.globalProperties = globalProperties;
    }

    @Override
    public GlobalFormulaProperties getGlobalProperties() {
        return globalProperties;
    }

    @Override
    public FormulaCurrencyData getCurrency(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        return null;
    }

    @Override
    public String getCurrencyIsoCode() throws FormulaException {
        return null;
    }

    @Override
    public String getCurrencyIsoCode(String fieldName) throws FormulaException {
        return getCurrencyIsoCode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name) {
        return (T)properties.get(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }

    @Override
    public FormulaRuntimeContext getOriginalValuesContext() throws FormulaException {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public ContextualFormulaFieldInfo lookup(String fieldName, boolean isDynamicRefBase)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DisplayField getDisplayField(ApiElement aei) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String fromDurableName(String reference) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toDurableName(String name) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BigDecimal getNumber(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMaskedString(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(String fieldName, boolean useNative)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FormulaDateTime getDateTime(String fieldName)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FormulaTime getTime(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getDate(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean getBoolean(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getObject(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FormulaGeolocation getLocation(String fieldName)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DisplayField[] getDisplayFields(Entity entityInfo) {
        return null;
    }

    @Override
    public boolean isFunctionSupported(FormulaCommandType function) {
        return false;
    }

    @Override
    public FormulaContext[] getAdditionalContexts() throws InvalidFieldReferenceException {
        return new FormulaContext[0];
    }

    @Override
    public FormulaContext getParentContext() {
        return null;
    }

    @Override
    public String getFullName(boolean useDurableName, FormulaContext relativeToContext) {
        return getName();
    }

    // Provided a default implementation that ignores the connection.
    @Override
    public String fromDurableName(Connection conn, String reference) throws InvalidFieldReferenceException, UnsupportedTypeException {
        return fromDurableName(reference);
    }

    private final Map<String, Object> properties = new HashMap<String, Object>();
}
