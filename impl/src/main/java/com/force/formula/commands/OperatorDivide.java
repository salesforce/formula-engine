/*
 * Created on Dec 10, 2004
 *
 */
package com.force.formula.commands;

import java.math.BigDecimal;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.JsValue;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.BigDecimalHelper;

/**
 * @author dchasman
 * @since 140
 */
@AllowedContext(section = SelectorSection.MATH,isOffline=true)
public class OperatorDivide extends BinaryMathCommandBehavior {
    private static final long serialVersionUID = 1L;

	@Override
    public FormulaCommand getCommand(FormulaCommandInfo info) {
        return new BinaryMathCommand(info) {
            private static final long serialVersionUID = 1L;

			@Override
            protected BigDecimal execute(BigDecimal lhs, BigDecimal rhs) {
                return lhs.divide(rhs, BigDecimalHelper.MC_PRECISION_INTERNAL);
            }
        };
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult(args[0] + ".div(" + args[1] + ")", args);
        }
        return JsValue.forNonNullResult("(" + args[0] + "/" + args[1] + ")", args);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
        String sql;
        String guard;
        sql = "(" + args[0] + "/" + args[1] + ")";
        FormulaAST divider = (FormulaAST)node.getFirstChild().getNextSibling();
        if (divider != null && divider.isLiteral() && divider.getDataType() == BigDecimal.class) {
            if (BigDecimal.ZERO.compareTo(new BigDecimal(args[1])) != 0) {
                // no guard needed
                guard = SQLPair.generateGuard(guards, null);
            } else {
                // we know it's false
                guard = SQLPair.generateGuard(guards, "0=0");
                sql = "NULL";
            }
        } else {
            guard = SQLPair.generateGuard(guards, args[1] + "=0");
        }

        return new SQLPair(sql, guard);
    }
  
}
