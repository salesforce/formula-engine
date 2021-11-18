/**
 * 
 */
package com.force.formula.sql;

/**
 * Parameterize the SQL style for low-level differences in sql style.
 * It isn't an enum so that you can override these behavior for non-ansi sql extensions
 * in a parameterize manner, but there is a DefaultSqlStyle included.
 * 
 * There are a few implementation details here around style, but mostly
 * 
 * Note: the default implementations in the formula engine assume oracle style, not postgresql style.
 * 
 * @author stamm
 * @since 0.1.0
 */
public interface FormulaSqlStyle {

	/**
	 * @return whether or not to default to Postgresql style of sql.
	 */
	boolean isPostgresStyle();
	
	/**
	 * @return whether or not to default to Oracle style of sql.
	 */
	boolean isOracleStyle();
	
	/**
	 * @return whether or not to default to mysql style of sql.
	 */
	default boolean isMysqlStyle() {
		return false;
	}
	
}
