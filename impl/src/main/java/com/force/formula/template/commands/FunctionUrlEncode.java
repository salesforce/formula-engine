package com.force.formula.template.commands;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaEvaluationException;
import com.force.formula.FormulaException;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaValidationHooks;
import com.google.common.base.Charsets;

@AllowedContext(section=SelectorSection.TEXT, isSql=false,displayOnly=true,isJavascript=false)
public class FunctionUrlEncode extends EncodingFunctionBase {
    
    public FunctionUrlEncode() {
        super("URLENCODE");
    }


    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws FormulaException {
        return new UrlencodeCommand(this);
    }

    static class UrlencodeCommand extends EncodingCommand {
        private static final long serialVersionUID = 1L;
        public UrlencodeCommand(FormulaCommandInfo formulaCommandInfo) {
            super(formulaCommandInfo);
        }

        @Override
        protected String encode(String arg) {
            String pageEncoding = FormulaValidationHooks.get().getUrlEncoding();
            if (pageEncoding == null) {
                pageEncoding = Charsets.UTF_8.name();
            }
            try {
                return URLEncoder.encode(arg, pageEncoding);
            } catch (UnsupportedEncodingException e) {
                // this is not likely, but go look at Globals.getPageEncoding if this exception is thrown
                throw (FormulaEvaluationException) new FormulaEvaluationException("Could not encode url with charset: " + pageEncoding).initCause(e);
            }
        }
        
    }
    
}
