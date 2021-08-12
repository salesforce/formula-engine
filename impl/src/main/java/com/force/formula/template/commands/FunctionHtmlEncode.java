package com.force.formula.template.commands;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.commands.FormulaCommand;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.impl.FormulaAST;

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
