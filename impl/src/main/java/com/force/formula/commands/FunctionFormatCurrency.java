/*
 * Copyright, 1999-2007, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.commands;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Deque;

import com.force.formula.*;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.impl.*;
import com.force.formula.sql.SQLPair;
import com.force.formula.util.FormulaI18nUtils;

/**
 * Format a currency into text, using ISOCODE + Format
 * FORMATCURRENCY(isocode,amount);
 * @author swong
 * @since 154
 */
@AllowedContext(section=SelectorSection.TEXT)
public class FunctionFormatCurrency extends FormulaCommandInfoImpl {
    public FunctionFormatCurrency() {
        super("FORMATCURRENCY", String.class, new Class[] { String.class, BigDecimal.class });
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) {
        return new FunctionFormatCurrencyCommand(this);
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) {

        FormulaAST isocodeNode = (FormulaAST)node.getFirstChild();
        FormulaAST amountNode = (FormulaAST)isocodeNode.getNextSibling();
        
        // Most of the heavy lifting is in FormulaSqlHooks, as the number formatting is defined there.
    	String sql = getSqlHooks(context).getCurrencyFormat(args[0], args[1], amountNode.canBeNull());


        String guard = SQLPair.generateGuard(guards, null);
        return new SQLPair(sql.toString(), guard);
    }
    
    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
    	// This replicates the IsoCode + ' ' + Number result that is the multicurrency display standard for salesforce.  
    	String currTag = FormulaI18nUtils.getLocalizer().getCurrencyLocale().toLanguageTag();
    	return JsValue.generate("("+args[0].buildJSWithGuard()+"?new Intl.NumberFormat('"+currTag+"',{style:'currency',currency:"+args[0]+",currencyDisplay:'code'}).format("+args[1]+"):"
    				+"(' '+new Intl.NumberFormat('"+currTag+"',{minimumFractionDigits:2}).format("+args[1]+")))"  // If isoCode is null, do a number with a ' '
    				+".replace(/\\u00a0/,' ')", // NBSP differs from core, so convert to regular space
    				args, false, args[1]); // It'll be empty string or null.  Also, ignore guard on args[0] since it's already in use

    	// This is for single currency display
    	//return JsValue.forNonNullResult("new Intl.NumberFormat('"+currTag+"',{style:'currency',currency:"+args[0]+"}).format("+args[1]+")", args);
    }


    public static class FunctionFormatCurrencyCommand extends AbstractFormulaCommand {
		private static final long serialVersionUID = 1L;

	    private static final BigDecimal MAX_LONG_AS_BIG_DECIMAL = new BigDecimal(Long.MAX_VALUE);
	    private static final BigDecimal MIN_LONG_AS_BIG_DECIMAL = new BigDecimal(Long.MIN_VALUE);

		
		public FunctionFormatCurrencyCommand(FormulaCommandInfo info) {
            super(info);
        }

	    /**
	     * Helper method that throws a NumberTooBigException for big decimals that are too big to display.
	     * @param wholeNumberComponent the whole number component of the decimal
	     * @return whether the string is too big to display
	     */
	    public static boolean overflowTest(BigDecimal wholeNumberComponent) {
	        return wholeNumberComponent.compareTo(MAX_LONG_AS_BIG_DECIMAL) >= 0
	            || wholeNumberComponent.compareTo(MIN_LONG_AS_BIG_DECIMAL) < 0;
	    }

		
        @Override
        public void execute(FormulaRuntimeContext context, Deque<Object> stack) throws Exception {
            BigDecimal amt = checkNumberType(stack.pop());
            String isoCode = checkStringType(stack.pop());

            // Handle null amt
            if (amt == null) {
            	stack.push("");  // We don't want nulls in this function, which is usually for templates.
            	// This means that there is a difference in the SQL behavior and the java behavior.
                return;
            }

            // Check for overflow
            if (overflowTest(amt)) {
            	// TODO: throw NumberTooBigException instead?
                throw new FormulaEvaluationException(new NumberFormatException(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "Overflow_Exception")));
            }

            // Generate the number formatter instance
            int scale = FormulaValidationHooks.get().getCurrencyScaleForIsoCode(isoCode);
            NumberFormat nf = FormulaI18nUtils.getLocalizer().getNumberFormat(scale, true);

            // Put everything together
            StringBuilder value = new StringBuilder(30);

            if (isoCode != null) value.append(isoCode);
            value.append(' ').append(nf.format(amt));
            /*
            // Alternative implementation where single currency orgs get formatted with currency sign
        	UddOrgInfo orgInfo = ...;
            DecimalFormatSymbols orgDfs = new DecimalFormatSymbols(orgInfo.getCurrencyLocale());
            boolean useCurrencySign = !orgInfo.isMultiCurrencyEnabled()
                && (isoCode == null || orgDfs.getInternationalCurrencySymbol().equals(isoCode));
            if (useCurrencySign) {
                if (amt.signum() < 0) value.append('(');
                value.append(orgDfs.getCurrencySymbol());
            } else {
                if (isoCode != null) value.append(isoCode);
                value.append(' ');
                if (amt.signum() < 0) value.append('-');
            }
            value.append(nf.format(amt.abs()));
            if (useCurrencySign) {
                if (amt.signum() < 0) value.append(')');
            }
            */

            stack.push(value.toString());
        }
    }
}

