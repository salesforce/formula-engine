package com.force.formula.commands;


import java.lang.reflect.Type;
import java.util.Deque;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaException;
import com.force.formula.FormulaGeolocation;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.impl.FormulaTypeUtils;
import com.force.formula.impl.IllegalArgumentTypeException;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.impl.WrongArgumentTypeException;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.SQLPair;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.LOGICAL,isOffline=true)
public class FunctionNullValue extends FormulaCommandInfoImpl implements FormulaCommandValidator, FormulaCommandOptimizer {
    public FunctionNullValue() {
        this("NULLVALUE");
    }

    protected FunctionNullValue(String name) {
        super(name);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionNullValueFormulaCommand(this, localTreatAsString(context, node));
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql;
        String guard;
        if (localTreatAsString(context, node)) {
            sql = args[0];
            guard = guards[0];
        } else {
            FormulaSqlHooks hooks = getSqlHooks(context);
        	if (!hooks.isNullArgument(args[0])) { 
        		sql = hooks.sqlNvl() + "(" + args[0] + ", " + args[1] + ")";
        	} else {
        		sql = args[1]; // If args[0] is obviously NULL from a formula reference, optimize it away. 
        	}
            guard = SQLPair.generateGuard(guards, null);
        }
        return new SQLPair(sql, guard);
    }

    @Override
    public FormulaAST optimize(FormulaAST ast, FormulaContext context) throws FormulaException {
        if (!((FormulaAST)ast.getFirstChild()).canBeNull() || ast.getFirstChild().getNextSibling().getType() == FormulaTokenTypes.NULL) {
            return ast.replace((FormulaAST)ast.getFirstChild());
        }
        return ast;
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
        if (lhs == Boolean.class || lhs == FormulaGeolocation.class)
            throw new IllegalArgumentTypeException(node.getText());
        if (!FormulaTypeUtils.hasCommonSuperType(lhs, rhs)) {
            throw new WrongArgumentTypeException(node.getText(), new Type[] { lhs }, rhsNode);
        }
        // "Inherit" the type of the args
        if (lhs == RuntimeType.class || rhs == RuntimeType.class) {
            return RuntimeType.class;
        }
        return (lhs != ConstantNull.class) ? lhs : rhs;
    }
    
    /**
     * @return whether the given node is the first child of NULLVALUE or BLANKVALUE
     * @param node the node you're checking on 
     */
    public static boolean isFirstNodeOfNullValue(FormulaAST node) {
        FormulaAST parent = node.getParent();
        if (parent == null || node.getFirstChild() != node) {
            return false;
        }
        return FormulaAST.isFunctionNode(parent, "nullvalue") || FormulaAST.isFunctionNode(parent, "blankvalue");
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        if (localTreatAsString(context, node)) {
            return args[0];
        }
        // Get guard from arg[0] and put in inside the value.  Same with the alternative.
        String value = args[0].guard != null ? "(" + args[0].guard + ")?(" + args[0].js + "):null": args[0].js;
        String alternate = args[1].guard != null ? "(" + args[1].guard + ")?(" + args[1].js + "):null": args[1].js;
        return JsValue.generate(jsNvl(value, alternate), new JsValue[0], args[1].couldBeNull && args[0].couldBeNull);  // Can be null only if both could be
    }

    protected boolean localTreatAsString(FormulaContext context, FormulaAST node) {
        return treatAsString(context,node);
    }

    public static boolean treatAsString(FormulaContext context, FormulaAST node) {
        if (context.getProperty(FormulaContext.DO_NOT_TREAT_NULLS_AS_STRING) != null) {
            return false;
        }

        // Check to see if this is contained in a template
        FormulaAST top = node;
        while (top.getParent() != null) {
            top = top.getParent();
        }

        if (FormulaAST.isFunctionNode((FormulaAST)top.getFirstChild(), "template")) {
            return false;
        }

        FormulaAST lhsNode = ((FormulaAST)node.getFirstChild());
        if (lhsNode == null) return false;
        if (lhsNode.getColumnType() != null && lhsNode.getColumnType().isMultiEnum()) return false; // never treat a MSP as string (see FieldReferenceCommandInfo)
        if (FormulaTypeUtils.isTypeText(lhsNode.getDataType())) return true;

        FormulaAST rhsNode = (FormulaAST)lhsNode.getNextSibling();
        if (rhsNode == null) return false;
        return FormulaTypeUtils.isTypeText(rhsNode.getDataType());
    }

    public static class FunctionNullValueFormulaCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;
		private final boolean treatAsString;

        public FunctionNullValueFormulaCommand(FormulaCommandInfo info, boolean treatAsString) {
            super(info);
            this.treatAsString = treatAsString;
        }

        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
            Object otherVal = stack.pop();
            Object normalVal = stack.pop();
            if (treatAsString) {
                stack.push(normalVal);
            } else {
                stack.push((unwrap(normalVal) != null) ? normalVal : otherVal);
            }
        }

        public static Object unwrap(Object value) {
            if (value != null) {
                if (value instanceof FormulaDateTime) {
                //  Extract the actual Date from the FormulaDateTime type safe wrapper
                    value = ((FormulaDateTime)value).getDate();
                } else if (value instanceof ConstantString.StringWrapper) {
                    value = value.toString();
                }
                value = FormulaEngine.getHooks().hook_unwrapForNullable(value);
            }

            return value;
        }
    }
}
