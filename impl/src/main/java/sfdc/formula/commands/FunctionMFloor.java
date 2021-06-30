package sfdc.formula.commands;

import java.math.*;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.FormulaAST;
import sfdc.formula.impl.JsValue;

import static sfdc.formula.commands.FormulaCommandInfoImpl.jsMathPkg;


/**
 * MFLOOR = Mathematical Floort, which will return the integer less than or equal to the given number,
 * instead of the excel like behavior from the original which returns the integer closest to zero.
 * @author stamm
 * @since 0.1.0
 */
@AllowedContext(section = SelectorSection.MATH, isOffline = true)
public class FunctionMFloor extends UnaryMathCommandBehavior {
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
                return value.setScale(0, RoundingMode.FLOOR);
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
        String sql = "FLOOR(ROUND(" + args[0] + ","+Formula.NUMBER_PRECISION_EXTERNAL+"))";
        return new SQLPair(sql, guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        if (context.useHighPrecisionJs()) {
            return JsValue.generate("("+args[0]+").toDP(18).floor()", args, false, args);
        }
        return JsValue.generate("("+jsMathPkg(context)+").floor("+args[0]+")", args, false, args);
    }

}
