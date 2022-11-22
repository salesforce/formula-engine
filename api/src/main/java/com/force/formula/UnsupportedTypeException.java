/*
 * Created on Dec 8, 2004 
 */
package com.force.formula;

import com.force.formula.util.FormulaI18nUtils;

/**
 * Thrown when use of an unsupported data type is encountered in a formula
 *
 * @author dchasman
 * @since 140
 */
public class UnsupportedTypeException extends FormulaException implements FormulaExceptionWithHTMLErrorMessage {

    private static final long serialVersionUID = 1L;
	private static final String MSP_SUFFIX = "_ISMULTIPICKVAL";
    private static final String PICKLIST_SUFFIX = "_ISPICKVAL";
    private static final String NOHTML_SUFFIX = "_NOHTML";

    private final String htmlFreeErrorMessage;

    public UnsupportedTypeException(Throwable e) {
        super(e.getMessage());
        htmlFreeErrorMessage = getMessage();
    }

    public UnsupportedTypeException(String name, FormulaDataType dataType) {
        super(createErrorMessage(name, dataType));
        htmlFreeErrorMessage = createHtmlFreeErrorMessage(name, dataType);
    }

    public UnsupportedTypeException(String name, FormulaSchema.Field info) {
        super(createErrorMessage(name, info));
        htmlFreeErrorMessage = createHtmlFreeErrorMessage(name, info.getDataType());
    }

    private static String createErrorMessage(String name, FormulaDataType dataType) {
        if (dataType == null) {
            return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages",
                    "UnsupportedTypeException", name);
        } else if (dataType.isPickval()) {
            return createErrorMessageForPicklists(name, PICKLIST_SUFFIX);
        } else if (dataType.isMultiEnum()) {
            return createErrorMessageForPicklists(name, MSP_SUFFIX);
        } else {
            return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "UnsupportedTypeException",
                dataType.getLabel(), name);
        }
    }
    
    private static String createErrorMessage(String name, FormulaSchema.Field info) {
        return createErrorMessage(name, info.getDataType());

    }

    private static String createErrorMessageForPicklists(String name, String suffix){
    	return FormulaEngine.getHooks().createErrorMessageForPicklists(name, suffix);
    }

    private String createHtmlFreeErrorMessage(String name, FormulaDataType dataType) {
        String suffix = null;
        if (dataType == null){
            suffix = null;
        } else if (dataType.isPickval()) {
            suffix = PICKLIST_SUFFIX;
        } else if (dataType.isMultiEnum()) {
            suffix = MSP_SUFFIX;
        }

        if (suffix != null) {
            return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages",
                    "UnsupportedTypeException" + suffix + NOHTML_SUFFIX, name);
        }

        return getMessage();
    }

    @Override
    public String getHtmlFreeErrorMessage() {
        return this.htmlFreeErrorMessage;
    }
}
