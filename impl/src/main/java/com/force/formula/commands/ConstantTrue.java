package com.force.formula.commands;

import java.util.Deque;

import com.force.formula.*;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
public class ConstantTrue extends ConstantBase {
    public ConstantTrue() {
        super("TRUE", Boolean.class, new Class[0]);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new ConstantTrueCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        return new SQLPair("(1=1)", null);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return new JsValue("true", null, false);
    }
}

class ConstantTrueCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public ConstantTrueCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        stack.push(Boolean.TRUE);
    }
}
