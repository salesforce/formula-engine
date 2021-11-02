package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
@AllowedContext(section=SelectorSection.MATH)
public class ConvertCurrencyToNumberCommandInfo extends FormulaCommandInfoImpl {
    public ConvertCurrencyToNumberCommandInfo() {
        super("CONVERTCURRENCY", BigDecimal.class, new Class[] { FormulaCurrencyData.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws FormulaException {
        return new ConvertCurrencyToNumberCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
        throws FormulaException {
        // NOOP for SQL, just pass the wrapped value through
        return new SQLPair(args[0], null);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return args[0];
    }
}

class ConvertCurrencyToNumberCommand extends AbstractFormulaCommand {
    
    private static final long serialVersionUID = 1L;

	public ConvertCurrencyToNumberCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        FormulaCurrencyData value = checkCurrencyDataType(stack.pop());

        stack.push((value != null) ? value.getAmount() : null);
    }
}
