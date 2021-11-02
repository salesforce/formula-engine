package com.force.formula.commands;

import java.util.Deque;
import java.util.List;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.SQLPair;
import com.force.i18n.commons.text.TextUtil;
import com.google.common.base.Joiner;

/**
 * Extend this class with the specific implementation of
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section = SelectorSection.TEXT, isOffline = true)
public class FunctionIsPickVal extends FormulaCommandInfoImpl
        implements FormulaCommandValidator, FormulaCommandPrefetcher {
    public FunctionIsPickVal() {
        super("ISPICKVAL");
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        return new FunctionIsPicklistValueCommand(this, getTargetPrivate(node, context, false));
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards,
            TableAliasRegistry registry) throws InvalidFieldReferenceException, UnsupportedTypeException {
        List<String> target = getTargetPrivate(node, context, true);
        if ((args[0] == null) || (target == null)) {
            return new SQLPair("(1=0)", null);
        } else {
            return new SQLPair(compareBulk(args[0], target), null);
        }
    }

    // Returns the DB values, (the String representation(s) of number(s), of the
    // enum value specified in the second argument to the function
    private List<String> getTargetPrivate(FormulaAST node, FormulaContext context, boolean forSql)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaAST first = (FormulaAST) node.getFirstChild();
        String fieldValue = ConstantString.getStringValue((FormulaAST) first.getNextSibling(), true);
        FormulaFieldInfo formulaFieldInfo = getFormulaFieldInfo(first, context);
        return getTarget(formulaFieldInfo, fieldValue, context, forSql, false);
    }

    public static FormulaFieldInfo getFormulaFieldInfo(FormulaAST targetNode, FormulaContext context)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        String name = targetNode.getText();
        if ((targetNode.getType() == FormulaTokenTypes.FUNCTION_CALL) && "PRIORVALUE".equalsIgnoreCase(name)) {
            // Use the first arg to PriorValue() as the actual target
            name = targetNode.getFirstChild().getText();
        }
        return context.lookup(name);
    }

    @Deprecated
    static List<String> getTarget(FormulaFieldInfo formulaFieldInfo, String fieldValue, FormulaContext context,
            boolean forSql, boolean forJs) throws InvalidFieldReferenceException, UnsupportedTypeException {
        return FormulaEngine.getHooks().getUnderlyingValuesForPicklist(formulaFieldInfo, fieldValue, context, forSql,
                forJs);
    }

    @Override
    public Class<?> validate(FormulaAST node, FormulaContext context, FormulaProperties properties)
            throws FormulaException {
        if (node.getNumberOfChildren() != 2) {
            throw new WrongNumberOfArgumentsException(node.getText(), 2, node);
        }

        FormulaAST fieldReferenceArgument = (FormulaAST) node.getFirstChild();
        FormulaDataType columnType = fieldReferenceArgument.getColumnType();
        if ((columnType == null) || !columnType.isPickval()) {
            throw new WrongArgumentTypeException(node.getText(),
                    new Class[] { FormulaEngine.getHooks().getPicklistType() }, fieldReferenceArgument);
        }
        FormulaAST targetArgument = (FormulaAST) fieldReferenceArgument.getNextSibling();
        if (targetArgument != null && targetArgument.getType() != FormulaTokenTypes.STRING_LITERAL) {
            throw new WrongArgumentTypeException(node.getText(), new Class[] { ConstantString.class }, targetArgument);
        }

        return Boolean.class;
    }

    static String compareBulk(String arg, List<String> target) {
        if (target.size() > 1) {
            return "(" + arg + " IS NOT NULL AND " + arg + " IN (" + Joiner.on(", ").join(target) + "))";
        } else if (target.get(0) == null || target.get(0).equals("")) {
            return "(" + arg + " IS NULL)";// The none selected picklist value
        } else {
            return "(" + arg + " IS NOT NULL AND " + arg + " = " + target.get(0) + ")";
        }
    }

    static boolean comparePointwise(Object lhs, List<String> target) {
        if (target == null) {
            return false;
        }
        if (lhs == null) {
            return target.contains("") || target.isEmpty(); // The none selected picklist value
        }
        return target.contains(lhs);
    }

    @Override
    public void prefetch(FormulaAST ast, FormulaContext context)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaAST first = (FormulaAST) ast.getFirstChild();
        String fieldValue = ConstantString.getStringValue((FormulaAST) first.getNextSibling(), true);
        FormulaFieldInfo formulaFieldInfo = getFormulaFieldInfo(first, context);
        FormulaPicklistInfo enumInfo = formulaFieldInfo.getEnumInfo();

        if (enumInfo != null && enumInfo instanceof FormulaPicklistInfo.Dynamic) {
            ((FormulaPicklistInfo.Dynamic) enumInfo).collectApiValueToFetch(fieldValue);
        }
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        // See W-3358047; FormulaGenericTests - Lead - testSalutation
        // Transformed SQL evaluates empty string as null. For parity, JS evaluation
        // needs to evaluate empty string
        // and nulls equivalently

        // args[0] = fieldName, args[1] = picklistValue literal
        final FormulaAST lhs = (FormulaAST) node.getFirstChild();
        final FormulaAST rhs = (FormulaAST) lhs.getNextSibling();

        final String lhsString = OperatorEquality.wrapJsForEquality(args[0], lhs.getDataType());
        final String rhsString = OperatorEquality.wrapJsForEquality(args[1], rhs.getDataType());

        JsValue[] values = new JsValue[2];
        values[0] = OperatorEquality.compareBulkJS(lhsString, lhs.getType(), rhsString, rhs.getType(), true, false,
                false, args); // Equality comparison JsValue
        values[1] = JsValue.generate(args[0] + "==null", args, false); // Null value comparison
        if ("\"\"".equals(args[1].js) || "''".equals(args[1].js)) {
            return JsValue.generate((jsNvl(values[0].buildJSWithGuard(), "false") + " || "
                    + jsNvl(values[1].buildJSWithGuard(), "false")), null, false);
        }
        return values[0];
    }

}

class FunctionIsPicklistValueCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;
    private final List<String> target;

    public FunctionIsPicklistValueCommand(FormulaCommandInfo info, List<String> target) {
        super(info);
        this.target = target;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        String rhs = (String) stack.pop();
        String lhs = (String) stack.pop();
        Boolean useDbValOverride = context.getProperty(FormulaContext.DO_NOT_USE_DB_VALUE_FOR_PICKLIST_EVALUATION);
        if (useDbValOverride != null && useDbValOverride) {
            if (lhs != null) {
                lhs = TextUtil.trim(lhs.toLowerCase());
            }
            if (rhs != null) {
                rhs = rhs.toLowerCase();
            }
            stack.push(OperatorEquality.comparePointwise(lhs, rhs, true, false));
        } else {
            stack.push(FunctionIsPickVal.comparePointwise(lhs, target));
        }
    }
}
