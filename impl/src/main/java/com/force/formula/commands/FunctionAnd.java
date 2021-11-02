package com.force.formula.commands;

import java.util.Arrays;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaTextUtil;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.LOGICAL, isOffline = true)
public class FunctionAnd extends FormulaCommandInfoImpl implements FormulaCommandOptimizer {

    public FunctionAnd() {
        super("AND", Boolean.class, new Class[] {});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new OperatorAndCommand(this, node.getNumberOfChildren());
    }

    @Override
    public Class<?>[] getArgumentTypes(FormulaAST node, FormulaContext context) {
        Class<?>[] argumentTypes = new Class[node.getNumberOfChildren()];
        Arrays.fill(argumentTypes, Boolean.class);
        return argumentTypes;
    }

    @Override
    public FormulaAST optimize(FormulaAST ast, FormulaContext context) throws FormulaException {
        FormulaAST child = (FormulaAST)ast.getFirstChild();
        while(child != null && child.getType() == FormulaTokenTypes.TRUE) {
            child = (FormulaAST)child.getNextSibling();
        }
        if (child != null && child.getType() == FormulaTokenTypes.FALSE) {
            ast = ast.replace(child);
        }
        return ast;
    }
    
    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        // Build SQL
        StringBuilder sql = new StringBuilder(64);
        sql.append("(").append(FunctionOr.fixBooleanNull(args[0]));
        for (int i = 1; i < args.length; i++)
            sql.append(" AND ").append(FunctionOr.fixBooleanNull(args[i]));
        sql.append(")");
        // Build guard
        StringBuilder guard = null;
        StringBuilder condition = null;
        StringBuilder close = new StringBuilder(64);
        /*
         * See FunctionOr.getSQL(...) for explanation for two passes.
         */
        for (int pass=1;pass<=2; pass++) {
            for (int i = 0; i < args.length; i++) {
                if (guards[i] != null && pass==1) {
                    if ((guard == null) && (condition == null)) {
                        // guards[i] != null, guard == null, condition == null
                        guard = new StringBuilder(guards[i]);
                    } else if (condition == null) {
                        // guards[i] != null, guard != null, condition == null
                        guard.append("OR ").append(guards[i]);
                    } else if (guard == null) {
                        // guards[i] != null, guard == null, condition != null
                        guard = new StringBuilder(64);
                        guard.append("(").append(condition).append(" AND (").append(guards[i]);
                        close.append("))");
                    } else {
                        // guards[i] != null, guard != null, condition != null
                        guard.append(" OR (").append(condition).append(" AND (").append(guards[i]);
                        close.append("))");
                    }
                    condition = new StringBuilder(FunctionOr.fixBooleanNull(args[i]));
                }
                if(guards[i] == null && pass==2){
                    // guards[i] == null
                    if (condition == null) condition = new StringBuilder(64);
                    else condition.append(" AND ");
                    condition.append(FunctionOr.fixBooleanNull(args[i]));
                }
            }
        }
        if (guard != null)
            guard.append(close);
        return new SQLPair(sql.toString(), guard != null ? guard.toString() : null);
    }
    
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult(FormulaTextUtil.collectionToStringEnclosed(Arrays.asList(args), "&&", "(", ")"), args);
    }
}

class OperatorAndCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	OperatorAndCommand(FormulaCommandInfo info, int numArgs) {
        super(info);
        this.numArgs = numArgs;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws FormulaException {
        Thunk[] args = new Thunk[numArgs];
        for (int i = 0; i < numArgs; i++)
            args[numArgs - i - 1] = (Thunk)stack.pop();
        for (int i = 0; i < numArgs; i++) {
            args[i].executeReally(context, stack);
            Boolean result = checkBooleanType(stack.pop());
            if (result == null || !result.booleanValue()) {
                stack.push(Boolean.FALSE);
                return;
            }
        }
        stack.push(Boolean.TRUE);
    }

    private final int numArgs;
}
