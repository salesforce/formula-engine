/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import static com.force.formula.MockFormulaDataType.BOOLEAN;
import static com.force.formula.MockFormulaDataType.DATEONLY;
import static com.force.formula.MockFormulaDataType.DOUBLE;
import static com.force.formula.MockFormulaDataType.TEXT;

import java.math.BigDecimal;
import java.util.Date;

import com.force.formula.MockFormulaDataType;
import com.force.formula.util.BigDecimalHelper;
import com.force.formula.util.FormulaDateUtil;

/**
 * FieldReferenceTest that includes a "bare" set of functions to validate
 * field references and null checking
 * @author stamm
 * @since 0.2.0
 */
public abstract class BaseFieldReferenceTest extends BaseCustomizableParserTest {
    public BaseFieldReferenceTest(String name) {
        super(name);
    }    

    /**
     * Validate some functions that reference non-null values to test functionality
     * @throws Exception
     */
    public void testFieldReference() throws Exception {
        // This is Jan1 1900 GMT
        //
        assertEquals(-2208988800000L, evaluateDateTime("$System.OriginDateTime").getTime());
        assertEquals(-2208988800000L, evaluateDateTime("$system.origindatetime").getTime());

        assertEquals(EXAMPLE_DATE, evaluateDate("Account.CreatedDate"));

        assertEquals(EXAMPLE_ID, evaluateString("AccountId"));
        // Test of javascript comparisons
        assertEquals(new BigDecimal("300"), evaluateBigDecimal("CASE(CreatedDate,NULL,200,300)"));
        assertFalse(evaluateBoolean("NOT(OptIn)"));
        assertEquals(new BigDecimal("20.41"), evaluateBigDecimal("10+Account.Amount"));
        assertEquals(new BigDecimal("1.041"), evaluateBigDecimal("Account.Amount/10"));
        assertEquals(new BigDecimal("0.41"), evaluateBigDecimal("MOD(Account.Amount,10)"));
        assertEquals(new BigDecimal("108.3681"), evaluateBigDecimal("Account.Amount^2"));
        assertTrue(evaluateBigDecimal("abs(log(sqrt(Account.Amount)))").toString().startsWith("0.508725")); // Differences in precision
        assertEquals(new BigDecimal("108"), evaluateBigDecimal("round(Account.Amount^2,0)"));
        assertEquals(new BigDecimal("108"), evaluateBigDecimal("mfloor(Account.Amount^2)"));
        assertEquals(new BigDecimal("108"), evaluateBigDecimal("mceiling(mfloor(Account.Amount^2))"));
        assertEquals(new BigDecimal("109"), evaluateBigDecimal("mfloor(mceiling(Account.Amount^2))"));
        assertEquals(new BigDecimal("108"), evaluateBigDecimal("ceiling(floor(Account.Amount^2))"));
        assertEquals(new BigDecimal("109"), evaluateBigDecimal("floor(ceiling(Account.Amount^2))"));
        assertEquals(new BigDecimal("0"), evaluateBigDecimal("blankvalue(Account.Percent,Account.Amount)"));
    }
    
