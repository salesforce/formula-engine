package com.force.formula.commands;


import java.lang.reflect.Type;
import java.util.Deque;
import java.util.List;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.commands.FunctionNullValue.FunctionNullValueFormulaCommand;
import com.force.formula.impl.*;
import com.google.common.base.Splitter;

import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.SQLPair;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.LOGICAL,isOffline=true)
public class FunctionIsNull extends FormulaCommandInfoImpl implements FormulaCommandValidator, FormulaCommandOptimizer {
    public FunctionIsNull() {
        this("ISNULL");
    }

    protected FunctionIsNull(String name) {
        super(name);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new OperatorIsNullCommand(this, treatAsString(context, node) && context.getProperty(FormulaContext.FORMULA_FILTER) == null);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql;
        String guard;
        if (args[0] == null )  {
            // flow passes null for arguments
            sql = "(" + args[0] + " IS NULL)";
        }
        else  {
            List<String> colList = Splitter.on(FunctionGeolocation.LOCATION_DELIMITER).splitToList(args[0]);
            String[] columns = colList.toArray(new String[colList.size()]);
            sql= "(";
            for (int i = 0; i < columns.length; i++)  {
                if (i != 0) sql+= " AND ";

                String col = columns[i];

                if ("''".equals(col)) {
                    // replacing '' with NULL for Postgres - see W-4808196    
                    sql += "NULL IS NULL";
                } else {
                    sql += col + " IS NULL";
                }
            }
            sql+= ")";
        }
        guard = guards[0];

        return new SQLPair(sql, guard);
    }

    @Override
    public FormulaAST optimize(FormulaAST ast, FormulaContext context) throws FormulaException {
        if (treatAsString(context, ast)) {
            ast.setType(FormulaTokenTypes.FALSE);
            ast.setText("false");
            ast.setCanBeNull(false);
            ast.setConstantExpression(true);
            ast.removeChildren();
        }
        return ast;
    }

    protected boolean treatAsString(FormulaContext context, FormulaAST ast) {
        return FunctionNullValue.treatAsString(context, ast);
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        if (node.getNumberOfChildren() != 1) {
            throw new WrongNumberOfArgumentsException(node.getText(), 1, node);
        }

        FormulaAST arg = (FormulaAST)node.getFirstChild();
        Type argDataType = arg.getDataType();
        if (argDataType == Boolean.class)
            throw new IllegalArgumentTypeException(node.getText());
        return Boolean.class;
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        String nullCheck = args[0].guard != null ? "!("+args[0].guard+")||" : "";
        if (treatAsString(context, node)) {
            return new JsValue("false", null, false);
        } else {
            return new JsValue(nullCheck+"(null=="+args[0]+")", null, false);
        }
    }
}

class OperatorIsNullCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;
	private final boolean treatAsString;

    public OperatorIsNullCommand(FormulaCommandInfo info, boolean treatAsString) {
        super(info);
        this.treatAsString = treatAsString;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        Object arg = stack.pop();
        if (treatAsString)
            stack.push(Boolean.FALSE);
        else
            stack.push(Boolean.valueOf(FunctionNullValueFormulaCommand.unwrap(arg) == null));
    }
}
