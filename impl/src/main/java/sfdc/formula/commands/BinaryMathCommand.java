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
abstract class BinaryMathCommand extends AbstractFormulaCommand {
    
    private static final long serialVersionUID = 1L;

	public BinaryMathCommand(FormulaCommandInfo formulaCommandInfo){
        super(formulaCommandInfo);
    }
    
    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        BigDecimal rhs = checkNumberType(stack.pop());
        BigDecimal lhs = checkNumberType(stack.pop());
        if ((lhs == null) || (rhs == null))
            stack.push(null);
        else
            stack.push(execute(lhs, rhs));
    }
    
    protected abstract BigDecimal execute(BigDecimal lhs, BigDecimal rhs);
}
