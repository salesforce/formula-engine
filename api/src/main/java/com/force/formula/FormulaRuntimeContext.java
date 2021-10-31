/*
 * Created on Dec 8, 2004
 */
package com.force.formula;

import java.math.BigDecimal;
import java.util.*;

/**
 * Adapter interface to provide for multiple sources of pointwise data access
 *
 * @author dchasman
 * @since 144
 */
public interface FormulaRuntimeContext extends FormulaContext {

    /**
     * At runtime a context may want to handle hidden fields differently. See {@link InaccessibleFieldStrategy}.
     * For example, for security reasons we don't want to send inaccessible fields over an API where the
     * formula contents are exposed. This only applies to top level field references, e.g. if formulaA
     * references formulaB which references fieldC, when applying this control to formulaA it only looks
     * at formulaB, not fieldC.
     */
    public static final String HANDLE_INACCESSIBLE_FIELDS = "com.force.formula.impl.HANDLE_INACCESSIBLE_FIELDS";
    /**
     * At runtime, what do you do with a field a user doesn't have access to?
     * Allow (Default) - elevate access and pretend the user has access
     * Throw - Throw an InvalidFieldReferenceException (to be handled by consumer)
     * Null - Replace the field with a null value.
     */
    public static enum InaccessibleFieldStrategy {ALLOW, THROW_EXCEPTION, REPLACE_WITH_NULL};

    default BigDecimal getNumber(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getNumber(fieldName.getElement());
    }
    BigDecimal getNumber(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    /**
     * Get a masked string, for encrypted field types.  Null if the field isn't encrypted.  Probably 
     * @param fieldName the field reference to lookup
     * @return the string masked if encrypted
     * @throws InvalidFieldReferenceException if the field reference is invalid
     * @throws UnsupportedTypeException if the field reference is an unsupported type
     */
    default String getMaskedString(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getMaskedString(fieldName.getElement());
    }
    String getMaskedString(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    /**
     * @return the value of the field reference
     * @param fieldName the field reference name
     * @param useNative use the "unmasked" format if there's.
     * @throws InvalidFieldReferenceException if the field reference is invalid
     * @throws UnsupportedTypeException if the field reference is an unsupported type
     */
    default String getString(FormulaFieldReference fieldName, boolean useNative) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getString(fieldName.getElement(), useNative);
    }
    String getString(String fieldName, boolean useNative) throws InvalidFieldReferenceException, UnsupportedTypeException;

    default FormulaDateTime getDateTime(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException{
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getDateTime(fieldName.getElement());
    }
    FormulaDateTime getDateTime(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    default FormulaTime getTime(FormulaFieldReference fieldReference) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldReference.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getTime(fieldReference.getElement());
    }
    FormulaTime getTime(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    default Date getDate(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getDate(fieldName.getElement());
    }
    Date getDate(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    default Boolean getBoolean(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getBoolean(fieldName.getElement());
    }
    Boolean getBoolean(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    default FormulaCurrencyData getCurrency(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getCurrency(fieldName.getElement());
    }
    FormulaCurrencyData getCurrency(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    default Object getObject(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (fieldName.getBase() != null) {
            throw new UnsupportedOperationException();
        }
        return getObject(fieldName.getElement());
    }
    Object getObject(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    default Object getMapElement(Map<?,?> base, Object key) throws UnsupportedTypeException, InvalidFieldReferenceException {
    	return base.get(key);
    }
    default Object getListElement(List<?> base, int intNum) throws UnsupportedTypeException, InvalidFieldReferenceException  {
    	return base.get(intNum);
    }

    /**
     * To support PriorValue, override this to return the formula context of the prior value.
     */
    default FormulaRuntimeContext getOriginalValuesContext() throws FormulaException {
        return this;
    }

    /**
     * @return the currency associated with this context, if available
     */
    String getCurrencyIsoCode() throws FormulaException;
    default String getCurrencyIsoCode(FormulaFieldReference fieldName) throws FormulaException {
        return getCurrencyIsoCode(fieldName.getElement());
    }
    /**
     * @return the currency associated with that field, if available
     */
    default String getCurrencyIsoCode(String fieldName) throws FormulaException {
        return getCurrencyIsoCode();
    }

    default FormulaGeolocation getLocation(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        return getLocation(fieldName.getElement());
    }
    FormulaGeolocation getLocation(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    /**
     * Salesforce-ism to return primary keys in they 18char format, not 15char format.  Reuse this for any purpose
     * that requires IDs to be "fixed" on return
     */
    default boolean convertIdto18Digits() {
        return false;
    }
    /**
     * @return <code>true</code> if this represents a clone to support ISNEW()
     */
    default boolean isNew() {
        return false;
    }
    /**
     * @return <code>true</code> if this represents a clone to support ISCLONE()
     */
    default boolean isClone() {
        return false;
    }

}
