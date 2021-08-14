package com.force.formula.commands;


import java.util.Deque;

import com.force.formula.FormulaRuntimeContext;

/**
 * @author djacobs
 * @since 140
 */
public class ConstantNullCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public ConstantNullCommand(FormulaCommandInfo info) {
        super(info);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        stack.push(null);
    }
}
