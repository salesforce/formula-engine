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
 * Describe your class here.
 * 
 * @author djacobs
 * @since 140
 */
@AllowedContext(section = SelectorSection.MATH)  // Not supported in offline because of the precision loss.
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
        String sql = "POWER(" + args[0] + ", " + args[1] + ")";
        String guard;
    	if (context.getSqlStyle().isMysqlStyle()) {
    		guard = SQLPair.generateGuard(guards, "TRUNCATE(" + args[1] + ",0)<>" + args[1] +
    	            " OR(" + args[0] + "<>0 AND LOG(10,ABS(" + args[0] + "))*" + args[1] + ">38)");    	
    	} else {
    		guard = SQLPair.generateGuard(guards, "TRUNC(" + args[1] + ")<>" + args[1] +
    	            " OR(" + args[0] + "<>0 AND LOG(10,ABS(" + args[0] + "))*" + args[1] + ">38)");    	
    	}
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        if (context.useHighPrecisionJs()) {
            // The next line works, but isn't secure, as it makes absurdly large numbers possible.
            //return JsValue.generate("Decimal.pow("+args[0] + "," + args[1]+ ")", args, false);
            
            // Doing the guard was difficult as well
            //String guard = "(Math.log(Math.abs("+args[0]+"))*"+args[1]+"<39)";
            //return JsValue.generate("("+guard+"?Decimal.pow("+ args[0] + "," + args[1] + ") : new $F.Decimal(0))", args, false, args);
            
            // This reduces the precision of Pow, but is safe and matches java
            return JsValue.generate("new $F.Decimal(Math.pow("+args[0] + "," + args[1]+ "))", args, false, args);
        }
        return JsValue.generate("Math.pow("+args[0] + "," + args[1]+ ")", args, false, args);
    }


}
