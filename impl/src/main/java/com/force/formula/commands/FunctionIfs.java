package com.force.formula.commands;

import java.lang.reflect.Type;
import java.util.Deque;
import java.util.StringJoiner;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * IFS(condition, result, [condition2, condition2, ...], else)
 * Supports shortcircuit behavior of multiple IFS in a single statement 
 * @author ashanjani
 * @since 234
 */
@AllowedContext(section = SelectorSection.LOGICAL, access = "beta")
public class FunctionIfs extends FormulaCommandInfoImpl implements FormulaCommandValidator {
    public static final String CAPITALIZED_NAME = "IFS";

    public FunctionIfs() {
        super(CAPITALIZED_NAME);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new OperatorIfsFormulaCommand(this, node.getNumberOfChildren());
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        Type resultDataType = node.getDataType();

        StringJoiner sj = new StringJoiner(" ", "CASE ", " END");

        boolean hasNonNullCondition = false;
        FormulaAST currentConditionNode = (FormulaAST) node.getFirstChild();
        for(int i = 0; i < args.length - 1; i += 2) {
            if("NULL".equals(args[i])) { //treat conditions that are NULL as false
                continue;
            }

            FormulaAST currentThenNode = (FormulaAST) currentConditionNode.getNextSibling();
            String thenArg = FunctionIf.wrap(args[i+1], currentThenNode, resultDataType, context);
            sj.add("WHEN").add(args[i]).add("THEN").add(thenArg);
            hasNonNullCondition = true;

            currentConditionNode = (FormulaAST) currentThenNode.getNextSibling();
        }

        if(!hasNonNullCondition) { //only the else-clause is relevant
            return new SQLPair(args[args.length - 1], guards[guards.length - 1]);
        }

        //once the above for-loop is over, currentConditionNode points to the last node (i.e. else node)
        String elseArg = FunctionIf.wrap(args[args.length - 1], currentConditionNode, resultDataType, context);
        sj.add("ELSE").add(elseArg);

        // If the return type is boolean, the then and else parts will have been generated as numeric 0 or 1.
        // Convert to an expression....SqlGeneratorOutput
        String sql = resultDataType == Boolean.class ? "(" + sj.toString() + ") = 1" : sj.toString();

        /**
         * Conditionally evaluate guards on the basis of condition clauses.
         *
         * The final guard will look exactly like it would have if we kept the IF functions nested. We start
         * with the inner-most IF function, generate its guard, and move up to the next IF function to generate
         * the next level of guards, incorporating the inner IF function's guard in it. If we were to start with
         * the outer-most IF function moving inwards, we would have to append the negative of all previous condition
         * clauses into each level.
         */
        String guard = guards[guards.length - 1];
        for(int i = guards.length - 3; i >= 0; i -= 2) {
            guard = FunctionIf.getGuard(args[i], guards[i], guards[i+1], guard);
        }

        return new SQLPair(sql, guard);
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        int numberOfParameters = node.getNumberOfChildren();
        if (numberOfParameters < 3 || numberOfParameters % 2 != 1) {
            int expectedNumberOfChildren = Math.max(3, 1+numberOfParameters);
            throw new WrongNumberOfArgumentsException(node.getText(), expectedNumberOfChildren, node);
        }

        if(node.getDataType() != null) {
            return node.getDataType(); //node is a converted IF node, and the data type has already been resolved.
        }
        else {
        	Type resultType = null; // The expected result type.  First time through is null
        	FormulaAST currentNode = (FormulaAST) node.getFirstChild();
        	// GO thro
        	for (int i = 0; i < numberOfParameters-1; i+=2) {
	            Type guardDataType = currentNode.getDataType();
	            if (guardDataType != Boolean.class && guardDataType != RuntimeType.class)
	                throw new WrongArgumentTypeException(node.getText(), new Type[] { Boolean.class }, currentNode);
	            currentNode = (FormulaAST)currentNode.getNextSibling();
	            resultType = getResultType(node, currentNode, resultType, true);
	            currentNode = (FormulaAST)currentNode.getNextSibling();
        	}
        	return getResultType(node, currentNode, resultType, true);
        }
    }
    
    /**
     * Compare the current result type to the one in the provided node and return the common supertype.
     * @param node the node of the function (ifs),  
     * @param currentNode the node of the result to return
     * @param currentResultType the previously evaluated result type
     * @param checkIfTypeEquality check functionHook_validateIfFunctionTypeEquality to allow mismatched return types
     * @return the common type between the currentResultType and the type of the currentNode
     * @throws WrongArgumentTypeException if the types don't match and checkIfTypeEquality
     */
    static Type getResultType(FormulaAST node, FormulaAST currentNode, Type currentResultType, boolean checkIfTypeEquality) throws WrongArgumentTypeException {
    	Type resultType = currentNode.getDataType();
    	if (currentResultType == null) {
    		return resultType;
    	}
        Type commonResultType = FormulaTypeUtils.getCommonSuperType(currentResultType, resultType);
        if (commonResultType == null) {
        	if (!checkIfTypeEquality || shouldValidateTypeEquality()) {
        		throw new WrongArgumentTypeException(node.getText(), new Type[] { currentResultType }, currentNode);
        	} else {
        		return RuntimeType.class;
        	}
        } else {
        	return commonResultType;
        }
    }
    
    // determines if then and else types should be validated for equality
    protected static boolean shouldValidateTypeEquality() {
    	return FormulaEngine.getHooks().functionHook_validateIfFunctionTypeEquality();
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        FormulaAST valueNode = (FormulaAST)node.getFirstChild();
        StringBuilder js = new StringBuilder(256);
        
        boolean couldBeNull = false;
        FormulaAST whenNode;
        for (int i = 0; i < args.length - 1; i += 2) {
            whenNode = (FormulaAST)valueNode.getNextSibling();
            js.append("("+args[i] + "?");
            valueNode = (FormulaAST)whenNode.getNextSibling();
            js.append("(").append(args[i+1].js).append("):");
            couldBeNull |= args[i+1].couldBeNull;
        }
        js.append(args[args.length - 1]).append(")");

        couldBeNull |= args[args.length - 1].couldBeNull; 
        return JsValue.generate(js.toString(), args, couldBeNull); 
    }
}

class OperatorIfsFormulaCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;
    private final int numArgs;

    public OperatorIfsFormulaCommand(FormulaCommandInfo formulaCommandInfo, int numArgs) {
        super(formulaCommandInfo);
        this.numArgs = numArgs;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws FormulaException {
        Thunk elseVal = (Thunk)stack.pop();
        // Evaluate in short-circuit order so pull all the args off the stack first into a Deque
        Deque<Object> ifs = new FormulaStack(numArgs);
        for (int i = 0 ; i < numArgs-1; i++) {
        	ifs.addFirst(stack.pop());
        }
        
        for (int i = 0; i < (numArgs / 2); i++) {
        	Object comparison = ifs.pop();
        	// The first comparison is evaluated immediately, but the rest will be thunks
            if (comparison instanceof Thunk) {
            	((Thunk)comparison).executeReally(context, stack);
            	comparison = stack.pop();
            }
            Boolean guard = checkBooleanType(comparison);
            Thunk thunk = (Thunk) ifs.pop();
            if ((guard != null) && guard.booleanValue()) { // Treat NULL as false
            	thunk.executeReally(context, stack);
            	return;
            }
        }
        // If we're here, nothing evaluated.
        elseVal.executeReally(context, stack);
    }
}