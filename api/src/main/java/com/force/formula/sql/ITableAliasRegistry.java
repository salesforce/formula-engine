package com.force.formula.sql;

import java.util.Collection;
import java.util.List;

import com.force.formula.FormulaFieldReferenceInfo;

/**
 * Maps spanning relationship paths to temporary table aliases for sql generation.
 *
 * @author stamm
 * @since 200 (from wmacklem 154)
 */
public interface ITableAliasRegistry {

    /**
     * @return the temporary standard and custom table aliases to use for the given fieldPath.  lateBindTableAliases() must
     * be called before the sql snippet is used so the temporary aliases are replaced with the real aliases.
     */
    TableSet getTableAliases(List<FormulaFieldReferenceInfo> fieldPath);

    /**
     * Take the inputted sql generated from the "other" registry and translate it to valid sql for <code>this</code> registry.
     */
    String translate(String sql, ITableAliasRegistry other, List<FormulaFieldReferenceInfo> fieldPath);

    String getRootTableStandardAlias();

    String getRootTableCustomAlias();

    Collection<List<FormulaFieldReferenceInfo>> getFieldPaths();
    
}