package com.force.formula.commands;

import com.force.formula.FormulaContext;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.FormulaAST;

@AllowedContext(section=SelectorSection.LOGICAL,isOffline=true)
public class FunctionIsBlank extends FunctionIsNull {
    public FunctionIsBlank() {
        super("ISBLANK");
    }

    @Override
    protected boolean treatAsString(FormulaContext context, FormulaAST ast) {
        return false;
    }
}
