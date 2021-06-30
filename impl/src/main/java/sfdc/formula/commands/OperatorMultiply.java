/*
 * Created on Dec 10, 2004
 *
 */
package sfdc.formula.commands;

import java.math.BigDecimal;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.FormulaAST;
import sfdc.formula.impl.JsValue;

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
                return lhs.multiply(rhs, Formula.MC_PRECISION_INTERNAL);
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
