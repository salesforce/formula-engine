/**
 * 
 */
package com.force.formula;

import java.lang.annotation.*;

/**
 * Represents a Formula Command (i.e. an operator or formula function)
 * You need to override FormulaCommandInfo in the impl package if you want to create your
 * own function types.
 * 
 * @author stamm
 * @since 0.0.1
 */
public interface FormulaCommandType {
	String getName();
    FormulaCommandType.AllowedContext getAllowedContext();

    enum SelectorSection {
	    DATE_TIME,
	    LOGICAL,
	    MATH,
	    TEXT,
	    ADVANCED,
	    REPORT_SUMMARIES
	}

	/**
	 * Mark the annotation as an annotation on a basic object
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE) @interface AllowedContext {
	    /** @return What section should the function appear in for the selector */
	    SelectorSection section();
	    
        /** @return Is this function implemented in javascript?<br>Default: true **/
        boolean isJavascript() default true;
	    /** @return Is this function available in the offline context?  This means it has *the same behavior* as java and SQL.  Higher bar<br>Default: false **/
	    boolean isOffline() default false;
	    
	    /** @return A customer defined access check variable to control access to formulas that aren't "public".  */
	    String access() default "";
	    
	    // The following contexts are deprecated because they were too confusing and don't make sense.
	    /** @return Allowed only if you are in a change context (validation rules, workflow, field updates, default values) */
	    @Deprecated
	    boolean changeOnly() default false;
	    /** @return Allowed only in a display context */
	    @Deprecated
	    boolean displayOnly() default false;
	    /** @return Allowed only in a javascript context*/
	    @Deprecated
	    boolean javascriptOnly() default false;
	    /** @return Disallowed in template contexts */
	    @Deprecated
	    boolean nonDisplayOnly() default false;
	    /** @return Allowed only for Validation Rules */
	    @Deprecated
	    boolean validationOnly() default false;
	    /** @return Allowed only for report summaries */
	    @Deprecated
	    boolean reportSummariesOnly() default false;
	    /** @return Disallowed in flow */
	    @Deprecated
	    boolean nonFlowOnly() default false;
	}

}
