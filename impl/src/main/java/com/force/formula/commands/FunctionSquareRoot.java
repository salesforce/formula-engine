package com.force.formula.commands;

import static com.force.formula.commands.FormulaCommandInfoImpl.jsMathPkg;

import java.math.BigDecimal;

import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.impl.JsValue;
import com.force.formula.sql.SQLPair;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.MATH,isOffline=true)
public class FunctionSquareRoot extends UnaryMathCommandBehavior {

    private static final long serialVersionUID = 1L;

	@Override
    public UnaryMathCommand getCommand(FormulaCommandInfo info) {
        return new UnaryMathCommand(info) {
            private static final long serialVersionUID = 1L;

			@Override
            protected BigDecimal execute(BigDecimal arg) {
                return BigDecimal.valueOf(Math.sqrt(arg.doubleValue()));
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
        String sql = "SQRT(" + args[0] + ")";
        FormulaSqlHooks hooks = (FormulaSqlHooks)context.getSqlStyle();
        if (hooks.isTransactSqlStyle()) {
        	sql = hooks.sqlTrigConvert(sql);
        }
        String guard = SQLPair.generateGuard(guards, args[0] + "<0");
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        return JsValue.forNonNullResult(jsMathPkg(context) + ".sqrt("+args[0]+")", args);
    }

}
