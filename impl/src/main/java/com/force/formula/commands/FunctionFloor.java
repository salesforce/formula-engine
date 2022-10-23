package com.force.formula.commands;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaSqlHooks;
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
public class FunctionFloor extends UnaryMathCommandBehavior {
    private static final long serialVersionUID = 1L;
	private static final MathContext MC = new MathContext(BigDecimalHelper.NUMBER_PRECISION_EXTERNAL, RoundingMode.HALF_UP);
    
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
        FormulaSqlHooks hooks = (FormulaSqlHooks)context.getSqlStyle();
        String ceil = hooks.isTransactSqlStyle() ? "CEILING" : "CEIL";
        int precision = hooks.getExternalPrecision();
        String sql;
        if (precision >= 0) { // If external precision is -1 don't reound before Ceil/Floor
            sql = "CASE WHEN " + args[0] + ">=0 THEN FLOOR(ROUND(" + args[0] + ","+precision+")) ELSE "+ceil+"(ROUND(" + args[0] + ","+precision+")) END";
        } else {
            sql = "CASE WHEN " + args[0] + ">=0 THEN FLOOR(" + args[0] + ") ELSE "+ceil+"(" + args[0] + ") END";
        }
        return new SQLPair(sql, guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        // See the oracle function above for the questionable implementation
        if (context.useHighPrecisionJs()) {
            // We can be sure it isn't null because we're already guarding against args[0]
            return JsValue.forNonNullResult("("+args[0]+".isPos()?"+args[0]+".toDP(18).floor():"+args[0]+".toDP(18).ceil())", args);
        }
        return JsValue.forNonNullResult("(("+args[0] + ")>=0?Math.floor("+args[0]+"):Math.ceil("+args[0]+"))", args);
    }

}
