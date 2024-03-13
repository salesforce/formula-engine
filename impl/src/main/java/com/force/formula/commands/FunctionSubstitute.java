package com.force.formula.commands;

import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaTextUtil;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 144
 */
@AllowedContext(section=SelectorSection.TEXT)
public class FunctionSubstitute extends FormulaCommandInfoImpl {
    public FunctionSubstitute() {
        super("SUBSTITUTE", String.class, new Class[] { String.class, String.class, String.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionSubstituteCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql = String.format("REPLACE(%s, %s, %s)", args[0], args[1], args[2]);
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        /**
         * JS replaceAll natively returns the original string if arg[1] is null and replaces with null if arg[2] is null
         * However, the SQL and Java implementation returns null if arg[1] is null and replaces with "" if arg[2] is null
         * To align with SQL and Java implementation, we are putting JS guards on arg[1] and arg[2] of substitute function
         * If arg[1] is null return null, else if arg[2] is null then consider arg[2] as "" and call replaceAll(arg[1],"")
         * If arg[1] and arg[2] are not null, then call replaceAll(arg[1],arg[2])
         * E.g.
         *     SUBSTITUTE("Hello; Text;", null, ",") returns null
         *     SUBSTITUTE("Hello; Text;", ";", null) returns "Hello Text"
         *     SUBSTITUTE("Hello; Text;", ";", ',') returns "Hello, Text,"
         */
        return JsValue.generate("(" + args[1] + " === null ? null : (" + args[2] + " === null ? "
                        + args[0] + ".replaceAll(" + args[1] + ",\"\") :"
                        + args[0] + ".replaceAll(" + args[1] + "," + args[2] + ")))",
                args, false, args[0]);
    }

}

class FunctionSubstituteCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	public FunctionSubstituteCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        String newText = checkStringType(stack.pop());
        String oldText = checkStringType(stack.pop());
        String sourceText = checkStringType(stack.pop());

        if ((sourceText == null) || (oldText == null)) {
            stack.push(null);
            return;
        }
        if (sourceText.length() == 0) {
            stack.push(sourceText);
            return;
        }
        
        if (newText == null) {
            newText = "";
        }

        stack.push(FormulaTextUtil.replaceSimple(sourceText, oldText, newText));
    }
}
