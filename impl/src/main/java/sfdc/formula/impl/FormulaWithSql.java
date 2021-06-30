/*
 * Created on Dec 14, 2004
 */
package sfdc.formula.impl;

import java.util.List;

import sfdc.formula.*;

/**
 * Provides access to runtime representation of a formula that can be converted to SQL
 *
 * @author dchasman
 * @since 140
 */
public interface FormulaWithSql extends Formula {

    /**
     * Return a SQL snippet representation of the formula
     * @param registry
     *
     * @return SQL snippet
     */
    String toSQL(FormulaTableRegistry registry) throws FormulaException;

    /**
     * Return a SQL snippet capturing errors in evaluating the formula
     *
     * @param registry
     * @return SQL snippet
     * @throws FormulaException
     */
    String toSQLError(FormulaTableRegistry registry) throws FormulaException;

    /**
     * Return a SQL snippet representation of the formula. Does not include
     * guard. Used in building nested formulas.
     * @return SQL snippet
     */
    String getSQLRaw();

    /**
     * Return guard for SQL snippet capturing errors in evaluating the formula.
     * Baked into results of toSQL and toSQLError. Used in building nested formulas.
     *
     * @return SQL snippet
     */
    String getGuard();

    /**
     * Binds raw sql to the standard and custom table aliases
     *
     * @param sql Raw sql as returned by getSQLRaw
     * @param registry
     * @return SQL snippet
     */
    String performLateBinding(String sql, FormulaTableRegistry registry) throws FormulaException;

    /**
     * To be custom indexable, a formula must be deterministic and must not reference any fields that might get updated in
     * a way that our indexing code would not see. For example, an account owner change can change the owner field of many
     * child objects in plsql, and our custom indexing code will not see those changes.
     *
     * @return true if the formula is deterministic
     */
    boolean isCustomIndexable(FormulaContext formulaContext);

    /**
     * To be flex indexable, a formula must be deterministic and must not reference any fields that might get updated in
     * a way that our indexing code would not see, except for certain fields that have special code to enable them,
     * such as Id, CreatedBy, and CreatedDate
     *
     * @return true if the formula is deterministic
     */
    boolean isFlexIndexable(FormulaContext formulaContext);


    /**
     * If {@link #isCustomIndexable(FormulaContext)}, a formula may require a post-save index update in order to account for values
     * not known until after the pl/sql save (for example, autonum fields).
     *
     * @param dmlType The dmlType of the operation.
     * @return true if this formula requires postSaveIndexUpdate for the given dmlType.
     */
    boolean isPostSaveIndexUpdated(FormulaContext formulaContext, FormulaDmlType dmlType);


    /**
     * A Formula field is stale if it (directly or indirectly) refers to a stale summary field.
     * @param formulaContext
     * @return
     */
    boolean isStale(FormulaContext formulaContext);

    /**
     * It's useful to be able to visit all FormulaCommands to perform validation and integrity checks.
     * Implement your own FormulaCommandVisitor to perform you own custom validation.
     * @throws FormulaException
     */
    void visitFormulaCommands(FormulaCommandVisitor visitor);

    /**
     * Dynamically find the SqlTables that are required by this formula
     */
    <T extends FormulaTableRegistry.TableIdentifier> List<T> getDependentTables(FormulaTableRegistry queryTableRegistry);

    /**
     * @return the TableAliasRegistry for use by other Formulas for translating sql
     */
    TableAliasRegistry getTableAliasRegistry();

}
