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

    BigDecimal getNumber(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;
    BigDecimal getNumber(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    /**
     * Get a masked string, for encrypted field types.  Null if the field isn't encrypted
     */
    String getMaskedString(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;
    String getMaskedString(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    String getString(FormulaFieldReference fieldName, boolean useNative) throws InvalidFieldReferenceException, UnsupportedTypeException;
    String getString(String fieldName, boolean useNative) throws InvalidFieldReferenceException, UnsupportedTypeException;

    FormulaDateTime getDateTime(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;
    FormulaDateTime getDateTime(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    FormulaTime getTime(FormulaFieldReference fieldReference) throws InvalidFieldReferenceException, UnsupportedTypeException;
    FormulaTime getTime(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    Date getDate(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;
    Date getDate(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    Boolean getBoolean(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;
    Boolean getBoolean(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    FormulaCurrencyData getCurrency(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;
    FormulaCurrencyData getCurrency(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    Object getObject(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;
    Object getObject(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    Object getMapElement(Map<?,?> base, Object key) throws UnsupportedTypeException, InvalidFieldReferenceException;
    Object getListElement(List<?> base, int intNum) throws UnsupportedTypeException, InvalidFieldReferenceException;

    FormulaRuntimeContext getOriginalValuesContext() throws FormulaException;

    String getCurrencyIsoCode() throws FormulaException;
    String getCurrencyIsoCode(FormulaFieldReference fieldName) throws FormulaException;
    String getCurrencyIsoCode(String fieldName) throws FormulaException;

    FormulaGeolocation getLocation(FormulaFieldReference fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;
    FormulaGeolocation getLocation(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException;

    boolean convertIdto18Digits();
    boolean isNew();
    boolean isClone();

}
