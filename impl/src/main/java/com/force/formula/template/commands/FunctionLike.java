package com.force.formula.template.commands;

import java.lang.reflect.Type;
import java.util.Deque;
import java.util.regex.Pattern;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaException;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.commands.AbstractFormulaCommand;
import com.force.formula.commands.ConstantString;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.commands.FormulaCommandInfoImpl;
import com.force.formula.commands.FormulaCommandValidator;
import com.force.formula.commands.RuntimeType;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaTypeUtils;
import com.force.formula.impl.IllegalArgumentTypeException;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.RegexTooComplicatedException;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.SQLPair;
import com.force.formula.template.commands.AccessCountedCharSequence.AccessCountExceededException;

/** 
 * LIKE(Text, LikeText)
 * Returns TRUE if Text matches the SQL Like expression LikeText. Otherwise, it returns FALSE.
 * Matches the entire string of Text, not just part, and should be case sensitive where possible.
 */
@AllowedContext(section=SelectorSection.TEXT)
public class FunctionLike extends FormulaCommandInfoImpl implements FormulaCommandValidator {
    public FunctionLike() {
        super("LIKE");
    }

    @Override
    public Class<?> validate(FormulaAST node, FormulaContext context, FormulaProperties properties)
        throws FormulaException {
        if (node.getNumberOfChildren() != 2) {
            throw new WrongNumberOfArgumentsException(node.getText(), 2, node);
        }

        FormulaAST firstArg = (FormulaAST)node.getFirstChild();
        FormulaAST secondArg = (FormulaAST)firstArg.getNextSibling();
        Type firstType = firstArg.getDataType();
        Type secondType = secondArg.getDataType();

        if ( (!FormulaTypeUtils.isTypeText(firstType) && firstType != RuntimeType.class)
                || (!FormulaTypeUtils.isTypeText(secondType) && secondType != RuntimeType.class)) {
            throw new IllegalArgumentTypeException(node.getText());
        }

        if (firstType == RuntimeType.class || secondType == RuntimeType.class) {
            return RuntimeType.class;
        }

        return Boolean.class;
    }
    
    public static Pattern getPatternFromLike(String like) {
        String converted = like.replaceAll("\\\\%", "\t")  // Get the escaped version
                .replaceAll("\\\\_", "\f")
                .replaceAll("([*?+^$(){}\\[])", "\\\\$1")  // Replace REGEX literals
                .replaceAll("_", ".")  // Switch Percent/Underscore to regex 
                .replaceAll("%", ".*")
                .replaceAll("\f", "_")  // switch back to literals
                .replaceAll("\t", "%");
        return Pattern.compile(converted);
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws FormulaException {
        Pattern p = null;
        FormulaAST regexNode = (FormulaAST)node.getFirstChild().getNextSibling();
        if (regexNode.getType() == FormulaTokenTypes.STRING_LITERAL) {
            p = getPatternFromLike(ConstantString.getStringValue(regexNode, true));
        }
        return new FunctionLikeCommand(this, p);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry)
        throws FormulaException {
        String sql = getSqlHooks(context).sqlLike(args[0], args[1]);
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        // If the regexp is null, it should be empty string test (i.e. '^$').  
        String likeStr = args[1].js;
        likeStr = likeStr + ".replaceAll(/\\\\%/g,'\\t').replaceAll(/\\\\_/g,'\\f')"
                + ".replaceAll(/([*?+^$(){}\\[])/g,'\\\\$1')"
                + ".replaceAll(/_/g,'.').replaceAll(/%/g,'.*')"
                + ".replaceAll(/\\f/g,'_').replaceAll(/\\t/g,'%')";
        String regex = FormulaCommandInfoImpl.jsNvl2(args[1], "'^'+"+likeStr+"+'$'", "'^$'");
        String js = "new RegExp("+regex+").test("+FormulaCommandInfoImpl.jsNvl(args[0].js, "''")+")";
        return JsValue.generate(js, args, false);
    }


    static class FunctionLikeCommand extends AbstractFormulaCommand {
        private static final long serialVersionUID = 1L;
        private final Pattern constantPattern;

        public FunctionLikeCommand(FormulaCommandInfo info, Pattern pattern) {
            super(info);
            this.constantPattern = pattern;
        }

        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws FormulaException {
            Object regex = stack.pop();
            Object input = stack.pop();
            input = (input == null) ? "" : input;
            regex = (regex == null) ? "" : regex;

            try {
                // use constant pattern if available
                Pattern p = constantPattern;
                if (p == null) {
                    // otherwise compile
                    p = getPatternFromLike(checkStringType(regex));
                }
                stack.push(p.matcher((new AccessCountedCharSequence(checkStringType(input), FunctionRegex.FORMULA_LIMIT))).matches());
            } catch(AccessCountExceededException x) {
                throw new RegexTooComplicatedException((String)regex); // NOPMD
            }
        }
    }
}
