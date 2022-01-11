/*
 * Created on Jan 14, 2005
 *
 */
package com.force.formula.commands;

import java.math.BigDecimal;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section = SelectorSection.MATH, isOffline = true)
public class FunctionExponent extends UnaryMathCommandBehavior {

    private static final long serialVersionUID = 1L;

	@Override
    public UnaryMathCommand getCommand(FormulaCommandInfo info) {
        return new UnaryMathCommand(info) {
            private static final long serialVersionUID = 1L;

			@Override
            protected BigDecimal execute(BigDecimal arg) {
                return BigDecimal.valueOf(Math.exp(arg.doubleValue()));
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
    	FormulaSqlHooks hooks = (FormulaSqlHooks) context.getSqlStyle();
    	String sql = hooks.sqlExponent(args[0]);
        return new SQLPair(sql, guards[0]);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        String pkg = FormulaCommandInfoImpl.jsMathPkg(context);
        return JsValue.forNonNullResult(pkg+".exp("+args[0]+")",args);
    }

}
