package com.force.formula.commands;

import static com.force.formula.commands.FormulaCommandInfoImpl.jsMathPkg;

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
 * MFLOOR = Mathematical Floort, which will return the integer less than or equal to the given number,
 * instead of the excel like behavior from the original which returns the integer closest to zero.
 * @author stamm
 * @since 0.1.0
 */
@AllowedContext(section = SelectorSection.MATH, isOffline = true)
public class FunctionMFloor extends UnaryMathCommandBehavior {
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
                return value.setScale(0, RoundingMode.FLOOR);
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
        FormulaSqlHooks hooks = (FormulaSqlHooks)context.getSqlStyle();
        int precision = hooks.getExternalPrecision();
        String sql;
        if (precision >= 0) { // If external precision is -1 don't reound before Ceil/Floor
            sql = "FLOOR(ROUND(" + args[0] + ","+precision+"))";
        } else {
            sql = "FLOOR(" + args[0] + ")";
        }
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
