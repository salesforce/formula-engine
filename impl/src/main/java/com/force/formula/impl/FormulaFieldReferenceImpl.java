/*
 * Copyright, 1999-2010, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import javax.annotation.concurrent.Immutable;

import com.force.formula.FormulaFieldReference;

/**
 * Implementation of FormulaFieldReference object
 * 
 * @author aballard
 * @since 168
 */
@Immutable
public class FormulaFieldReferenceImpl implements FormulaFieldReference {
    
    private final Object baseReference;  
    private final Object elementReference;
    // isSelector indicates whether elementReference in a dynamic expression is [elementReference] or .elementReference.
    // Only relevant when base is non-null
    private final boolean isSelector; 
    // isDynamicBase indicates whether this reference occurs as the base of a subscript expression.  
    private final boolean isDynamicBase; 
    
    public FormulaFieldReferenceImpl(Object baseReference, Object elementReference, boolean fieldSelector, boolean isDynamicBase) {
        this.baseReference = baseReference;
        this.elementReference = elementReference;
        this.isSelector = fieldSelector;
        this.isDynamicBase = isDynamicBase;
    }
  
    public FormulaFieldReferenceImpl(Object baseReference, Object elementReference) {
        this(baseReference, elementReference, true, false);
    }
    
    // True if the element reference comes from an expression.
    @Override
    public boolean isDynamic() {
        return !isSelector;
    }
    
    // True if this reference is the base of a dynamic ref; i.e. it is followed by a subscript expression 
    @Override
    public boolean isDynamicBase() {
        return isDynamicBase; 
    }
    
    @Override
    public Object getBase() {
        return baseReference;
    }
    
    @Override
    public String getElement() {
        return elementReference.toString();
    }
    
    @Override
    public Object getSelector() {
        return elementReference;
    }
        
    @Override
    public String toString() {
        if (baseReference == null) {
            return getElement();
        } else {
            StringBuffer s = new StringBuffer();
            s.append(baseReference.toString());
            if (isSelector) {
                s.append(".").append(elementReference);
            } else {
                s.append("['").append(elementReference).append("']");
            }
            return s.toString();
        }
    }
    
}
