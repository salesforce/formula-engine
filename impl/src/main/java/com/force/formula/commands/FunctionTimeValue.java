package com.force.formula.commands;

import java.lang.reflect.Type;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaDateUtil;
import com.force.i18n.BaseLocalizer;

/**
 * Describe your class here.
 *
 * @author pjain
 * @since 208
 */
@AllowedContext(section=SelectorSection.DATE_TIME, nonFlowOnly=true, isOffline=true)
public class FunctionTimeValue extends FormulaCommandInfoImpl implements FormulaCommandValidator {

    protected final static String JS_FORMAT_TEMPLATE = "new Date(new Date(%s).setUTCFullYear(1970,0,1))";

    public FunctionTimeValue() {
        super("TIMEVALUE", FormulaTime.class, new Class[] {});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionTimeValueCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards,
            TableAliasRegistry registry) {
        Type inputDataType = ((FormulaAST)node.getFirstChild()).getDataType();

        String sql;
        String guard;
        if (inputDataType == FormulaTime.class) {
            sql =  args[0];
            guard = SQLPair.generateGuard(guards, null);
        }
        else if (inputDataType == FormulaDateTime.class) {
            sql =  String.format("TO_NUMBER(TO_CHAR(%s, 'SSSSS')) * 1000", args[0]); // date does not have millisec info
            guard = SQLPair.generateGuard(guards, null);
        } 
        else {
            sql= String.format("TO_NUMBER(TO_CHAR(TO_TIMESTAMP(%s, 'HH24:mi:ss.FF'), 'SSSSS.FF3')) * 1000", args[0]);

            FormulaAST child = (FormulaAST)node.getFirstChild();
            if (child != null && child.isLiteral() && child.getDataType() == String.class) {
                if (FunctionTimeValueCommand.isTimeValid(ConstantString.getStringValue(child, true))) {
                    // no guard needed
                    guard = SQLPair.generateGuard(guards, null);
                } else {
                    // we know it's false
                    guard = SQLPair.generateGuard(guards, "0=0");
                    sql = "NULL";
                }
            } else {
                // Guard protects against malformed times as strings.
                guard = SQLPair
                        .generateGuard(
                                guards,
                                String.format(
                                        " NOT REGEXP_LIKE (%s, '^([01]\\d|2[0-3]):[0-5][0-9]:[0-5][0-9]\\.[0-9][0-9][0-9]$') /*comments to keep size */ ",
                                        args[0]));
            }
        }

        return new SQLPair(sql, guard);
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties)
            throws FormulaException {
        if (node.getNumberOfChildren() != 1) { throw new WrongNumberOfArgumentsException(node.getText(), 1, node); }

        Type inputDataType = ((FormulaAST)node.getFirstChild()).getDataType();

        if (inputDataType != FormulaDateTime.class && inputDataType != String.class && inputDataType != FormulaTime.class
                && inputDataType != RuntimeType.class) { throw new IllegalArgumentTypeException(node.getText()); }

        return inputDataType == RuntimeType.class ? inputDataType : FormulaTime.class;
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        Type inputDataType = ((FormulaAST)node.getFirstChild()).getDataType();
        if (inputDataType == FormulaDateTime.class) {
            return JsValue.forNonNullResult("new Date("+args[0] + ".setUTCFullYear(1970,0,1))", args);
        } else if (inputDataType == FormulaTime.class) {
            return args[0];
        } else {
            return JsValue.forNonNullResult(String.format(JS_FORMAT_TEMPLATE, args[0].js), args);
        }
    }

    public static class FunctionTimeValueCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;

		public FunctionTimeValueCommand(FormulaCommandInfo formulaCommandInfo) {
            super(formulaCommandInfo);
        }

        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
            Object input = stack.pop();

            FormulaTime value = null;
            if (input != null && input != ConstantString.NullString) {
                if (input instanceof FormulaTime)  {
                    value = (FormulaTime)input;
                }
                else if (input instanceof FormulaDateTime) {
                    // Convert from FormulaDateTime to Date
                    FormulaDateTime dateTimeInput = (FormulaDateTime)input;
                    if (dateTimeInput != null) {
                        Date date = dateTimeInput.getDate();
                        if (date != null) {
                            Calendar cal = new GregorianCalendar(BaseLocalizer.GMT_TZ);
                            cal.setTime(date);
                            value = FormulaEngine.getHooks().constructTime(FormulaDateUtil.millisecondOfDay(cal));
                        }
                    }
                } else {
                    try {
						value  = parseTime(checkStringType(input));
					} catch (FormulaDateException e) {
						FormulaEngine.getHooks().handleFormulaTimeException(e);
					}
                }
            }

            stack.push(value);
        }

        protected static  boolean isTimeValid(String date) {
            try {
                parseTime(date);
                return true;
            } catch (FormulaDateException x) {
                return false;
            }
        }

        // Convert from string of the form "HH:mm:ss.SSS" to Time
        protected static FormulaTime parseTime(String input) throws FormulaDateException{
            if (FormulaEngine.getHooks().isFormulaContainerCompiling()) {
                return null; // dummy values during compile won't work
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
            dateFormat.setLenient(false);
            dateFormat.setTimeZone(BaseLocalizer.GMT_TZ);
            ParsePosition p = new ParsePosition(0);
            Date d = dateFormat.parse(input, p);
            FormulaTime ret = null;
            if (d != null)
            	ret = FormulaEngine.getHooks().constructTime(d.getTime());
            if (ret == null || p.getIndex() != input.length() || p.getErrorIndex() != -1) {
                throw new FormulaDateException("Invalid time format: " + input);
            }
            return ret;
        }
    }
}
