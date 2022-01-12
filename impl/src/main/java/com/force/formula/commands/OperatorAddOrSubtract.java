/*
 * Created on Dec 10, 2004
 */
package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Deque;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaException;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.FormulaTime;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaRuntimeTypeException;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.impl.FormulaTypeUtils;
import com.force.formula.impl.IllegalArgumentTypeException;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.impl.WrongArgumentTypeException;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.BigDecimalHelper;
import com.force.formula.util.FormulaDateUtil;

/**
 * Describe your class here.
 *
 * @author dchasman, others
 * @since 140
 */
@AllowedContext(section = SelectorSection.MATH,isOffline=true)
public class OperatorAddOrSubtract extends FormulaCommandInfoImpl implements FormulaCommandValidator, FormulaCommandOptimizer {

    public OperatorAddOrSubtract(boolean doAddition) {
        super((doAddition) ? "+" : "-");

        this.performAddition = doAddition;
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        int numberOfChildren = node.getNumberOfChildren();
        if (numberOfChildren < 1) {
            throw new WrongNumberOfArgumentsException(node.getText(), 1, node);
        } else if (numberOfChildren > 2) {
            throw new WrongNumberOfArgumentsException(node.getText(), 2, node);
        }

        FormulaAST lhsNode = (FormulaAST)node.getFirstChild();
        Type lhs = lhsNode.getDataType();

        // handle unary operation
        if (numberOfChildren == 1) {
            if (lhs != BigDecimal.class && lhs != RuntimeType.class) {
                throw new WrongArgumentTypeException(node.getText(), new Class[] { BigDecimal.class }, lhsNode);
            }

            return lhs == RuntimeType.class ? lhs : BigDecimal.class;
        } else {
            // Valid datatype combinations are [Number +/- Number], [Date - (Number | Date)], [Date + Number], [String + String], Time + Number, [Time - (Time + Number)
            FormulaAST rhsNode = (FormulaAST)lhsNode.getNextSibling();
            FormulaAST invalidNode = lhsNode;

            Type rhs = rhsNode.getDataType();

            if ((lhs == ConstantNull.class) || (rhs == ConstantNull.class))
                return ConstantNull.class;

            if ((lhs == RuntimeType.class) || (rhs == RuntimeType.class))
                return RuntimeType.class;

            if (lhs == BigDecimal.class) {
                invalidNode = rhsNode;
                if (rhs == BigDecimal.class) {
                    return BigDecimal.class;
                } else if (rhs == Date.class) {
                    if (performAddition) {
                        return Date.class;
                    } else {
                        throw new WrongArgumentTypeException(node.getText(), new Class[] { BigDecimal.class }, rhsNode);
                    }
                } else if (rhs == FormulaDateTime.class) {
                    if (performAddition) {
                        return FormulaDateTime.class;
                    } else {
                        throw new WrongArgumentTypeException(node.getText(), new Class[] { BigDecimal.class }, rhsNode);
                    }
                }
            } else if (lhs == Date.class) {
                invalidNode = rhsNode;
                if (rhs == BigDecimal.class) {
                    return Date.class;
                } else if (rhs == Date.class) {
                    if (!performAddition) {
                        return BigDecimal.class;
                    } else {
                        throw new WrongArgumentTypeException(node.getText(), new Class[] { BigDecimal.class }, rhsNode);
                    }
                }
            } else if (lhs == FormulaDateTime.class) {
                invalidNode = rhsNode;
                if (rhs == BigDecimal.class) {
                    return FormulaDateTime.class;
                } else if (rhs == FormulaDateTime.class) {
                    if (!performAddition) {
                        return BigDecimal.class;
                    } else {
                        throw new WrongArgumentTypeException(node.getText(), new Class[] { BigDecimal.class }, rhsNode);
                    }
                }
            } else if (lhs == FormulaTime.class) {
                invalidNode = rhsNode;
                if (rhs == BigDecimal.class) {
                    return FormulaTime.class;
                } else if (rhs == FormulaTime.class) {
                    // we are allowed to subtract 2 time fields to get a number, but cannot add 2 time fields
                    if (!performAddition)  {
                        return BigDecimal.class;
                    }
                    else  {
                        throw new IllegalArgumentTypeException(node.getText());   
                    }
                }
            }else if (FormulaTypeUtils.isTypeText(lhs) && performAddition) {
            	//invalidNode = rhsNode;
                if (FormulaTypeUtils.isTypeText(rhs)) {
                    return String.class;
                } else {
                    throw new WrongArgumentTypeException(node.getText(), new Class[] { String.class }, rhsNode);
                }
            }

            throw new WrongArgumentTypeException(node.getText(), new Class[] { BigDecimal.class, Date.class,
                FormulaDateTime.class }, invalidNode);
        }
    }

