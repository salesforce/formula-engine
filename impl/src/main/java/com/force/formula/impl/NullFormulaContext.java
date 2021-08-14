/*
 * Copyright, 2006, salesforce.com
 */

package com.force.formula.impl;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import com.force.formula.*;
import com.force.formula.FormulaSchema.Entity;

/**
 *
 * @author dchasman
 * @since 144
 */
public abstract class NullFormulaContext extends BaseFormulaRuntimeContextImpl {

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
    public boolean convertIdto18Digits() {
        return false;
    }

    @Override
    public boolean isNew() {
        return false;
    }

    @Override
    public boolean isClone() {
        return false;
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
