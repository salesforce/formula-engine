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
    public static final FormulaSqlHooks MARIADB = new FormulaMySQLHooks.MariaDBHooks() {};
	public static final FormulaSqlHooks TSQL = new FormulaTransactSQLHooks() {};
    public static final FormulaSqlHooks PRESTO = new FormulaPrestoHooks() {};
    public static final FormulaSqlHooks GOOGLE = new FormulaGoogleHooks() {};
    public static final FormulaSqlHooks SQLITE = new FormulaSqliteHooks() {};
}
