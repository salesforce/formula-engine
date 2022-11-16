package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDataType;
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
import com.google.common.collect.Lists;

/**
 * Implementation of CASE that relies on some code in BaseIsPicklistVal
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section=SelectorSection.LOGICAL, isOffline=true)
public class FunctionCase extends FormulaCommandInfoImpl implements FormulaCommandValidator, FormulaCommandPrefetcher, FormulaCommandOptimizer {
    public FunctionCase() {
        super("CASE");
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws InvalidFieldReferenceException,
            UnsupportedTypeException {
        return new FunctionCaseCommand(this, node.getNumberOfChildren(), treatAsString(node),
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
        int firstType = valueNode.getType();
        boolean isPicklistCase = isPicklistCase(valueNode);
        List<List<String>> targets = getTargets(node, context, true, false);
        boolean treatAsString = treatAsString(node);
        StringBuilder sql = new StringBuilder(256);

        sql.append("CASE ");
        if (treatAsString || isPicklistCase) {
            FormulaAST whenNode;
            for (int i = 1; i < args.length - 2; i += 2) {
                String condition;
                whenNode = (FormulaAST)valueNode.getNextSibling();
                if (isPicklistCase) {
                    if (targets.get(i) == null || targets.get(i).isEmpty())
                        condition = "(1=0)";
                    else
                        condition = FunctionIsPickVal.compareBulk(args[0], targets.get(i));
                } else {
                    condition = OperatorEquality.compareBulk(context, args[0], firstType, args[i], whenNode.getType(),
                            treatAsString, false);
                }
                valueNode = (FormulaAST)whenNode.getNextSibling();
                sql.append(" WHEN ").append(condition).append(" THEN ").append(wrap(args[i + 1], valueNode, node.getDataType(), context));
            }
        } else {
            sql.append(args[0]);
            for (int i = 1; i < args.length - 2; i += 2) {
                valueNode = (FormulaAST)valueNode.getNextSibling().getNextSibling();
                sql.append(" WHEN ").append(args[i]).append(" THEN ").append(wrap(args[i + 1], valueNode, node.getDataType(), context));
            }
        }
        // the else node:
        valueNode = (FormulaAST)valueNode.getNextSibling();
        sql.append(" ELSE ").append(wrap(args[args.length - 1], valueNode, node.getDataType(), context)).append(" END");

        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql.toString(), guard);
    }

    static String wrap(String expression, FormulaAST valueNode, Type resultDataType, FormulaContext context) {
        // get the args type of the value node of the passed whenNode (for ELSE pass the previous node)
        Type argType = valueNode.getDataType();
        if (argType == ConstantNull.class && resultDataType != ConstantNull.class) {
            if ((resultDataType == FormulaDateTime.class) || (resultDataType == Date.class)) {
                return getSqlHooks(context).sqlNullToDate(resultDataType);
            } else if (resultDataType == BigDecimal.class) {
                return getSqlHooks(context).sqlNullToNumber();
            } else if (resultDataType == Boolean.class) {
                return "0";     // for compare false
            } else {
                return "NULL";
            }
        }
        
        // replace empty strings ('') with NULLs - see W-5160563
        if ("''".equals(expression)) { return "NULL"; }
        
        return expression;
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        // Check that correct number of args was supplied
        int numberOfChildren = node.getNumberOfChildren();
        if (((numberOfChildren % 2) != 0) || (numberOfChildren < 4)) {
            int expectedNumberOfChildren = 4 + (((numberOfChildren - 4) / 2) * 2);
            throw new WrongNumberOfArgumentsException(node.getText(), expectedNumberOfChildren, node);
        }

        FormulaAST currentNode = (FormulaAST)node.getFirstChild();
        Type expType = currentNode.getDataType();
        if (expType == Boolean.class || expType == FormulaGeolocation.class) { throw new IllegalArgumentTypeException(node.getText()); }

        boolean isPicklistCase = isPicklistCase(currentNode);

        Type resultType = (expType == RuntimeType.class) ? expType : null;
        Type commonType = ConstantNull.class;  // commonType will be the resultType if no runtimeTyped operands

        for (int n = 1; n < numberOfChildren - 2; n += 2) {
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
                if (caseType == null)
                    throw new WrongArgumentTypeException(node.getText(), new Type[] { expType }, currentNode);

                if (caseType == RuntimeType.class) {
                    resultType = caseType;
                }
            }

            // Check the result type
            currentNode = (FormulaAST)currentNode.getNextSibling();
            Type valueType = currentNode.getDataType();
            if (valueType == Boolean.class) throw new IllegalArgumentTypeException(node.getText());
            // If we don't already know the common type, try this
            Type commonSuperType = FormulaTypeUtils.getCommonSuperType(commonType, valueType);
            if (commonSuperType == null)
                throw new WrongArgumentTypeException(node.getText(), new Type[] { commonType }, currentNode);
            commonType = commonSuperType;
            if (valueType == RuntimeType.class) {
                resultType = valueType;
            }
        }

        // Check the "else" type
        currentNode = (FormulaAST)currentNode.getNextSibling();
        Type elseType = currentNode.getDataType();
        if (elseType == Boolean.class) throw new IllegalArgumentTypeException(node.getText());
        Type commonSuperType = FormulaTypeUtils.getCommonSuperType(commonType, elseType);
        if (commonSuperType == null)
            throw new WrongArgumentTypeException(node.getText(), new Type[] { commonType }, currentNode);
        commonType = commonSuperType;
        if (elseType == RuntimeType.class) {
            resultType = elseType;
        }

        // "Inherit" the type of the value args
        return resultType == null ? commonType : resultType;
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        // TODO SLT 204 FIXME
        // The case function can be implemented as a switch, and for picklists, that will work, but
        // we allow non-literal comparisons in formulas, which is difficult to implement without constantly reevaluating
        // args[0].  Putting it in a local variable would work, but requires more javascript than I know.
        
        FormulaAST valueNode = (FormulaAST)node.getFirstChild();
        Type expType = valueNode.getDataType();
        //int firstType = valueNode.getType();
        boolean isPicklistCase = isPicklistCase(valueNode);
        List<List<String>> targets = getTargets(node, context, false, true);
        //boolean treatAsString = treatAsString(node);
        StringBuilder js = new StringBuilder(256);
        
        boolean couldBeNull = false;
        
        if (isPicklistCase) {
            js.append("(");
            FormulaAST whenNode;
            for (int i = 1; i < args.length - 2; i += 2) {
                whenNode = (FormulaAST)valueNode.getNextSibling();
                List<String> cases = targets.get(i);
                if (cases != null) {  // If the case is valid...
                    boolean hasEmpty = cases.contains("");
                    String arr = cases.stream().map(c->"'"+FormulaTextUtil.escapeForJavascriptString(c)+"'").collect(Collectors.joining(","));
                    // Match the null semantics of SQL here when matching picklists.
                    js.append("[").append(arr).append("].indexOf(").append(!hasEmpty?args[0]:jsNvl(context, args[0].js, "\"\"")).append(")>=0?");
                    valueNode = (FormulaAST)whenNode.getNextSibling();
                    js.append("(").append(args[i+1].js).append("):");
                    couldBeNull |= args[i+1].couldBeNull;
                }
            }
            //valueNode = (FormulaAST)valueNode.getNextSibling();
            js.append(args[args.length - 1]).append(")");
        } else {
            // Use ternaries
            FormulaAST whenNode;
            js.append("(");
            for (int i = 1; i < args.length - 2; i += 2) {
                //String condition;
                whenNode = (FormulaAST)valueNode.getNextSibling();
                // Make sure null != null by testings args[i]
                js.append("("+args[i] + "&&("+OperatorEquality.wrapJsForEquality(context, args[0], expType)+"=="+OperatorEquality.wrapJsForEquality(context, args[i], expType)+"))?");
                valueNode = (FormulaAST)whenNode.getNextSibling();
                js.append("(").append(args[i+1].js).append("):");
                couldBeNull |= args[i+1].couldBeNull;
            }
            //valueNode = (FormulaAST)valueNode.getNextSibling();
            js.append(args[args.length - 1]).append(")");
        }
        couldBeNull |= args[args.length - 1].couldBeNull; 
        return JsValue.generate(js.toString(), args, couldBeNull); 
    }
  
    static boolean treatAsString(FormulaAST node) {
        return FormulaTypeUtils.isTypeText(((FormulaAST)node.getFirstChild()).getDataType());
    }

    public static boolean isPicklistCase(FormulaAST node) {
        FormulaDataType columnType = node.getColumnType();
        return (columnType != null) && columnType.isPickval();
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

        if (!isPicklistCase(targetNode)) return null;

        FormulaFieldInfo formulaFieldInfo = FunctionIsPickVal.getFormulaFieldInfo(targetNode, context);

        int numberOfChildren = node.getNumberOfChildren();

        List<List<String>> targets = Lists.newArrayListWithExpectedSize(numberOfChildren);
        for (int i = 0; i < numberOfChildren; i++) {
            //fill list with nulls, so we can access each element by index later
            targets.add(null);
        }
        FormulaAST currentNode = targetNode;
        for (int i = 1; i < numberOfChildren - 2; i += 2) {
            // Get the value
            currentNode = (FormulaAST)currentNode.getNextSibling();
            String fieldValue = ConstantString.getStringValue(currentNode, true);
            targets.set(i, FormulaValidationHooks.get().getUnderlyingValuesForPicklist(formulaFieldInfo, fieldValue, context, forSql, forJs));
            // Skip the result type
            currentNode = (FormulaAST)currentNode.getNextSibling();
        }

        return targets;
    }

    @Override
    public void prefetch(FormulaAST ast, FormulaContext context) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaAST targetNode = (FormulaAST)ast.getFirstChild();

        if (!isPicklistCase(targetNode)) {
            return;
        }

        // Prefetch dynamic enum items needed for this Case function
        FormulaFieldInfo formulaFieldInfo = FunctionIsPickVal.getFormulaFieldInfo(targetNode, context);
        FormulaPicklistInfo enumInfo = formulaFieldInfo.getEnumInfo();
        if (enumInfo != null && enumInfo instanceof FormulaPicklistInfo.Dynamic) {
            FormulaAST currentNode = targetNode;
            int numberOfChildren = ast.getNumberOfChildren();
            for (int i = 1; i < numberOfChildren - 2; i += 2) {
                currentNode = (FormulaAST)currentNode.getNextSibling();
                String fieldValue = ConstantString.getStringValue(currentNode, true);
                ((FormulaPicklistInfo.Dynamic)enumInfo).collectApiValueToFetch(fieldValue);
                currentNode = (FormulaAST)currentNode.getNextSibling();
            }
        }
    }
}

class FunctionCaseCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;
    private final int numArgs;
    private final boolean treatAsString;
    private final List<List<String>> targets;

    FunctionCaseCommand(FormulaCommandInfo info, int numArgs, boolean treatAsString, List<List<String>> targets) {
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
        Object result;
        int i = 1;
        while (true) {
            Boolean check;
            Boolean useDbValOverride = context.getProperty(FormulaContext.DO_NOT_USE_DB_VALUE_FOR_PICKLIST_EVALUATION);
            if (targets == null || (useDbValOverride != null && useDbValOverride)) {
                check = OperatorEquality.comparePointwise(first, args[i], treatAsString, false);
            } else {
                check = FunctionIsPickVal.comparePointwise(first, targets.get(i));
            }

            if ((check != null) && (check)) {
                result = args[i + 1];
                break;
            }
            i += 2;
            if (i == numArgs - 1) {
                result = args[i];
                break;
            }
        }
        stack.push(result);
    }
}
