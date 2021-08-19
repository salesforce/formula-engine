/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import static com.force.formula.MockFormulaDataType.*;

import java.math.BigDecimal;
import java.util.*;

import com.force.formula.*;
import com.force.formula.commands.FieldReferenceCommandInfo;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.impl.BeanFormulaContext.BeanEntity;
import com.force.formula.impl.BeanFormulaContext.BeanFormulaType;
import com.force.formula.template.commands.DynamicReference;
import com.force.formula.util.BigDecimalHelper;
import com.force.formula.util.FormulaDateUtil;

/**
 * FieldReferenceTest that includes a "bare" set of functions to validate
 * field references and null checking
 * @author stamm
 * @since 0.2.0
 */
public abstract class BaseFieldReferenceTest extends ParserTestBase {
    static final FormulaFactory TEST_FACTORY;
    static final Date EXAMPLE_DATE;
    static final String EXAMPLE_ID = "00100x000000000";
    static final BigDecimal EXAMPLE_NUM = new BigDecimal("10.41");
    static final String EXAMPLE_NAME = "FORMULA_ENGINE";
    
    protected BigDecimal accountAmountOverride;
    protected BigDecimal secondNumberOverride;
    protected BigDecimal percentOverride;
    protected boolean nullAsNull;
    
    public BaseFieldReferenceTest(String name) {
        super(name);
    }
    
    FormulaEngineHooks oldHooks = FormulaEngine.getHooks();
    FormulaFactory oldFactory = FormulaEngine.getFactory();

