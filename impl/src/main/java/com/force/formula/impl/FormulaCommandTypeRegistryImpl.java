package com.force.formula.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.force.formula.FormulaCommandType;
import com.force.formula.FormulaCommandTypeRegistry;
import com.force.formula.commands.BinaryMathCommandInfo;
import com.force.formula.commands.ConstantFalse;
import com.force.formula.commands.ConstantNull;
import com.force.formula.commands.ConstantNumber;
import com.force.formula.commands.ConstantString;
import com.force.formula.commands.ConstantTemplateString;
import com.force.formula.commands.ConstantTrue;
import com.force.formula.commands.ConvertCurrencyToNumberCommandInfo;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.commands.FunctionAbsoluteValue;
import com.force.formula.commands.FunctionAddMonths;
import com.force.formula.commands.FunctionAnd;
import com.force.formula.commands.FunctionBegins;
import com.force.formula.commands.FunctionBlankValue;
import com.force.formula.commands.FunctionCase;
import com.force.formula.commands.FunctionCeiling;
import com.force.formula.commands.FunctionContains;
import com.force.formula.commands.FunctionDate;
import com.force.formula.commands.FunctionDateValue;
import com.force.formula.commands.FunctionDatetimeValue;
import com.force.formula.commands.FunctionDay;
import com.force.formula.commands.FunctionExponent;
import com.force.formula.commands.FunctionFind;
import com.force.formula.commands.FunctionFloor;
import com.force.formula.commands.FunctionGeolocation;
import com.force.formula.commands.FunctionHour;
import com.force.formula.commands.FunctionIf;
import com.force.formula.commands.FunctionIsBlank;
import com.force.formula.commands.FunctionIsClone;
import com.force.formula.commands.FunctionIsNew;
import com.force.formula.commands.FunctionIsNull;
import com.force.formula.commands.FunctionLeft;
import com.force.formula.commands.FunctionLen;
import com.force.formula.commands.FunctionLog;
import com.force.formula.commands.FunctionLower;
import com.force.formula.commands.FunctionLpad;
import com.force.formula.commands.FunctionMCeiling;
import com.force.formula.commands.FunctionMFloor;
import com.force.formula.commands.FunctionMax;
import com.force.formula.commands.FunctionMid;
import com.force.formula.commands.FunctionMillisecond;
import com.force.formula.commands.FunctionMin;
import com.force.formula.commands.FunctionMinute;
import com.force.formula.commands.FunctionMod;
import com.force.formula.commands.FunctionMonth;
import com.force.formula.commands.FunctionNaturalLog;
import com.force.formula.commands.FunctionNow;
import com.force.formula.commands.FunctionNullValue;
import com.force.formula.commands.FunctionOr;
import com.force.formula.commands.FunctionReverse;
import com.force.formula.commands.FunctionRight;
import com.force.formula.commands.FunctionRound;
import com.force.formula.commands.FunctionRpad;
import com.force.formula.commands.FunctionSecond;
import com.force.formula.commands.FunctionSquareRoot;
import com.force.formula.commands.FunctionSubstitute;
import com.force.formula.commands.FunctionSubstring;
import com.force.formula.commands.FunctionText;
import com.force.formula.commands.FunctionTimeNow;
import com.force.formula.commands.FunctionTimeValue;
import com.force.formula.commands.FunctionToday;
import com.force.formula.commands.FunctionTrim;
import com.force.formula.commands.FunctionUpper;
import com.force.formula.commands.FunctionValue;
import com.force.formula.commands.FunctionWeekday;
import com.force.formula.commands.FunctionYear;
import com.force.formula.commands.OperatorAddOrSubtract;
import com.force.formula.commands.OperatorComparison;
import com.force.formula.commands.OperatorConcat;
import com.force.formula.commands.OperatorDivide;
import com.force.formula.commands.OperatorEquality;
import com.force.formula.commands.OperatorMultiply;
import com.force.formula.commands.OperatorNot;
import com.force.formula.commands.OperatorPower;
import com.force.formula.commands.UnaryMathCommandInfo;
import com.force.formula.template.commands.FunctionHtmlEncode;
import com.force.formula.template.commands.FunctionIsNumber;
import com.force.formula.template.commands.FunctionJSEncode;
import com.force.formula.template.commands.FunctionRegex;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * Default set of formula commands.  You don't need to have all of these, but you probably should.
 * 
 * @author stamm
 * @since 0.0.1
 */
public class FormulaCommandTypeRegistryImpl implements FormulaCommandTypeRegistry {
	// Types that are used to represent specific functions.  
    private final Map<String, FormulaCommandInfo> commandInfosByName;
    private final List<FormulaCommandInfo> commandInfos;
	
    /**
     * Construct a FormulaCommandTypeRegistry with the default commands 
     */
	public FormulaCommandTypeRegistryImpl() {
		this(getDefaultCommands());
	}

