package sfdc.formula.commands;


import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.*;

import java.util.Deque;

/**
 * 
 * @author dchasman
 * @since 144
 */
@AllowedContext(section=SelectorSection.LOGICAL,changeOnly=true,nonFlowOnly=true,isJavascript=false)
public class FunctionIsNew extends FormulaCommandInfoImpl {
    public FunctionIsNew() {
        super("ISNEW", Boolean.class, new Class[] {});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        return new FunctionIsNewCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
        throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return new JsValue("record.isNew", null, false);
    }
}

class FunctionIsNewCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionIsNewCommand(FormulaCommandInfo info) {
        super(info);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        stack.push(context.isNew());
    }

    @Override
    public boolean isDeterministic(FormulaContext formulaContext) {
        return false;
    }
}
