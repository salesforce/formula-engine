package sfdc.formula.commands;

import java.math.*;

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
public class FunctionFloor extends UnaryMathCommandBehavior {
    private static final long serialVersionUID = 1L;
	private static final MathContext MC = new MathContext(Formula.NUMBER_PRECISION_EXTERNAL, RoundingMode.HALF_UP);
    
    @Override
    public UnaryMathCommand getCommand(FormulaCommandInfo info) {
        return new UnaryMathCommand(info) {
            private static final long serialVersionUID = 1L;

			@Override
            protected BigDecimal execute(BigDecimal value) {
                // see FunctionCeiling for explanation
                value = value.round(MC);
                return value.setScale(0, value.signum() == -1 ? RoundingMode.CEILING: RoundingMode.FLOOR);
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
        String sql = "CASE WHEN " + args[0] + ">=0 THEN FLOOR(ROUND(" + args[0] + ","+Formula.NUMBER_PRECISION_EXTERNAL+")) ELSE CEIL(ROUND(" + args[0] + ","+Formula.NUMBER_PRECISION_EXTERNAL+")) END";
        return new SQLPair(sql, guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        // See the oracle function above for the stupidity of this functionality.
        if (context.useHighPrecisionJs()) {
            // We can be sure it isn't null because we're already guarding against args[0]
            return JsValue.forNonNullResult("("+args[0]+".isPos()?"+args[0]+".toDP(18).floor():"+args[0]+".toDP(18).ceil())", args);
        }
        return JsValue.forNonNullResult("(("+args[0] + ")>=0?Math.floor("+args[0]+"):Math.ceil("+args[0]+"))", args);
    }

}
