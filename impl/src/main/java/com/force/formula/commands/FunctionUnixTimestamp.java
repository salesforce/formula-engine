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
import com.force.formula.FormulaTime;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.IllegalArgumentTypeException;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.BaseLocalizer;

/**
 * Return the Unix Epoch Time for a Date or DateTime.
 * UNIXTIMESTAMP(Date/Time/Datetime) returns a Number
 *
 * @author stamm
 * @since 0.1.2
 */
@AllowedContext(section=SelectorSection.DATE_TIME, isOffline=true)
public class FunctionUnixTimestamp extends FormulaCommandInfoImpl implements FormulaCommandValidator  {
    public FunctionUnixTimestamp() {
        super("UNIXTIMESTAMP", BigDecimal.class, new Class[] { Date.class});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionUnixTimestampCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        Type type = ((FormulaAST)node.getFirstChild()).getDataType();
        if (type == FormulaTime.class) {
            String str = getSqlHooks(context).sqlCastNull(args[0], FormulaTime.class);
            return new SQLPair(String.format(getSqlHooks(context).sqlGetTimeInSeconds(), str), guards[0]);
        }
        String str = getSqlHooks(context).sqlCastNull(args[0], Date.class);
        return new SQLPair(String.format(getSqlHooks(context).sqlGetEpoch(type), str), guards[0]);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult(jsToDec(context, "Math.trunc(("+args[0].js+").getTime()/1000)"), args);
    }


    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties)
            throws FormulaException {
        if (node.getNumberOfChildren() != 1) { throw new WrongNumberOfArgumentsException(node.getText(), 1, node); }

        FormulaAST firstNode = (FormulaAST)node.getFirstChild();
        Type inputDataType = firstNode.getDataType();

        if (inputDataType != FormulaDateTime.class && inputDataType != Date.class && inputDataType != FormulaTime.class
                && inputDataType != RuntimeType.class) { throw new IllegalArgumentTypeException(node.getText()); }

        return BigDecimal.class;
    }


    static class FunctionUnixTimestampCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;

        public FunctionUnixTimestampCommand(FormulaCommandInfo formulaCommandInfo) {
            super(formulaCommandInfo);
        }

        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
            Object obj = stack.pop();
            if (obj instanceof FormulaTime) {
                stack.push(new BigDecimal(((FormulaTime) obj).getTimeInSeconds()));
            } else {
                Date d = checkDateType(obj);
                if (d == null)
                    stack.push(null);
                else {
                    Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.GMT);
                    c.setTime(d);
                    stack.push(new BigDecimal(c.getTimeInMillis()/1000));
                }
            }
         }
    }
}
