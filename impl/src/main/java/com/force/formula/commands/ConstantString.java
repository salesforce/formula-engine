/*
 * Created on Dec 10, 2004
 *
 */
package com.force.formula.commands;

import java.io.Serializable;

import com.force.formula.*;
import com.force.formula.impl.*;
import com.google.common.base.CharMatcher;

/**
 * Push the command's associated string value onto the stack
 *
 * @author dchasman
 * @since 140
 */
public class ConstantString extends ConstantBase {
    private final boolean unescape;
    
    public ConstantString(String name, boolean unescape) {
        super(name, String.class, new Class[0]);

        this.unescape = unescape;
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new StringConstantCommand(this, getStringValue(node, unescape));
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String value = getStringValue(node, unescape);
        if (value != null) {
            // Escape single quotes
            value = CharMatcher.is('\'').replaceFrom(value, "''");

            // Wrap with single quotes
            StringBuilder result = new StringBuilder("'");
            result.append(value);
            result.append("'");

            value = result.toString();
        }

        return new SQLPair(value, null);
    }

    
    public static String getStringValue(FormulaAST node, boolean unescape) {
        String result = node.getText();

        if (result != null && unescape) {
            // Remove double quotes of literal string value
            result = result.substring(1, result.length() - 1);

            // Un-escape double quotes and backslashes
            result = FormulaTextUtil.replaceSimple(result, new String[] { "\\\"", "\\\\" }, new String[] { "\"", "\\" });
        }

        return result;
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        if (node.getText() == null) return new JsValue(null, null, true);
        return new JsValue('"' + FormulaTextUtil.escapeForJavascriptString(getStringValue(node, true)) + '"', null, false);
    }
    

    public static class StringWrapper implements Serializable {
        
        private static final long serialVersionUID = 1L;

		protected StringWrapper() {
            this.value = null;
        }
        
        protected StringWrapper(String value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return value;
        }
        
        private String value;
    }
    
    // Special value used to represent a Null String on the evaluation stack.  This is used with dynamic references to
    // allow comparison operations to know if an operand is a String type, even if the value is null.  
    public static final StringWrapper NullString = new StringWrapper(null);
    
}

