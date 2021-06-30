package sfdc.formula.commands;

import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.FormulaContext;
import sfdc.formula.impl.FormulaAST;

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
