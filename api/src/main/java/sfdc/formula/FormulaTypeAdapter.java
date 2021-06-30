/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package sfdc.formula;

/**
 * Describe your class here.
 *
 * @author stamm
 * @since 0.2.0
 */
public interface FormulaTypeAdapter {
    /**
     * @return the underlying java class that represents the value, when this is an "adapter" class.
     */
    Class<?> getType();
}
