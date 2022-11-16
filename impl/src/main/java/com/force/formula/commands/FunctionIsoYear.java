package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
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
import com.force.formula.impl.IllegalArgumentTypeException;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.sql.SQLPair;

/**
 * Return the ISO 8601 Week Number
 * WEEKNUMBER(Date/Time/Datetime) returns a Number
 *
 * @author stamm
 * @since 0.1.2
 */
@AllowedContext(section=SelectorSection.DATE_TIME, isOffline=true)
public class FunctionIsoYear extends FormulaCommandInfoImpl implements FormulaCommandValidator  {
    public FunctionIsoYear() {
        super("ISOYEAR", BigDecimal.class, new Class[] { Date.class});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionIsoWeekNumberCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
    	// Pass in the date twice because sqlserver uses is
        return new SQLPair(String.format(getSqlHooks(context).sqlGetIsoYear(), args[0], args[0]), guards[0]);
    }
  
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        String js =  jsToDec(context, context.getJsEngMod() + ".isoyear(" + args[0] + ")");
        return JsValue.generate(js, args, true, args[0]);
    }
    
    
    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties)
            throws FormulaException {
        if (node.getNumberOfChildren() != 1) { throw new WrongNumberOfArgumentsException(node.getText(), 1, node); }

        FormulaAST firstNode = (FormulaAST)node.getFirstChild();
        Type inputDataType = firstNode.getDataType();

        if (inputDataType != FormulaDateTime.class && inputDataType != Date.class
                && inputDataType != RuntimeType.class) { throw new IllegalArgumentTypeException(node.getText()); }

        return BigDecimal.class;
    }

    
    static class FunctionIsoWeekNumberCommand extends AbstractFormulaCommand {
	    private static final long serialVersionUID = 1L;
	
		public FunctionIsoWeekNumberCommand(FormulaCommandInfo formulaCommandInfo) {
	        super(formulaCommandInfo);
	    }
	
	    @Override
	    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
	        Date d = checkDateType(stack.pop());
	        if (d == null)
	            stack.push(null);
	        else {
	            stack.push(new BigDecimal(LocalDateTime.ofInstant(d.toInstant(), ZoneId.of("GMT")).get(IsoFields.WEEK_BASED_YEAR)));
	        }
	     }
    }
}
