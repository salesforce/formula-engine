package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaException;
import com.force.formula.FormulaFieldInfo;
import com.force.formula.FormulaGeolocation;
import com.force.formula.FormulaPicklistInfo;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.InvalidFieldReferenceException;
import com.force.formula.UnsupportedTypeException;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.impl.FormulaTypeUtils;
import com.force.formula.impl.FormulaValidationHooks;
import com.force.formula.impl.IllegalArgumentTypeException;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.impl.WrongArgumentTypeException;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaTextUtil;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * IN(target,[value1,value2...]).  Validates that target is one of the discrete values to the right.
 * If target is null, it will return false, regardless of whether NULL is one of the values.
 *
 * @author stamm
 * @since 0.3
 */
@AllowedContext(section=SelectorSection.LOGICAL, isOffline=true)
public class FunctionIn extends FormulaCommandInfoImpl implements FormulaCommandValidator, FormulaCommandPrefetcher, FormulaCommandOptimizer {
    public FunctionIn() {
        super("IN");
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws InvalidFieldReferenceException,
            UnsupportedTypeException {
        return new FunctionInCommand(this, node.getNumberOfChildren(), FunctionCase.treatAsString(node),
                getTargets(node, context, false, false));
    }

    @Override
    public FormulaAST optimize(FormulaAST node, FormulaContext context) throws FormulaException {
        FormulaAST valueNode = (FormulaAST)node.getFirstChild();
        if (FormulaAST.isFunctionNode(valueNode, "text") && ((FormulaAST)valueNode.getFirstChild()).getColumnType() != null && ((FormulaAST)valueNode.getFirstChild()).getColumnType().isAnySingleEnum()) {
            valueNode.replace((FormulaAST)valueNode.getFirstChild());
        }
        return node;
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaAST valueNode = (FormulaAST)node.getFirstChild();
        boolean isPicklistCase = FunctionCase.isPicklistCase(valueNode);
        List<List<String>> targets = getTargets(node, context, true, false);
        boolean treatAsString = FunctionCase.treatAsString(node);
        StringBuilder sql = new StringBuilder(256);

        List<String> values = new ArrayList<>(args.length);
        if (treatAsString || isPicklistCase) {
            for (int i = 1; i < args.length; i++) {
                if (isPicklistCase) {
                    if (targets.get(i) != null && !targets.get(i).isEmpty()) {
                        values.addAll(values);
                    }   
                } else {
                    values.add(args[i]);
                }
            }
        } else {
            for (int i = 1; i < args.length; i++) {
                values.add(args[i]);
            }
        }
        
        if (values.size() > 0) {
            FormulaSqlHooks hooks = getSqlHooks(context);
            if (FormulaTypeUtils.isTypeText(valueNode.getDataType()))  {
                sql.append(hooks.sqlMakeStringComparable(args[0], false));  // Mysql does case insensitive without this.
            } else {
                sql.append(args[0]);
            }
            sql.append(" IN (").append(Joiner.on(',').join(values)).append(")");
        } else {
            sql.append(args[0]).append(" IS NULL");
        }

        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql.toString(), guard);
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        // Check that correct number of args was supplied
        int numberOfChildren = node.getNumberOfChildren();
        if (numberOfChildren < 2) {
            throw new WrongNumberOfArgumentsException(node.getText(), 2, node);
        }

        FormulaAST currentNode = (FormulaAST)node.getFirstChild();
        Type expType = currentNode.getDataType();
        if (expType == Boolean.class || expType == FormulaGeolocation.class) { throw new IllegalArgumentTypeException(node.getText()); }

        boolean isPicklistCase = FunctionCase.isPicklistCase(currentNode);

        for (int n = 1; n < numberOfChildren; n ++) {
            // Check the case type
            currentNode = (FormulaAST)currentNode.getNextSibling();
            if (isPicklistCase) {
                if (currentNode.getType() != FormulaTokenTypes.STRING_LITERAL){
                    throw new WrongArgumentTypeException(node.getText(), new Class[] {}, currentNode); // TODO fix this
                }
            } else {
                Type caseType = currentNode.getDataType();
                if (caseType == Boolean.class) throw new IllegalArgumentTypeException(node.getText());
                // If we don't already know the expression type, try this one
                if (expType == ConstantNull.class || expType == RuntimeType.class) expType = caseType;
                caseType = FormulaTypeUtils.getCommonSuperType(caseType, expType);
                if (caseType == null) {
                    throw new WrongArgumentTypeException(node.getText(), new Type[] { expType }, currentNode);
                }
            }
        }

        return Boolean.class;
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        FormulaAST valueNode = (FormulaAST)node.getFirstChild();
        //int firstType = valueNode.getType();
        boolean isPicklistCase = FunctionCase.isPicklistCase(valueNode);
        Type type = valueNode.getDataType();
        List<List<String>> targets = getTargets(node, context, false, true);
        //boolean treatAsString = treatAsString(node);
        StringBuilder js = new StringBuilder(256);
                
        if (isPicklistCase) {
            js.append("([");
            for (int i = 1; i < args.length; i ++) {
                List<String> cases = targets.get(i);
                if (cases != null) {  // If the case is valid...
                    String arr = cases.stream().map(c->"'"+FormulaTextUtil.escapeForJavascriptString(c)+"'").collect(Collectors.joining(","));
                    js.append(arr).append(",");
                }
            }
            if (js.length() > 0) {
                js.setLength(js.length()-1);
                js.append("].filter(e=>e!=null).indexOf(").append(args[0]).append(")>=0)");
            } else {
                js.append("null==").append(args[0]);
            }
        } else {
            js.append("([");
            for (int i = 1; i < args.length; i++) {
                // Decimal.js doesn't work right with Array includes...
                if (type == BigDecimal.class && context.useHighPrecisionJs()) {
                    js.append(jsNvl2(context, args[i], jsToNum(context, args[i].js),null)).append(",");
                } else if (type == Date.class || type == FormulaDateTime.class) {
                    // Need to use getTime.
                    js.append(jsNvl2(context, args[i], args[i].js + ".getTime()",null)).append(",");
                } else {
                    js.append(args[i]).append(",");
                }
            }
            js.setLength(js.length()-1);
            Object value = args[0];
            if (type == BigDecimal.class && context.useHighPrecisionJs()) {
                value = jsNvl2(context, args[0], jsToNum(context, args[0].js),null);
            } else if (type == Date.class || type == FormulaDateTime.class) {
                value = jsNvl2(context, args[0], args[0].js + ".getTime()",null);
            }           
            js.append("].filter(e=>e!=null).indexOf(").append(value).append(")>=0)");
        }
        return JsValue.generate(js.toString(), args, false); 
    }

