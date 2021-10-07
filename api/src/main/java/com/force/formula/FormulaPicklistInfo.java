package com.force.formula;

/**
 * Represents the information about a picklist or another enumerated value.
 * Corresponds to EnumInfo in core  
 * 
 * Note: this doesn't include anything about what the items are, since the implementation
 * of choices is very application specific. 
 * 
 * @author stamm
 * @since 0.0.1
 */
public interface FormulaPicklistInfo {
	/**
	 * Implement this for dynamic picklists
	 * @author stamm
	 */
	public interface Dynamic {
		/**
		 * Register that this value needs to be cached appropriately
		 * @param dbValue the DB Value
		 */
	    void collectDbValueToFetch(String dbValue);

	    /**
	     * Record the given api value, to be lazily fetched.
	     */
	    void collectApiValueToFetch(String apiValue);


	}
}
