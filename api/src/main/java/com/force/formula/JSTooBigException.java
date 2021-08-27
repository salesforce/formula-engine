/*
 * Copyright, 1999-2017, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula;

import com.force.formula.util.FormulaI18nUtils;

/**
 * Thrown when the generated JavaScript string for a formula is too big.
 *
 * @author a.rich
 * @since 208
 */
public class JSTooBigException extends FormulaException {
    
    private static final long serialVersionUID = 1L;
	private int size;
    private boolean finished;
    
    /**
     * @param size - the size of the generated JavaScript string
     * @param finished - whether or not the generation has completed.
     */
    public JSTooBigException(int size, boolean finished) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "JSTooBigException" + (finished ? "" : "_INCOMPLETE"), size, FormulaInfo.MAX_JS_SIZE));
        this.size = size;
        this.finished = finished;
    }
    
    public int getSize() {
        return size;
    }
    
    public boolean isFinished() {
        return finished;
    }
}
