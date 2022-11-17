package com.force.formula.template.commands;

import java.util.Deque;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaException;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.commands.AbstractFormulaCommand;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.commands.FormulaCommandInfoImpl;
import com.force.formula.commands.FunctionFormat;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.sql.SQLPair;
import com.force.i18n.Renameable;
import com.force.i18n.grammar.GrammaticalLocalizer;
import com.force.i18n.grammar.LanguageDictionary;

/**
 * Mock function for getting a mock renameable by name.
 * 
 * You *do not* want to use this in production.  You want the Renameable class to be provided
 * by a FormulaContext so that references to the functions are managed correctly and validated.
 *
 * @author stamm
 * @since 0.2.0
 * @see FunctionFormat
 */
@AllowedContext(section=SelectorSection.TEXT,isSql=false,displayOnly=true)
public class FunctionRenameable extends FormulaCommandInfoImpl {
    public FunctionRenameable() {
        super("RENAMEABLE", Renameable.class, new Class[] { String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new RenameableCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        throw new UnsupportedOperationException();
    }     
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.forNonNullResult("{latitude:"+args[0]+",longitude:"+args[1]+"}", args);
    }

    static class RenameableCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;

        RenameableCommand(FormulaCommandInfo info) {
            super(info);
        }
    
        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
            String key = checkStringType(stack.pop());
            
            LanguageDictionary dict = ((GrammaticalLocalizer)FormulaEngine.getHooks().getLocalizer()).getLabelSet().getDictionary();
            
            stack.push(new MockRenamingProvider.MockExistingRenameable(key, dict));
        }
    }
}
