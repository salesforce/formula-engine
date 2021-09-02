/*
 * Created on Dec 10, 2004
 *
 */
package com.force.formula.commands;

import java.math.BigDecimal;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.JsValue;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.BigDecimalHelper;

/**
 * @author dchasman
 * @since 140
 */
@AllowedContext(section = SelectorSection.MATH,isOffline=true)
public class OperatorMultiply extends BinaryMathCommandBehavior {    
    private static final long serialVersionUID = 1L;

	@Override
    public FormulaCommand getCommand(FormulaCommandInfo info) {
        return new BinaryMathCommand(info) {
            private static final long serialVersionUID = 1L;

			@Override
            protected BigDecimal execute(BigDecimal lhs, BigDecimal rhs) {
                return lhs.multiply(rhs, BigDecimalHelper.MC_PRECISION_INTERNAL);
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
        String sql = "(" + args[0] + " * " + args[1] + ")";
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult(args[0] + ".mul(" + args[1] + ")", args);
        }
        return JsValue.forNonNullResult(args[0] + "*" + args[1], args);
    }


}
