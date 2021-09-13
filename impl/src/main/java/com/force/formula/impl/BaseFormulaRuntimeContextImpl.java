/*
 * Copyright, 1999-2010, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import java.math.BigDecimal;
import java.util.*;

import com.force.formula.*;
import com.force.formula.sql.FormulaSqlStyle;

/**
 * Base Implementation for formula methods that use a FormulaFieldReference.  This provides compatibility for
 * implementations that only support String fieldName.
 *
 * @author aballard
 * @since 168
 */
public abstract class BaseFormulaRuntimeContextImpl implements FormulaRuntimeContext {

    private final FormulaSqlStyle sqlStyle = FormulaEngine.getHooks().getSqlStyle();
    
    @Override
    public Boolean getBoolean(FormulaFieldReference fieldName) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getBoolean(fieldName.getElement());
    }

    @Override
    public Boolean getBoolean(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FormulaCurrencyData getCurrency(FormulaFieldReference fieldName) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getCurrency(fieldName.getElement());
    }

    @Override
    public FormulaCurrencyData getCurrency(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCurrencyIsoCode(FormulaFieldReference fieldName) throws FormulaException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getCurrencyIsoCode(fieldName.getElement());
    }

    @Override
    public String getCurrencyIsoCode(String fieldName) throws FormulaException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Date getDate(FormulaFieldReference fieldName) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getDate(fieldName.getElement());
    }

    @Override
    public Date getDate(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FormulaDateTime getDateTime(FormulaFieldReference fieldName) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getDateTime(fieldName.getElement());
    }

    @Override
    public FormulaDateTime getDateTime(String fieldName) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FormulaTime getTime(FormulaFieldReference fieldName) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getTime(fieldName.getElement());
    }

    @Override
    public FormulaTime getTime(String fieldName) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getMaskedString(FormulaFieldReference fieldName) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getMaskedString(fieldName.getElement());
    }

    @Override
    public String getMaskedString(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BigDecimal getNumber(FormulaFieldReference fieldName) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getNumber(fieldName.getElement());
    }

    @Override
    public BigDecimal getNumber(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getObject(FormulaFieldReference fieldName) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getObject(fieldName.getElement());
    }

    @Override
    public Object getObject(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(FormulaFieldReference fieldName, boolean useNative) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getString(fieldName.getElement(), useNative);
    }

    @Override
    public String getString(String fieldName, boolean useNative) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public FormulaGeolocation getLocation(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getLocation(fieldName.getElement());
    }

    @Override
    public FormulaGeolocation getLocation(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContextualFormulaFieldInfo lookup(FormulaFieldReference fieldName) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return lookup(fieldName.getElement(), fieldName.isDynamicBase());
    }

    @Override
    public ContextualFormulaFieldInfo lookup(String fieldName, boolean isDynamicRefBase) throws InvalidFieldReferenceException, UnsupportedTypeException {
      throw new UnsupportedOperationException();
    }

    // Don't override this.  You must override the version that takes a isDynamicRefBase parameter also.
    @Override
    public final ContextualFormulaFieldInfo lookup(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        return lookup(fieldName, false);
    }

    @Override
    public Object getMapElement(Map<?, ?> base, Object key) throws UnsupportedTypeException, InvalidFieldReferenceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getListElement(List<?> base, int intNum) throws UnsupportedTypeException, InvalidFieldReferenceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isUIDeprecated(){
        return false;
    }

    @Override
    public DisplayField getDisplayField(FormulaSchema.ApiElement aei) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public FormulaSqlStyle getSqlStyle() {
        return sqlStyle;
    }
}
