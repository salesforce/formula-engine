package sfdc.formula.commands;

import java.math.BigDecimal;

import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.FormulaContext;
import sfdc.formula.SQLPair;
import sfdc.formula.impl.FormulaAST;
import sfdc.formula.impl.JsValue;
import sfdc.formula.util.BigDecimalHelper;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.MATH,isOffline=true)
public class FunctionRound extends BinaryMathCommandBehavior {

    private static final long serialVersionUID = 1L;

	@Override
    public FormulaCommand getCommand(FormulaCommandInfo info) {
        return new BinaryMathCommand(info) {
            private static final long serialVersionUID = 1L;

			@Override
            protected BigDecimal execute(BigDecimal lhs, BigDecimal rhs) {
                int scale = rhs.intValue();
                return BigDecimalHelper.round(lhs, scale);
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
        String sql = "ROUND(" + args[0] + ", " + args[1] + ")";
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        // Short circuit the complexity if we know it's positive.
        if (args[1].js.length()>0 && Character.isDigit(args[1].js.charAt(0))) {
            if (context.useHighPrecisionJs()) {
                return JsValue.forNonNullResult(args[0]+".toDecimalPlaces("+args[1]+".toNumber())", args);
            }
            // If it's a positive digit, assume we can round it without testing
            return JsValue.forNonNullResult("Number(Number(" + args[0] + ").toFixed("+args[1]+"<=20?"+args[1]+":20))", args);
        }
        // Use the '1e'+digits for High Precision math in all cases because it just sets the scale.
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult("("+args[0]+").mul('1e'+"+args[1]+".toFixed(0)).round().div('1e'+"+args[1]+".toFixed(0))", args);
        }
        // For regular math, it's more complicated because you need to use toFixed to prevent precision loss if the round is > 0
        return JsValue.forNonNullResult("("+args[1]+">0)?Number(Number(" + args[0] + ").toFixed("+args[1]+"<=20?"+args[1]+":20)):"
                    +"Math.round(("+args[0]+")*('1e'+"+args[1]+"))/('1e'+"+args[1]+")", args);
    }

}
