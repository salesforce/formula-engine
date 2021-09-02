package com.force.formula.commands;

import java.math.*;

import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.JsValue;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.BigDecimalHelper;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section = SelectorSection.MATH, isOffline = true)
public class FunctionCeiling extends UnaryMathCommandBehavior {
    private static final long serialVersionUID = 1L;
	private static final MathContext MC = new MathContext(BigDecimalHelper.NUMBER_PRECISION_EXTERNAL, RoundingMode.HALF_DOWN);

    @Override
    public UnaryMathCommand getCommand(FormulaCommandInfo info) {
        return new UnaryMathCommand(info) {
            private static final long serialVersionUID = 1L;

			@Override
            protected BigDecimal execute(BigDecimal value) {
                /*
                 * Cut of the digit just past the Oracle limit.
                 * We do this in order to deal with infinite fractions like 1/3, which can be said to be inexact just at the last digit.
                 */
                value = value.round(MC);

                return value.setScale(0, value.signum() == -1 ? RoundingMode.FLOOR: RoundingMode.CEILING);
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
        String sql = "CASE WHEN " + args[0] + ">=0 THEN CEIL(ROUND(" + args[0] + ","+BigDecimalHelper.NUMBER_PRECISION_EXTERNAL+")) ELSE FLOOR(ROUND(" + args[0] + ","+BigDecimalHelper.NUMBER_PRECISION_EXTERNAL+")) END";
        return new SQLPair(sql, guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        // See the oracle function above for the questionable implementation
        if (context.useHighPrecisionJs()) {
            // We can be sure it isn't null because we're already guarding against args[0]
            return JsValue.forNonNullResult("("+args[0]+".isPos()?"+args[0]+".toDP(18).ceil():"+args[0]+".toDP(18).floor())", args);
        }
        return JsValue.forNonNullResult("(("+args[0] + ")>=0?Math.ceil("+args[0]+"):Math.floor("+args[0]+"))", args);
    }

}
