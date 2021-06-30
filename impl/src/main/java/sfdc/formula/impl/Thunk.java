package sfdc.formula.impl;

import java.util.List;

import sfdc.formula.FormulaRuntimeContext;
import sfdc.formula.commands.AbstractFormulaCommand;
import sfdc.formula.commands.FormulaCommand;

import java.util.Deque;

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
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        stack.push(this);
    }

    @Override
    public void visit(FormulaCommandVisitor visitor) {
        for (FormulaCommand command : commands) {
            command.visit(visitor);
        }
    }

    // Really executes the commands
    public void executeReally(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        for (FormulaCommand command : commands) {
            command.execute(context, stack);
        }
    }

    @Override
    public void preExecuteInBulk(List<FormulaRuntimeContext> contexts) throws Exception {
        for (FormulaCommand command : commands) {
            command.preExecuteInBulk(contexts);
        }
    }

}
