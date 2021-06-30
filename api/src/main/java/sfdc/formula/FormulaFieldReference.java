/*
 * Copyright, 1999-2010, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package sfdc.formula;

/**
 * Encapsulates a field reference in a formula. This may be either a multipart literal field name of the form a.b.c.d, or 
 * a base object + simple field name for a property or element of the base object, generated from any kind of dynamic reference.  
 *
 * @author aballard
 * @since 168
 */
public interface FormulaFieldReference {

    // True if the element reference comes from an expression.
    boolean isDynamic();
    boolean isDynamicBase(); 
    
    Object getBase();
    
    String getElement();   // Properties
    Object getSelector();  // subscripts, map keys, properties 
}
