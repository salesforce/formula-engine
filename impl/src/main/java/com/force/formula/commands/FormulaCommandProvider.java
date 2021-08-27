package com.force.formula.commands;

import java.util.List;

import com.force.formula.FormulaCommand;

/**
 * Normally you can just use FormulaCommandInfoProvider to plug additional functions
 * into the formula engine. But in some cases the formula engine needs a way to access
 * the actual command.
 *
 * @author a.rich
 * @since 226
 */
public interface FormulaCommandProvider {
    FormulaCommand get(FormulaCommandInfo info, List<FormulaCommand> commands);
} 
