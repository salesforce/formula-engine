/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.force.formula.DisplayField;
import com.force.formula.Formula;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaException;
import com.force.formula.FormulaFactory;
import com.force.formula.FormulaFieldInfo;
import com.force.formula.FormulaFieldReferenceInfo;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.FormulaRuntimeContext.InaccessibleFieldStrategy;
import com.force.formula.FormulaSchema.Field;
import com.force.formula.InvalidFieldReferenceException;
import com.force.formula.MockFormulaDataType;
import com.force.formula.MockFormulaType;
import com.force.formula.RuntimeFormulaInfo;
import com.force.formula.sql.FormulaWithSql;
import com.google.common.collect.ImmutableSet;

/**
 * Perform a test of Field References
 * @author stamm
 * @since 0.2
 */
public class FieldReferenceTest extends BaseFieldReferenceTest {
    public FieldReferenceTest(String name) {
        super(name);
    }
    
    /**
     * This works like "Contact", where you can access Contact fields 
     * @throws Exception
     */
    public void testDisplayFields() throws Exception {            
        BeanFormulaContext context = setupMockContext(null);
        DisplayField[] fields = context.getDisplayFields(context.getEntity());
        Map<String,FormulaFieldInfo> ffis = new HashMap<>();
        for (DisplayField field : fields) {
            assertEquals("BaseCustomizableParserTest$TestContact", field.getCategoryLabel());
            ffis.put(field.getFormulaFieldInfo().getName(), field.getFormulaFieldInfo());
        }
        assertEquals(ImmutableSet.of("account", "accountId", "createdDate", "optIn", "nullNumber", "nullAccount", "nullDate", "nullText",
                "dateFormula", "numberFormula", "list", "map"), ffis.keySet());
        assertTrue(ffis.get("createdDate").getDataType().isDateOnly());            
        assertTrue(ffis.get("optIn").getDataType().isBoolean());            
    }
 
    public void testEncoding() throws Exception {
        assertEquals("ENCODED:{!ID:$System.OriginDateTime}", encode("$System.OriginDateTime", MockFormulaDataType.DATEONLY));
        assertEquals("ENCODED:NOT({!ID:optIn})", encode("NOT(OptIn)", MockFormulaDataType.BOOLEAN));
        assertEquals("ENCODED:{!ID:Account.createdDate}", encode("Account.CreatedDate", MockFormulaDataType.DATEONLY));
        
        validateDecode("$System.OriginDateTime", MockFormulaDataType.DATEONLY);
        validateDecode("NOT(OptIn)", MockFormulaDataType.BOOLEAN);
        // TODO doesn't work yet...
        //validateDecode("Account.CreatedDate", MockFormulaDataType.DATEONLY);
    }
    
