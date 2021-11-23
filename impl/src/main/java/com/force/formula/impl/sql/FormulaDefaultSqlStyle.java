/**
 * 
 */
package com.force.formula.impl.sql;

import com.force.formula.impl.FormulaSqlHooks;

/**
 * @author stamm
 *
 */
public final class FormulaDefaultSqlStyle {
	public static final FormulaSqlHooks POSTGRES = new FormulaPostgreSQLHooks() {};
	public static final FormulaSqlHooks ORACLE = new FormulaOracleHooks() {};
	public static final FormulaSqlHooks MYSQL = new FormulaMySQLHooks() {};
	public static final FormulaSqlHooks TSQL = new FormulaTransactSQLHooks() {};
}
