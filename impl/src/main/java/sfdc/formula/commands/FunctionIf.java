package sfdc.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Deque;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.*;
import sfdc.formula.parser.gen.SfdcFormulaTokenTypes;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section = SelectorSection.LOGICAL, isOffline = true)
public class FunctionIf extends FormulaCommandInfoImpl implements FormulaCommandValidator, FormulaCommandOptimizer {

    public static final String CAPITALIZED_NAME = "IF";

    public FunctionIf() {
        super(CAPITALIZED_NAME);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new OperatorIfFormulaCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql;
        String guard;
        if ("NULL".equals(args[0])) { // Treat NULL as false
            sql = args[2];
            guard = guards[2];
        } else {
            FormulaAST guardNode = (FormulaAST)node.getFirstChild();
            FormulaAST thenNode = (FormulaAST)guardNode.getNextSibling();
            FormulaAST elseNode = (FormulaAST)thenNode.getNextSibling();

            Type resultDataType = node.getDataType();

            String thenArg = wrap(args[1], thenNode, resultDataType, context);
            String elseArg = wrap(args[2], elseNode, resultDataType, context);

            // Validate guarantees that types match or one is null.
            sql = "CASE WHEN " + args[0] + " THEN " + thenArg + " ELSE "
                    + elseArg + " END";

            // If the return type is boolean, the then and else parts will have been generated as numeric 0 or 1.
            // Convert to an expression....
            if (resultDataType == Boolean.class) {
                sql = "(" + sql + ") = 1";
            }

            // Conditionally evaluate guards on the basis of args[0]
            guard = guards[0];
            if (guards[1] != null) {
                String extra = "((" + args[0] + ") AND (" + guards[1] + "))";
                if (guard != null)
                    guard = guard + " OR " + extra;
                else
                    guard = extra;
            }
            if (guards[2] != null) {
                String extra = "((CASE WHEN " + args[0] + " THEN 1 ELSE 0 END = 0" + ") AND (" + guards[2] + "))";
                if (guard != null)
                    guard = guard + " OR " + extra;
                else
                    guard = extra;
            }
        }
        return new SQLPair(sql, guard);
    }

    // Prepare the arg for inclusion as then or else part of expression.  This deals with possible null expressions
    // and also, if we have a boolean, converts to numeric 1 or 0 to pass out of the generated expression
    private String wrap(String expression, FormulaAST node, Type resultDataType, FormulaContext context) {
        Type argType = node.getDataType();
        if (argType == ConstantNull.class) {
            if ((resultDataType == FormulaDateTime.class) || (resultDataType == Date.class)) {
                return "TO_DATE(NULL)";
            } else if (resultDataType == BigDecimal.class) {
                return "TO_NUMBER(NULL)";
            } else if (resultDataType == Boolean.class) {
                return "0";     // for compare false
            } else if (resultDataType == ConstantNull.class) {
                // This can happen only if both then and else parts are constant null.
                return expression;   // which will be NULL
            } else {
                return "NULL";
            }
        }

        if ((argType == FormulaDateTime.class) || (argType == Date.class) && node.canBeNull()) {
            return String.format("NVL(%s,TO_DATE(NULL))", expression);
        } else if (argType == BigDecimal.class && node.canBeNull() && node.getType() != SfdcFormulaTokenTypes.IDENT) {
            return String.format("NVL(%s,TO_NUMBER(NULL))", expression);
        } else if (argType == Boolean.class) {
            // convert boolean back to number for comparison
            int nodeType = node.getType();
            if (nodeType == SfdcFormulaTokenTypes.TRUE)
                return "1";
            else if (nodeType == SfdcFormulaTokenTypes.FALSE)
                return "0";
            else
                return String.format("CASE WHEN %s THEN 1 ELSE 0 END", expression);
        }
        else if (FormulaCommandInfoImpl.shouldGeneratePsql(context) && argType == String.class &&  "''".equals(expression)) {
             return "NULL"; // Postgres related: W-5552193
        }
        else {
            return expression;
        }
    }

    @Override
    public FormulaAST optimize(FormulaAST ast, FormulaContext context) throws FormulaException {
        Type dataType = ast.getDataType();
        FormulaAST first = (FormulaAST)ast.getFirstChild();
        if (first.getType() == SfdcFormulaTokenTypes.TRUE) {
            ast = ast.replace((FormulaAST)first.getNextSibling());
        } else if (first.getType() == SfdcFormulaTokenTypes.FALSE) {
            ast = ast.replace((FormulaAST)first.getNextSibling().getNextSibling());
        }

        // slight hack to make sure the datatype of the returned null is correct
        if (ast.getDataType() == ConstantNull.class)
            ast.setDataType(dataType);

        optimizeNestedIfs(ast);
        return ast;
    }

