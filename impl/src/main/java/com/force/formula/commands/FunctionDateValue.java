package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.regex.Pattern;

import com.force.formula.BindingObserver;
import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDateException;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaException;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.IllegalArgumentTypeException;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaDateUtil;
import com.force.formula.util.FormulaI18nUtils;
import com.force.formula.util.FormulaTextUtil;
import com.force.i18n.BaseLocalizer;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 144
 */
@AllowedContext(section = SelectorSection.DATE_TIME, isOffline=true)
public class FunctionDateValue extends FormulaCommandInfoImpl implements FormulaCommandValidator {

    public static final String FUNCTION_NAME = "DATEVALUE";
    protected static final String JS_FORMAT_TEMPLATE = "new Date(new Date(%s).setUTCHours(0,0,0,0))";

    public FunctionDateValue() {
        super(FUNCTION_NAME);

        FormulaCommandInfoRegistry.addBindingObserver(timeZoneIdBindingObserver);
        FormulaCommandInfoRegistry.addBindingObserver(timeZoneOffsetBindingObserver);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        Type inputDataType = ((FormulaAST)node.getFirstChild()).getDataType();
        boolean isDateTime = inputDataType == FormulaDateTime.class;
        return new OperatorDateValueFormulaCommand(this, isDateTime);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards,
            TableAliasRegistry registry) {
        Type inputDataType = ((FormulaAST)node.getFirstChild()).getDataType();

        String sql;
        String guard;
        if (inputDataType == FormulaDateTime.class) {
        	sql = getSqlHooks(context).sqlConvertDateTimeToDate(args[0], USERS_TIMEZONE_ID_MARKER, USERS_TIMEZONE_OFFSET_MARKER);
            guard = SQLPair.generateGuard(guards, null);
        } else {
            sql = String.format(getSqlHooks(context).sqlToDateIso(), args[0]);

            FormulaAST child = (FormulaAST)node.getFirstChild();
            if (child != null && child.isLiteral() && child.getDataType() == String.class) {
                if (OperatorDateValueFormulaCommand.isDateValid(ConstantString.getStringValue(child, true))) {
                    // no guard needed
                    guard = SQLPair.generateGuard(guards, null);
                } else {
                    // we know it's false
                    guard = SQLPair.generateGuard(guards, "0=0");
                    sql = String.format(getSqlHooks(context).sqlToDate(), "NULL");
                }
            } else {
                // Guard protects against malformed dates as strings. It assumes all months have 31 days. Validates invalid months. Accepts years from 0000-9999.
                 guard = SQLPair
                     .generateGuard(
                     guards,
                     String.format(
                    	getSqlHooks(context).sqlDateValueGuard(),
                        args[0]));
               /*Other 2 options of guards, probably when we get versioning we can switch to either one of these:

                 Option1: guards against invalid months not in 1-12 and invalid days in a month (>31). Assumes all months have 31 days. So Apr 31 and Feb 30 are not guarded.
                 Year 0000 is guarded.
                 " NOT REGEXP_LIKE (%s, '^(000[1-9]|00[1-9]\\d|0[[1-9]\\d\\d|[1-9]\\d{3})-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$')",

                 Option 2: most robust guard, this will guard invalid dates like april 31 or february 30. The only problem with this one
                 is that it assumes february always has 29 days, so will not catch as invalid date feb 29 in a non leap year.
                 " NOT REGEXP_LIKE (%s, '^(000[1-9]|00[1-9]\\d|0[[1-9]\\d\\d|[1-9]\\d{3})-(((0?[1-9]|1[012])-(0?[1-9]|[12][0-9]))|((0?[13578]|1[02])-3[01])|((0?[469]|11)-30))$')",

                */

            }
        }

        return new SQLPair(sql, guard);
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties)
            throws FormulaException {
        if (node.getNumberOfChildren() != 1) { throw new WrongNumberOfArgumentsException(node.getText(), 1, node); }

        Type inputDataType = ((FormulaAST)node.getFirstChild()).getDataType();

        if (inputDataType != FormulaDateTime.class && inputDataType != String.class
                && inputDataType != RuntimeType.class) { throw new IllegalArgumentTypeException(node.getText()); }

        return inputDataType == RuntimeType.class ? inputDataType : Date.class;
    }

