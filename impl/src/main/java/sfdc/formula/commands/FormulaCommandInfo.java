/*
 * Created on Dec 10, 2004
 *
 */
package sfdc.formula.commands;

import java.lang.reflect.Type;

import sfdc.formula.*;
import sfdc.formula.impl.*;

/**
 * Metadata for formula operations
 *
 * @author dchasman
 * @since 140
 */
public interface FormulaCommandInfo extends FormulaCommandType {
    Type getReturnType(FormulaAST node, FormulaContext context) throws FormulaException;

    Type[] getArgumentTypes(FormulaAST node, FormulaContext context) throws FormulaException;

    FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws FormulaException;

    SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, ITableAliasRegistry registry) throws FormulaException;

    JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException;
}
