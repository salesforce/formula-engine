package com.force.formula.commands;

import java.math.BigDecimal;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.BigDecimalHelper;

/**
 * Describe your class here.
 * 
 * @author djacobs
 * @since 140
 */
@AllowedContext(section = SelectorSection.MATH,isOffline=true)
// We are enabling support for offline. But in case of High precision, result might be different than the result from SQL.
public class OperatorPower extends BinaryMathCommandBehavior {
    private static final long serialVersionUID = 1L;

	@Override
    public FormulaCommand getCommand(FormulaCommandInfo info) {
        return new BinaryMathCommand(info) {
            private static final long serialVersionUID = 1L;

			@Override
            protected BigDecimal execute(BigDecimal arg1, BigDecimal arg2) {
                try {
                    return BigDecimalHelper.formulaPower(arg1, arg2);
                } catch (BigDecimalHelper.FormulaPowerException e) {
                    throw new FormulaEvaluationException(e.getMessage());  // NOPMD
                }
            }
        };
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards) {
        FormulaSqlHooks sqlHooks = (FormulaSqlHooks) context.getSqlStyle();
        return sqlHooks.getPowerSql(args, guards);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        if (context.useHighPrecisionJs()) {
            // The next line works, but isn't secure, as it makes absurdly large numbers possible.
            //return JsValue.generate("Decimal.pow("+args[0] + "," + args[1]+ ")", args, false);
            
            // Doing the guard was difficult as well
            //String guard = "(Math.log(Math.abs("+args[0]+"))*"+args[1]+"<39)";
            //return JsValue.generate("("+guard+"?Decimal.pow("+ args[0] + "," + args[1] + ") : new " + context.getJsModule() + ".Decimal(0))", args, false, args);
            
            // This reduces the precision of Pow, but is safe and matches java
            return JsValue.generate("new " + context.getJsEngMod() + ".Decimal(Math.pow("+args[0] + "," + args[1]+ "))", args, false, args);
        }
        return JsValue.generate("Math.pow("+args[0] + "," + args[1]+ ")", args, false, args);
    }


}
