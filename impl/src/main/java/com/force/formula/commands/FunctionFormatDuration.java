/*
 * Copyright, 1999-2007, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaException;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.FormulaTime;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.impl.WrongArgumentTypeException;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.sql.SQLPair;

/**
 * Format a duration as DDD:HH:MM:SS 
 * FORMATDURATION(number[, includeDays]) - render the duration as a number of seconds as HH:MM:SS, or DDD:HH:MM:SS if includeDays is true
 * FORMATDURATION(DateTimeStart, DateTimeEnd) - render the duration from date start to date end as DDD:HH:MM:SS
 * FORMATDURATION(TimeStart, TimeEnd) - render the duration from time start to date end as HH:MM:SS
 * 
 * @author stamm
 * @since 0.2.0
 */
@AllowedContext(section=SelectorSection.DATE_TIME)
public class FunctionFormatDuration extends FormulaCommandInfoImpl implements FormulaCommandValidator {
    public FunctionFormatDuration() {
        super("FORMATDURATION", String.class, new Class[] { String.class, BigDecimal.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionFormatDurationCommand(this, node.getNumberOfChildren());
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

        // handle unary operation for a number of seconds (BigDecimal)
        if (numberOfChildren == 1) {
            if (lhs != BigDecimal.class && lhs != RuntimeType.class) {
                throw new WrongArgumentTypeException(node.getText(), new Class[] { BigDecimal.class }, lhsNode);
            }

            return lhs == RuntimeType.class ? lhs : String.class;
        } else {
            // Valid datatype combinations are BigDecimal,Boolean or DateTime,DateTime or Time,Time
            FormulaAST rhsNode = (FormulaAST)lhsNode.getNextSibling();
            FormulaAST invalidNode = lhsNode;

            Type rhs = rhsNode.getDataType();

            if ((lhs == ConstantNull.class) || (rhs == ConstantNull.class))
                return String.class;

            if ((lhs == RuntimeType.class) || (rhs == RuntimeType.class))
                return String.class;

            if (lhs == FormulaDateTime.class) {
                if (rhs != FormulaDateTime.class) {
                    throw new WrongArgumentTypeException(node.getText(), new Class[] { FormulaDateTime.class }, rhsNode);
                }
            } else if (lhs == FormulaTime.class) {
                if (rhs != FormulaTime.class) {
                    throw new WrongArgumentTypeException(node.getText(), new Class[] { FormulaTime.class }, rhsNode);
                }
            } else if (lhs == BigDecimal.class) {
                if (rhs != Boolean.class) {
                    throw new WrongArgumentTypeException(node.getText(), new Class[] { Boolean.class }, rhsNode);
                }
            } else {
                throw new WrongArgumentTypeException(node.getText(), new Class[] { BigDecimal.class, FormulaTime.class,
                        FormulaDateTime.class }, invalidNode);
            }

            return String.class;
        }
    }
    
    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String guard = SQLPair.generateGuard(guards, null);
        FormulaSqlHooks hooks = getSqlHooks(context);
        
        FormulaAST lhs = (FormulaAST)node.getFirstChild();
        Type lhsDataType = lhs.getDataType();
        
        if (node.getNumberOfChildren() == 1) {
            String interval = String.format(hooks.sqlIntervalFromSeconds(lhsDataType), args[0]);
            return new SQLPair(hooks.sqlIntervalToDurationString(interval, false, null), guard);
        }

        String sql;
        if (lhsDataType == BigDecimal.class) {
            // The second parameter is the boolean to include date or not.  Simplify the logic if it's a constant
            String interval = String.format(hooks.sqlIntervalFromSeconds(lhsDataType), args[0]);
            if ("(1=1)".equals(args[1]) || "TRUE".equalsIgnoreCase(args[1])) {
                sql = hooks.sqlIntervalToDurationString(interval, true, null);  // Include days
            } else if ("(0=1)".equals(args[1]) || "FALSE".equalsIgnoreCase(args[1])) {
                sql = hooks.sqlIntervalToDurationString(interval, false, null); // Don't include days
            } else {
                sql = hooks.sqlIntervalToDurationString(interval, true, args[1]);  // Have the complicated logic
            }
        } else if (lhsDataType == FormulaTime.class) {
            String diff = String.format(hooks.sqlSubtractTwoTimes(), args[1], args[0]);
            String interval = String.format(hooks.sqlIntervalFromSeconds(lhsDataType), diff);
            sql = hooks.sqlIntervalToDurationString(interval, false, null);
        } else if (lhsDataType == FormulaDateTime.class) {
            String diff = String.format(hooks.sqlSubtractTwoTimestamps(true, lhsDataType), args[1], args[0]);
            String interval = String.format(hooks.sqlIntervalFromSeconds(lhsDataType), diff);
            sql = hooks.sqlIntervalToDurationString(interval, true, null);
        } else {
            throw new UnsupportedOperationException();
        }        

        return new SQLPair(sql.toString(), guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        FormulaAST lhs = (FormulaAST)node.getFirstChild();
        Type lhsDataType = lhs.getDataType();
        if (lhsDataType == BigDecimal.class) {
            if (args.length == 1) {
                return JsValue.forNonNullResult("$F.formatduration(Math.abs("+jsToNum(context,args[0].js)+"),false)", args);
            } else {
                // Only guard against the value, not the isDays boolean.
                return JsValue.forNonNullResult("$F.formatduration(Math.abs("+jsToNum(context,args[0].js)+"),"+args[1]+")", new JsValue[] {args[0]});
            }
        } else if (lhsDataType == FormulaTime.class) {
            return JsValue.forNonNullResult("$F.formatduration(Math.abs(("+args[1]+".getTime()-"+args[0]+".getTime())/1000),false)", args);
        } else if (lhsDataType == FormulaDateTime.class) {
            return JsValue.forNonNullResult("$F.formatduration(Math.abs(("+args[1]+".getTime()-"+args[0]+".getTime())/1000),true)", args);
        } else {
            throw new UnsupportedOperationException();
        }    
    }

    public static class FunctionFormatDurationCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;

        private int kids;

        public FunctionFormatDurationCommand(FormulaCommandInfo info, int kids) {
            super(info);
            this.kids = kids;
        }

        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
            Object rhs = stack.pop();
            Long numSeconds = null;
            boolean includeDays = false;
            if (kids == 1) {
                BigDecimal amt = checkNumberType(rhs); // not really rhs.
                numSeconds = amt != null ? amt.longValue() : null;
            } else {
                Object lhs = stack.pop();
                if (lhs instanceof BigDecimal) {
                    BigDecimal amt = checkNumberType(lhs); 
                    numSeconds = amt.longValue();
                    includeDays = checkBooleanType(rhs) == Boolean.TRUE;
                } else if (lhs instanceof FormulaTime) {
                    FormulaTime lhsTime = (FormulaTime)lhs;
                    FormulaTime rhsTime = (FormulaTime)rhs;
                    numSeconds = rhsTime != null ? (rhsTime.getTimeInSeconds() - lhsTime.getTimeInSeconds()) : null;
                } else if (lhs instanceof FormulaDateTime) {
                    FormulaDateTime lhsDate = checkDateTimeType(lhs);
                    FormulaDateTime rhsDate = checkDateTimeType(rhs);
                    numSeconds = rhsDate != null ? (rhsDate.getTime() - lhsDate.getTime()) / 1000 : null;
                    includeDays = true;
                }
            }

            if (numSeconds == null) {
                stack.push(null);
                return;
            }

            // java.time doesn't include a suitable DurationFormat and using ICU4J is not easy
            String result;
            numSeconds = Math.abs(numSeconds);
            if (includeDays) {
                result = String.format("%d:%02d:%02d:%02d", numSeconds / 86400, (numSeconds / 3600) % 24,
                        (numSeconds / 60) % 60, numSeconds % 60);
            } else {
                result = String.format("%02d:%02d:%02d", (numSeconds / 3600), (numSeconds / 60) % 60, numSeconds % 60);
            }
            stack.push(result);
        }
    }
}