    /**
     * This method is called by {@link FunctionIf#optimizeNestedIfs} which is called
     * by {@link BaseFormulaInfoImpl#optimizeParseTree} in a **depth-first** order. This means that the children
     * nodes of this IF node are already visited.
     *
     * If we look our else-clause child, we can find out immediately whether we are in a nested-IFs situation.
     * If our else-child is another IF node, it's clear that we can optimize the current IF node by converting
     * it to IFS function.
     * If our else-child is an IFS node, it's clear that some optimization has already happened, and the current IF
     * node is the outer-most IF function that has been visited **so far**.
     *
     * The goal is to keep building the IFS function from inner IF function to outer IF function, until we reach
     * the very last IF function in the nested-IFs pattern.
     */
    protected static void optimizeNestedIfs(FormulaAST ast) {
        if(!FormulaAST.isFunctionNode(ast, FunctionIf.CAPITALIZED_NAME) ||
                !FormulaValidationHooks.get().parseHook_shouldOptimizeNestedIfs()) {
            return;
        }

        FormulaAST elseClause = (FormulaAST) ast.getFirstChild() //condition
                .getNextSibling() //if-clause
                .getNextSibling(); //else-clause
        if(!FormulaAST.isFunctionNode(elseClause, FunctionIf.CAPITALIZED_NAME) &&
                !FormulaAST.isFunctionNode(elseClause, FunctionIfs.CAPITALIZED_NAME)) {
            return;
        }

        //change ast node to IFS function
        ast.setText(FunctionIfs.CAPITALIZED_NAME);

        //remove else-clause from the tree
        ast.getFirstChild() //condition
                .getNextSibling() //if-clause
                .setNextSibling(null); //else-clause
        elseClause.setParent(null);

        //end of ast's children linked list
        FormulaAST tail = (FormulaAST) ast.getFirstChild() //condition
                .getNextSibling(); //if-clause

        //move over else-clause's children to ast
        FormulaAST elseClauseChild = (FormulaAST) elseClause.getFirstChild();
        while(elseClauseChild != null) {
            tail.setNextSibling(elseClauseChild); //also updates elseClauseChild's parent to ast
            tail = (FormulaAST) tail.getNextSibling();
            elseClauseChild = (FormulaAST) elseClauseChild.getNextSibling();
        }

        //elseClause should be automatically garbage collected, but just in case:
        elseClause.setFirstChild(null);
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        if (node.getNumberOfChildren() != 3) {
            throw new WrongNumberOfArgumentsException(node.getText(), 3, node);
        }

        FormulaAST guardNode = (FormulaAST)node.getFirstChild();
        Type guardDataType = guardNode.getDataType();
        if (guardDataType != Boolean.class && guardDataType != RuntimeType.class)
            throw new WrongArgumentTypeException(node.getText(), new Type[] { Boolean.class }, guardNode);

        FormulaAST thenNode = (FormulaAST)guardNode.getNextSibling();
        FormulaAST elseNode = (FormulaAST)thenNode.getNextSibling();
        Type thenType = thenNode.getDataType();
        Type elseType = elseNode.getDataType();

        if (!FormulaTypeUtils.hasCommonSuperType(thenType, elseType) && shouldValidateTypeEquality()) {
            throw new WrongArgumentTypeException(node.getText(), new Type[] { thenType }, elseNode);
        }

        // "Inherit" the type of the args.
        return FormulaTypeUtils.getCommonSuperType(thenType, elseType);
    }

    // determines if then and else types should be validated for equality
    protected boolean shouldValidateTypeEquality() {
    	return FormulaEngine.getHooks().functionHook_validateIfFunctionTypeEquality();
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        // Swallow all the guards in an if statement.  It *must* return the false value if the guards are wrong.  
        String arg0 = args[0].guard!=null? args[0].guard + " && (" + args[0].js + ")" : args[0].js;
        // Return null if guards fails for the results
        String arg1 = args[1].guard!=null? "(" + args[1].guard + "?(" + args[1].js + "):null)" : args[1].js;
        String arg2 = args[2].guard!=null? "(" + args[2].guard + "?(" + args[2].js + "):null)" : args[2].js;
        return new JsValue("("+arg0+"?" + arg1 + ":" + arg2 + ")", null, args[1].couldBeNull || args[2].couldBeNull);
    }
}

class OperatorIfFormulaCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public OperatorIfFormulaCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        Thunk elseVal = (Thunk)stack.pop();
        Thunk thenVal = (Thunk)stack.pop();
        Boolean guard = checkBooleanType(stack.pop());
        if ((guard == null) || !guard.booleanValue()) // Treat NULL as false
            elseVal.executeReally(context, stack);
        else
            thenVal.executeReally(context, stack);
    }
}