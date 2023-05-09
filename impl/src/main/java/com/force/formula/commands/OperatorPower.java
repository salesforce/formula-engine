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
        // For small values close to zero
        String guardZero = "(Math.log10(Math.abs("+args[0]+"))*"+args[1]+"<-39)";
        // For big values of the order of 10^39, return null. This behaviour is same as SQL.
        String guardBigNo = "(Math.log10(Math.abs("+args[0]+"))*"+args[1]+">39)";
        if (context.useHighPrecisionJs()) {
            // The next line works, but isn't secure, as it makes absurdly large numbers possible.
            // return JsValue.generate("Decimal.pow("+args[0] + "," + args[1]+ ")", args, false);

            // If isNaN(Math.pow(a,b)) or guardBigNo is true return null; else if guardZero is true return 0 else execute $F.Decimal(Math.pow(a,b));
            // Using $F.Decimal(Math.pow(a,b)) Reduces the precision of Pow, but is safe and matches java.
            return JsValue.generate("("+ guardBigNo + " || isNaN(Math.pow("+ args[0] + "," + args[1] + ")) ? null : (" +guardZero+" ? 0 : new " + context.getJsEngMod() + ".Decimal(Math.pow("+args[0] + "," + args[1]+ "))))", args, false, args);
        }
        // If isNaN(Math.pow(a,b)) or guardBigNo is true return null; else if guardZero is true return 0 else execute Math.pow(a,b));
        return JsValue.generate("("+ guardBigNo + " || isNaN(Math.pow("+ args[0] + "," + args[1] + ")) ? null : (" +guardZero+" ? 0 :Math.pow("+args[0] + "," + args[1]+ ")))", args, false, args);
    }


}
