package sfdc.formula.commands;

import java.util.Arrays;
import java.util.stream.Collectors;

import java.util.Deque;

import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;
import sfdc.formula.FormulaContext;
import sfdc.formula.FormulaException;
import sfdc.formula.FormulaRuntimeContext;
import sfdc.formula.SQLPair;
import sfdc.formula.impl.FormulaAST;
import sfdc.formula.impl.JsValue;
import sfdc.formula.impl.TableAliasRegistry;
import sfdc.formula.impl.Thunk;
import sfdc.formula.parser.gen.SfdcFormulaTokenTypes;

/**
 * Describe your class here.
 *
 * @author djacobs
 * @since 140
 */
@AllowedContext(section = SelectorSection.LOGICAL, isOffline = true)
public class FunctionOr extends FormulaCommandInfoImpl implements FormulaCommandOptimizer {

    public FunctionOr() {
        super("OR", Boolean.class, new Class[0]);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new OperatorOrCommand(this, node.getNumberOfChildren());
    }

    @Override
    public Class<?>[] getArgumentTypes(FormulaAST node, FormulaContext context) {
        Class<?>[] argumentTypes = new Class[node.getNumberOfChildren()];
        Arrays.fill(argumentTypes, Boolean.class);
        return argumentTypes;
    }

    @Override
    public FormulaAST optimize(FormulaAST ast, FormulaContext context) throws FormulaException {
        FormulaAST child = (FormulaAST) ast.getFirstChild();
        while (child != null && child.getType() == SfdcFormulaTokenTypes.FALSE) {
            child = (FormulaAST) child.getNextSibling();
        }
        if (child != null && child.getType() == SfdcFormulaTokenTypes.TRUE) {
            ast = ast.replace(child);
        }
        return ast;
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards,
            TableAliasRegistry registry) {
        // Build SQL
        StringBuilder sql = new StringBuilder(64);
        sql.append("(").append(fixBooleanNull(args[0]));
        for (int i = 1; i < args.length; i++)
            sql.append(" OR ").append(fixBooleanNull(args[i]));
        sql.append(")");

        // guard = guard + "OR (NOT(" + args[i-1] + ") AND (" + guards[i];

        // Build guard
        StringBuilder guard = null;
        StringBuilder condition = null;
        StringBuilder close = new StringBuilder(64);
        /*
         * Use two passes here in order to move guarded expressions before unguarded
         * expressions to reduce the chance to hit a bug with Oracle short-circuiting of
         * boolean expressions.
         *
         * See bug W-600480 for a longer discussion.
         */
        for (int pass = 1; pass <= 2; pass++) {
            for (int i = 0; i < args.length; i++) {
                if (guards[i] != null && pass == 1) {
                    if ((guard == null) && (condition == null)) {
                        // guards[i] != null, guard == null, condition == null
                        guard = new StringBuilder(guards[i]);
                    } else if (condition == null) {
                        // guards[i] != null, guard != null, condition == null
                        guard.append("OR ").append(guards[i]);
                    } else if (guard == null) {
                        // guards[i] != null, guard == null, condition != null
                        guard = new StringBuilder("(NOT(").append(condition).append(") AND (").append(guards[i]);
                        close.append("))");
                    } else {
                        // guards[i] != null, guard != null, condition != null
                        guard.append(" OR (NOT(").append(condition).append(") AND (").append(guards[i]);
                        close.append("))");
                    }
                    condition = new StringBuilder(fixBooleanNull(args[i]));
                }
                if (guards[i] == null && pass == 2) {
                    // guards[i] == null
                    if (condition == null)
                        condition = new StringBuilder(64);
                    else
                        condition.append(" OR ");
                    condition.append(fixBooleanNull(args[i]));
                }
            }
        }
        if (guard != null)
            guard.append(close);
        return new SQLPair(sql.toString(), guard != null ? guard.toString() : null);
    }

    /**
     * Handles special case boolean NULLs, replacing with valid boolean SQL: (1=0)
     * It would be slightly better would be for
     * {@link #optimize(FormulaAST, FormulaContext)} to optimize away this case, but
     * it's tough-to-test tree rebuilding code, and, anyway, we shouldn't be relying
     * on optimize in order to generate valid SQL. More ideally, this would be
     * statically validated / not allowed (for example, if(null, 0, 1) is caught,
     * but if(null || null, 0, 1) isn't, but customers already have formula using
     * this...
     */
    public static String fixBooleanNull(String str) {
        if (str != null && str.equals("NULL"))
            return "(1=0)";
        return str;
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        return new JsValue(jsNvl(Arrays.asList(args).stream().map((a) -> a.buildJSWithGuard())
                .collect(Collectors.joining(")||(", "(", ")")), "false"), null, false);
    }
}

class OperatorOrCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;

    OperatorOrCommand(FormulaCommandInfo info, int numArgs) {
        super(info);
        this.numArgs = numArgs;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
        Thunk[] args = new Thunk[numArgs];
        for (int i = 0; i < numArgs; i++)
            args[numArgs - i - 1] = (Thunk) stack.pop();
        for (int i = 0; i < numArgs; i++) {
            args[i].executeReally(context, stack);
            Boolean result = checkBooleanType(stack.pop());
            if (result != null && result.booleanValue()) {
                stack.push(Boolean.TRUE);
                return;
            }
        }
        stack.push(Boolean.FALSE);
    }

    private final int numArgs;
}
