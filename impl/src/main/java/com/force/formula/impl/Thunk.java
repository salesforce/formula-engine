package com.force.formula.impl;

import java.util.Deque;
import java.util.List;

import com.force.formula.*;
import com.force.formula.commands.AbstractFormulaCommand;

/**
 * A list of commands that will be executed later. Used to delay evaluation of
 * arguments to IF and CASE.
 *
 * @author djacobs
 * @since 140
 */
public class Thunk extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;
	private FormulaCommand[] commands;

    public Thunk(FormulaCommand[] commands) {
        super("thunk");
        this.commands = commands;
    }

    // Pushes itself on the stack for later execution
    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        stack.push(this);
    }

    @Override
    public void visit(FormulaCommandVisitor visitor) {
        for (FormulaCommand command : commands) {
            command.visit(visitor);
        }
    }

    // Really executes the commands
    public void executeReally(FormulaRuntimeContext context, Deque<Object> stack) throws FormulaException {
        for (FormulaCommand command : commands) {
            command.execute(context, stack);
        }
    }

    @Override
    public void preExecuteInBulk(List<FormulaRuntimeContext> contexts) throws FormulaException {
        for (FormulaCommand command : commands) {
            command.preExecuteInBulk(contexts);
        }
    }

}
