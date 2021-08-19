/*
 * Created on Dec 8, 2004
 */
package com.force.formula;

import com.force.formula.util.FormulaI18nUtils;

/**
 * Thrown when a reference is encountered to a field that is not visible in the current formula context
 *
 * @author dchasman
 * @since 140
 */
public class InvalidFieldReferenceException extends FormulaException {

    private static final long serialVersionUID = 1L;
    protected final String fieldName;
    private final String reason;
    private int location;

	public InvalidFieldReferenceException(String fieldName, String reason) {
        this(fieldName, reason, false);
    }

    public InvalidFieldReferenceException(String fieldName, String reason, boolean isRuntimeError) {
        this(fieldName, reason, FormulaI18nUtils.getLocalizer().getLabel(isRuntimeError ? "FormulaFieldExceptionMessages_runtime" : "FormulaFieldExceptionMessages", "InvalidFieldReferenceException", fieldName));
    }

    public InvalidFieldReferenceException(String fieldName, String reason, String message) {
        this(fieldName, reason, message, -1);
    }

    public InvalidFieldReferenceException(String fieldName, String reason, String message, Throwable t) {
        this(fieldName, reason, message, -1, t);
    }

    public InvalidFieldReferenceException(String fieldName, String reason, String message, int location) {
        this(fieldName, reason, message, location, null);
    }

    public InvalidFieldReferenceException(String fieldName, String reason, String message, int location, Throwable t) {
        super(message, t);

        this.fieldName = fieldName;
        this.reason = reason;
        this.location = location;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getReason() {
        return reason;
    }

    public int getLocation() {
        return location;
    }

    public void setLocation(int loc) {
        this.location = loc;
    }

    @Override
    public String toString() {
        String s = getClass().getName();
        s = getMessage() != null ? (s + ": " + getMessage()) : s;
        s = reason != null ? (s + " Reason: " + reason) : s;
        s = location != -1 ? (s + " Location: " + location) : s;
        return s;
    }

}