	/**
     * Construct a FormulaCommandTypeRegistry with the specified set of commands an
	 * @param commands a set of commands to use for this registry.
	 */
	public FormulaCommandTypeRegistryImpl(List<FormulaCommandInfo> commands) {
		this.commandInfos = ImmutableList.copyOf(commands);
		this.commandInfosByName = Maps.uniqueIndex(commands, (a)->a.getName());
	}
	
	@Override
	public List<? extends FormulaCommandType> getCommands() {
		return commandInfos;
	}

	@Override
    public FormulaCommandInfo getAllowNull(String name) {
        return commandInfosByName.get(Ascii.toUpperCase(name));
    }

    
	private static final List<FormulaCommandInfo> DEFAULT_COMMANDS;
	public static List<FormulaCommandInfo> getDefaultCommands() {
		return DEFAULT_COMMANDS;
	}
	
    static {
        // Register all of the commands
    	List<FormulaCommandInfo> types = new ArrayList<>();
        types.add(new ConstantNumber());
        types.add(new ConstantString("STRING_LITERAL", true));
        types.add(new ConstantString("NOUNESCAPE_STRING_LITERAL", false));
        types.add(new ConstantTemplateString());

        types.add(new ConstantTrue());
        types.add(new ConstantFalse());
        types.add(new ConstantNull());

        types.add(new OperatorAddOrSubtract(true));
        types.add(new OperatorAddOrSubtract(false));

        types.add(new BinaryMathCommandInfo("*", new OperatorMultiply()));
        types.add(new BinaryMathCommandInfo("/", new OperatorDivide()));

        types.add(new BinaryMathCommandInfo("MOD", new FunctionMod()));
        types.add(new FunctionMax());
        types.add(new FunctionMin());
        types.add(new BinaryMathCommandInfo("^", new OperatorPower()));
        types.add(new BinaryMathCommandInfo("ROUND", new FunctionRound()));
        types.add(new FunctionGeolocation());

        types.add(new FunctionIsNumber());
        types.add(new FunctionRegex());

        types.add(new OperatorEquality("="));
        types.add(new OperatorEquality("<>"));
        types.add(new OperatorComparison("<"));
        types.add(new OperatorComparison(">"));
        types.add(new OperatorComparison("<="));
        types.add(new OperatorComparison(">="));
        types.add(new FunctionIsNull());
        types.add(new FunctionIsBlank());

        types.add(new FunctionAnd());
        types.add(new OperatorNot());
        types.add(new FunctionOr());

        types.add(new UnaryMathCommandInfo("ABS", new FunctionAbsoluteValue()));
        types.add(new UnaryMathCommandInfo("SQRT", new FunctionSquareRoot()));

        types.add(new UnaryMathCommandInfo("CEILING", new FunctionCeiling()));
        types.add(new UnaryMathCommandInfo("FLOOR", new FunctionFloor()));
        types.add(new UnaryMathCommandInfo("MCEILING", new FunctionMCeiling()));
        types.add(new UnaryMathCommandInfo("MFLOOR", new FunctionMFloor()));
        types.add(new UnaryMathCommandInfo("LOG", new FunctionLog()));
        types.add(new UnaryMathCommandInfo("EXP", new FunctionExponent()));
        types.add(new UnaryMathCommandInfo("LN", new FunctionNaturalLog()));

        types.add(new FunctionText());
        types.add(new FunctionValue());
        types.add(new OperatorConcat());
        types.add(new FunctionLen());
        types.add(new FunctionLeft());
        types.add(new FunctionMid());
        types.add(new FunctionRight());
        types.add(new FunctionBegins());
        types.add(new FunctionContains());
        types.add(new FunctionSubstitute());
        types.add(new FunctionTrim());
        types.add(new FunctionHtmlEncode());
        types.add(new FunctionJSEncode());
        types.add(new FunctionSubstring());
        types.add(new FunctionReverse());

        types.add(new FunctionIf());
        types.add(new FunctionCase());
        types.add(new FunctionNullValue());
        types.add(new FunctionBlankValue());

        types.add(new FunctionDate());
        types.add(new FunctionDay());
        types.add(new FunctionMonth());
        types.add(new FunctionYear());
        types.add(new FunctionToday());
        types.add(new FunctionNow());
        types.add(new FunctionTimeNow());
        types.add(new FunctionTimeValue());
        types.add(new FunctionHour());
        types.add(new FunctionMinute());
        types.add(new FunctionSecond());
        types.add(new FunctionMillisecond());
        types.add(new FunctionAddMonths());
        types.add(new FunctionWeekday());

        types.add(new FunctionLpad());
        types.add(new FunctionRpad());
        types.add(new FunctionUpper());
        types.add(new FunctionLower());

        types.add(new ConvertCurrencyToNumberCommandInfo());
        types.add(new FunctionFind());
        types.add(new FunctionDateValue());
        types.add(new FunctionDatetimeValue());
        types.add(new FunctionIsNew());
        types.add(new FunctionIsClone());

        DEFAULT_COMMANDS = ImmutableList.copyOf(types);
    }

	
}