    @Override
    protected void setUp() throws Exception {
        oldHooks = FormulaEngine.getHooks();
        oldFactory = FormulaEngine.getFactory();
        FormulaEngine.setHooks(new FieldTestFormulaValidationHooks());
        FormulaEngine.setFactory(TEST_FACTORY);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        FormulaEngine.setHooks(oldHooks);
        FormulaEngine.setFactory(oldFactory);
    }
    
    
    @Override
    protected MockFormulaType getFormulaType() {
        return nullAsNull ? MockFormulaType.NULLASNULL : MockFormulaType.DEFAULT;
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
     * @param asZero the value that should be returned if null numbers are treated as null
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
    

    @Override
    protected BeanFormulaContext setupMockContext(FormulaDataType columnType) {
        TestAccount acc = new TestAccount(EXAMPLE_ID, EXAMPLE_DATE, EXAMPLE_NUM, EXAMPLE_NAME);
        TestContact con = new TestContact(acc, null, true);
        
        if (accountAmountOverride != null) {
            acc.setAmount(accountAmountOverride);
        }
        if (secondNumberOverride != null) {
            acc.setSecondNumber(secondNumberOverride);
        }
        if (percentOverride != null) {
            acc.setPercent(percentOverride);
        }
        
        return new BeanFormulaContext(super.setupMockContext(columnType), MockFormulaType.JAVASCRIPT, con);
    }
    
    /**
     * Extract all the values from the beans (including foreign keys) into a map
     * @param context the Bean context
     * @return a nested map from Key to Value for the object
     */
    Map<String,Object> getBeanValues(BeanFormulaContext context) throws UnsupportedTypeException, InvalidFieldReferenceException {
        Map<String,Object> result = new HashMap<>();
        Object o = context.getObject();
        if (o == null) return null;  // If the bean isn't set, don't bother filling the fields
        for (DisplayField f : context.getDisplayFields(context.getEntity())) {
            ContextualFormulaFieldInfo ffi = context.lookup(f.getFormulaFieldInfo().getName());
            if (ffi.getDataType().isId()) {
                BeanFormulaContext parentContext = (BeanFormulaContext) context.getAdditionalContext(ffi.getName());
                Map<String,Object> children = getBeanValues(parentContext);
                result.put(f.getFormulaFieldInfo().getName(), children);
            } else if (!ffi.getFieldOrColumnInfo().getFieldInfo().isCalculated()) {
                Object val = context.getObject(ffi.getName());
                if (val != null) result.put(f.getFormulaFieldInfo().getName(), val);
            }
        }
        return result;
    }
    
    static {
        // Register all of the commands defined in SFDC, and then use provider factory to get the rest.
        List<FormulaCommandInfo> types = new ArrayList<>(FormulaCommandTypeRegistryImpl.getDefaultCommands());
        types.add(new FieldReferenceCommandInfo());
        types.add(new DynamicReference());
        TEST_FACTORY = new FormulaFactoryImpl(new FormulaCommandTypeRegistryImpl(types));
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.clear();
        c.set(2004, 10, 2); // Months are zero-based in Java Calenders
        EXAMPLE_DATE = c.getTime();

    }


    
    static class FieldTestFormulaValidationHooks implements FormulaValidationHooks {

        @Override
        public FormulaDataType getDataTypeByName(String name) {
            return MockFormulaDataType.fromCamelCaseName(name);
        }
        
        @Override
        public boolean shouldLogRuntime() {
            return true;
        }

        @Override
        public List<FormulaFieldReferenceInfo> getFieldPath(ContextualFormulaFieldInfo formulaFieldInfo,
                boolean handlePersonContact) {
            List<FormulaFieldReferenceInfo> fieldPath = null;

            FormulaContext currentContext = formulaFieldInfo.getFormulaContext();
            FormulaContext parentContext = currentContext.getParentContext();
            FormulaSchema.Entity domain = null;
            while (parentContext != null) {
                domain = domain == null ? (FormulaSchema.Entity) formulaFieldInfo.getFieldOrColumnInfo().getEntityInfo() : domain;
                if (fieldPath == null) {
                    fieldPath = new ArrayList<>();
                }

                // TODO SLT: This calls parent context in core because it stores the contexts diffrently.
                if (!(parentContext instanceof BeanFormulaContext)) {
                    break;
                }
                BeanEntity parentEntity = ((BeanFormulaContext)parentContext).getEntity();
                FormulaSchema.Field parentFieldOrColumnInfo = parentEntity.getField(currentContext.getName());
                fieldPath.add(new FormulaFieldReferenceInfoImpl(parentFieldOrColumnInfo, domain));

                currentContext = parentContext;
                parentContext = parentContext.getParentContext();
                domain = parentFieldOrColumnInfo.getEntityInfo();
            }

            if (fieldPath != null) {
                Collections.reverse(fieldPath);
            }

            return fieldPath;        
         } 
    }
   
    static class TestContact {
        private TestAccount account;
        private Date createdDate;
        private boolean optIn;
        public TestContact(TestAccount account, Date createdDate, boolean optIn) {
            this.account = account;
            this.createdDate = createdDate;
            this.optIn = optIn;
        }
        
        public String getAccountId() {
            return this.account != null ? this.account.getId() : null;
        }
        public TestAccount getAccount() {
            return this.account;
        }
        public void setAccount(TestAccount account) {
            this.account = account;
        }
        public Date getCreatedDate() {
            return this.createdDate;
        }
        public void setCreatedDate(Date createdDate) {
            this.createdDate = createdDate;
        }
        public boolean isOptIn() {
            return this.optIn;
        }
        public void setOptIn(boolean optIn) {
            this.optIn = optIn;
        }
        public TestAccount getNullAccount() {
            return null;
        }
        public void setNullAccount(TestAccount nullAccount) {
        }
        public Date getNullDate() {
            return null;
        }
        public void setNullDate() {
        }
        public BigDecimal getNullNumber() {
            return null;
        }
        public void setNullNumber() {
        }
        public String getNullText() {
            return null;
        }
        public void setNullText() {
        }
        // NOTE: These would normally be on Account, but right now BeanFormulaContext evaluation doesn't support 
        // nesting of formulas.  The formulaSwitcher causes a freakout as it ends up causing a cycle.
        @BeanFormulaType(value=MockFormulaDataType.DATEONLY, formulaSource="account.createdDate+account.secondNumber")
        public Date getDateFormula() {
            throw new UnsupportedOperationException();  // The bean converter calls this, but ignore it.
        }
        @BeanFormulaType(value=MockFormulaDataType.DOUBLE, formulaSource="account.amount+account.secondNumber")
        public BigDecimal getNumberFormula() {
            throw new UnsupportedOperationException();  // The bean converter calls this, but ignore it.
        }
    }
    
    static class TestAccount {
        private String id;
        private Date createdDate;
        //private boolean isActive;
        private BigDecimal amount;
        private BigDecimal secondNumber;
        private BigDecimal percent;
        private String name;
		private TestParentAccount parent;
		private TestParentAccount nullParent;
        public TestAccount(String id, Date createdDate, BigDecimal amount, String name) {
            this.id = id;
            this.createdDate = createdDate;
            this.amount = amount;
            this.name = name;
			this.parent = new TestParentAccount(id + "_parent", name + "_parent");
        }
        public String getId() {
            return this.id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public Date getCreatedDate() {
            return this.createdDate;
        }
        public void setCreatedDate(Date createdDate) {
            this.createdDate = createdDate;
        }
        public BigDecimal getAmount() {
            return this.amount;
        }
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
        public String getNullText() {
            return null;
        }
        public void setNullText() {
        }
        public BigDecimal getSecondNumber() {
            return this.secondNumber;
        }
        public void setSecondNumber(BigDecimal secondNumber) {
            this.secondNumber = secondNumber;
        }
        @BeanFormulaType(MockFormulaDataType.PERCENT)
        public BigDecimal getPercent() {
            return this.percent;
        }
        public void setPercent(BigDecimal percent) {
            this.percent = percent;
        }
        public Date getNullDate() {
            return null;
        }
        public void setNullDate() {
        }
        public String getName() {
            return this.name;
        }
        public void setName(String name) {
            this.id = name;
        }
        public TestParentAccount getParent() {
			return this.parent;
        }
		public TestParentAccount getNullParent() {
			return this.nullParent;
		}
		public boolean getIsActive() {
			return true;
		}
    }
    static class TestParentAccount {
        private String id;
        private String name;
        //private boolean isActive;
        public TestParentAccount(String id, String name) {
            this.id = id;
            this.name = name;
        }
        public String getId() {
            return this.id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getName() {
            return this.name;
        }
        public void setName(String name) {
            this.id = name;
        }
		public boolean getIsActive() {
			return false;
		}
    }
}
