/*
 * Created on Dec 21, 2004
 */
package com.force.formula.impl;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import com.force.formula.*;
import com.force.formula.commands.*;

import com.force.formula.parser.gen.SfdcFormulaTokenTypes;

/**
 * Traverses AST and decorates nodes with data type information, checks for type mismatches, records field references,
 * and checks for cycle.
 *
 * @author dchasman
 * @since 140
 */
public class AnnotationVisitor implements FormulaASTVisitor {
    public AnnotationVisitor(FormulaContext context, FormulaProperties properties) {
        this.context = context;
        this.properties = properties;
    }

    public ContextualFormulaFieldInfo[] getFieldReferences() {
        return fieldReferences.toArray(new ContextualFormulaFieldInfo[fieldReferences.size()]);
    }

    @Override
    public void visit(FormulaAST node) throws FormulaException {
        // Allow each child node to enrich itself
        FormulaAST child = (FormulaAST)node.getFirstChild();
        while (child != null) {
            FormulaCommandInfo childInfo = FormulaCommandInfoRegistry.get(child);
            if (childInfo instanceof FormulaCommandEnricher) {
                child = ((FormulaCommandEnricher)childInfo).enrich(child, context, properties);
            }

            child = (FormulaAST)child.getNextSibling();
        }

        // Process the node
        FormulaCommandInfo commandInfo = FormulaCommandInfoRegistry.get(node);

        // Check to see if the context supports the command
        if (!context.isFunctionSupported(commandInfo)) {
            throw new FunctionNotAllowedException(commandInfo);
        }

        FormulaValidationHooks.get().parseHook_validateCommandInfoInContext(node, commandInfo, context);

        if (properties.getGenerateJavascript() && !commandInfo.getAllowedContext().isJavascript()) {
            throw new FunctionNotAllowedException(commandInfo);
        }
        
        if (properties.getGenerateJavascript()
                && !context.isFunctionSupportedOffline(commandInfo, node.getNumberOfChildren())) {
            throw new FunctionNotAllowedException(commandInfo);
        }

        Type nodeDataType;
        if (commandInfo instanceof FormulaCommandValidator) {
            // Allow for advanced/special processing (e.g. polymorphic args)
            nodeDataType = ((FormulaCommandValidator)commandInfo).validate(node, context, properties);
        } else {
            Type[] inputs = commandInfo.getArgumentTypes(node, context);
            nodeDataType = verifyInputs(node, inputs, commandInfo.getReturnType(node, context));
        }

        node.setDataType(nodeDataType);

        if (node.getType() == SfdcFormulaTokenTypes.IDENT) {
            // Add to the referenced field set
            ContextualFormulaFieldInfo referencedFieldInfo = context.lookup(node.getText(), node.isDynamicReferenceBase());
            // if this is a custom field column, add the field as well
            FormulaValidationHooks.get().parseHook_addExtraReferences(fieldReferences, referencedFieldInfo, context);
            fieldReferences.add(referencedFieldInfo);
        }

        if (commandInfo instanceof FormulaCommandPrefetcher) {
            ((FormulaCommandPrefetcher)commandInfo).prefetch(node, context);
        }
    }

    private Type verifyInputs(FormulaAST node, Type[] inputDataTypes, Type returnType)
            throws WrongArgumentTypeException, WrongNumberOfArgumentsException {
        if (inputDataTypes.length != node.getNumberOfChildren()) {
            throw new WrongNumberOfArgumentsException(node.getText(), inputDataTypes.length, node);
        }

        assert (inputDataTypes.length == node.getNumberOfChildren());

        FormulaAST child = (FormulaAST)node.getFirstChild();
        for (int i = 0; i < inputDataTypes.length; i++) {
            Type actualInputType = child.getDataType();
            Type expectedInputType = inputDataTypes[i];
            // We allow an actual type of RuntimeType here also; this is a special marker interface type used by dynamic expressions. .
            if (!FormulaTypeUtils.hasCommonSuperType(actualInputType, expectedInputType)) {
                throw new WrongArgumentTypeException(node.getText(), new Type[] { expectedInputType }, child);

            }
            child = (FormulaAST)child.getNextSibling();
            // If any of the inputs has unknown type we must propagate this to the result. (Even if the operator delivers
            // a known type, correct functioning of variables in other expressions depends on knowing we have a dynamic expression.
            if (actualInputType == RuntimeType.class) {
                returnType = RuntimeType.class;
            }
        }

        return returnType;

    }

    private FormulaContext context;
    private FormulaProperties properties;
    private Set<ContextualFormulaFieldInfo> fieldReferences = new HashSet<ContextualFormulaFieldInfo>();
}
