/**
 * 
 */
package com.force.formula.commands;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.force.formula.*;
import com.force.formula.FormulaSchema.Entity;
import com.force.formula.MockFormulaPicklistInfo.Item;
import com.force.formula.impl.*;
import com.force.formula.impl.BeanFormulaContext.BeanFormulaType;
import com.force.formula.impl.sql.FormulaDefaultSqlStyle;
import com.force.formula.sql.SQLPair;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * Test of IsPickVal
 * @author stamm
 */
public class FormulaPickValTest extends BaseCustomizableParserTest {

	public FormulaPickValTest(String name) {
        super(name, new FormulaPicklistValTestFormulaValidationHooks()) ;
	}

    @Test
    public void testIsPickVal() throws Exception {
        parseTest("ispickval(pickval__c, \"Mr\")", " ( ispickval pickval__c \"Mr\" )");
        assertTrue(evaluateBoolean("ispickval(type, \"Apple\")"));
        assertFalse(evaluateBoolean("ispickval(type, \"Pear\")"));
        assertFalse(evaluateBoolean("ispickval(type, \"Banana\")"));
    }

    @Test
    public void testPickvalCase() throws Exception {
        assertEquals("Red", evaluateString("CASE(type, \"Apple\", \"Red\", \"Banana\", \"Yellow\", \"Other\")"));
        assertEquals("Other", evaluateString("CASE(status, \"Open\", \"Green\", \"Closed\", \"Red\", \"Other\")"));
    }

    @Test
    public void testText() throws Exception {
    	// There's some optimization to convert text to ispickval
        assertTrue(evaluateBoolean("TEXT(type) = \"Apple\""));
        assertFalse(evaluateBoolean("TEXT(type) <> \"Apple\""));
        assertFalse(evaluateBoolean("TEXT(type) = \"Pear\""));
        assertFalse(evaluateBoolean("\"Banana\" = TEXT(type)"));
        assertTrue(evaluateBoolean("\"Banana\" <> TEXT(type)"));
        assertFalse(evaluateBoolean("TEXT(status) = \"Open\""));
    }

    @Test
    public void testTextPicklistConversionFail() throws Exception {
    	// Formula type is dynamic and TextPicklistConversion is false
    	try {
    		evaluateBoolean("TEXT(type) = \"Apple\"");
    		fail();
    	} catch (WrongArgumentTypeException e) {
    		assertTrue(e.getMessage(), e.getMessage().contains("Incorrect parameter type for TEXT."));
    	}
    }
    
    @Override
    protected MockFormulaType getFormulaType() {
    	if ("testTextPicklistConversionFail".equals(getName())) {
    		return MockFormulaType.DYNAMIC;  // Make sure it fails
    	}
    	return super.getFormulaType();
    }
    
    @Override
    protected BeanFormulaContext setupMockContext(FormulaDataType columnType) {
    	MockPicklistData type = new MockPicklistData(TestPickval.typeInfo(), "A");
    	MockPicklistData status = new MockPicklistData(TestPickval.staticInfo(), null);
    	TestPickval pick = new TestPickval(type, status);
        
        return new BeanFormulaContext(new MockFormulaContext(getFormulaType(), columnType), getFormulaType(), pick);
    }
 
    

    public static class TestPickval {
        private MockPicklistData type;
        private MockPicklistData status;
        
        private static final MockFormulaPicklistInfo typeInfo = new MockFormulaPicklistInfo(ImmutableMap.of("A", "Apple", "B", "Banana"));
        private static final MockFormulaPicklistInfo staticInfo = new MockFormulaPicklistInfo(ImmutableMap.of("1", "Open", "2", "Closed"));
        
        
        public TestPickval(MockPicklistData type, MockPicklistData status) {
        	this.type = type;
        	this.status = status;
        }
        @BeanFormulaType(value=MockFormulaDataType.STATICENUM, picklistInfoMethodName = "typeInfo")
        public MockPicklistData getType() {
        	return this.type;
        }
        public static MockFormulaPicklistInfo typeInfo() {
        	return typeInfo;
        }
        
        @BeanFormulaType(value=MockFormulaDataType.STATICENUM, picklistInfoMethodName = "staticInfo")
        public MockPicklistData getStatus() {
        	return this.status;
        }
        public static MockFormulaPicklistInfo staticInfo() {
        	return staticInfo;
        }
     }

    
    static class FormulaPicklistValTestFormulaValidationHooks extends BaseCustomizableParserTest.FieldTestFormulaValidationHooks {

        @Override
		public FormulaSqlHooks getSqlStyle() {
        	return FormulaDefaultSqlStyle.ORACLE;
		}

		@Override
		public SQLPair getPicklistSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards,
				TableAliasRegistry registry) throws FormulaException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Class<?> getPicklistType() {
			return MockPicklistData.class;
		}

		@Override
		public Object getTextForPicklist(Object toConvert, FormulaFieldInfo picklistFieldInfo) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> getUnderlyingValuesForPicklist(FormulaFieldInfo formulaFieldInfo, String fieldValue,
				FormulaContext context, boolean forSql, boolean forJs)
				throws InvalidFieldReferenceException, UnsupportedTypeException {
	        if (fieldValue == null || fieldValue.length() == 0) {
	        	List<String> arrayList = new ArrayList<>(1);
	        	arrayList.add("");
	        	return arrayList;
	        }

	        // In production, you'd look up the field.   We're making it up.
	        List<String> dbValues = Lists.newArrayList();

	        MockFormulaPicklistInfo enumInfo = (MockFormulaPicklistInfo) formulaFieldInfo.getEnumInfo();
            if (enumInfo == null) { return null; }

            List<Item> items = Lists.newArrayList();
            Item item = enumInfo.getEnumItemByApiValue(fieldValue);
            if (item != null) items.add(item);

            if (items.isEmpty()) { return null; }

            Boolean useDbValOverride = context.getProperty(FormulaContext.DO_NOT_USE_DB_VALUE_FOR_PICKLIST_EVALUATION);
            useDbValOverride = forJs || (useDbValOverride != null && useDbValOverride);
            for (Item enumItem : items) {
                final String dbValue;
                if (useDbValOverride) {
                    dbValue = enumItem.getApiValue();
                } else if (forSql) {
                    // Even if it's a numeric value, return it as a quoted string because that's how it'll be compared in compareBulk.
                    dbValue = "'" + enumItem.getDbValue() + "'";
                } else {
                    dbValue = enumItem.getDbValue();
                }
                dbValues.add(dbValue);
            }
	        assert !dbValues.isEmpty();
	        return dbValues;
	        
		}
		
	    @Override
	    public Type parseHook_getFieldReturnTypeOverride(FormulaDataType columnType, FormulaAST node,
	            FormulaContext context, Entity[] domain) {
	        Type returnType = null;
	        if (columnType == null) {
	            return null;
	        }
	        if (columnType == MockFormulaDataType.STATICENUM) {
	            returnType = MockPicklistData.class;
	        }
	        return returnType;
	     }

    } 
  
}
