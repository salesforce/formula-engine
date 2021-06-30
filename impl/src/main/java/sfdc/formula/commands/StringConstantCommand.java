/*
 * Created on Dec 10, 2004
 *
 */
package sfdc.formula.commands;


import java.util.Deque;

import sfdc.formula.FormulaRuntimeContext;

/**
 * @author dchasman
 * @since 140
 */
public class StringConstantCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public StringConstantCommand(FormulaCommandInfo info, Object value) {
        super(info);
        if ((value != null) && (value.equals("")))
            this.value = null;
        else
            this.value = value;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        stack.push(value);
    }

    private final Object value;
}
