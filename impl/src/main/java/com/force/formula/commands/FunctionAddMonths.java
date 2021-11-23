/**
 * 
 */
package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.BaseLocalizer;

/**
 * @author stamm
 * @since 0.1.0
 */
@AllowedContext(section=SelectorSection.DATE_TIME)
public class FunctionAddMonths extends FormulaCommandInfoImpl implements FormulaCommandValidator {
    public FunctionAddMonths() {
        super("ADDMONTHS", BigDecimal.class, new Class[] { Date.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionAddMonthsCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = getSqlHooks(context).sqlAddMonths(args[0], args[1]);
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties)
            throws FormulaException {
        if (node.getNumberOfChildren() != 2) { throw new WrongNumberOfArgumentsException(node.getText(), 2, node); }

        FormulaAST firstNode = (FormulaAST)node.getFirstChild();
        Type inputDataType = firstNode.getDataType();
        Type rhsType = ((FormulaAST)firstNode.getNextSibling()).getDataType();

        if (inputDataType != FormulaDateTime.class && inputDataType != Date.class
                && inputDataType != RuntimeType.class) { throw new IllegalArgumentTypeException(node.getText()); }

        if (rhsType != RuntimeType.class && rhsType != BigDecimal.class) {
        	throw new IllegalArgumentTypeException(node.getText());
        }
        
        return inputDataType;
    }

    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        String js =  "$F.addmonths(" + args[0] + "," + jsToNum(context, args[1].js) + ")";
        return JsValue.generate(js, args, true, args[0]);
    }
    
    static class FunctionAddMonthsCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;

    	public FunctionAddMonthsCommand(FormulaCommandInfo formulaCommandInfo) {
            super(formulaCommandInfo);
        }
    	
    	// Uncomment this out if you want to support fractions in addmonths
    	//private static BigDecimal MONTH_FRACTION = new BigDecimal("365.24").divide(new BigDecimal("12.00"), RoundingMode.HALF_DOWN);

        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
            BigDecimal months = (BigDecimal) stack.pop();
        	Object input = stack.pop();
            Date d = checkDateType(input);
            Object result;
            if (d == null) {
            	result = null;  // No NPEs if null
            } else if (months == null) {
            	result = input; // Assume null = 0 months?
            } else {
                Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.GMT);
                c.setTime(d);
                c.add(Calendar.DAY_OF_YEAR, 1);  // 
                c.add(Calendar.MONTH, months.intValue());
                /*
    			// Uncomment this out if you want to support fractions in addmonths
                BigDecimal fractionalPart = months.remainder(BigDecimal.ONE).multiply(MONTH_FRACTION);
                int fractionalDays = fractionalPart.intValue();
                if (fractionalDays != 0) {
                    c.add(Calendar.DAY_OF_YEAR, fractionalDays);
                }
                */
                c.add(Calendar.DAY_OF_YEAR, -1);
                result = input instanceof FormulaDateTime ? new FormulaDateTime(c.getTime()) : c.getTime();
            }
            stack.push(result);
        }
    }
}


