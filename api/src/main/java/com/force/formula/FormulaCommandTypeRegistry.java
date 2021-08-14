/**
 * 
 */
package com.force.formula;

import java.util.List;

/**
 * Interface for the types of formula commands 
 * @author stamm
 * @since 0.0.1
 */
public interface FormulaCommandTypeRegistry {
    List<? extends FormulaCommandType> getCommands();
    
    FormulaCommandType getAllowNull(String name);
}
