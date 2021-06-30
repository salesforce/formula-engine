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
public class FunctionLog extends UnaryMathCommandBehavior {

    private static final long serialVersionUID = 1L;

	@Override
    public UnaryMathCommand getCommand(FormulaCommandInfo info) {
        return new UnaryMathCommand(info) {
            private static final long serialVersionUID = 1L;

			@Override
            protected BigDecimal execute(BigDecimal arg) {
                // Math.log() is the natural log (base e).
                // Use log_10(X) = log_e(X) / log_e(10)
                return BigDecimal.valueOf(Math.log(arg.doubleValue()) / Math.log(10));
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
        String sql = "LOG(10, " + args[0] + ")";
        String guard = SQLPair.generateGuard(guards, args[0] + "<=0");
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult("("+args[0]+").log(10)", args);
        }
        return JsValue.forNonNullResult("Math.log("+args[0]+")/Math.LN10", args);
    }

}
