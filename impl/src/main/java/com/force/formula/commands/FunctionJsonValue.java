/*
 * Copyright, 1999-2021, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.commands;

import java.util.Deque;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaEvaluationException;
import com.force.formula.FormulaException;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.InvalidFieldReferenceException;
import com.force.formula.UnsupportedTypeException;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.impl.FormulaTypeUtils;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.impl.WrongArgumentTypeException;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.SQLPair;

/**
 * JSONVALUE(JsonString as KV, Key)
 * 
 * Returns 
 * 
 * Example:
 * JSONVALUE('{"Temperature": 45}', 'Temperature') returns '45'.
 * 
 * Note, the key must be a string literal
 * 
 * @author stamm
 * @since 0.2.15
 */
@AllowedContext(section=SelectorSection.TEXT,access="beta",isOffline=true)
public class FunctionJsonValue extends FormulaCommandInfoImpl implements FormulaCommandValidator {    
    public FunctionJsonValue() {
        super("JSONVALUE", String.class, new Class[] { String.class, String.class });
    }
    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws InvalidFieldReferenceException,
            UnsupportedTypeException {
        return new FunctionJsonValueCommand(this);
    }

    
    @Override
    public Class<?> validate(FormulaAST node, FormulaContext context, FormulaProperties properties)
            throws FormulaException {
        if (node.getNumberOfChildren() != 2) {
            throw new WrongNumberOfArgumentsException(node.getText(), 2, node);
        }

        FormulaAST jsonValue = (FormulaAST) node.getFirstChild();
        if ((jsonValue == null) || !FormulaTypeUtils.isTypeTextUgly(jsonValue.getDataType())) {
            throw new WrongArgumentTypeException(node.getText(),
                    new Class[] { FormulaEngine.getHooks().getPicklistType() }, jsonValue);
        }
        
        // The path must be a literal for it to work in the DB.
        FormulaAST targetArgument = (FormulaAST) jsonValue.getNextSibling();
        if (targetArgument != null && targetArgument.getType() != FormulaTokenTypes.STRING_LITERAL) {
            throw new WrongArgumentTypeException(node.getText(), new Class[] { ConstantString.class }, targetArgument);
        }

        return String.class;
    }
    
    /**
     * @return whether we should use pljson (which is specifically we have Oracle11g and not any version higher)
     */
    protected boolean usePljson() {
        return false;  
    }
    
    
    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        String guard = SQLPair.generateGuard(guards, null);

        // TODO: move these to FormulaSqlHooks when it's more stable
        FormulaSqlHooks hooks = getSqlHooks(context);
        if (hooks.isPostgresStyle()) {
            String sql = "json_extract_path_text(" + args[0] + "::json," + args[1] + ")";
            return new SQLPair(sql, guard);
        } else if (hooks.isOracleStyle() ) {
            
            String sql;
            // Pljson version.  https://github.com/pljson/pljson
            if (usePljson()) {
                sql = "pljson(NVL("+args[0]+",'{}')).get(NVL("+args[1]+",'')).to_char()";
            } else {
                String path = "'$." + args[1].substring(1);  // must be literal in oracle
                sql = "json_value("+args[0] + "," + path + ")";
            }
            return new SQLPair(sql, guard);
        } else if (hooks.isMysqlStyle()) {
            String path = "'$." + args[1].substring(1); 
            String sql = "json_unquote(json_extract(" + args[0] + "," + path + "))";
            return new SQLPair(sql, guard);
        } else if (hooks.isPrestoStyle()) {
            String path = "'$." + args[1].substring(1);  
            String sql = "CAST(json_extract(" + args[0] + "," + path + ") AS VARCHAR)";
            return new SQLPair(sql, guard);
        } else if (hooks.isSqliteStyle()) {
            String path = "'$." + args[1].substring(1);  
            String sql = "json_extract(" + args[0] + "," + path + ")";
            return new SQLPair(sql, guard);
        } else {
            String sql = "json_value("+args[0] + ","+String.format(hooks.sqlConcat(false),"'$.'", args[1]) + ")";
            return new SQLPair(sql, guard);
        }
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return JsValue.generate(context.getJsEngMod() + ".tostr(JSON.parse("+args[0]+"||'{}')["+args[1]+"])", args, true);
    }

    static class FunctionJsonValueCommand extends AbstractFormulaCommand {
        public FunctionJsonValueCommand(FormulaCommandInfo info) {
            super(info);
        }
    
        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
            String key = checkStringType(stack.pop());
            String str = checkStringType(stack.pop());
            
            String result = null;
            if (key != null && str != null) {
                JsonNode node;
                try {
                    node = new ObjectMapper().readTree(str);
                } catch (JsonProcessingException x) {
                    throw new FormulaEvaluationException(x);
                }
                JsonNode leaf = node != null ? node.get(key) : null;
                result = leaf != null ? leaf.asText() : null;
            }
            
            stack.push(result);
        }
    }
}