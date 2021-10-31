package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;

import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.SQLPair;

/**
 * Describe your class here.
 *
 * @author djacobs and others
 * @since 140
 */
@AllowedContext(section = SelectorSection.MATH, isOffline = true)
public class OperatorEquality extends FormulaCommandInfoImpl implements FormulaCommandValidator, FormulaCommandOptimizer {
    public OperatorEquality(String token) {
        super(token);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new OperatorEqualityFormulaCommand(this, getName(), treatAsString(node));
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        final FormulaAST lhs = (FormulaAST)node.getFirstChild();
        final FormulaAST rhs = (FormulaAST)lhs.getNextSibling();

        final String lhsString = wrapBoolean(args[0], lhs.getDataType(), lhs.getType());
        final String rhsString = wrapBoolean(args[1], rhs.getDataType(), rhs.getType());

        String sql = compareBulk(lhsString, lhs.getType(), rhsString, rhs.getType(), treatAsString(node),
            "<>".equals(getName()));
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }

    // replace TEXT(picklist) = "value" (or "value" = TEXT(picklist)) with ISPICKVAL(picklist, "value")
    @Override
    public FormulaAST optimize(FormulaAST node, FormulaContext context) throws FormulaException {
        FormulaAST lhs = (FormulaAST)node.getFirstChild();
        FormulaAST rhs = (FormulaAST)lhs.getNextSibling();

        if (lhs.getType() == FormulaTokenTypes.STRING_LITERAL && isTextPicklistCase(rhs))  {
            return optimize(node, rhs, lhs, "<>".equals(getName()));
        } else if (rhs.getType() == FormulaTokenTypes.STRING_LITERAL && isTextPicklistCase(lhs))  {
            return optimize(node, lhs, rhs, "<>".equals(getName()));
        }
        return node;
    }

    /**
     * Stolen from FunctionText
     * @param node
     * @return
     */
    static protected boolean isTextPicklistCase(FormulaAST node) {
        if (FormulaAST.isFunctionNode(node, "text")) {
            FormulaDataType argType = ((FormulaAST)node.getFirstChild()).getColumnType();
            return argType != null && argType.isAnySingleEnum();
        }
        return false;
    }
    
    private FormulaAST optimize(FormulaAST ast, FormulaAST textnode, FormulaAST stringNode, boolean negate) {
        stringNode.setNextSibling(null);
        stringNode.setParent(null);
        FormulaAST pickFieldNode = (FormulaAST)textnode.getFirstChild();
        pickFieldNode.setNextSibling(null);
        pickFieldNode.setParent(null);

        FormulaAST pickval = new FormulaAST("ispickval");

        pickval.setType(FormulaTokenTypes.FUNCTION_CALL);
        pickval.setCanBeNull(false);
        pickval.setDataType(Boolean.class);

        pickval.addChild(pickFieldNode);
        pickval.addChild(stringNode);

        if (negate) {
            FormulaAST not = new FormulaAST("not");
            not.setType(FormulaTokenTypes.NOT);
            not.setDataType(Boolean.class);
            not.addChild(pickval);
            return ast.replace(not);
        }
        return ast.replace(pickval);
    }

    private String wrapBoolean(String arg, Type argType, int nodeType) {
        // convert boolean back to number for comparison
        if (argType == Boolean.class) {
            if (nodeType == FormulaTokenTypes.TRUE)
                return "1";
            else if (nodeType == FormulaTokenTypes.FALSE)
                return "0";
            else
                return String.format("CASE WHEN %s THEN 1 ELSE 0 END", arg);
        }
        return arg;
    }

