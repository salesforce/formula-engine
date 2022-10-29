package com.force.formula.template.commands;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaException;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.impl.FormulaAST;
import com.force.formula.util.FormulaTextUtil;

@AllowedContext(section=SelectorSection.TEXT,isSql=false, displayOnly=true,isJavascript=false)
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
