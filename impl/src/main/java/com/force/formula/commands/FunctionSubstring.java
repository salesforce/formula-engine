package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaException;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.sql.SQLPair;

/**
 * SUBSTRING(string, position[, length])
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.TEXT,isOffline=true)
public class FunctionSubstring extends FormulaCommandInfoImpl {
    public FunctionSubstring() {
        super("SUBSTR", String.class, new Class[] { String.class, BigDecimal.class, BigDecimal.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionSubstringCommand(this, node.getNumberOfChildren() == 3);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
    	FormulaSqlHooks hooks = getSqlHooks(context);
    	String sql;
        if (args.length == 3) {
            sql = hooks.sqlSubstrWithNegStart(args[0], args[1], args[2]);
        } else {
            sql = hooks.sqlSubstrWithNegStart(args[0], args[1]);
    	} 
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        
        String sql;
        if (args.length > 2) {
            sql = args[0]+".substr(Math.max("+jsToNum(context,args[1].js)+"+("+jsToNum(context,args[1].js)+">=0 ? -1 : (" + args[0] + ").length),0),Math.max("+args[2]+",0))";
        } else {
            sql = args[0]+".substr(Math.max("+jsToNum(context,args[1].js)+"+("+jsToNum(context, args[1].js)+">=0 ? -1 : (" + args[0] + ").length),0))";
        }
        return JsValue.forNonNullResult(sql, args);
    }
    
    @Override
    public Class<?>[] getArgumentTypes(FormulaAST node, FormulaContext context) {
        return (node.getNumberOfChildren() == 3) ? new Class[] { String.class, BigDecimal.class, BigDecimal.class }
            : new Class[] { String.class, BigDecimal.class };
    }

    static class FunctionSubstringCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;
        private final boolean hasLength;

    	public FunctionSubstringCommand(FormulaCommandInfo formulaCommandInfo, boolean hasLength) {
            super(formulaCommandInfo);
            this.hasLength = hasLength;
        }
    
    	private String safeSubstring(String str, int start, int end) {
    	    int realStart = Math.min(Math.max(start, 0), str.length());
            int realEnd = Math.min(Math.max(end, 0), str.length());
    	    return str.substring(realStart, realEnd);
    	}
    	
        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
            BigDecimal count = (hasLength) ? checkNumberType(stack.pop()) : null;
            BigDecimal start = checkNumberType(stack.pop());
            String target = checkStringType(stack.pop());
            if ((start == null) || (hasLength && count == null)) {
                stack.push(null);
                return;
            }
            if (hasLength && count.compareTo(BigDecimal.ZERO) < 0) {
                count = BigDecimal.ZERO;
            }
            
            if ((target == null) || (target.equals("")) || (hasLength && count.compareTo(BigDecimal.ZERO) == 0)
                    || (start.compareTo(new BigDecimal(target.length())) > 0) // If start > length or start < -length..
                    || (start.compareTo(new BigDecimal(-target.length())) < 0)) {
                stack.push(null);
            } else if (start.compareTo(BigDecimal.ZERO) < 0) {
                // Negative Start
                if (hasLength) {
                    // negative start.   Go from end + start to end + length - start
                    stack.push(safeSubstring(target, target.length() + start.intValue(), target.length() + start.intValue() + count.intValue()));
                } else {
                    stack.push(safeSubstring(target, target.length() + start.intValue(), target.length()));
                }
                
            } else {
                if (start.compareTo(BigDecimal.ONE) < 0) {
                    start = BigDecimal.ONE;
                }
                // Positive Start
                if (hasLength) {
                    // Positive length
                    stack.push(safeSubstring(target, start.intValue() - 1, start.intValue() - 1 + count.intValue()));
                } else {
                    stack.push(safeSubstring(target, start.intValue() - 1, target.length()));
                }
            }
        }
    }
}
