package com.force.formula.commands;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.BigDecimalHelper;

/**
 * Trunc with a number of decimal places.  Not supported "well", because there
 * isn't a good precise solution for plain javascript numbers, as it'll have 
 * precision issues because there isn't a native functions for it.  Also
 * Internet Explorer (RIP) doesn't support Math.trunc.
 * 
 * @author stamm
 * @since 0.1.10
 */
@AllowedContext(section=SelectorSection.MATH,isOffline=true)
public class FunctionTrunc extends BinaryMathCommandBehavior {

    private static final long serialVersionUID = 1L;

	@Override
    public FormulaCommand getCommand(FormulaCommandInfo info) {
        return new BinaryMathCommand(info) {
            private static final long serialVersionUID = 1L;

			@Override
            protected BigDecimal execute(BigDecimal lhs, BigDecimal rhs) {
                int scale = rhs.intValue();
                return BigDecimalHelper.round(lhs, scale, RoundingMode.DOWN);
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
    	FormulaSqlHooks hooks = (FormulaSqlHooks) context.getSqlStyle();
        String sql = "TRUNC(" + args[0] + ", " + hooks.sqlRoundScaleArg(args[1]) + ")";
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        // Short circuit the complexity if we know it's positive.
        if (context.useHighPrecisionJs() && args[1].js.length()>0 && Character.isDigit(args[1].js.charAt(0))) {
            return JsValue.forNonNullResult(args[0]+".toDecimalPlaces("+args[1]+".toNumber(),1)", args);  //,1 == RoundingMode.down
        }
        // Use the '1e'+digits for High Precision math in all cases because it just sets the scale.
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult("("+args[0]+").mul('1e'+"+args[1]+".toFixed(0)).trunc().div('1e'+"+args[1]+".toFixed(0))", args);
        }
        // For regular math, it's more complicated because you need to use toFixed to prevent precision loss if the round is > 0,
        // however there isn't a native "Trunc" in javascript, so we have precision loss.  Use at your own risk.
        // This will not work with internet explorer, beu
        return JsValue.forNonNullResult("Math.trunc(("+args[0]+")*('1e'+"+args[1]+"))/('1e'+"+args[1]+")", args);
    }

}
