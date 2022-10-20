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
public class FunctionPi extends FormulaCommandInfoImpl {
    public FunctionPi() {
        super("PI", BigDecimal.class, new Class[] {});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionPiCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        FormulaSqlHooks hooks = getSqlHooks(context);
        String function = "PI()";
        if (hooks.isOracleStyle() || hooks.isGoogleStyle()) {
            function = "3.14159265358979323846"; // Use a constant
        }
        return new SQLPair(hooks.sqlTrigConvert(function), null);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        if (context.useHighPrecisionJs()) {
            // $F.Decimal.PI doesn't work right, but acos does.
            return JsValue.forNonNullResult("$F.Decimal.acos(-1)",null);
        } else {
            return JsValue.forNonNullResult("Math.PI",null);
        }
    }

    static class FunctionPiCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;
    
    	public FunctionPiCommand(FormulaCommandInfo formulaCommandInfo) {
            super(formulaCommandInfo);
        }
    
    	@Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
    	    stack.push(new BigDecimal("3.14159265358979323846"));
        }
    }	
}
