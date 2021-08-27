package com.force.formula;

import com.force.formula.util.FormulaI18nUtils;

/**
 *
 * @author djacobs
 * @since 142
 */
public class FunctionNotAllowedException extends FormulaException {
    private static final long serialVersionUID = 1L;
	private FormulaCommandType command;

    public FunctionNotAllowedException(FormulaCommandType command) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "FunctionNotAllowedException", 
            command.getName().toUpperCase()));

        this.command = command;
    }

    public FormulaCommandType getCommand() {
        return this.command;
    }
}
