package com.force.formula.commands;

import java.lang.reflect.Type;
import java.util.Deque;
import java.util.Locale;

import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaException;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaTypeUtils;
import com.force.formula.impl.FormulaValidationHooks;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.impl.WrongArgumentTypeException;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.sql.SQLPair;
import com.force.i18n.LocaleUtils;

/**
 * Perform a Lower function which takes a string and truncates it
 * to the given
 *
 * @author stamm
 * @since 150
 */
@AllowedContext(section=SelectorSection.TEXT,isOffline=true)
public class FunctionLower extends FormulaCommandInfoImpl implements FormulaCommandValidator {
    public FunctionLower() {
        super("LOWER");
    }


    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionLowerCommand(this, node.getNumberOfChildren() == 2);

    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {
        String sql;
        if (node.getNumberOfChildren() == 2) {
            sql = String.format(getSqlHooks(context).sqlLowerCaseWithLocaleFormat(true), args[0], args[1]);
        } else {
            sql = String.format(getSqlHooks(context).sqlLowerCaseWithLocaleFormat(false), args[0], "'en'");
        }
        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql, guard);
    }

    @Override
    public Type validate(FormulaAST node, FormulaContext context, FormulaProperties properties) throws FormulaException {
        int kids = node.getNumberOfChildren();
        if (kids > 2 || kids == 0) {
            throw new WrongNumberOfArgumentsException(node.getText(), 1, node);
        }

        FormulaAST toConvert = (FormulaAST)node.getFirstChild();
        Type clazz = toConvert.getDataType();
        Type resultType = String.class;

        if ((clazz != ConstantNull.class) && !FormulaTypeUtils.isTypeTextUgly(clazz) && (clazz != RuntimeType.class))
            throw new WrongArgumentTypeException(node.getText(), new Type[] { clazz }, toConvert);
        if (clazz == RuntimeType.class) {
            resultType = clazz;
        }

        if (kids == 2) {
            toConvert = (FormulaAST) toConvert.getNextSibling();
            Type clazz2 = toConvert.getDataType();
            if ((clazz2 != ConstantNull.class) && !FormulaTypeUtils.isTypeTextUgly(clazz2) && (clazz2 != RuntimeType.class))
                throw new WrongArgumentTypeException(node.getText(), new Type[] { clazz, clazz2 }, toConvert);
            if (clazz2 == RuntimeType.class) {
                resultType = clazz2;
            }
        }
        return resultType;
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        if (node.getNumberOfChildren() == 2) {
            // Ignore guards[1] in js
            return JsValue.generate(args[0] + ".toLocaleLowerCase("+FunctionUpper.getLanguageTag(context, args[1])+")", args, false, args[0]);
        } else {
            return JsValue.forNonNullResult(args[0] + ".toLowerCase()", args); 
        }
    }
}

class FunctionLowerCommand extends AbstractFormulaCommand {
    private static final long serialVersionUID = 1L;
	private final boolean hasLocale;

    public FunctionLowerCommand(FormulaCommandInfo info, boolean hasLocale) {
        super(info);
        this.hasLocale = hasLocale;
    }

    @Override
    public void execute(FormulaRuntimeContext context, Deque<Object> stack) {
        String localeStr = hasLocale ? checkStringType(stack.pop()) : null;
        String target = checkStringType(stack.pop());
        if ((target == null) || (target.equals(""))) {
            stack.push(null);
        } else {
            Locale locale = LocaleUtils.get().getLocaleByIsoCode(localeStr);
            String lower;
            // Match the implementation to the sql style, because they handle double-s, turkish i differently
            if (context.getSqlStyle() != null && context.getSqlStyle().isOracleStyle()) {
                lower = FormulaValidationHooks.get().toOracleLowerCase(target, locale);
            } else {
                lower = locale == null ? target.toLowerCase() : target.toLowerCase(locale);
            }
            stack.push(lower);
        }
    }
}
