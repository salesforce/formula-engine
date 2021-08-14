/*
 * Copyright, 2006, salesforce.com
 */
package com.force.formula.commands;

import com.force.formula.*;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.TemplateStaticMarkupString;

/**
 * Push the command's associated string value onto the stack.  Only difference between this and ConstantString is
 * that it wraps the pushed value to distinguish it. 
 *
 * @author dchasman
 * @since 144
 */
public class ConstantTemplateString extends ConstantString {
    public ConstantTemplateString() {
        super("TEMPLATE_STRING_LITERAL", false);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new StringConstantCommand(this, new TemplateStaticMarkupString(ConstantString.getStringValue(node, false)));
    }
}

