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
import com.force.formula.FormulaException;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.FormulaTime;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaTypeUtils;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.impl.Thunk;
import com.force.formula.impl.WrongArgumentTypeException;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.sql.SQLPair;

/**
 * IFERROR(value, uponError)
 * 
 * Evaluates the conditions for value, and will return the value if it is not an error, but would return "uponError" value 
 * otherwise.  
 *
 * This doesn't work very will in javascript because the engine doesn't use guards, but instead relies upon javascript's
 * extremely lenient and confusing parsing.
 *
 * @author stamm
 * @since 0.2.4
 */
@AllowedContext(section=SelectorSection.LOGICAL,isOffline=true)
public class FunctionIfError extends FormulaCommandInfoImpl implements FormulaCommandValidator {
    public FunctionIfError() {
        this("IFERROR");
    }

    protected FunctionIfError(String name) {
        super(name);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionIfErrorCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        if (guards[0] == null) {
            return new SQLPair(args[0], guards[0]);
        } else {
            return new SQLPair("CASE WHEN " + guards[0] + " THEN " + args[1] + " ELSE " + args[0] + " END", guards[1]);
        }

    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        if (node.getNumberOfChildren() != 2) {
            throw new WrongNumberOfArgumentsException(node.getText(), 2, node);
        }

        FormulaAST arg = (FormulaAST)node.getFirstChild();
        Type argDataType = arg.getDataType();
        FormulaAST ifError = (FormulaAST)arg.getNextSibling();
        Type ifErrorType = ifError.getDataType();
        
        if ((argDataType != Object.class) && (ifErrorType != Object.class)
                && (!FormulaTypeUtils.hasCommonSuperType(argDataType,ifErrorType)))
            throw new WrongArgumentTypeException(node.getText(), new Type[] { argDataType }, ifError);

        if (argDataType == RuntimeType.class || ifErrorType == RuntimeType.class) {
            return RuntimeType.class;
        }
        return argDataType;
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        FormulaAST arg = (FormulaAST)node.getFirstChild();
        Type argDataType = arg.getDataType();
        String guard = args[0].guard != null ? "!(" + args[0].guard + ") || " : "";
        if (argDataType == BigDecimal.class) {
            return JsValue.generate("(" + guard + "isNaN("+args[0]+"))?" + args[1] + ":" + args[0], null, args[0].couldBeNull || args[1].couldBeNull);
        } else if (argDataType == Date.class || argDataType == FormulaDateTime.class || argDataType == FormulaTime.class) {
            return JsValue.generate("(" + guard+ "Object.prototype.toString.call("+args[0]+") !== '[object Date]') || isNaN("+args[0]+".getTime())?" + args[1] + ":" + args[0], null, args[0].couldBeNull || args[1].couldBeNull);
        }
        // TODO: When you put TEXT(...), javascript doesn't understand that because it uses very loose guards.
        if (args[0].guard == null) {
            return args[0];
        }
        return JsValue.generate("!(" + args[0].guard + ")?" + args[1] + ":" + args[0], null, args[0].couldBeNull || args[1].couldBeNull);
    }

    static class FunctionIfErrorCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;
    
        public FunctionIfErrorCommand(FormulaCommandInfo info) {
            super(info);
        }
    
        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws FormulaException {
            Thunk elseVal = (Thunk)stack.pop();
            Thunk thenVal = (Thunk)stack.pop();
            
            try {
                thenVal.executeReally(context, stack);
            } catch (RuntimeException | FormulaException ex) {
                // Using RuntimeException here instead of FormulaEvaluationException | ArithmeticException | IllegalArgumentException
                // because Date exceptions can be turned into any RuntimeException by FormuleEngineHooks.handleFormulaDateException
                elseVal.executeReally(context, stack);
            }
        }
    }
}