/*
 * Copyright 2023 salesforce.com, inc.
 * All Rights Reserved
 * Company Confidential
 */

package com.force.formula.commands;


import java.util.Deque;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaException;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.sql.SQLPair;

/**
 * Returns whether a given string ends with a specified string or not
 *
 * @author karan.jain
 * @since 246
 */
@AllowedContext(section=SelectorSection.TEXT)
public class FunctionEnds extends FormulaCommandInfoImpl {
    public FunctionEnds() {
        super("ENDS", Boolean.class, new Class[] { String.class, String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionEndsCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = "((" + args[1] + " IS NULL) OR (" + getSqlHooks(context).sqlInstr2("REVERSE(" + args[0] + ")", "REVERSE(" + args[1] + ")") + " = 1))";
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        /*
         * If arg1 is null return true
         * If arg0 is null return false
         * If bot of the argument or non null return result after applying endsWith function
         */
        String js =  "!" + args[1] + "||" + "(" + args[0] + "!= null && " + jsNvl(context, args[0].js,"''") + ".endsWith("+jsNvl(context, args[1].js,"''")+"))";
        return JsValue.generate(js, args, false);
    }
}

class FunctionEndsCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

    public FunctionEndsCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        String sub = checkStringType(stack.pop());
        String target = checkStringType(stack.pop());
        if ((sub == null) || (sub == ""))
            stack.push(true);
        else if ((target == null) || (target.equals("")))
            stack.push(null);
        else
            stack.push(target.endsWith(sub));
    }
}
