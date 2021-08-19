/*
 * Copyright, 2006, salesforce.com All Rights Reserved Company Confidential
 */
package com.force.formula.template.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.commands.*;
import com.force.formula.impl.*;

import antlr.collections.AST;
import com.force.formula.parser.gen.SfdcFormulaTokenTypes;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaI18nUtils;

/**
 * Formula command info/command for a dynamic field reference <expr>[b] or <expr>.c
 *
 * @author aballard
 * @since 168
 */
@AllowedContext(section=SelectorSection.ADVANCED,isJavascript=false)
public class DynamicReference extends FormulaCommandInfoImpl implements FormulaCommandValidator {

    public final static String DYNAMIC_REF = FormulaCommandInfoRegistry.DYNAMIC_REF;

    public DynamicReference() {
        super(DYNAMIC_REF);
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        if (node.getNumberOfChildren() != 2) {
            throw new WrongNumberOfArgumentsException(node.getText(), 1, node);
        }
        // Would like to do more checking here, but currently can't. The base type is always RuntimeType.
        // If we distinguished map and list we could at least check for compatible subscript type some
        // of the time. To do that would mean changing the return type from other FieldReferences to provide this,
        // which would have a lot of ripple.
        FormulaAST baseArg = (FormulaAST)node.getFirstChild();
        final Type dataType = baseArg.getDataType();
        if (dataType != Object.class && dataType != RuntimeType.class) {
            final String actualType = FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionDataTypes", FormulaTypeUtils.getTypeName(dataType));
            throw new GenericFormulaException(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages",
                "InvalidSubscriptBase", actualType));
        }

        return RuntimeType.class;
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        AST child = node.getFirstChild();
        if (child != null) {
            child = child.getNextSibling();
        }
        // Determine if this is an <expr>[y] reference or an <expr>.y reference
        boolean fieldref = child != null && child.getType() == SfdcFormulaTokenTypes.DYNAMIC_REF_IDENT;

        return new DynamicReferenceCommand(getName(), FormulaAST.isTopOfReferenceFormula(node), fieldref, node.isDynamicReferenceBase());
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
        throws FormulaException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        throw new UnsupportedOperationException();
    }

    public static class DynamicReferenceCommand extends BaseFieldReferenceCommand {
        private static final long serialVersionUID = 1L;

        public DynamicReferenceCommand(String commandName, boolean isRoot, boolean fieldref, boolean isDynamicRefBase) {
            super(commandName, false, isRoot, isDynamicRefBase);
            this.isFieldRef = fieldref;
        }

        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
            Object subscript = stack.pop();
            Object base  = stack.pop();
            if (base == null) {
                stack.push(null);
            } else {
                stack.push(doSubscript(context, base, subscript));
            }
        }

        private Object doSubscript(FormulaRuntimeContext context, Object base, Object subscript) throws Exception {
            // Map, List, Array, and object are different cases.
            Class<? extends Object> type;

            // if java metadata, get underlying type
            if (base instanceof FormulaTypeAdapter) {  // TODO SLT JavaMetadataELAdapter
                type = ((FormulaTypeAdapter)base).getType();
            } else {
                type = base.getClass();
            }

            if (List.class.isAssignableFrom(type)) {
                List<?> list = (List<?>)base;
                return subscriptArray(context, list, subscript);
            } else if (type.isArray()) {
                // I think this only happens with metadata proxy elements; other case will be
                // using a List.
                Object[] a = (Object[])base;
                return subscriptArray(context, Arrays.asList(a), subscript);
            } else if (Map.class.isAssignableFrom(type)) {
                return subscriptMap(context, (Map<?,?>)base, subscript);
            }
            String ref = checkStringType(subscript);
            if (ref == null) {
                // Should possibly throw an error rather than pushing null, but would have to version
                // since there maybe code depending on it. Not worth the trouble.

                return null;
            }
            return execute(context, new FormulaFieldReferenceImpl(base, ref, isFieldRef, isDynamicReferenceBase));
        }

        private Object subscriptArray(FormulaRuntimeContext context, List<?> base, Object subscript) throws UnsupportedTypeException, InvalidFieldReferenceException {
            if (subscript == null) {
                // Should possibly throw an error rather than pushing null, but would have to version
                // since there maybe code depending on it. Not worth the trouble.
                // (Also, dynamic references can hit this path...)
                return null;
            }
            BigDecimal decNum = checkNumberType(subscript);
            int intNum;
            try {
                intNum = decNum.intValueExact();
            } catch (ArithmeticException ex) {
                throw new FormulaEvaluationException("Subscript for list or array must be an Integer"); // NOPMD
            }
            // Can't range check during metadata compile
            if (FormulaValidationHooks.get().isFormulaContainerCompiling()) {
                return null;
            }

            if (base.size() == 0) {
                throw new FormulaEvaluationException("Subscript is invalid because list is empty");
            }

            if (intNum < 0 || intNum >= base.size()) {
                throw new FormulaEvaluationException("Subscript value " + intNum + " not valid.  Must be between 0 and " +
                        (base.size() - 1));
            }

            // isRoot is set only for the root node and only if the expression is supposed to evaluate to
            // evaluate to a reference.
            if (isRoot) {
                return new FormulaFieldReferenceImpl(base, Integer.valueOf(intNum));
            } else {
               return context.getListElement(base, intNum);
            }
        }

        private Object subscriptMap(FormulaRuntimeContext context, Map<?, ?> base, Object subscript) throws UnsupportedTypeException, InvalidFieldReferenceException {
            // Can't error check during metadata compile
            if (FormulaValidationHooks.get().isFormulaContainerCompiling()) {
                return null;
            }

            // isRoot is set only for the root node and only if the expression is supposed to evaluate to
            // evaluate to a reference.
            if (isRoot) {
                return new FormulaFieldReferenceImpl(base, subscript);
            }

            Object value = context.getMapElement(base, subscript);
            if (value == null && ((subscript != null || context.getProperty(FormulaContext.ALLOW_NULL_MAP_KEY) == null))) {
                throw new FormulaEvaluationException("Map key " + (subscript == null ? "null" : subscript.toString()) + " not found in map.");
            }
            return value;
        }

        // True if this corresponds to an expression of the form <expr>.field; i.e. the reference part is a literal selector, not
        // a dynamic expression.
        private boolean isFieldRef;

    }
}