    /**
     * Validate formulas referencing fields with logical equality.
     * @throws Exception
     */
    public void testEqualityFieldReference() throws Exception {
        assertEquals(Boolean.TRUE, evaluateBoolean("Account.Name=\"FORMULA_ENGINE\" || Account.Amount=Account.Amount^2"));
        assertEquals(Boolean.TRUE, evaluateBoolean("OR(CONTAINS(Account.Name, \"FORMULA\")) || OR(CONTAINS(Account.Parent.Name, \"NONE\"))"));
		assertEquals(Boolean.FALSE, evaluateBoolean("OR(CONTAINS(Account.Name, \"ABC\")) || OR(CONTAINS(Account.nullParent.Name, \"NONE\"))"));
		assertEquals(Boolean.TRUE, evaluateBoolean("OR(CONTAINS(Account.Name, \"FORMULA\")) || OR(CONTAINS(Account.nullParent.Name, \"NONE\"))"));

		assertEquals(Boolean.FALSE, evaluateBoolean("Account.Name = \"FORMULA\" || Account.Parent.Name = \"NONE\""));
		assertEquals(Boolean.TRUE, evaluateBoolean("Account.Name = \"FORMULA_ENGINE\" || Account.Parent.Name = \"NONE\""));
		assertEquals(Boolean.TRUE, evaluateBoolean("Account.Name = \"FORMULA\" || Account.Parent.Name = \"FORMULA_ENGINE_parent\""));
		assertEquals(Boolean.TRUE, evaluateBoolean("Account.nullParent.Name = \"FORMULA_ENGINE_parent\" || Account.Parent.Name = \"FORMULA_ENGINE_parent\""));

		assertEquals(Boolean.FALSE, evaluateBoolean("OR((Account.Name = \"FORMULA\"), (Account.Parent.Name = \"NONE\"))"));
		assertEquals(Boolean.TRUE, evaluateBoolean("OR((Account.Name = \"FORMULA_ENGINE\"), (Account.Parent.Name = \"NONE\"))"));
		assertEquals(Boolean.TRUE, evaluateBoolean("OR((Account.Name = \"FORMULA\"), (Account.Parent.Name = \"FORMULA_ENGINE_parent\"))"));

		assertEquals(Boolean.FALSE, evaluateBoolean("Account.nullParent.isActive"));
		assertEquals(Boolean.TRUE, evaluateBoolean("Account.isActive || Account.Parent.isActive"));
		assertEquals(Boolean.FALSE, evaluateBoolean("Account.Parent.isActive || Account.nullParent.isActive"));
		assertEquals(Boolean.TRUE, evaluateBoolean("Account.isActive || Account.nullParent.isActive"));
    }
    
    public void testNumericValues() throws Exception {
        secondNumberOverride = new BigDecimal("-0.00034");
        percentOverride = new BigDecimal("10");
        try {
            secondNumberOverride = new BigDecimal("-0.00034");
            assertEquals(BigDecimal.ONE, evaluateBigDecimal("abs(ceiling(Account.SecondNumber))"));
            Date plus20 = FormulaDateUtil.addDurationToDate(true, EXAMPLE_DATE, new BigDecimal(20), false);
            assertEquals(plus20, evaluateDate("(DateFormula+Account.Percent+NumberFormula+Account.Amount+LEN(NullText))"));
        } finally {
            secondNumberOverride = null;
            percentOverride = null;
        }
    }
    
    /**
     * Helper function for validating formulas with zero and null handling being different, assuming when asNull, it's null
     * @param type the result type of the formula
     * @param formula the formula source to evaluate 
     * @param asZero the value that should be returned if null numbers are treated as zero
     * @throws Exception
     */
    protected void validateFieldReferences(MockFormulaDataType type, String formula, Object asZero) throws Exception {
        validateFieldReferences(type, formula, asZero, null);
    }
    
    /**
     * Helper function for validating formulas with zero and null handling being different.  
     * @param type the result type of the formula
     * @param formula the formula source to evaluate 
     * @param asZero the value that should be returned if null numbers are treated as zero
     * @param asNull the value that should be returned if null numbers are treated as null
     */
    protected void validateFieldReferences(MockFormulaDataType type, String formula, Object asZero, Object asNull) throws Exception {
        Object value = null;
        switch (type) {
        case BOOLEAN:
            value = evaluateBoolean(formula); break;
        case DATEONLY:
            value = evaluateDate(formula); break;
        case DATETIME:
            value = evaluateDateTime(formula); break;
        case DOUBLE:
            value = evaluateBigDecimal(formula); break;
        case TEXT:
            value = evaluateString(formula); break;
        default:
        }
        
        Object expected = nullAsNull ? asNull : asZero;
        if (type == DOUBLE && expected != null && value != null) {
            // Trailing zeros are annoying for comparison
            assertEquals("Formula mismatch: " + formula, BigDecimalHelper.formatBigDecimal((BigDecimal)expected), BigDecimalHelper.formatBigDecimal((BigDecimal)value));
        } else {
            assertEquals("Formula mismatch: " + formula, expected, value);
        }
    }