    // optimizing things like -2 is quite pointless
    @Override
    public FormulaAST optimize(FormulaAST ast, FormulaContext context) throws FormulaException {
        if (ast.getNumberOfChildren() == 1 && ((FormulaAST)ast.getFirstChild()).isLiteral()) {
            ast.setConstantExpression(false);
        }
        if (ast.getDataType() == ConstantNull.class) {
            ast.setType(FormulaTokenTypes.NULL);
            ast.setText("null");
            ast.setCanBeNull(true);
            ast.setConstantExpression(true);
            ast.removeChildren();
        }
        return ast;
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws FormulaException {
        return new OperatorAddOrSubtractFormulaCommand(this, performAddition, node.getNumberOfChildren() == 1);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        // Unary operation
        String operator = getName();
        if (node.getNumberOfChildren() == 1) {
            return new SQLPair("(" + operator + args[0] + ")", guards[0]);
        }

        FormulaAST lhs = (FormulaAST)node.getFirstChild();
        FormulaAST rhs = (FormulaAST)lhs.getNextSibling();
        Type lhsDataType = lhs.getDataType();
        Type rhsDataType = rhs.getDataType();

        String lhsValue = truncateIfRequired(context, lhsDataType, rhsDataType, args[0]);
        String rhsValue = truncateIfRequired(context, rhsDataType, lhsDataType, args[1]);

        rhsValue = castNullIfNeeded(context, node.getDataType(), lhsDataType, rhsValue);
        lhsValue = castNullIfNeeded(context, node.getDataType(), rhsDataType, lhsValue);

        // Handle the string + string case
        if (FormulaTypeUtils.isTypeText(lhsDataType) && FormulaTypeUtils.isTypeText(rhsDataType)) {
            operator = "||";
        }
        
        String sql;
        if (lhsDataType == FormulaTime.class)  {
        	sql = getSqlHooks(context).sqlAddMillisecondsToTime(lhsValue, lhsDataType, rhsValue, rhsDataType, !"-".equals(operator));
        }
        else  {
        	FormulaSqlHooks hooks = getSqlHooks(context);
        	if (hooks.isOracleStyle()) { // Short circuit for oracle
                sql = "(" + lhsValue + operator + rhsValue + ")";
        	} else {
                // operations with date|timestamp types require special handling for Psql - W-7066598
                if (isSubtractionOfDateTimeValues(lhsDataType, rhsDataType)) {
                    // <date|timestamp> - <date|timestamp>
                    sql = String.format(getSqlHooks(context).sqlSubtractTwoTimestamps(false), lhsValue, rhsValue);
                } else if (isDateTimeAndNumberOperation(lhsDataType, rhsDataType) && !hooks.isOracleStyle()) {
                	sql = hooks.sqlAddDaysToDate(lhsValue, lhsDataType, rhsValue, rhsDataType, !"-".equals(operator));
                } else if ("||".equals(operator)) {
                    sql = "(" + String.format(getSqlHooks(context).sqlConcat(false), args[0], args[1]) + ")";
                } else {
                    sql = "(" + lhsValue + operator + rhsValue + ")";
                }
            }
        }

        String guard = SQLPair.generateGuard(guards, null);

        return new SQLPair(sql, guard);
    }

    /**
     * Returns true if we are subtracting date/time values. This method does no check if date|timestamp
     * operand types are compatible - it is done in
     * {@link #validate(FormulaAST node, FormulaContext context, FormulaProperties properties)}.
     */
    private boolean isSubtractionOfDateTimeValues(Type lhsDataType, Type rhsDataType) {
        return !this.performAddition && isDateTimeDatatype(lhsDataType) && isDateTimeDatatype(rhsDataType);
    }

    /**
     * Returns true for <date|timestamp> <+|-> <number> and <number> <+> <date|timestamp>
     * expressions. This method does no check if date|timestamp operand types are compatible - it is done in
     * {@link #validate(FormulaAST node, FormulaContext context, FormulaProperties properties)}.
     */
    private boolean isDateTimeAndNumberOperation(Type lhsDataType, Type rhsDataType) {

        // <date|timestamp> <+|-> <number>
        if (isDateTimeDatatype(lhsDataType) && rhsDataType == BigDecimal.class) { return true; }

        // <number> <+> <date|timestamp>
        if (lhsDataType == BigDecimal.class && this.performAddition && isDateTimeDatatype(rhsDataType)) { return true; }

        return false;
    }

    private static boolean isDateTimeDatatype(Type dataType) {
        return (dataType == Date.class || dataType == FormulaDateTime.class);
    }

    /*
     * LH:
     * Oracle support   date - date1 -> number   and   date - number -> date  .
     * date - NULL is ambiguous in this case (is it NULL the number or NULL the date?).
     *
     * Oracle assumes the number case by default.
     * So we need to cast that NULL to a date if the result is expected to be a number and the other operator is a date.
     */
    private String castNullIfNeeded(FormulaContext context, Type parentType, Type otherType, String value) {
        if (!performAddition && parentType == BigDecimal.class && (otherType == FormulaDateTime.class || otherType == Date.class) && "NULL".equals(value)) {
        	return String.format(getSqlHooks(context).sqlToDate(), "NULL");
        }
        return value;
    }

    private String truncateIfRequired(FormulaContext context, Type valueClass, Type otherValueClass, String value) {
        if ((valueClass == BigDecimal.class) && (otherValueClass == Date.class)) {
        	value = getSqlHooks(context).sqlTrunc(value);
        }

        return value;
    }

    private final boolean performAddition;

    public static Class<?> getCommandClass() {
        return OperatorAddOrSubtractFormulaCommand.class;
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        // Unary operation
        String operator = getName();
        if (node.getNumberOfChildren() == 1) {
            if (context.useHighPrecisionJs()) {
                return performAddition ? args[0] : JsValue.forNonNullResult(args[0] + ".neg()", args);
            }
            return JsValue.forNonNullResult("(" + operator + args[0] + ")", args);
        }
  
        // Handle dates
        FormulaAST lhsNode = (FormulaAST)node.getFirstChild();
        Type lhs = lhsNode.getDataType();
        FormulaAST rhsNode = (FormulaAST)lhsNode.getNextSibling();
        Type rhs = rhsNode.getDataType();

        // NOTE: Most of the complications here are due to the fact that in Java & SQL, we assume there is only second granularity, and not millisecond granularity
        String js;
        if (lhs == FormulaDateTime.class) {
            if (rhs == FormulaDateTime.class) {
                // Get the diff between the dates
                if (context.useHighPrecisionJs()) {
                    js = "new $F.Decimal((" + args[0] + ".getTime()" + operator + args[1] + ".getTime())/86400000)";
                } else {
                    js = "(" + args[0] + ".getTime()" + operator + args[1] + ".getTime())/86400000";
                }
            } else {
                js = "new Date(" + args[0] + ".getTime()" + operator + "(Math.round(86400*(" + args[1] + "))*1000))";
            }
        } else if (lhs == Date.class) {
            if (rhs == Date.class) {
                if (context.useHighPrecisionJs()) {
                    js = "new $F.Decimal((" + args[0] + ".getTime()" + operator + args[1] + ".getTime())/86400000)";
                } else {
                    js = "(" + args[0] + ".getTime()" + operator + args[1] + ".getTime())/86400000";
                }
            } else {
                // Math.trunc isn't available in all browsers, so use (|0) integer truncation trick assuming the number of days is < 2^31
                js = "new Date(" + args[0] + ".getTime()" + operator + "86400000*((" + args[1] + ")|0))";  // Math.trunc not available in all 
            }
        } else if (rhs == FormulaDateTime.class) {
            js = "new Date(" + args[1] + ".getTime()" + operator + "(Math.round(86400*(" + args[0] + "))*1000))";
        } else if (rhs == Date.class) {
            js = "new Date(" + args[1] + ".getTime()" + operator + "86400000*((" + args[0] + ")|0))";
        } else if (lhs == FormulaTime.class) {
            if (rhs == FormulaTime.class) {
                // Negative mod issues, so add Millis before mod-ing
                if (context.useHighPrecisionJs()) {
                    js = "(new $F.Decimal(" + args[0] + ".getTime()" + operator + args[1]+".getTime()+"+FormulaDateUtil.MILLISECONDSPERDAY+").mod("+FormulaDateUtil.MILLISECONDSPERDAY+"))";
                } else {
                    js = "((" + args[0] + ".getTime()" + operator + args[1]+".getTime()+"+FormulaDateUtil.MILLISECONDSPERDAY+")%"+FormulaDateUtil.MILLISECONDSPERDAY+")";
                }
            } else {
                js = "new Date(" + args[0] + ".getTime()" + operator + jsToNum(context, args[1].js)+")"   ;
            }
        } else if (rhs == FormulaTime.class) {
            js = "new Date(" + args[1] + ".getTime()" + operator + jsToNum(context, args[0].js)+")"  ;          
        } else if (lhs == String.class && rhs == String.class) {
            // TODO: see FunctionConcat
            return JsValue.generate("(" + FormulaCommandInfoImpl.jsNvl(args[0].js, "''") + operator + FormulaCommandInfoImpl.jsNvl(args[1].js, "''") + ")", args, false);
        } else if (lhs == BigDecimal.class && context.useHighPrecisionJs()) {
            js = args[0] + (performAddition ? ".add(" : ".sub(" ) + args[1] + ")";
        } else {
            js = args[0] + operator + args[1];
        }
        return JsValue.forNonNullResult(js, args);
     }

}

class OperatorAddOrSubtractFormulaCommand extends AbstractFormulaCommand {