    // sets timezone IDs during the late binding stage ("__TZ_ID__" -> "America/Los_Angeles")
    private static final BindingObserver timeZoneIdBindingObserver = new BindingObserver() {
        @Override
        public String bind(String value) {
            Calendar cal = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.LOCAL);
            String timezone = FormulaEngine.getHooks().getDbTimeZoneID(cal.getTimeZone());
            return FormulaTextUtil.replaceSimple(value, FunctionDateValue.USERS_TIMEZONE_ID_MARKER, timezone);
        }
    };

    private static final BindingObserver timeZoneOffsetBindingObserver = new BindingObserver() {
        @Override
        public String bind(String value) {
            int offsetInHours = FunctionDateValue.getUserTimezoneOffsetHours();
            return FormulaTextUtil.replaceSimple(value, FunctionDateValue.USERS_TIMEZONE_OFFSET_MARKER, String.valueOf(offsetInHours));
        }
    };

    public static int getUserTimezoneOffsetHours() {
        Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.LOCAL);
        return c.getTimeZone().getRawOffset() / (1000 * 60 * 60);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        Type inputDataType = ((FormulaAST)node.getFirstChild()).getDataType();
        if (inputDataType == FormulaDateTime.class) {
            return JsValue.forNonNullResult("new Date("+args[0] + ".setUTCHours(0,0,0,0))", args);
        } else if (inputDataType == BigDecimal.class) {
            return JsValue.forNonNullResult(String.format(JS_FORMAT_TEMPLATE, jsToNum(context,args[0].js)), args);
        } else {
            return JsValue.forNonNullResult(String.format(JS_FORMAT_TEMPLATE, args[0].js),args);
        }
    }

    private static final String USERS_TIMEZONE_ID_MARKER = "__TZ_ID__";
    private static final String USERS_TIMEZONE_OFFSET_MARKER = "__TZ_OFFSET__";

    public static class OperatorDateValueFormulaCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;
		private final boolean isDateTime;

        public OperatorDateValueFormulaCommand(FormulaCommandInfo formulaCommandInfo, boolean isDateTime) {
            super(formulaCommandInfo);
            this.isDateTime = isDateTime;
        }

        @Override
        public boolean isDeterministic(FormulaContext context) {
            // When we convert from dateTime to date, we use User's TZ, so not deterministic.
            return !isDateTime;
        }

        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws FormulaException {
            Object input = stack.pop();

            Date value = null;
            if (input != null && input != ConstantString.NullString) {
                if (input instanceof FormulaDateTime) {
                    // Convert from FormulaDateTime to Date
                    FormulaDateTime dateTimeInput = (FormulaDateTime)input;
                    if (dateTimeInput != null) {
                        Date date = dateTimeInput.getDate();
                        if (date != null) {
                            Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.LOCAL);
                            value = FormulaDateUtil.truncateDateToOwnersGmtMidnight(c.getTimeZone(), date);
                        }
                    }
                } else {
                	try {
                		value = parseDate(checkStringType(input));
                	} catch (FormulaDateException x) {
                		FormulaEngine.getHooks().handleFormulaDateException(x);
                	}
                }
            }

            stack.push(value);
        }

        protected static boolean isDateValid(String date) {
            try {
                parseDate(date);
                return true;
            } catch (FormulaDateException x) {
                return false;
            }
        }
        
        private static Pattern DATE_PATTERN = Pattern.compile("\\d{4}-.*");

        // Convert from string of the form "YYYY-MM-DD" to Date
        protected static Date parseDate(String input) throws FormulaDateException {
            if (FormulaEngine.getHooks().isFormulaContainerCompiling()) {
                return null; // dummy values during compile won't work
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setLenient(false);
            try {
                // Do a pre-check for 4-digit year (setLenient does not require this)
                if (!DATE_PATTERN.matcher(input).matches()) { throw new FormulaDateException(
                        "Invalid year for DATEVALUE function"); }
                return dateFormat.parse(input);
            } catch (ParseException x) {
                throw new FormulaDateException(x);
            }

        }
    }
}
