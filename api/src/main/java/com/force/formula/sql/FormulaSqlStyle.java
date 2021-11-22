/**
 * 
 */
package com.force.formula.sql;

/**
 * Parameterize the SQL style for low-level differences in sql style.
 * It isn't an enum so that you can override these behavior for non-ansi sql extensions
 * in a parameterized manner, but there is a DefaultSqlStyle included.
 * 
 * There are a few implementation details here around style, but mostly functional differenceds
 * in the DBs.
 * 
 * Note: the default implementations in the formula engine generally assume oracle style, not postgresql style.
 * 
 * @author stamm
 * @since 0.1.0
 */
public interface FormulaSqlStyle {

	/**
	 * @return whether or not to default to Postgresql style of sql.
	 */
	default boolean isPostgresStyle() {
		return false;
	}
	
	/**
	 * @return whether or not to default to Oracle style of sql. 
	 */
	default boolean isOracleStyle() {
		return false;
	}
	
	/**
	 * @return whether or not to default to mysql (or mariadb) style of sql.
	 */
	default boolean isMysqlStyle() {
		return false;
	}
	
	/**
	 * @return whether or not to default to transactsql (sybase/ms sql server) style of sql.
	 */
	default boolean isTsqlStyle() {
		return false;
	}	
}
