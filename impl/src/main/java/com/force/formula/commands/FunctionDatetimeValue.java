package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Deque;
import java.util.regex.Pattern;

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
import com.force.i18n.BaseLocalizer;

/**
 * Describe your class here.
 *
 * @author lhofhansl
 * @since 148
 */
@AllowedContext(section=SelectorSection.DATE_TIME, isOffline=true)
public class FunctionDatetimeValue extends FormulaCommandInfoImpl implements FormulaCommandValidator {

    public FunctionDatetimeValue() {
        super("DATETIMEVALUE");
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new OperatorDatetimeValueFormulaCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        Type inputDataType = ((FormulaAST)node.getFirstChild()).getDataType();

        String sql;
        String guard;
        if (inputDataType == FormulaDateTime.class || inputDataType == Date.class) {
            sql = args[0];
            guard = SQLPair.generateGuard(guards, null);
        } else {
            sql = String.format(getSqlHooks(context).sqlToTimestampIso(), args[0]);

            FormulaAST child = (FormulaAST)node.getFirstChild();
            if (child != null && child.isLiteral() && child.getDataType() == String.class) {
                if (OperatorDatetimeValueFormulaCommand.isValidDateTime(ConstantString.getStringValue(child, true))) {
                    // no guard needed
                    guard = SQLPair.generateGuard(guards, null);
                } else {
                    // we know it's false
                    guard = SQLPair.generateGuard(guards, "0=0");
                    sql = "NULL";
                }
            } else {
                // Guard protects against malformed dates as strings
                guard = SQLPair
                    .generateGuard(
                        guards,
                        String
                            .format(
                            		getSqlHooks(context).sqlDatetimeValueGuard(),
                                args[0]));
            }
        }

        return new SQLPair(sql, guard);
        
        
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) {
        Type inputDataType = ((FormulaAST)node.getFirstChild()).getDataType();
        if (inputDataType == FormulaDateTime.class) {
            return args[0];
        } else if (inputDataType == BigDecimal.class) {
            return JsValue.forNonNullResult("new Date(" + jsToNum(context, args[0].js) + ")",args);
        } else {
            return JsValue.forNonNullResult(context.getJsEngMod() + ".parseDateTime("+ args[0].js + ")", args);
        }
    }
   
    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        if (node.getNumberOfChildren() != 1) {
            throw new WrongNumberOfArgumentsException(node.getText(), 1, node);
        }

        Type inputDataType = ((FormulaAST)node.getFirstChild()).getDataType();

        if (inputDataType != FormulaDateTime.class && inputDataType != String.class && inputDataType != Date.class
                        && inputDataType != RuntimeType.class) {
            throw new IllegalArgumentTypeException(node.getText());
        }

        return inputDataType == RuntimeType.class ? inputDataType : FormulaDateTime.class;
    }
}

class OperatorDatetimeValueFormulaCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public OperatorDatetimeValueFormulaCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws FormulaException {
        Object input = stack.pop();

        Object value = null;
        if (input != null) {
            if (input instanceof FormulaDateTime) {
                value = input;
            } else if (input instanceof Date) {
                value = new FormulaDateTime((Date)input);
            } else {
            	try {
            		value = parseDateTime(checkStringType(input));
            	} catch (FormulaDateException ex) {
            		FormulaEngine.getHooks().handleFormulaDateException(ex);
            	}
            }
        }

        stack.push(value);
    }

    protected static boolean isValidDateTime(String datetime) {
        try {
            parseDateTime(datetime);
            return true;
        } catch (FormulaDateException x) {
            return false;
        }
    }

    private static Pattern DATE_PATTERN = Pattern.compile("\\d{4}-.*");
    protected static FormulaDateTime parseDateTime(String input) throws FormulaDateException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setLenient(false);
        dateFormat.setTimeZone(BaseLocalizer.GMT_TZ);
        try {
            // Do a pre-check for 4-digit year (setLenient does not require this)
            if (!DATE_PATTERN.matcher(input).matches()) {
                throw new FormulaDateException("Invalid year for DATEVALUE function");
            }
            return new FormulaDateTime(dateFormat.parse(input));
        }
        catch (ParseException x) {
            throw new FormulaDateException(x);
        }

    }
}
