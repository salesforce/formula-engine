package com.force.formula.template.commands;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.commands.*;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.BaseLocalizer;
import com.google.common.collect.Lists;

/**
 * @author dchasman
 * @since 144
 */
@AllowedContext(section=SelectorSection.ADVANCED, displayOnly=true,isJavascript=false)
public class FunctionTemplate extends FormulaCommandInfoImpl {

    public FunctionTemplate() {
        super("TEMPLATE", String.class, new Class[] {});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionTemplateCommand(this);
    }

    @Override
    public Type[] getArgumentTypes(FormulaAST node, FormulaContext context) {
        Type[] argumentTypes = new Type[node.getNumberOfChildren()];

        // Just match the types of the passed args
        int n = 0;
        FormulaAST child = (FormulaAST)node.getFirstChild();
        while (child != null) {
            argumentTypes[n++] = child.getDataType();
            child = (FormulaAST)child.getNextSibling();
        }

        return argumentTypes;
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
        throws FormulaException {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return new JsValue("", null, false);
    }
}

class FunctionTemplateCommand extends AbstractFormulaCommand {
    public FunctionTemplateCommand(FormulaCommandInfo formulaCommandInfo) {
        super(formulaCommandInfo);
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        String urlEncoding = FormulaValidationHooks.get().getTemplateUrlEncoding(context);

        // Dethunk all of the template() arguments and evaluate each one using its own isolated stack to handle exceptions on a
        // per expression block basis

        // Note, that Deque toArray is in LIFO order, but it needs to be put in reverse order when pushing for thunks
        Object[] result = Lists.reverse(new ArrayList<Object>(stack)).toArray();

        for (int n = 0; n < result.length; n++) {
            Object entry = result[n];
            if (entry instanceof Thunk) {
                try {
                    Deque<Object> localStack = new FormulaStack();
                    ((Thunk)entry).executeReally(context, localStack);
                    entry = localStack.pop();
                } catch (Exception x) {
                    entry = handleException(context, x);
                }
            }

            // Handle formating of Dates and DateTimes
            if (entry instanceof Date) {
                entry = FormulaI18nUtils.getLocalizer().getDateFormat(BaseLocalizer.GMT).format((Date)entry);
            } else if (entry instanceof FormulaDateTime) {
                FormulaDateTime fdt = (FormulaDateTime) entry;
                if (fdt.getDate() != null) {
                    entry = FormulaI18nUtils.getLocalizer().getDateTimeFormat().format(fdt.getDate());
                } else {
                    entry = "";
                }
            }

            // See if we need to perform url encoding on the constituent and dynamic parts of a template
            if ((urlEncoding != null) && (entry != null) && !(entry instanceof TemplateStaticMarkupString)){
                try {
                    entry = FormulaValidationHooks.get().templateUrlEncodeString(entry.toString(), urlEncoding);
                } catch (UnsupportedEncodingException x) {
                    // Not much we can do about this (should never happen) - giving up
                    entry = handleException(context, x);
                }
            }

            result[n] = entry;
        }

        stack.push(result);
    }

    private Object handleException(FormulaRuntimeContext context, Exception x) throws Exception {
        if (context instanceof FormulaExceptionListerner) {
            return ((FormulaExceptionListerner)context).onException(x);
        } else {
            throw x;
        }
    }
}
