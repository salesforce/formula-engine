package sfdc.formula.commands;

import static sfdc.formula.commands.FormulaCommandInfoImpl.jsMathPkg;

import java.math.*;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.FormulaAST;
import sfdc.formula.impl.JsValue;

/**
 * MCEILING = Mathematical Ceiling, which will return the integer greater than or equal to the given number,
 * instead of the excel like behavior from the original which returns the integer furthest away from zero.
 * @author stamm
 * @since 0.1.0
 */
@AllowedContext(section = SelectorSection.MATH, isOffline = true)
public class FunctionMCeiling extends UnaryMathCommandBehavior {
    private static final long serialVersionUID = 1L;
	private static final MathContext MC = new MathContext(Formula.NUMBER_PRECISION_EXTERNAL, RoundingMode.HALF_DOWN);

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

                return value.setScale(0, RoundingMode.CEILING);
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
        String sql = "CEIL(ROUND(" + args[0] + ","+Formula.NUMBER_PRECISION_EXTERNAL+"))";
        return new SQLPair(sql, guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        // See the oracle function above for the stupidity of this functionality.
        if (context.useHighPrecisionJs()) {
            // We can be sure it isn't null because we're already guarding against args[0]
            return JsValue.forNonNullResult("("+args[0]+").toDP(18).ceil()", args);
        }
        return JsValue.forNonNullResult("("+jsMathPkg(context)+").ceil("+args[0]+")", args);
    }

}
