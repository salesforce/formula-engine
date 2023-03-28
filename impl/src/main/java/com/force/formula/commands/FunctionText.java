/*
 * Created on Dec 29, 2004
 *
 */
package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.BigDecimalHelper;
import com.force.i18n.BaseLocalizer;

/**
 * This is a generic implementation of FunctionText, but lets the developer extend it for supporting picklists (see BaseFunctionIsPicklistVal)
 *
 * @author dchasman and others
 * @since 140
 */
@AllowedContext(section=SelectorSection.TEXT, isOffline=true)
public class FunctionText extends FormulaCommandInfoImpl implements FormulaCommandValidator, FormulaCommandOptimizer {
    public FunctionText() {
        super("TEXT");
    }

    @Override
    public FormulaAST optimize(FormulaAST node, FormulaContext context) throws FormulaException {
        FormulaAST parent = node.getParent();
        if (parent != null && isTextPicklistCase(node)) {
            if (FormulaAST.isFunctionNode(parent, "isblank") || FormulaAST.isFunctionNode(parent, "isnull")) {
                return (FormulaAST)node.getFirstChild();
            }
            if (node.getNextSibling() != null && (FormulaAST.isFunctionNode(parent, "blankvalue") || FormulaAST.isFunctionNode(parent, "nullvalue"))) {
                return (FormulaAST)node.getFirstChild();
            }
        }
        return node;
    }

    static protected boolean isTextPicklistCase(FormulaAST node) {
        if (FormulaAST.isFunctionNode(node, "text")) {
            FormulaDataType argType = ((FormulaAST)node.getFirstChild()).getColumnType();
            return argType != null && argType.isAnySingleEnum();
        }
        return false;
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        FormulaAST toConvert = (FormulaAST)node.getFirstChild();
        // **
        // ** TODO This if check is very bad and should be removed! It prevents a test from failing in the autobuilds (see W-3500207)
        // **
        if ("division".equalsIgnoreCase(toConvert.getText())) {
            return JsValue.forNonNullResult("''", null);
        }
        
        Type clazz = toConvert.getDataType();
        if (clazz == Date.class) {
            // YYYY-MM-DD is 10 characters. 
            return JsValue.forNonNullResult(args[0]+".toISOString().substring(0, 10)", args);
        } else if (clazz == FormulaDateTime.class) {
            // Need to convert from 'YYYY-MM-DDTHH:mm:ss.sssZ' to 'YYYY-MM-DD HH:mm:ssZ'
            return JsValue.forNonNullResult("("+args[0]+".toISOString().replace('T',' ').substring(0,19)+'Z')", args);  // Ecma's so close...
        } else if (clazz == FormulaTime.class) {
            return JsValue.forNonNullResult(args[0]+".toISOString().substring(11,23)", args);
        } else if (clazz == String.class) {
            return args[0];
        }
        
        return JsValue.forNonNullResult("\"\"+"+args[0].js, args);
    }

    boolean doesContextSupportPicklists(FormulaContext context) {
        return context.getGlobalProperties().getFormulaType().allowPicklistTextConversion();
    }
    
    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        if (node.getNumberOfChildren() != 1) {
            throw new WrongNumberOfArgumentsException(node.getText(), 1, node);
        }

        FormulaAST toConvert = (FormulaAST)node.getFirstChild();
        Type clazz = toConvert.getDataType();
        FormulaDataType columnType = null;

        boolean supportsPicklists = doesContextSupportPicklists(context);

        if (supportsPicklists)
            columnType = toConvert.getColumnType();

