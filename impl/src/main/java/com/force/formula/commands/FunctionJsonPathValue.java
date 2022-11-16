/*
 * Copyright, 1999-2021, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.commands;

import java.util.Deque;
import java.util.EnumSet;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaEngine;
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
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * JSONPATHVALUE(JsonString, JsonPath)
 * 
 * Returns 
 * 
 * Example:
 * JSONPATHVALUE('{"Temperature": 45}', '$.Temperature') returns '45'.
 * 
 * Note, this always returns text.
 * 
 * @see <a href="https://goessner.net/articles/JsonPath/">JsonPath</a>
 * @author stamm
 * @since 0.2.15
 */
@AllowedContext(section=SelectorSection.TEXT,access="beta",isOffline=true)
public class FunctionJsonPathValue extends FormulaCommandInfoImpl implements FormulaCommandValidator {
    
    static final Configuration CONFIG;
    
    static {
        JsonProvider jsonProvider = new JacksonJsonProvider();
        MappingProvider mappingProvider = new JacksonMappingProvider();
        CONFIG = Configuration.builder().jsonProvider(jsonProvider).mappingProvider(mappingProvider).options(EnumSet.noneOf(com.jayway.jsonpath.Option.class)).build();
    }
    
    public FunctionJsonPathValue() {
        super("JSONPATHVALUE", String.class, new Class[] { String.class, String.class });
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
    

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        String guard = SQLPair.generateGuard(guards, null);
        FormulaSqlHooks hooks = getSqlHooks(context);

        // TODO: move these to FormulaSqlHooks when it's more stable
        if (hooks.isPostgresStyle()) {
            // JSON support in postgres is very version dependent

            // FIXME: This is Postgres 11 version where paths aren't supported.  This is broken.
            //String sql = "json_extract_path_text(" + args[0] + "::json,SUBSTRING(" + args[1] + ",3))";

            // TODO SLT: Postgres 12 required for this...
             String sql = "(jsonb_path_query(" + args[0] + "::jsonb," + args[1] + ") #>> '{}')";
            // Postgres 15 will allow json_value() again
            //String sql = "json_value(to_jsonb("+args[0] + ")," + args[1] + " RETURNING text NULL ON ERROR)";
            return new SQLPair(sql, guard);
        } else if (hooks.isMysqlStyle()) {
            String sql = "json_unquote(json_extract(" + args[0] + "," + args[1] + "))";
            return new SQLPair(sql, guard);
        } else if (hooks.isPrestoStyle()) {
            String sql = "CAST(json_extract(" + args[0] + "," + args[1] + ") AS VARCHAR)";
            return new SQLPair(sql, guard);
        } else if (hooks.isSqliteStyle()) {
            String sql = "json_extract(" + args[0] + "," + args[1] + ")";
            return new SQLPair(sql, guard);
        } else { // oracle and transactsql
            // json_value(`JSONSTRING`, `JSONPATH`)
            String sql = "json_value("+args[0] + "," + args[1] + ")";
            return new SQLPair(sql, guard);
        }
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        //return JsValue.forNonNullResult("typeof $F==='undefined'?undefined:(" + context.getJsModule() + ".jsonPath("+args[0]+",((!"+args[1]+"||'').startsWith('$')?'$.':'')+"+args[1]+")||'')", args);
        return JsValue.forNonNullResult("typeof "+context.getJsEngMod()+"==='undefined'?undefined:(String(" + context.getJsEngMod() + ".jsonPath(JSON.parse("+args[0]+"),"+args[1]+")||''))", args);
    }

    static class FunctionJsonValueCommand extends AbstractFormulaCommand {
        public FunctionJsonValueCommand(FormulaCommandInfo info) {
            super(info);
        }
    
        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
            String path = checkStringType(stack.pop());
            String str = checkStringType(stack.pop());
            
            String result = null;
            if (path != null && str != null) {
                // Use json path if it starts with '$'
                if (!path.startsWith("$")) {
                    path = "$." + path;
                }
                try {
                    Object o = JsonPath.using(CONFIG).parse(str).read(path);
                    result = o != null ? String.valueOf(o) : null;
                } catch (JsonPathException | IllegalArgumentException ex) {
                    // Errors = null.  Yes, you can't distinguish null from empty string.
                    result = null;
                }
                
            }
            
            stack.push(result);
        }
    }
}