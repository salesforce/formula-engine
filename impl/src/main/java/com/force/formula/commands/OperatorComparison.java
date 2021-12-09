package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.google.common.base.Objects;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section = SelectorSection.MATH, isOffline = true)
public class OperatorComparison extends FormulaCommandInfoImpl implements FormulaCommandValidator {
    public OperatorComparison(String token) {
        super(token);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new OperatorComparisonFormulaCommand(this, getName());
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
    	String lhs = args[0];
    	String rhs = args[1];
        String sql = "(" + lhs + getName() + rhs + ")";
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        if (node.getNumberOfChildren() != 2) {
            throw new WrongNumberOfArgumentsException(node.getText(), 2, node);
        }

        FormulaAST lhsNode = (FormulaAST)node.getFirstChild();
        FormulaAST rhsNode = (FormulaAST)lhsNode.getNextSibling();

        Type lhs = lhsNode.getDataType();
        Type rhs = rhsNode.getDataType();

        if (lhs == FormulaGeolocation.class || rhs == FormulaGeolocation.class)
            throw new IllegalArgumentTypeException(node.getText());

        String operator = node.getText();
        if ((lhs != ConstantNull.class) && (rhs != ConstantNull.class)
                && (lhs != RuntimeType.class) && (rhs != RuntimeType.class)
                && (!Objects.equal(lhs,rhs)))
            throw new WrongArgumentTypeException(operator, new Type[] { lhs }, rhsNode);

        checkType(operator, lhsNode);
        checkType(operator, rhsNode);

        if (lhs == RuntimeType.class || rhs == RuntimeType.class) {
            return RuntimeType.class;
        }

        return Boolean.class;
    }

    private void checkType(String operator, FormulaAST node) throws FormulaException {
        Type clazz = node.getDataType();

        if (clazz == ConstantNull.class  || clazz == RuntimeType.class)
            return;

        // Only support comparison operation for Text, Numeric, Date, and DateTime data types
        if ((clazz != BigDecimal.class) && (clazz != Date.class) && (clazz != FormulaTime.class) && (clazz != FormulaDateTime.class)
        		&& !FormulaTypeUtils.isTypeText(clazz))
            throw new WrongArgumentTypeException(operator, new Class[] { BigDecimal.class, Date.class,
                FormulaDateTime.class }, node);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        // null means false in SQL, 5 < null is false and 5 > null is false.
        Type clazz = ((FormulaAST)node.getFirstChild()).getDataType();
        // Comparisons are particularly tricky in JS because null<1 = true and null>1 = new Date().  Sigh...
        String argGuard = JsValue.makeArgumentGuard(args);
        if (clazz == BigDecimal.class && context.useHighPrecisionJs()) {
            if (argGuard != null) {
                return JsValue.generate("("+argGuard + "?(" + args[0] + ".comparedTo(" + args[1] + ") "+ getName() + " 0):null)", new JsValue[0], args[0].couldBeNull || args[1].couldBeNull);
            } else {
                return JsValue.forNonNullResult("(" + args[0] + ".comparedTo(" + args[1] + ") "+ getName() + " 0)", args);
            }
        }
        if (argGuard != null) {
        	boolean hasNullResult = true;
        	if (FormulaTypeUtils.isTypeText(clazz)) {  // Text comparisons shouldn't be null
        		hasNullResult = false;
        	}
            return JsValue.generate("("+argGuard + "?(" + args[0] + getName() + args[1] + "):"+(hasNullResult?"null":"false")+")", new JsValue[0], hasNullResult && (args[0].couldBeNull || args[1].couldBeNull));
        } else {
            return JsValue.forNonNullResult("(" + args[0] + getName() + args[1] + ")", args);
        }
    }
}

class OperatorComparisonFormulaCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public OperatorComparisonFormulaCommand(FormulaCommandInfo info, String token) {
        super(info);
        this.token = token;
    }


    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        Comparable<Object> rhs = checkComparableType(stack.pop());
        Comparable<Object> lhs = checkComparableType(stack.pop());

        if (FormulaCommandInfoImpl.isNull(lhs) || FormulaCommandInfoImpl.isNull(rhs)) {
            if (shouldReturnBoolResult(context) && !FormulaEngine.getHooks().isFormulaContainerCompiling()) {
                // executing context requires boolean result and for the comparison operator null values
                // cannot be evaluated
                // vf compile may return null as a proxy value during compilation instead of
                // actually invoking an expression's method and/or field reference
                throw new InvalidNumericValueException("null", token);
            } else {
                stack.push(null); // Follow Oracle three-value semantics
            }
        } else {
            boolean value;
            try {
                if (">".equals(token)) {
                    value = (lhs.compareTo(rhs) > 0);
                } else if ("<".equals(token)) {
                    value = (lhs.compareTo(rhs) < 0);
                } else if (">=".equals(token)) {
                    value = (lhs.compareTo(rhs) >= 0);
                } else if ("<=".equals(token)) {
                    value = (lhs.compareTo(rhs) <= 0);
                } else {
                    throw new UnsupportedOperationException(token);
                }
            } catch (ClassCastException e) {
                throw new FormulaRuntimeTypeException(token);  // NOPMD
            }

            stack.push(Boolean.valueOf(value));
        }
    }

    private final String token;
}
