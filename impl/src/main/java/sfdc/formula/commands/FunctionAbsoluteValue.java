package sfdc.formula.commands;

import java.math.BigDecimal;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.FormulaAST;
import sfdc.formula.impl.JsValue;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section = SelectorSection.MATH, isOffline = true)
public class FunctionAbsoluteValue extends UnaryMathCommandBehavior {

    private static final long serialVersionUID = 1L;

	@Override
    public UnaryMathCommand getCommand(FormulaCommandInfo info) {
        return new UnaryMathCommand(info) {
            private static final long serialVersionUID = 1L;

			@Override
            protected BigDecimal execute(BigDecimal arg) {
                return arg.abs();
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
        return new SQLPair("ABS(" + args[0] + ")", guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult(args[0]+".abs()", args);
        }
        return JsValue.forNonNullResult("Math.abs("+args[0]+")", args);
    }

}