    /**
     * You can't natively compare dates in javascript; either you do a&gt;=b &amp;&amp; a&lt;=b, or you call getTime().  getTime() seems less awful
     * @param arg the value for the type to compare 
     * @param argType the argument type
     * @return a javascript value suitable for testing equality
     */
    public static String wrapJsForEquality(JsValue arg, Type argType) {
        // convert date to number for comparison so it doesn't try and compare date objects
        if (argType == Date.class || argType == FormulaDateTime.class) {
            if (arg.couldBeNull) {
                return jsNvl2(arg, arg.js+".getTime()", "null");
            } else {
                return arg.js + ".getTime()";
            }
        }
        if (argType == String.class && arg.couldBeNull) {
            return jsNvl2WithGuard(arg, arg.js, "null", true);
        } else {
            return arg.js;
        }
    }
    
    @Override
    public Class<?> validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        if (node.getNumberOfChildren() != 2) {
            throw new WrongNumberOfArgumentsException(node.getText(), 2, node);
        }

        FormulaAST lhsNode = (FormulaAST)node.getFirstChild();
        FormulaAST rhsNode = (FormulaAST)lhsNode.getNextSibling();
        Type lhs = lhsNode.getDataType();
        Type rhs = rhsNode.getDataType();
        
        if (lhs == FormulaGeolocation.class || rhs == FormulaGeolocation.class) 
            throw new IllegalArgumentTypeException(node.getText());
        
        if ((lhs != Object.class) && (rhs != Object.class)
                && (!FormulaTypeUtils.hasCommonSuperType(lhs,rhs)))
            throw new WrongArgumentTypeException(node.getText(), new Type[] { lhs }, rhsNode);

        if (lhs == RuntimeType.class || rhs == RuntimeType.class) {
            return RuntimeType.class;
        }

