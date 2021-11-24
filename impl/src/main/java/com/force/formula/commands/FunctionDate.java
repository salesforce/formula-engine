package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.BaseLocalizer;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
// This does handle leap years correctly with bug W-878810
@AllowedContext(section=SelectorSection.DATE_TIME, isOffline=true)
public class FunctionDate extends FormulaCommandInfoImpl {
    public FunctionDate() {
        super("DATE", Date.class, new Class[] { BigDecimal.class, BigDecimal.class, BigDecimal.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionDateCommand(this);
    }

    private static final int BAD_VALUE = -1;

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
    	FormulaSqlHooks hooks = getSqlHooks(context);
    	FormulaAST yearNode = (FormulaAST)node.getFirstChild();
        int yearValue = getInt(yearNode);
        FormulaAST monthNode = (FormulaAST)yearNode.getNextSibling();
        int monthValue = getInt(monthNode);
        FormulaAST dayNode = (FormulaAST)monthNode.getNextSibling();
        int dayValue = getInt(dayNode);

        String year = (yearValue != BAD_VALUE) ? String.valueOf(yearValue) : String.format(getSqlHooks(context).sqlToChar(), "FLOOR(" + args[0] + ")");
        String month = (monthValue != BAD_VALUE) ? String.valueOf(monthValue) : String.format(getSqlHooks(context).sqlToChar(), "FLOOR(" + args[1] + ")");
        String day = (dayValue != BAD_VALUE) ? String.valueOf(dayValue) : String.format(getSqlHooks(context).sqlToChar(), "FLOOR(" + args[2] + ")");
        String date;
        if (hooks.isMysqlStyle()) {
        	date = "DATE(CONCAT(" + year + ",'-'," + month + ",'-'," + day + "))";
        } else if (hooks.isTransactSqlStyle()) {
        	date = "DATEFROMPARTS(" + year + "," + month + "," + day + ")";
        } else {
        	date = "TO_DATE(" + year + " || '-' || " + month + " || '-' || " + day + ", 'YYYY-MM-DD')";
        }

        String nullBits = "";
        boolean yearCanBeNull = yearValue == BAD_VALUE && yearNode.canBeNull();
        if (yearCanBeNull) {
            nullBits = "WHEN " + args[0] + " IS NULL THEN NULL ";
        }
        boolean monthCanBeNull = monthValue == BAD_VALUE && monthNode.canBeNull();
        if (monthCanBeNull) {
            nullBits = nullBits + "WHEN " + args[1] + " IS NULL THEN NULL ";
        }
        boolean dayCanBeNull = dayValue == BAD_VALUE && dayNode.canBeNull();
        if (dayCanBeNull) {
            nullBits = nullBits + "WHEN " + args[2] + " IS NULL THEN NULL ";
        }

        String sql;
        if ("".equals(nullBits)) {
            sql = date;
        } else {
            sql = "CASE " + nullBits + "ELSE " + date + " END";
        }
        String guard = SQLPair.generateGuard(
                guards,
                errorCondition(hooks, args, yearValue, yearCanBeNull, monthValue, monthCanBeNull, dayValue,
                        dayCanBeNull));
        return new SQLPair(sql, guard);
    }

    private int getInt(FormulaAST currentNode) {
        if (currentNode.getType() != FormulaTokenTypes.NUMBER)
            return BAD_VALUE;
        try {
            return Integer.parseInt(currentNode.getText());
        }
        catch (NumberFormatException e) {
            return BAD_VALUE;
        }
    }

    private String errorCondition(FormulaSqlHooks hooks, String[] args, int yearValue, boolean yearCanBeNull, int monthValue,
            boolean monthCanBeNull, int dayValue, boolean dayCanBeNull) {
        String result = null;
        if (yearValue != BAD_VALUE) {
            if ((yearValue < 1) || (yearValue > 9999)) {
                return "1=1";
            }
        } else {
            result = args[0] + " is null OR " + args[0] + "< 1 OR " + args[0] + "> 9999";
        }
        if (monthValue != BAD_VALUE) {
            if ((monthValue < 1) || (monthValue > 12)) {
                return "1=1";
            }
        } else {
            String month = args[1] + " is null  OR FLOOR(" +args[1] + ") NOT IN (1,2,3,4,5,6,7,8,9,10,11,12)";
            if (result == null) {
                result = month;
            } else {
                result = result + " OR " + month;
            }
        }
        if (dayValue != BAD_VALUE) {
            if ((dayValue < 1) || (dayValue > 31)) {
                return "1=1";
            }
        } else {
            String day = args[2] + " is null OR " + args[2] + "< 1 OR " + args[2] + " >= 32";
            if (result == null) {
                result = day;
            } else {
                result = result + " OR " + day;
            }
        }
        if ((monthValue == BAD_VALUE) && (dayValue == BAD_VALUE)) {
            result = result + " OR " + getValidDayInMonthSQL(hooks, args, yearValue, monthValue, dayValue);
        } else if ((monthValue != BAD_VALUE) && (dayValue == BAD_VALUE)) {
            if ((monthValue == 4) || (monthValue == 6) || (monthValue == 9) || (monthValue == 11))
                result = result + " OR " + args[2] + " >= 31";
            else if (monthValue == 2){
                if(yearValue == BAD_VALUE){
                    result = result + " OR " + getValidDayInMonthSQL(hooks, args, yearValue, monthValue, dayValue);
                }else{
                  result = result + " OR " + args[2] + " >= " + (getFebruaryLastDay(yearValue)+1);
                }
            }
        } else if ((monthValue == BAD_VALUE) && (dayValue != BAD_VALUE)) {
            if (dayValue > 30)
                result = result + " OR FLOOR(" + args[1] + ") IN (4,6,9,11)";
            if(yearValue == BAD_VALUE){
                result = result + " OR " + getValidDayInMonthSQL(hooks, args, yearValue, monthValue, dayValue);
            }else{
                if (dayValue > getFebruaryLastDay(yearValue))
                    result = result + " OR FLOOR(" + args[1] + ")=2";
            }

        } else {
            if (((monthValue == 4) || (monthValue == 6) || (monthValue == 9) || (monthValue == 11)) && (dayValue > 30))
                return "1=1";
            else if ((monthValue == 2) && (dayValue > 28)){
                if(yearValue == BAD_VALUE){
                    result = result + " OR " + getValidDayInMonthSQL(hooks, args, yearValue, monthValue, dayValue);
                }else{
                    if (dayValue > getFebruaryLastDay(yearValue))
                      return "1=1";
                }
            }
        }
        if ((yearCanBeNull || monthCanBeNull || dayCanBeNull)) {
            String nullChecks = "";
            if (yearCanBeNull)
                nullChecks = nullChecks + args[0] + " IS NULL ";
            if (monthCanBeNull)
                nullChecks = nullChecks + (nullChecks.length() > 0 ? "OR " : "") + args[1] + " IS NULL ";
            if (dayCanBeNull)
                nullChecks = nullChecks + (nullChecks.length() > 0 ? "OR " : "") + args[2] + " IS NULL ";
            result = "NOT (" + nullChecks + ") AND (" + result + ")";
        }
        return result;
    }

    private String getValidDayInMonthSQL(FormulaSqlHooks hooks, String[] args, int yearValue, int monthValue, int dayValue) {    	
        String toDateSQL;
    	if (hooks.isMysqlStyle()) {
    		toDateSQL =   "DATE(CONCAT("
    		+ (yearValue == BAD_VALUE ? "FLOOR(" + args[0] + ")" : yearValue)
                        + ",'-',"
                + (monthValue == BAD_VALUE ? "FLOOR(" + args[1] + ")" : monthValue)
                        + ",'-01'))";
    	} else if (hooks.isTransactSqlStyle()) {
    		toDateSQL = "DATEFROMPARTS(" + (yearValue == BAD_VALUE ? "FLOOR(" + args[0] + ")" : yearValue) 
    				+ "," + (monthValue == BAD_VALUE ? "FLOOR(" + args[1] + ")" : monthValue) + ",1)";
    	} else {
    		toDateSQL =   "TO_DATE("
    		+ (yearValue == BAD_VALUE ? "FLOOR(" + args[0] + ")" : yearValue)
                        + " || '-' || "
                + (monthValue == BAD_VALUE ? "FLOOR(" + args[1] + ")" : monthValue)
                        + ",'YYYY-MM')";
    	}
        String lastDaySQL = String.format(hooks.sqlLastDayOfMonth(), toDateSQL);
        
        return " " + (dayValue == BAD_VALUE ? args[2]: dayValue) + " >= " + lastDaySQL + "+1 ";
    }

    private int getFebruaryLastDay(int year){
       return isLeapYear(year) ? 29 : 28;
    }

    private boolean isLeapYear(int  year){
        if (year % 4 == 0) {
            if (year % 100 == 0) {
                if (year % 400 == 0) {
                    return true;
                }
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult("new Date(Date.UTC("+args[0] + ".toNumber()," + args[1] + ".toNumber()-1," + args[2] + ".toNumber()))", args);            
        }
        return JsValue.forNonNullResult("new Date(Date.UTC("+args[0] + "," + args[1] + "-1," + args[2] + "))", args);
    }
}

class FunctionDateCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionDateCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        BigDecimal day = checkNumberType(stack.pop());
        BigDecimal month = checkNumberType(stack.pop());
        BigDecimal year = checkNumberType(stack.pop());
        if ((day == null) || (month == null) || (year == null) || FormulaEngine.getHooks().isFormulaContainerCompiling())
            stack.push(null);
        else {
            int y = year.intValue();
            int m = month.intValue();
            int d = day.intValue();
            if ((y < 1) || (y > 9999))
                throw new FormulaEvaluationException("Year out of range in DATE() function");
            try {
                Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.GMT);
                c.clear();
                c.setLenient(false);
                c.set(y, m - 1, d); // Months are zero-based in Java Calenders
                stack.push(c.getTime());
            } catch (IllegalArgumentException x) {
                throw new FormulaEvaluationException("Month or Day out of range in DATE() function"); // NOPMD
            }
        }
    }
}