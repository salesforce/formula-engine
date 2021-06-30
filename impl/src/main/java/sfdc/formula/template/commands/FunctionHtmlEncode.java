package sfdc.formula.template.commands;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.commands.FormulaCommand;
import sfdc.formula.commands.FormulaCommandInfo;
import sfdc.formula.impl.FormulaAST;

@AllowedContext(section=SelectorSection.TEXT, displayOnly=true,isJavascript=false)
public class FunctionHtmlEncode extends EncodingFunctionBase {

    public FunctionHtmlEncode() {
        super("HTMLENCODE");
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws FormulaException {
        return new HtmlEncodeCommand(this);
    }
    
    static class HtmlEncodeCommand extends EncodingCommand {
        private static final long serialVersionUID = 1L;

		public HtmlEncodeCommand(FormulaCommandInfo formulaCommandInfo) {
            super(formulaCommandInfo);
        }

        @Override
        protected String encode(String arg) {
            return FormulaTextUtil.escapeToHtml(arg);
        }
        
    }


}
