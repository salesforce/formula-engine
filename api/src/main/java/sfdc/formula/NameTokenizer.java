/*
 * Copyright, 2007, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package sfdc.formula;

/**
 * @author dchasman
 * @since 150
 */
public interface NameTokenizer {
    public abstract String toDurableName(String name) throws InvalidFieldReferenceException, UnsupportedTypeException;
}
