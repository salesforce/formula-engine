package com.force.formula;

/**
 * Wrapper for Exceptions that contain HTML in their messages.  
 * Child classes should handle escaping internally.
 *
 * @author ldelascurain
 * @since 160
 */
public interface FormulaExceptionWithHTMLErrorMessage {

    /**
     * Sometimes we can't have HTML. This method returns a message to the user
     * that may be less informative but does not have any HTML.
     * @return an error message without HTML
     */
    String getHtmlFreeErrorMessage();

}
