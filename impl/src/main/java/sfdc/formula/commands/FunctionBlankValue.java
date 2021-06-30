package sfdc.formula.commands;

import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaContext;
import sfdc.formula.impl.FormulaAST;

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
