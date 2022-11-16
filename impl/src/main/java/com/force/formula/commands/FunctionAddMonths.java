/**
 * 
 */
package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaException;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaValidationHooks;
import com.force.formula.impl.IllegalArgumentTypeException;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.impl.WrongNumberOfArgumentsException;
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
        FormulaAST firstNode = (FormulaAST)node.getFirstChild();
        Type dateDataType = firstNode.getDataType();
        String sql = getSqlHooks(context).sqlAddMonths(args[0], dateDataType, args[1]);
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties)
            throws FormulaException {
        if (node.getNumberOfChildren() != 2) { throw new WrongNumberOfArgumentsException(node.getText(), 2, node); }

        FormulaAST firstNode = (FormulaAST)node.getFirstChild();
        Type inputDataType = firstNode.getDataType();
        
        if (inputDataType != FormulaDateTime.class && inputDataType != Date.class
                && inputDataType != RuntimeType.class) { throw new IllegalArgumentTypeException(node.getText()); }

        Type rhsType = ((FormulaAST)firstNode.getNextSibling()).getDataType();

        if (rhsType != RuntimeType.class && rhsType != BigDecimal.class) {
        	throw new IllegalArgumentTypeException(node.getText());
        }
        
        return inputDataType;
    }

    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        String js =  context.getJsEngMod() + ".addmonths(" + args[0] + "," + jsToNum(context, args[1].js) + ")";
        return JsValue.generate(js, args, true, args);
    }

    /**
     * Specification:
     * https://www.oracletutorial.com/oracle-date-functions/oracle-add_months
     *
     * The ADD_MONTHS() returns a DATE value with the number of months away from a date.
     * If  date_expression is the last day of the month, the resulting date is always the last day of the month e.g., adding 1 month to 29-FEB-2016 will result in 31-MAR-2016, not 29-MAR-2016.
     * In case the resulting date whose month has fewer days than the day component of date_expression, the resulting date is the last day of the month. For example, adding 1 month to 31-JAN-2016 will result in 29-FEB-2016.
     * Otherwise, the function returns a date whose day is the same as the day component of the date_expression.
     *
     */
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
            	result = FormulaValidationHooks.get().functionHook_addNullMonthsAsZero() ? d : null; // match behavior of sql.
            } else {
                Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.GMT);
                c.setTime(d);
                boolean isLastDay = c.get(Calendar.DATE) == c.getActualMaximum(Calendar.DATE);

                // Fix W-5603209
                if (isLastDay) {
                    c.add(Calendar.DAY_OF_YEAR, 1);
                }

                c.add(Calendar.MONTH, months.intValue());

                /*
    			// Uncomment this out if you want to support fractions in addmonths
                BigDecimal fractionalPart = months.remainder(BigDecimal.ONE).multiply(MONTH_FRACTION);
                int fractionalDays = fractionalPart.intValue();
                if (fractionalDays != 0) {
                    c.add(Calendar.DAY_OF_YEAR, fractionalDays);
                }
                */

                // Fix W-5603209
                if (isLastDay) {
                    c.add(Calendar.DAY_OF_YEAR, -1);
                }

                result = input instanceof FormulaDateTime ? new FormulaDateTime(c.getTime()) : c.getTime();
            }
            stack.push(result);
        }
    }
}


