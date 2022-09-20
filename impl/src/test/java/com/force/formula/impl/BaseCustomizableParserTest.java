/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.force.formula.ContextualFormulaFieldInfo;
import com.force.formula.DisplayField;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaEngineHooks;
import com.force.formula.FormulaFactory;
import com.force.formula.FormulaFieldReferenceInfo;
import com.force.formula.FormulaGeolocation;
import com.force.formula.InvalidFieldReferenceException;
import com.force.formula.MockFormulaDataType;
import com.force.formula.MockFormulaType;
import com.force.formula.MockLocation;
import com.force.formula.ParserTestBase;
import com.force.formula.UnsupportedTypeException;
import com.force.formula.commands.BinaryMathCommandInfo;
import com.force.formula.commands.FieldReferenceCommandInfo;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.commands.FunctionAscii;
import com.force.formula.commands.FunctionAtan2;
import com.force.formula.commands.FunctionChr;
import com.force.formula.commands.FunctionDayOfYear;
import com.force.formula.commands.FunctionDistance;
import com.force.formula.commands.FunctionFormat;
import com.force.formula.commands.FunctionFormatCurrency;
import com.force.formula.commands.FunctionFormatDuration;
import com.force.formula.commands.FunctionFromUnixTime;
import com.force.formula.commands.FunctionIfError;
import com.force.formula.commands.FunctionInitCap;
import com.force.formula.commands.FunctionIsChanged;
import com.force.formula.commands.FunctionIsPickVal;
import com.force.formula.commands.FunctionIsoWeek;
import com.force.formula.commands.FunctionIsoYear;
import com.force.formula.commands.FunctionJsonPathValue;
import com.force.formula.commands.FunctionJsonValue;
import com.force.formula.commands.FunctionPi;
import com.force.formula.commands.FunctionPriorValue;
import com.force.formula.commands.FunctionTrunc;
import com.force.formula.commands.FunctionUnixTimestamp;
import com.force.formula.commands.TrigCommandInfo;
import com.force.formula.impl.BeanFormulaContext.BeanFormulaType;
import com.force.formula.impl.sql.FormulaDefaultSqlStyle;
import com.force.formula.template.commands.DynamicReference;
import com.force.formula.template.commands.FunctionTemplate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * FieldReferenceTest that includes a "bare" set of functions to validate
 * field references and null checking
 * @author stamm
 * @since 0.2.0
 */
public abstract class BaseCustomizableParserTest extends ParserTestBase {
    static final FormulaFactory TEST_FACTORY;
    static final Date EXAMPLE_DATE;
    static final String EXAMPLE_ID = "00100x000000000";
    static final BigDecimal EXAMPLE_NUM = new BigDecimal("10.41");
    static final String EXAMPLE_NAME = "FORMULA_ENGINE";
    
    protected BigDecimal accountAmountOverride;
    protected BigDecimal secondNumberOverride;
    protected BigDecimal percentOverride;
    protected boolean nullAsNull;

    public BaseCustomizableParserTest(String name) {
        this(name, new FieldTestFormulaValidationHooks());
    }

    public BaseCustomizableParserTest(String name, FieldTestFormulaValidationHooks hooks) {
        super(name);

        if(hooks == null) {
            throw new IllegalArgumentException("hooks cannot be null");
        }

        this.hooks = hooks;
    }

    FormulaEngineHooks oldHooks = FormulaEngine.getHooks();
    FormulaFactory oldFactory = FormulaEngine.getFactory();
    FieldTestFormulaValidationHooks hooks;

    @Override
    protected void setUp() throws Exception {
        oldHooks = FormulaEngine.getHooks();
        oldFactory = FormulaEngine.getFactory();
        FormulaEngine.setHooks(hooks);
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
        
        return new BeanFormulaContext(super.setupMockContext(columnType), getFormulaType(), con);
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
        types.add(new FunctionTemplate());
        types.add(new FieldReferenceCommandInfo());
        types.add(new DynamicReference());
        types.add(new FunctionIfError());
        //types.add(new FunctionIfs());
        types.add(new FunctionDistance());
        types.add(new FunctionIsChanged());
        types.add(new FunctionIsPickVal());
        types.add(new FunctionPriorValue());
        types.add(new FunctionFormat());
        types.add(new FunctionFormatCurrency());
        types.add(new FunctionUnixTimestamp());
        types.add(new FunctionFromUnixTime());
        types.add(new FunctionIsoWeek());
        types.add(new FunctionIsoYear());
        types.add(new FunctionDayOfYear());
        types.add(new FunctionInitCap());
        types.add(new FunctionChr());
        types.add(new FunctionAscii());
        types.add(new BinaryMathCommandInfo("TRUNC", new FunctionTrunc()));
        types.add(new FunctionFormatDuration());
        types.add(new TrigCommandInfo(FormulaTrigFunction.SIN));
        types.add(new TrigCommandInfo(FormulaTrigFunction.COS));
        types.add(new TrigCommandInfo(FormulaTrigFunction.TAN));
        types.add(new TrigCommandInfo(FormulaTrigFunction.ASIN));
        types.add(new TrigCommandInfo(FormulaTrigFunction.ACOS));
        types.add(new TrigCommandInfo(FormulaTrigFunction.ATAN));
        types.add(new FunctionPi());
        types.add(new FunctionAtan2());
        types.add(new FunctionJsonPathValue());
        types.add(new FunctionJsonValue());
        TEST_FACTORY = new FormulaFactoryImpl(new FormulaCommandTypeRegistryImpl(types));
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.clear();
        c.set(2004, 10, 2); // Months are zero-based in Java Calenders
        EXAMPLE_DATE = c.getTime();

    }


    
    protected static class FieldTestFormulaValidationHooks implements FormulaValidationHooks {

        @Override
        public FormulaDataType getDataTypeByName(String name) {
            return MockFormulaDataType.fromCamelCaseName(name);
        }
        
        @Override
        public boolean shouldLogRuntime() {
            return true;
        }

        @Override
		public FormulaSqlHooks getSqlStyle() {
        	return FormulaDefaultSqlStyle.POSTGRES;
		}

		@Override
        public List<FormulaFieldReferenceInfo> getFieldPath(ContextualFormulaFieldInfo formulaFieldInfo,
                boolean handlePersonContact) {
			return BeanFormulaContext.getFieldPath(formulaFieldInfo, handlePersonContact);
        }

		@Override
		public FormulaGeolocation constructGeolocation(Number latitude, Number longitude) {
			return new MockLocation(latitude, longitude);
		}

		// For testing, restrict the set of currencies to avoid JDK issues.
		@Override
		public Map<String,Integer> getCurrencyScaleByIsoCode() {
			return ImmutableSet.of("USD", "EUR", "GBP", "INR", "JPY", "KWD").stream().map(isoCode->Currency.getInstance(isoCode)).collect(
	     			Collectors.toMap(cur->cur.getCurrencyCode(), cur->cur.getDefaultFractionDigits()));
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

        public List<Object> getList() {
        	return new ArrayList<>(ImmutableList.of("First", "Second"));
        }
        public Map<String,Object> getMap() {
        	return new HashMap<>(ImmutableMap.of("Foo", new BigDecimal("1"), "Bar", new BigDecimal("2")));
        }
    }
    
    static class TestAccount {
        private String id;
        private Date createdDate;
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
        public String getCurrencyIsoCode() {
            return "USD";
        }
        public void setCurrencyIsoCode(String ignored) {
        }
    }
    static class TestParentAccount {
        private String id;
        private String name;
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