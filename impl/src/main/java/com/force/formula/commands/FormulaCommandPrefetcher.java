package com.force.formula.commands;

import com.force.formula.*;
import com.force.formula.impl.FormulaAST;

/**
 * Useful for prefetching data that the FormulaCommand uses.  For instance, picklist values can be prefetched across
 * every command in the formula all at once.  
 *
 * @author wmacklem
 * @since 154
 */
public interface FormulaCommandPrefetcher {
    void prefetch(FormulaAST node, FormulaContext context) throws InvalidFieldReferenceException, UnsupportedTypeException;
}
