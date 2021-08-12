package com.force.formula.impl;

import java.util.List;

import com.force.formula.FormulaFieldReferenceInfo;
import com.force.formula.FormulaSchema;


/**
 * Register QueryTables to QueryBuilder based on the contents of a formula.
 *
 * @author wmacklem
 * @since 154
 */
public interface FormulaTableRegistry {
    /**
     * @param fieldPath the foreign key field path to this entity
     * @param isCustomTable is true if the field is a custom field, otherwise false if it is a standard field
     * @return the SqlTable for a given fieldPath.
     */
    TableIdentifier getLogicalSqlTable(List<? extends FormulaFieldReferenceInfo> fieldPath, boolean isCustomTable, boolean isCurrency);

    /**
     * Given a foreign key path, return the table aliases that should be used to generate sql.
     * @param the foreign key path from the root QueryTable
     * @param currentTableAliases The temporary table aliases that are being used
     * @return the standard and custom table aliases
     */
    TableSet getTableAliases(List<? extends FormulaFieldReferenceInfo> fieldPath, TableSet currentTableAliases);
    
    /**
     * Represents a SqlTableIdentifier, but in a less dynamic-sql specific format
     */
    interface TableIdentifier {
        String getAlias();
        FormulaSchema.Entity getEntityInfo();
    }

}
