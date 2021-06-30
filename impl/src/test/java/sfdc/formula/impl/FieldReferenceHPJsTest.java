/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package sfdc.formula.impl;

import sfdc.formula.*;

/**
 * Describe your class here.
 *
 * @author stamm
 * @since 212
 */
public class FieldReferenceHPJsTest extends FieldReferenceJsTest {
    /**
     * @param name
     */
    public FieldReferenceHPJsTest(String name) {
        super(name);
    }

    @Override
    protected BeanFormulaContext setupMockContext(FormulaDataType columnType) {
        BeanFormulaContext context = super.setupMockContext(columnType); 
        context.setProperty(FormulaContext.HIGHPRECISION_JS, Boolean.TRUE);
        return context;
    }


}