        return Boolean.class;
    }

    private Boolean treatAsString(FormulaAST node) {
        FormulaAST lhsNode = ((FormulaAST)node.getFirstChild());
        Type lhs = lhsNode.getDataType();
        Type rhs = ((FormulaAST)lhsNode.getNextSibling()).getDataType();
        // If either has unknown type, it might be a string at runtime so must
        // defer determining whether to treat as string.
        if (lhs == RuntimeType.class || rhs == RuntimeType.class) {
            return null;
        }
        return FormulaTypeUtils.isTypeText(lhs) || FormulaTypeUtils.isTypeText(rhs);
    }

    public static Boolean comparePointwise(Object lhs, Object rhs, boolean treatAsString, boolean negate) {
        return comparePointwise(lhs, rhs, treatAsString, negate, false);
    }

    public static Boolean comparePointwise(Object lhs, Object rhs, boolean treatAsString, boolean negate,
            boolean returnNonNull) {
        if (treatAsString) {
            lhs = (lhs == null || lhs == ConstantString.NullString) ? "" : lhs;
            rhs = (rhs == null || rhs == ConstantString.NullString) ? "" : rhs;
            // We might have a FieldSetMember here which has to be converted to string.  Has to be special cased because we don't use the
            // regular type check logic here...
            // This is required for back compatibility of Fieldsets with 170.
            if (lhs instanceof FieldSetMemberInfo) {
                lhs = ((FieldSetMemberInfo)lhs).getFieldPath();
            }
            if (rhs instanceof FieldSetMemberInfo) {
                rhs = ((FieldSetMemberInfo)rhs).getFieldPath();
            }
        }

        if (returnNonNull) {
            // return T/F and not null
            if (lhs == null) {
                boolean nullResult = rhs == null;
                if (negate) nullResult = !nullResult;
                return nullResult;
            }
        } else {
            if ((lhs == null) || (rhs == null)) return null; // Follow Oracle three-value semantics
        }

        boolean result;
        if (lhs instanceof BigDecimal && rhs instanceof BigDecimal)
            result = ((BigDecimal)lhs).compareTo((BigDecimal)rhs) == 0;
        else
            result = lhs.equals(rhs);
        if (negate)
            result = !result;
        return Boolean.valueOf(result);
    }

    public static String compareBulk(Object lhs, int lhsType, Object rhs, int rhsType, boolean treatAsString,
            boolean negate) {

        if (treatAsString) {
            // If null feeds into a comparison, the result is null: that's Oracle 3-value logic.
            // But we want to force the result to true or false so subsequent operations do
            // the right thing. This little beauty forces null to a value that is different than
            // the other side unless both are null. Cool eh? Note this relies on (null || 'x')
            // being 'x'.
            Object saveRhs = rhs;
            if (rhsType != FormulaTokenTypes.STRING_LITERAL)
                rhs = "nvl(" + rhs + ", " + lhs + "||'x')";
            else if ("''".equals(rhs))
                rhs = lhs + "||'x'";
            if (lhsType != FormulaTokenTypes.STRING_LITERAL)
                lhs = "nvl(" + lhs + ", " + saveRhs + "||'x')";
            else if ("''".equals(lhs))
                lhs = saveRhs + "||'x'";
        }
        return "(" + lhs + (negate ? "<>" : "=") + rhs + ")";
    }
    
    /**
     * See compareBulk for the reasoning behind this function. Javascript behavior must match 
     * SQL and formula engine.
     */
    public static JsValue compareBulkJS(String lhs, int lhsType, String rhs, int rhsType, boolean treatAsString,
            boolean negate, boolean highPrec, JsValue... guards) {

        if (treatAsString) {
            String saveRhs = rhs;
            if (rhsType != FormulaTokenTypes.STRING_LITERAL)
                rhs = jsNoe(rhs, jsNvl(lhs, "''") + "+'x'");
            else if ("''".equals(rhs) || "\"\"".equals(rhs))
            	rhs = jsNvl(lhs, "''") + "+'x'";
            
            if (lhsType != FormulaTokenTypes.STRING_LITERAL)
                lhs = jsNoe(lhs, jsNvl(saveRhs, "''") + "+'x'");
            else if ("''".equals(lhs) || "\"\"".equals(lhs))
                lhs = jsNvl(saveRhs, "''") + "+'x'";
        }
        
        if (highPrec) {
            // Decimal(1)!=Decimal(1)... Need to use eq or neq
            if (negate) {
                String guard = JsValue.makeGuard(guards);
                return JsValue.generate((guard != null ? guard + "&&" : "") + "(!(" + lhs + ").eq(" + rhs + "))", null, false);
            } else {
                return JsValue.generate("(" + lhs + ").eq(" + rhs + ")", guards, false, guards);
            }
        }
        if (negate) {
            return JsValue.generate("(" + lhs + ")!=(" + rhs + ")", null, false);
        } else {
            return JsValue.generate("(" + lhs + ")==(" + rhs + ")", null, false);
        }
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        
        final FormulaAST lhs = (FormulaAST)node.getFirstChild();
        final FormulaAST rhs = (FormulaAST)lhs.getNextSibling();
        
        final String lhsString = wrapJsForEquality(args[0], lhs.getDataType());
        final String rhsString = wrapJsForEquality(args[1], rhs.getDataType());
        
        // See note above...
        final boolean highPres = lhs.getDataType() == BigDecimal.class && context.useHighPrecisionJs();
        
        return compareBulkJS(lhsString, lhs.getType(), rhsString, rhs.getType(), treatAsString(node),
                "<>".equals(getName()), highPres, args);
    }
}

class OperatorEqualityFormulaCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;
	// Note treatAsString may be null to indicate it must be runtime determined because operands have unknown
    // type when formula is compiled.
    public OperatorEqualityFormulaCommand(FormulaCommandInfo info, String token, Boolean treatAsString) {
        super(info);
        this.token = token;
        this.treatAsString = treatAsString;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        Object rhs = stack.pop();
        Object lhs = stack.pop();
        boolean asString = treatAsString != null ? treatAsString
                : (rhs == ConstantString.NullString || lhs == ConstantString.NullString);

        Boolean result = OperatorEquality.comparePointwise(rhs, lhs, asString, "<>".equals(token),
                shouldReturnBoolResult(context));
        stack.push(result);
    }

    private final Boolean treatAsString;
    private final String token;
}
