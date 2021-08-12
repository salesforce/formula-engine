package com.force.formula.commands;

import java.math.BigDecimal;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;

/**
 * Base class for unary math commands
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.MATH)
public class UnaryMathCommandInfo extends FormulaCommandInfoImpl {
    public UnaryMathCommandInfo(String token, UnaryMathCommandBehavior behavior) {
        super(token, BigDecimal.class, new Class[] { BigDecimal.class });
        this.behavior = behavior;
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return behavior.getCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        return behavior.getSQL(node, context, args, guards);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return behavior.getJavascript(node, context, args);
    }
    
    @Override
    protected FormulaCommandType.AllowedContext getDefaultContext() {
        return UnaryMathCommandInfo.class.getAnnotation(FormulaCommandType.AllowedContext.class);
    }
    
    @Override
    public FormulaCommandType.AllowedContext getAllowedContext() {
        FormulaCommandType.AllowedContext context = behavior.getClass().getAnnotation(FormulaCommandType.AllowedContext.class);
        return context == null ? getDefaultContext() : context;
    }
    
    private final UnaryMathCommandBehavior behavior;
}
