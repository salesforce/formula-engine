package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaException;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.sql.SQLPair;

/**
 * Return a suitable constant for PI (or acos(-1) if not otherwise available).
 *
 * @author stamm
 * @since 0.2.0
 */
@AllowedContext(section=SelectorSection.MATH, isOffline=true)
public class FunctionAtan2 extends FormulaCommandInfoImpl {
    public FunctionAtan2() {
        super("ATAN2", BigDecimal.class, new Class[] {BigDecimal.class, BigDecimal.class});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionAtan2Command(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String function = "ATAN2";
        FormulaSqlHooks hooks = (FormulaSqlHooks)context.getSqlStyle();
        if (hooks.isTransactSqlStyle()) {
            function = "ATN2";
        }
        String sql = function + "(" + args[0] + "," + args[1] + ")";
        sql = hooks.sqlTrigConvert(sql);
        String guard;
        FormulaAST divider = (FormulaAST)node.getFirstChild().getNextSibling();
        
        // Like with divide, we need to prevent a zero result in oracle in sqlserver.
        // ATAN2 can be defined as the angle in radiants between the positive x-acis and the ray from (0,0) to the point (x,y)
        //    this is what Math.atan2, Decimal.js, and Postgres2 do
        // Oracle & SqlServer, does the division first and fails.  So they need a guard and will return null, not 0.

        if (hooks.isOracleStyle() || hooks.isTransactSqlStyle()) {
            if (divider != null && divider.isLiteral() && divider.getDataType() == BigDecimal.class) {
                if (BigDecimal.ZERO.compareTo(new BigDecimal(args[1])) != 0) {
                    // no guard needed
                    guard = SQLPair.generateGuard(guards, null);
                } else {
                    // we know it's false, so return NULL
                    guard = SQLPair.generateGuard(guards, "0=0");
                    sql = "NULL";
                }
            } else {
                guard = SQLPair.generateGuard(guards, args[1] + "=0");
            } 
        } else {
            guard = SQLPair.generateGuard(guards, null);
        }
        

        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult(jsMathPkg(context) + ".atan2(" + args[0] + "," + args[1] + ")",null);
    }
    static class FunctionAtan2Command extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;
    
    	public FunctionAtan2Command(FormulaCommandInfo formulaCommandInfo) {
            super(formulaCommandInfo);
        }
    
    	@Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
    	    BigDecimal x = checkNumberType(stack.pop());
    	    /*  // If you want to match the behavior of oracle, include this.
    	    if (BigDecimal.ZERO.compareTo(x) != 0) {
                throw new ArithmeticException("Division by zero");
    	    }
    	    */
            BigDecimal y = checkNumberType(stack.pop());
            stack.push(BigDecimal.valueOf(Math.atan2(y.doubleValue(), x.doubleValue())));
        }
    }
}
