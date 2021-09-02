package com.force.formula.commands;

import java.math.BigDecimal;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.template.commands.FunctionIsNumber;
import com.force.formula.util.BigDecimalHelper;

/**
 * Describe your class here.
 *
 * @author dchasman and others
 * @since 140
 */
@AllowedContext(section = SelectorSection.TEXT, isOffline = true)
public class FunctionValue extends FormulaCommandInfoImpl {
    public FunctionValue() {
        super("VALUE", BigDecimal.class, new Class[] { String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new OperatorValueFormulaCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String guard = SQLPair.generateGuard(guards, "NOT "+FunctionIsNumber.getSQL(args[0]));
        return new SQLPair("TO_NUMBER(" + args[0] + ")", guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        if (context.useHighPrecisionJs()) {
            return JsValue.forNonNullResult("$F.Decimal(" + args[0] + ")", args);
        }
        return JsValue.forNonNullResult("Number(" + args[0] + ")", args);
    }
}

class OperatorValueFormulaCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public OperatorValueFormulaCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        String input = checkStringType(stack.pop());
        BigDecimal num = null;
        if (input != null) {
            try {
                num = new BigDecimal(input, BigDecimalHelper.MC_PRECISION_INTERNAL);
            } catch (NumberFormatException ex) {
                if (!FormulaEngine.getHooks().isFormulaContainerCompiling()) {
                    // set to null for GDPR compliance
                    throw new InvalidNumericValueException("null"); // NOPMD
                }
            }
        }
        stack.push(num);
    }
}