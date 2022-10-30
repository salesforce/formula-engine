/*
 * Copyright, 1999-2015, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.commands;

import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * Return true of the object isn this context is a Clone of another entity
 *
 * @author stamm
 * @since 198
 */
@AllowedContext(section=SelectorSection.LOGICAL,isSql = false, changeOnly=true,nonFlowOnly=true,isJavascript=false)
public class FunctionIsClone extends FormulaCommandInfoImpl {
    public FunctionIsClone() {
        super("ISCLONE", Boolean.class, new Class[] {});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        return new FunctionIsCloneCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
        throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return new JsValue("d.isClone", null, true);
    }
}

class FunctionIsCloneCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionIsCloneCommand(FormulaCommandInfo info) {
        super(info);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        stack.push(context.isClone());
    }

    @Override
    public boolean isDeterministic(FormulaContext formulaContext) {
        return false;
    }
}