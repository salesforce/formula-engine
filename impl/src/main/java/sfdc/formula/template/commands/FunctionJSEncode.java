package sfdc.formula.template.commands;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.commands.FormulaCommand;
import sfdc.formula.commands.FormulaCommandInfo;
import sfdc.formula.impl.FormulaAST;

@AllowedContext(section=SelectorSection.TEXT, displayOnly=true,isJavascript=false)
public class FunctionJSEncode extends EncodingFunctionBase {

    public FunctionJSEncode() {
        super("JSENCODE");
    }
    
    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws FormulaException {
        return new JSEncodeCommand(this);
    }
    
    static class JSEncodeCommand extends EncodingCommand {
        private static final long serialVersionUID = 1L;

		public JSEncodeCommand(FormulaCommandInfo formulaCommandInfo) {
            super(formulaCommandInfo);
        }

        @Override
        protected String encode(String arg) {
            return FormulaTextUtil.escapeForJavascriptString(arg);
        }
        
    }
                                                          

}