    private static final long serialVersionUID = 1L;
	public OperatorAddOrSubtractFormulaCommand(FormulaCommandInfo info, boolean doAddition, boolean unary) {
        super(info);
        this.performAddition = doAddition;
        this.unary = unary;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        Object rhs = stack.pop();

        // Unary math
        if (unary) {
            BigDecimal value = checkNumberType(rhs);
            if (value == null)
                stack.push(null);
            else
                stack.push(performAddition ? value : value.negate());
            return;
        }

        rhs = fixString(rhs);
        Object lhs = fixString(stack.pop());
        Object value = null;

        if (notNull(rhs, lhs)) {
            if (lhs instanceof BigDecimal) {
                BigDecimal lhsValue = (BigDecimal)lhs;

                if (rhs instanceof BigDecimal) {
                    value = (performAddition) ? lhsValue.add((BigDecimal)rhs, BigDecimalHelper.MC_PRECISION_INTERNAL) : lhsValue.subtract((BigDecimal)rhs, BigDecimalHelper.MC_PRECISION_INTERNAL);
                } else if (rhs instanceof Date) {
                    value = addDurationToDate((Date)rhs, lhsValue, true);
                } else if (rhs instanceof FormulaDateTime) {
                    value = addDurationToDateTime((FormulaDateTime)rhs, lhsValue);
                }
            } else if (lhs instanceof Date) {
                if (rhs instanceof BigDecimal) {
                    value = addDurationToDate((Date)lhs, (BigDecimal)rhs, true);
                } else if (rhs instanceof Date && !performAddition) {
                    // this is fine since there is no DST in GMT timezone
                    value = new BigDecimal((((Date)lhs).getTime() - ((Date)rhs).getTime())).divide(FormulaDateUtil.MILLISECONDSPERDAY, BigDecimalHelper.MC_PRECISION_INTERNAL);
                }
            } else if (lhs instanceof FormulaDateTime) {
                if (rhs instanceof BigDecimal) {
                    value = addDurationToDateTime((FormulaDateTime)lhs, (BigDecimal)rhs);
                } else if (rhs instanceof FormulaDateTime && !performAddition) {
                    // this is fine because we're just concerned about 24 hour periods, not calendar days
                    value = new BigDecimal(((FormulaDateTime)lhs).getDate().getTime() - ((FormulaDateTime)rhs).getDate().getTime()).divide(FormulaDateUtil.MILLISECONDSPERDAY, BigDecimalHelper.MC_PRECISION_INTERNAL);
                }
            } else if (lhs instanceof FormulaTime) {
                if (rhs instanceof BigDecimal) {
                    long millisecsToAdd = ((BigDecimal)rhs).longValue();
                    millisecsToAdd = (performAddition) ? millisecsToAdd : -1 * millisecsToAdd;
                    value = FormulaEngine.getHooks().constructTime (((FormulaTime)lhs).getTimeInMillis() + millisecsToAdd);
                }
                else if (rhs instanceof FormulaTime) {
                    long millisecsToSubtract = ((FormulaTime)rhs).getTimeInMillis();
                    // we can only subtract 2 time fields
                    value = new BigDecimal(((FormulaTime)lhs).getTimeInMillis() - millisecsToSubtract).add(FormulaDateUtil.MILLISECONDSPERDAY).remainder(FormulaDateUtil.MILLISECONDSPERDAY, BigDecimalHelper.MC_PRECISION_INTERNAL);
                }
            }else if (lhs instanceof String && rhs instanceof String && performAddition) {
                // Handle string concatenation
                value = lhs.toString() + rhs.toString();
            }

            if (value == null) {
                throw new FormulaRuntimeTypeException(performAddition ? "+" : "-");
            }
        } else if (performAddition) {
            // Handle string concatenation.  At least one was null.
            // Other could be anything; we only care about string.
            // Other combinations with null result in null.
            if (lhs instanceof String) {
                value = lhs.toString();
            } else if (rhs instanceof String) {
                value = rhs.toString();
            }
        }

        stack.push(value);
    }

    static boolean notNull(Object rhs, Object lhs) {
        return !(rhs == null || lhs == null || rhs == ConstantString.NullString  || lhs == ConstantString.NullString
            || (rhs instanceof FormulaDateTime && ((FormulaDateTime)rhs).getDate() == null) || (lhs instanceof FormulaDateTime && ((FormulaDateTime)lhs)
            .getDate() == null));
    }

    private Date addDurationToDate(Date value, BigDecimal duration, boolean truncate) {
        return FormulaDateUtil.addDurationToDate(performAddition, value, duration, truncate);
    }

    /**
     * add a duration to a datetime(units of duration is days)
     */
    private FormulaDateTime addDurationToDateTime(FormulaDateTime value, BigDecimal duration) {
        return new FormulaDateTime(addDurationToDate(value.getDate(), duration, false));
    }

    private final boolean performAddition;
    private final boolean unary;
}
