package sfdc.formula.commands;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Deque;

import com.google.common.base.Joiner;

import sfdc.formula.*;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.impl.*;

/**
 *
 * @author djacobs
 * @since 144
 */
@AllowedContext(section=SelectorSection.MATH,isOffline=true)
public class FunctionMin extends FormulaCommandInfoImpl {

    private static final int MAX_NUMBER_OF_ARGS = 255;

    public FunctionMin() {
        super("MIN", BigDecimal.class, new Class[] {});
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionMinCommand(this, node.getNumberOfChildren());
    }

    @Override
    public Class<?>[] getArgumentTypes(FormulaAST node, FormulaContext context) throws WrongNumberOfArgumentsException {
        int numberOfChildren = node.getNumberOfChildren();
        if (numberOfChildren > MAX_NUMBER_OF_ARGS) {
            throw new WrongNumberOfArgumentsException(node.getText(), MAX_NUMBER_OF_ARGS, node);
        }

        Class<?>[] argumentTypes = new Class[numberOfChildren];
        Arrays.fill(argumentTypes, BigDecimal.class);
        return argumentTypes;
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        StringBuilder sql = new StringBuilder(64);
        sql.append("LEAST(").append(args[0]);
        for (int i = 1; i < args.length; i++)
            sql.append(", ").append(args[i]);
        sql.append(")");
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql.toString(), guard);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        // TODO: doesn't work with nulls with Decimal
        /*if (context.useHighPrecisionJs()) {
            return "Decimal.min.apply(Decimal,[" + TextUtil.collectionToString(Arrays.asList(args), ",") + "].filter(Number))";
        }*/
        return JsValue.forNonNullResult(jsMathPkg(context)+".min(" + Joiner.on(',').join(args) + ")", args);
    }
    
    
}

class FunctionMinCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

	FunctionMinCommand(FormulaCommandInfo info, int numArgs) {
        super(info);
        this.numArgs = numArgs;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        BigDecimal result = checkNumberType(stack.pop());
        if (result == null) {
            stack.push(null);
            return;
        }
        for (int i = 1; i < numArgs; i++) {
            BigDecimal next = checkNumberType(stack.pop());
            if (next == null) {
                stack.push(null);
                return;
            }
            if (next.compareTo(result) < 0) {
                result = next;
            }
        }
        stack.push(result);
    }

    private final int numArgs;
}
