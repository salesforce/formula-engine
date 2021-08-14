package com.force.formula.impl;

import java.util.*;

import com.force.formula.FormulaFieldReferenceInfo;
import com.force.formula.FormulaTextUtil;
import com.google.common.collect.Lists;

/**
 * Maps spanning relationship paths to temporary table aliases for sql generation.
 *
 * @author wmacklem
 * @since 154
 */
public class TableAliasRegistry implements ITableAliasRegistry {
    // Do not change the number of characters in these delimiters.  It effects the generated sql size for formulas
    // and some of our customers are already hitting the upper bound for sql size.
    private static final String START_DELIMITER = "$!s";
    private static final String END_DELIMITER = "!$";

    private final Map<List<FormulaFieldReferenceInfo>, TableSet> relationships;
    private int tableAliasCounter;

    public TableAliasRegistry() {
        // Use a linked map so that we get consistent table aliases for ftests
        this.relationships = new LinkedHashMap<List<FormulaFieldReferenceInfo>, TableSet>(4);
        tableAliasCounter = 0;
    }

    /**
     * @return the temporary standard and custom table aliases to use for the given fieldPath.  lateBindTableAliases() must
     * be called before the sql snippet is used so the temporary aliases are replaced with the real aliases.
     */
    @Override
    public TableSet getTableAliases(List<FormulaFieldReferenceInfo> fieldPath) {
        if (fieldPath != null && fieldPath.size() == 0)
            fieldPath = null;

        TableSet aliases = relationships.get(fieldPath);
        if (aliases != null)
            return aliases;

        aliases = new TableSet(String.format("%s%ss%s", START_DELIMITER, tableAliasCounter, END_DELIMITER), String.format("%s%sc%s", START_DELIMITER, tableAliasCounter, END_DELIMITER), String.format("%s%su%s", START_DELIMITER, tableAliasCounter, END_DELIMITER));
        relationships.put(fieldPath, aliases);
        tableAliasCounter++;

        return aliases;
    }

    /**
     * @return the sql snippet that has replaced all the temporary table aliases with the table aliases required by QueryBuilder
     */
    public String lateBindTableAliases(String sql, FormulaTableRegistry queryTableRegistry) {
        if (sql == null)
            return null;

        List<String> oldAliases = Lists.newArrayList();
        List<String> newAliases = Lists.newArrayList();

        for (Map.Entry<List<FormulaFieldReferenceInfo>, TableSet> entry : relationships.entrySet()) {
            TableSet replacements = queryTableRegistry.getTableAliases(entry.getKey(), entry.getValue());
            oldAliases.add(entry.getValue().mainAlias);
            oldAliases.add(entry.getValue().cfAlias);
            oldAliases.add(entry.getValue().currencyAlias);
            newAliases.add(replacements.mainAlias);
            newAliases.add(replacements.cfAlias);
            newAliases.add(replacements.currencyAlias);
        }

        if (oldAliases.size() > 0)
            return FormulaTextUtil.replaceSimple(sql, oldAliases.toArray(new String[oldAliases.size()]), newAliases.toArray(new String[newAliases.size()]));
        else
            return sql;
    }

    /**
     * Take the inputted sql generated from the "other" registry and translate it to valid sql for <code>this</code> registry.
     */
    @Override
    public String translate(String sql, ITableAliasRegistry other, List<FormulaFieldReferenceInfo> fieldPath) {
        if (sql == null)
            return null;

        List<String> oldAliases = Lists.newArrayList();
        List<String> newAliases = Lists.newArrayList();

        for (Map.Entry<List<FormulaFieldReferenceInfo>, TableSet> entry : ((TableAliasRegistry)other).relationships.entrySet()) {
            List<FormulaFieldReferenceInfo> fieldIds;
            if (fieldPath != null)
                fieldIds = Lists.newArrayList(fieldPath);
            else
                fieldIds = Lists.newArrayList();

            if (entry.getKey() != null)
                fieldIds.addAll(entry.getKey());

            TableSet replacements = getTableAliases(fieldIds);

            oldAliases.add(entry.getValue().mainAlias);
            oldAliases.add(entry.getValue().cfAlias);
            oldAliases.add(entry.getValue().currencyAlias);
            newAliases.add(replacements.mainAlias);
            newAliases.add(replacements.cfAlias);
            newAliases.add(replacements.currencyAlias);
        }

        if (oldAliases.size() > 0)
            return FormulaTextUtil.replaceSimple(sql, oldAliases.toArray(new String[oldAliases.size()]), newAliases.toArray(new String[newAliases.size()]));
        else
            return sql;
    }

    /**
     * Search through the sql snippet and find all the SqlTables we need
     */
    @SuppressWarnings("unchecked")  // To make the life of callers easier when returning a List
	public <T extends FormulaTableRegistry.TableIdentifier> List<T> getDependentTables(String sql, FormulaTableRegistry queryTableRegistry) {
        List<T> sqlTables = new ArrayList<>();

        for (Map.Entry<List<FormulaFieldReferenceInfo>, TableSet> entry : relationships.entrySet()) {
            String standardAlias = entry.getValue().mainAlias;
            String customAlias = entry.getValue().cfAlias;
            String currencyAlias = entry.getValue().currencyAlias;

            if (sql.contains(standardAlias)) {
                sqlTables.add((T)queryTableRegistry.getLogicalSqlTable(entry.getKey(), false, false));
            }

            if (sql.contains(customAlias)) {
                sqlTables.add((T)queryTableRegistry.getLogicalSqlTable(entry.getKey(), true, false));
            }

            if (sql.contains(currencyAlias)) {
                sqlTables.add((T)queryTableRegistry.getLogicalSqlTable(entry.getKey(), false, true));
            }
        }

        return sqlTables;
    }

    @Override
    public String getRootTableStandardAlias() {
        return getTableAliases(null).mainAlias;
    }

    @Override
    public String getRootTableCustomAlias() {
        return getTableAliases(null).cfAlias;
    }

    @Override
    public Collection<List<FormulaFieldReferenceInfo>> getFieldPaths() {
        return this.relationships.keySet();
    }
}