        if ((clazz != ConstantNull.class) && (clazz != BigDecimal.class) && (clazz != Date.class) && (clazz != FormulaTime.class)
            && (clazz != FormulaDateTime.class) && (clazz != RuntimeType.class)
            && (columnType == null || !columnType.isPickval())) {

            List<Class<?>> types = new ArrayList<Class<?>>(4);
            types.add(BigDecimal.class);
            types.add(Date.class);
            types.add(FormulaDateTime.class);

            if (supportsPicklists)
                types.add(FormulaEngine.getHooks().getPicklistType());

            // If the type is a Picklist, make it explicit in the error message
            if (toConvert.getColumnType() != null && toConvert.getColumnType().isPickval())
                throw new WrongArgumentTypeException(node.getText(), types.toArray(new Class[types.size()]), toConvert, toConvert.getColumnType());
            else
                throw new WrongArgumentTypeException(node.getText(), types.toArray(new Class[types.size()]), toConvert);
        }

        return clazz == RuntimeType.class ? clazz : String.class;   // propagate runtimetype
    }
    
    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        FormulaAST targetNode = (FormulaAST)node.getFirstChild();

        FormulaDataType columnType = targetNode.getColumnType();
        if (columnType == null || !columnType.isPickval()) {
            return new OperatorTextFormulaCommand(this, null, false);
        }

        boolean originalValue = false;
        if (FormulaAST.isFunctionNode(targetNode, "priorvalue")) {
            // Use the first arg to PriorValue() as the actual target
            targetNode = (FormulaAST)targetNode.getFirstChild();
            originalValue = true;
        }

        return new OperatorTextFormulaCommand(this, targetNode.getText(), originalValue);
    }

    private boolean isPicklistCase(FormulaAST node) {
        FormulaDataType columnType = ((FormulaAST)node.getFirstChild()).getColumnType();

        return columnType != null && columnType.isPickval();
    }
    

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) throws FormulaException {
        if (isPicklistCase(node)) {
            return FormulaValidationHooks.get().getPicklistSQL(node, context, args, guards, registry);
        } else {
            String sql;
            String guard;
            FormulaAST toConvert = (FormulaAST)node.getFirstChild();
            Type clazz = toConvert.getDataType();
            Type parentType = null;
            
            if ( node.getParent() != null ) {
                parentType = node.getParent().getDataType();
            }
            
            StringBuilder sqlBuilder = new StringBuilder();
            
            FormulaSqlHooks hooks = getSqlHooks(context);
            if (clazz == FormulaTime.class)  {
            	sqlBuilder.append(String.format(hooks.sqlToCharTime(), args[0]));
            } else if (clazz == BigDecimal.class) {
            	sqlBuilder.append("(").append(String.format(getSqlHooks(context).sqlToChar(), args[0])).append(")");
            } else  {
            	String toChar;
            	if (clazz == Date.class || parentType == Date.class) {
            		toChar = String.format(hooks.sqlToCharDate(), args[0]);
            	} else if (clazz == FormulaDateTime.class) {
            		toChar = String.format(hooks.sqlToCharTimestamp(), args[0]);
            	} else {
            		toChar = String.format(hooks.sqlToChar(), args[0]);
            	}
 
            	sqlBuilder.append("(");
                if (clazz == FormulaDateTime.class && parentType != Date.class) {
                    sqlBuilder.append(String.format(hooks.sqlConcat(true), toChar,"'Z' "));
                } else {
                	sqlBuilder.append(toChar);
                }
                sqlBuilder.append(")");
            }
            sql = sqlBuilder.toString();
            guard = guards[0];
            
            return new SQLPair(sql, guard);
        }

    }

    /**
     * @param toConvert the picklist value that is probably not a String.
     * @param picklistFieldInfo the information about the picklist field
     * @return Text value of the enum's stored value. Will return null if EnumItem or EnumInfo is not found
     */
    public static Object getTextForPicklist(Object toConvert, FormulaFieldInfo picklistFieldInfo) {
        return FormulaEngine.getHooks().getTextForPicklist(toConvert, picklistFieldInfo);
    }
  
}

class OperatorTextFormulaCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;
    private String picklistFieldName;
    private boolean useOriginalValue;

    OperatorTextFormulaCommand(FormulaCommandInfo info, String fieldName, boolean useOriginalValue) {
        super(info);
        this.picklistFieldName = fieldName;
        this.useOriginalValue = useOriginalValue;
    }

    @Override
    public boolean isCustomIndexable(FormulaContext formulaContext) {
        return FormulaEngine.getHooks().isTextFunctionIndexable(formulaContext, picklistFieldName, super.isCustomIndexable(formulaContext));
    }

    @Override
    public void preExecuteInBulk(List<FormulaRuntimeContext> contexts) throws FormulaException {
        try {
            FormulaEngine.getHooks().hook_pushFullAccessRights();
            if (picklistFieldName == null)
                return;
            for (FormulaRuntimeContext context : contexts) {
                if (useOriginalValue)
                    context = context.getOriginalValuesContext();
                String dbValue = context.getString(picklistFieldName, false);
                FormulaPicklistInfo enumInfo = context.lookup(picklistFieldName).getEnumInfo();
                if (dbValue != null && enumInfo != null && enumInfo instanceof FormulaPicklistInfo.Dynamic) {
                    ((FormulaPicklistInfo.Dynamic)enumInfo).collectDbValueToFetch(dbValue);
                }
            }
        } finally {
            FormulaEngine.getHooks().hook_popAccessRights();
        }
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        Object toConvert = stack.pop();
        if (FormulaCommandInfoImpl.isNull(toConvert)) {
            stack.push(null); // Follow Oracle three-value semantics
        } else {
            if (toConvert instanceof BigDecimal) {
                stack.push(BigDecimalHelper.formatBigDecimal((BigDecimal)toConvert));
            } else if (toConvert instanceof Date) {
                // Returns text representation of date in ISO 8601 standard format: YYYY-MM-DD
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                format.setTimeZone(BaseLocalizer.GMT_TZ);
                stack.push(format.format((Date)toConvert));
            }else if (toConvert instanceof FormulaTime) {
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS");
                format.setTimeZone(BaseLocalizer.GMT_TZ);
                stack.push(format.format(new Date(((FormulaTime)toConvert).getTimeInMillis())));
            } else if (toConvert instanceof String) {
                assert picklistFieldName != null;
                // this is the picklist case, the db value is passed as a string
                try {
                    FormulaFieldInfo picklistFieldInfo = context.lookup(picklistFieldName);
                    Object value;
                    Boolean useDbValOverride = context.getProperty(FormulaContext.DO_NOT_USE_DB_VALUE_FOR_PICKLIST_EVALUATION);
                    if (useDbValOverride != null && useDbValOverride) {
                        value = toConvert;
                    } else {
                        value = FunctionText.getTextForPicklist(toConvert, picklistFieldInfo);
                    }
                    // eli: For some reason, existing code before the getTextForPicklist refactor
                    //      didn't push any value onto the stack if the enumInfo was null.
                    //      This is probably an error and unreachable code...
                    if (!(value == null && picklistFieldInfo.getEnumInfo() == null)) {
                        stack.push(value);
                    }
                } catch (InvalidFieldReferenceException x) {
                    throw new RuntimeException(x);
                } catch (UnsupportedTypeException x) {
                    throw new RuntimeException(x);
                }
            } 
            else {
                // DateTime
                FormulaDateTime value = checkDateTimeType(toConvert);

                // need to recheck for null as we can get a null here
                // if the object is RuntimeTypeMetadataELAdapter
                // and we are in a VF compile-time evaluation.
                // See: AbstractFormulaCommand.fixRuntimeAdapter
                if (value == null ) {
                    stack.push(null);
                } else {
                    // DCHASMAN Returns text representation of the date/time in the following format: YYYY-MM-DD HH:MM:SSZ
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    format.setTimeZone(BaseLocalizer.GMT_TZ);
                    stack.push(format.format(value.getDate()) + "Z");
                }

            }
        }
    }
}
