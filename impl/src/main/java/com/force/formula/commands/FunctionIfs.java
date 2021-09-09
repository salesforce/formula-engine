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
 * TODO(ifs):
 *  - implement the remaining methods
 *
 * @author ashanjani
 * @since 234
 */
@AllowedContext(section = SelectorSection.LOGICAL, access = "beta", isJavascript = false)
public class FunctionIfs extends FormulaCommandInfoImpl implements FormulaCommandValidator {
    public static final String CAPITALIZED_NAME = "IFS";

    public FunctionIfs() {
        super(CAPITALIZED_NAME);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new OperatorIfsFormulaCommand(this);
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
            throw new WrongNumberOfArgumentsException(node.getText(), 3, node); //TODO(ifs): should fix the expected value because the expected value could be 3, 5, 7, ...

        }

        if(node.getDataType() != null) {
            return node.getDataType(); //node is a converted IF node, and the data type has already been resolved.
        }
        else {
            //TODO(ifs): in this scenario, the IFS node is not from a converted IF node, and we need to validate children's types
            // and also resolve the return data type of the IFS node.
            // Resolve data type similar to FunctionIf.validate(); dedup code if possible.
            // This implementation is temporary to pass unit tests.
            return ((FormulaAST) node.getFirstChild().getNextSibling()).getDataType();
        }
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        throw new UnsupportedOperationException();
    }
}

class OperatorIfsFormulaCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

    public OperatorIfsFormulaCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        //TODO(ifs):
    }
}