package com.force.formula.commands;

import com.force.formula.FormulaContext;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.FormulaAST;

@AllowedContext(section=SelectorSection.LOGICAL, isOffline=true)
public class FunctionBlankValue extends FunctionNullValue {
    public FunctionBlankValue() {
        // Use traditional nvl (which converts String nulls)
        super("BLANKVALUE");
    }

    @Override
    public boolean localTreatAsString(FormulaContext context, FormulaAST node) {
        return false;
    }

}
