package sfdc.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import sfdc.formula.FormulaRuntimeContext;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public abstract class UnaryMathCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public UnaryMathCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        BigDecimal arg = checkNumberType(stack.pop());
        if (arg == null)
            stack.push(null);
        else
            stack.push(execute(arg));
    }
    
    protected abstract BigDecimal execute(BigDecimal value);
}