    public void _testNullHandling() throws Exception {
        validateFieldReferences(DOUBLE, "NullAccount.Amount", BigDecimal.ZERO);
        validateFieldReferences(DOUBLE, "11+NullAccount.Amount", new BigDecimal("11"));
        validateFieldReferences(DATEONLY, "11+Account.NullDate", null);
        validateFieldReferences(DATEONLY, "Account.NullNumber+Account.NullDate", null);
        validateFieldReferences(DATEONLY, "Account.NullDate-Account.NullNumber", null);
        validateFieldReferences(DOUBLE, "Account.Amount*Account.NullNumber", BigDecimal.ZERO);
        validateFieldReferences(DOUBLE, "NullAccount.Amount*Account.Amount", BigDecimal.ZERO);
        validateFieldReferences(DOUBLE, "NullAccount.Amount/Account.Amount", BigDecimal.ZERO);
        validateFieldReferences(DOUBLE, "Mod(NullAccount.Amount,Account.Amount)", BigDecimal.ZERO);
        validateFieldReferences(DATEONLY, "DATETIMEVALUE(Account.NullText)-Account.Amount", null);
        validateFieldReferences(DOUBLE, "abs(log(sqrt(NullAccount.Amount+1)))", BigDecimal.ZERO);
        validateFieldReferences(BOOLEAN, "IsNull(10+Account.NullDate)", Boolean.TRUE, Boolean.TRUE);
        validateFieldReferences(BOOLEAN, "IsNull(Account.Amount)", Boolean.FALSE, Boolean.FALSE);
        validateFieldReferences(DOUBLE, "round(Account.Amount^2,NullAccount.Amount)", new BigDecimal("108"));
        validateFieldReferences(DOUBLE, "floor(ceiling(NullAccount.Amount^2))", BigDecimal.ZERO);
        validateFieldReferences(DOUBLE, "mfloor(mceiling(NullAccount.Amount^2))", BigDecimal.ZERO);
        validateFieldReferences(DOUBLE, "round(LEN(NullAccount.NullText)+11,2)", new BigDecimal("11"), new BigDecimal("11"));
        validateFieldReferences(TEXT, "LOWER(Account.NullText,NullAccount.NullText)", null);
        validateFieldReferences(TEXT, "UPPER(Account.NullText,NullAccount.NullText)", null);
        validateFieldReferences(DOUBLE, "ABS(CEILING(Account.Percent))", BigDecimal.ZERO);
        validateFieldReferences(DOUBLE, "ABS(Account.Percent+24)", new BigDecimal("24"));
        validateFieldReferences(DOUBLE, "ABS(NullAccount.Amount)", BigDecimal.ZERO);

        // This one is the strange one that shows the difference between NVL and BlankValue
        validateFieldReferences(DOUBLE, "blankvalue(Account.Percent,Account.Amount)", BigDecimal.ZERO, new BigDecimal("10.41"));

        Date plus20 = FormulaDateUtil.addDurationToDate(true, EXAMPLE_DATE, new BigDecimal(20), false);
        validateFieldReferences(DATEONLY, "(DateFormula+Account.Percent+NumberFormula+Account.Amount+LEN(NullText))", plus20);

        // Validate FIND with different versions of null & ""
        validateFieldReferences(DOUBLE, "FIND(NullAccount.NullText,\"foo\")", BigDecimal.ZERO, BigDecimal.ZERO);
        validateFieldReferences(DOUBLE, "FIND(\"foo\",NullAccount.NullText)", BigDecimal.ZERO, BigDecimal.ZERO);
        validateFieldReferences(DOUBLE, "FIND(NullAccount.NullText,NullAccount.NullText)", BigDecimal.ZERO, BigDecimal.ZERO);
        validateFieldReferences(DOUBLE, "FIND(NullAccount.NullText,\"\")", BigDecimal.ZERO, BigDecimal.ZERO);
        validateFieldReferences(DOUBLE, "FIND(\"\",NullAccount.NullText)", BigDecimal.ZERO, BigDecimal.ZERO);

        
        // TODO: The next one causes an error in Java and a non-error in Javascript
        //validateFieldReferences(DATEONLY, "DATE(2016,11,NullAccount.Amount)+Account.Amount", null);
    }
    
    /**
     * This is used to validate that the NPE exception handling works appropriately
     * @throws Exception
     */
    public void testNullAsNullHandling() throws Exception {
        nullAsNull= true;
        try {
            _testNullHandling();
        } finally {
            nullAsNull = false;
        }
    }
    
    // Validate the null as zero handling.
    public void testNullAsZeroHandling() throws Exception {
        _testNullHandling();
    }
   
}