    public String encode(String formulaSource, FormulaDataType columnType) throws FormulaException {
        FormulaRuntimeContext context = setupMockContext(columnType);
        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, formulaSource);
        return formulaInfo.encode();
    }

    public void validateDecode(String formulaSource, FormulaDataType columnType) throws FormulaException {
        FormulaRuntimeContext context = setupMockContext(columnType);
        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, formulaSource);
        String decoded = FormulaUtils.decode(context, formulaInfo.encode(), getFormulaType().getDefaultProperties());
        assertTrue(decoded + "!=" + formulaSource, decoded.equalsIgnoreCase(formulaSource));
    }
    
    /**
     * Test the command visitors based on a formlu
     * @throws Exception
     */
    public void testCommandVisitor() throws Exception {            
        FormulaRuntimeContext context = setupMockContext(MockFormulaDataType.BOOLEAN);
        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, "Account.OptIn");
        assertFalse(formulaInfo.hasFormatCurrencyCommand());
        assertFalse(formulaInfo.hasAIPredictionFieldReference());
        assertFalse(formulaInfo.referenceEncryptedFields());
        assertTrue(formulaInfo.isDeterministic());
        FormulaWithSql formula = (FormulaWithSql) formulaInfo.getFormula();
        assertTrue(formula.isCustomIndexable(context));
        assertTrue(formula.isDeterministic(context));
        assertFalse(formula.isPostSaveIndexUpdated(context, null));
        assertFalse(formula.isStale(context));
        
        // Now try one that isn't  deterministic
        formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, "TODAY() > Account.CreatedDate + 7");
        formula = (FormulaWithSql) formulaInfo.getFormula();
        assertFalse(formulaInfo.isDeterministic());
        assertFalse(formula.isCustomIndexable(context));
        assertFalse(formula.isFlexIndexable(context));
        assertFalse(formula.isDeterministic(context));
        
        // Try one with a composite formula
        formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, "DATE(2022,12,1) > DateFormula + 7");
        formula = (FormulaWithSql) formulaInfo.getFormula();
        assertTrue(formulaInfo.isDeterministic());
        assertTrue(formula.isCustomIndexable(context));
        assertTrue(formula.isFlexIndexable(context));
        assertTrue(formula.isDeterministic(context));
        // Try bulk processing
        formula.bulkProcessingBeforeEvaluation(Collections.singletonList(context));
        assertFalse(formula.hasAttribute(FormulaUtils.PRODUCES_SQL_ERROR_COLUMN));  

        // Now format currency
        context = setupMockContext(MockFormulaDataType.TEXT);
        formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, "FORMATCURRENCY(Account.CurrencyIsoCode, Account.Amount)");
        assertTrue(formulaInfo.hasFormatCurrencyCommand());
        assertFalse(formula.hasAttribute(FormulaUtils.PRODUCES_SQL_ERROR_COLUMN));  
        
    }

    
    /**
     * This works like "Contact", where you can access Contact fields 
     * @throws Exception
     */
    public void testArgumentTypes() throws Exception {            
        FormulaRuntimeContext context = setupMockContext(MockFormulaDataType.BOOLEAN);
        try {
            FormulaEngine.getFactory().create(getFormulaType(), context, "Account.OptIn=Account.CreatedDate");
            fail("Can't compare these types");
        } catch (WrongArgumentTypeException ex) {
            assertEquals("Incorrect parameter type for operator '='. Expected Boolean, received Date", ex.getMessage());
        }
    }
    
	public void testIsChanged() throws Exception {
        FormulaFactory oldFactory = FormulaEngine.getFactory();
        try {
            FormulaEngine.setFactory(TEST_FACTORY);
	        FormulaRuntimeContext context = setupMockContext(MockFormulaDataType.BOOLEAN);
	        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(MockFormulaType.TEMPLATE, context, "ISCHANGED(Account.OptIn)");
	        Formula formula = formulaInfo.getFormula();
	        assertEquals(Boolean.FALSE, formula.evaluate(context));
        } finally {
            FormulaEngine.setFactory(oldFactory);
        }
	}
	
	public void testPriorValue() throws Exception {
        FormulaFactory oldFactory = FormulaEngine.getFactory();
        try {
            FormulaEngine.setFactory(TEST_FACTORY);
	        FormulaRuntimeContext context = setupMockContext(MockFormulaDataType.ENTITYID);
	        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(MockFormulaType.TEMPLATE, context, "PRIORVALUE(Account.OptIn)");
	        Formula formula = formulaInfo.getFormula();
	        assertEquals(null, formula.evaluate(context));
        } finally {
            FormulaEngine.setFactory(oldFactory);
        }
	}

	public void testDirectReference() throws Exception {
	    AtomicBoolean caseSafe = new AtomicBoolean();
        FormulaRuntimeContext context = setupMockContext(MockFormulaDataType.BOOLEAN);
        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, "Account.OptIn");
        List<FormulaFieldReferenceInfo> reference = formulaInfo.getFormula().getFieldPathIfDirectReferenceToAnotherField(context, false, true, caseSafe, "ns");
        assertEquals(1, reference.size());
        assertEquals("optIn", reference.get(0).getFieldOrColumn().getName());
        

        formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, "TODAY() > DateFormula + 7");
        reference = formulaInfo.getFormula().getFieldPathIfDirectReferenceToAnotherField(context, false, true, caseSafe, null);
        assertNull(reference); // not a direct reference.
        Set<String> references = FormulaUtils.getFieldReferences(formulaInfo.getFormula(), context);
        assertEquals(ImmutableSet.of("account.createdDate", "account.secondNumber"), references); // Get references from DateFormula
        
        context = setupMockContext(MockFormulaDataType.DATEONLY);
        formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, "DateFormula");
        reference = formulaInfo.getFormula().getFieldPathIfDirectReferenceToAnotherField(context, false, true, caseSafe, null);
        assertNull(reference); // not a direct reference.

        context = setupMockContext(MockFormulaDataType.DOUBLE);
        formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, "NULLVALUE(Account.Amount,0)");
        reference = formulaInfo.getFormula().getFieldPathIfDirectReferenceToAnotherField(context, true, true, caseSafe, null);
        assertNotNull(reference); // Direct reference if null allowed (i.e. zero excluded)
        reference = formulaInfo.getFormula().getFieldPathIfDirectReferenceToAnotherField(context, false, true, caseSafe, null);
        assertNull(reference); // not a direct reference since null allowed
        
	}
	
	public void testInaccessibleFields() throws Exception {
        FormulaRuntimeContext context = setupMockContext(MockFormulaDataType.BOOLEAN);
        context.setProperty(FormulaRuntimeContext.HANDLE_INACCESSIBLE_FIELDS, InaccessibleFieldStrategy.REPLACE_WITH_NULL);
        RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, "Account.OptIn");
        FormulaFieldInfo[] references = formulaInfo.getReferences();
        assertEquals(1, references.length);
        
        // Make OptIn inaccessible
        FormulaEngine.setHooks(new FieldTestFormulaValidationHooks() {
            @Override
            public boolean isFieldReadable(Field field) {
                return false;
            }
        });
        formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, "Account.OptIn");
        references = formulaInfo.getReferences();
        assertEquals(0, references.length);

        // Now throw exception
        context.setProperty(FormulaRuntimeContext.HANDLE_INACCESSIBLE_FIELDS, InaccessibleFieldStrategy.THROW_EXCEPTION);
        try {
            formulaInfo = FormulaEngine.getFactory().create(getFormulaType(), context, "Account.OptIn");
            fail("Should throw exception");
        } catch (InvalidFieldReferenceException ex) {
            assertTrue(ex.getMessage().contains("optIn"));
        }
    }

}