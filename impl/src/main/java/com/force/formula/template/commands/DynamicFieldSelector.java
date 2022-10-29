/*
 * Copyright, 2010, salesforce.com All Rights Reserved Company Confidential
 */
package com.force.formula.template.commands;

import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.commands.*;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
/**
 * Formula Command Info/Command for a dynamic reference field element; i.e. in an expression like a[b].c, the .c part.   
 * @author aballard
 * @since 168
 */
@AllowedContext(section=SelectorSection.ADVANCED,isSql = false, isJavascript=false)
public class DynamicFieldSelector extends FormulaCommandInfoImpl {

    public final static String DYNAMIC_REF_IDENT = FormulaCommandInfoRegistry.DYNAMIC_REF_IDENT; 
    
    public DynamicFieldSelector() {
        super(DYNAMIC_REF_IDENT, String.class, new Class[]{});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new DynamicFieldSelectorCommand(this, node.getText());
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
}

class DynamicFieldSelectorCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;
    DynamicFieldSelectorCommand(FormulaCommandInfo info, String fieldName) {
        super(info);
        this.fieldName = fieldName;
    }

    // The name is the value.
    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        stack.push(fieldName);
    }

    private final String fieldName;

}