    // Returns the DB values, which are the String representations of numbers, of the
    // enum values specified in every other argument to the function
    private List<List<String>> getTargets(FormulaAST node, FormulaContext context, boolean forSql, boolean forJs)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        
        Boolean useDbValOverride = context.getProperty(FormulaContext.DO_NOT_USE_DB_VALUE_FOR_PICKLIST_EVALUATION);
        if (useDbValOverride != null && useDbValOverride) {
            return null;
        }
        
        FormulaAST targetNode = (FormulaAST)node.getFirstChild();

        if (!FunctionCase.isPicklistCase(targetNode)) return null;

        FormulaFieldInfo formulaFieldInfo = FunctionIsPickVal.getFormulaFieldInfo(targetNode, context);

        int numberOfChildren = node.getNumberOfChildren();

        List<List<String>> targets = Lists.newArrayListWithExpectedSize(numberOfChildren);
        for (int i = 0; i < numberOfChildren; i++) {
            //fill list with nulls, so we can access each element by index later (for ease in debugging like case)
            targets.add(null);
        }
        FormulaAST currentNode = targetNode;
        for (int i = 1; i < numberOfChildren; i ++) {
            // Get the value
            currentNode = (FormulaAST)currentNode.getNextSibling();
            String fieldValue = ConstantString.getStringValue(currentNode, true);
            targets.set(i, FormulaValidationHooks.get().getUnderlyingValuesForPicklist(formulaFieldInfo, fieldValue, context, forSql, forJs));
        }

        return targets;
    }

    @Override
    public void prefetch(FormulaAST ast, FormulaContext context) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaAST targetNode = (FormulaAST)ast.getFirstChild();

        if (!FunctionCase.isPicklistCase(targetNode)) {
            return;
        }

        // Prefetch dynamic enum items needed for this IN function
        FormulaFieldInfo formulaFieldInfo = FunctionIsPickVal.getFormulaFieldInfo(targetNode, context);
        FormulaPicklistInfo enumInfo = formulaFieldInfo.getEnumInfo();
        if (enumInfo != null && enumInfo instanceof FormulaPicklistInfo.Dynamic) {
            FormulaAST currentNode = targetNode;
            int numberOfChildren = ast.getNumberOfChildren();
            for (int i = 1; i < numberOfChildren; i++) {
                currentNode = (FormulaAST)currentNode.getNextSibling();
                String fieldValue = ConstantString.getStringValue(currentNode, true);
                ((FormulaPicklistInfo.Dynamic)enumInfo).collectApiValueToFetch(fieldValue);
            }
        }
    }

    static class FunctionInCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;
        private final int numArgs;
        private final boolean treatAsString;
        private final List<List<String>> targets;
    
        FunctionInCommand(FormulaCommandInfo info, int numArgs, boolean treatAsString, List<List<String>> targets) {
            super(info);
            this.numArgs = numArgs;
            this.treatAsString = treatAsString;
            this.targets = targets;
        }
    
        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
            Object[] args = new Object[numArgs];
            for (int i = 0; i < numArgs; i++) {
                args[numArgs - i - 1] = stack.pop();
            }

            Object first = args[0];
            
            // IN(...) will do the SQL thing and not have IN(NULL,NULL) return false
            if (first == null) {
                stack.push(false);
                return;
            }
            
            boolean result = false;
            for (int i = 1; i < args.length; i++) {
                Boolean check;
                Boolean useDbValOverride = context.getProperty(FormulaContext.DO_NOT_USE_DB_VALUE_FOR_PICKLIST_EVALUATION);
                if (targets == null || (useDbValOverride != null && useDbValOverride)) {
                    check = OperatorEquality.comparePointwise(first, args[i], treatAsString, false);
                } else {
                    check = FunctionIsPickVal.comparePointwise(first, targets.get(i));
                }
    
                if ((check != null) && (check)) {
                    result = true;
                    break;
                }
            }
            stack.push(result);
        }
    }
}
