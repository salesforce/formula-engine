/**
 * 
 */
package com.force.formula.impl;

import com.force.formula.FormulaDataType;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaException;
import com.force.formula.FormulaFactory;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.FormulaTestBase;
import com.force.formula.MockFormulaDataType;
import com.force.formula.MockFormulaType;
import com.force.formula.impl.BaseCustomizableParserTest.FieldTestFormulaValidationHooks;
import com.force.formula.util.SingleValueFormulaContext;

/**
 * Test of SingleValueFormulaContext in the context of a 
 *
 * @author stamm
 * @since 0.2.5
 */
public class ValidationFormulaTest extends FormulaTestBase {
    Object singleValue;
    FormulaDataType singleType;

    public ValidationFormulaTest(String name) {
        super(name);
    }
    
    FormulaFactory oldFactory = FormulaEngine.getFactory();
    FieldTestFormulaValidationHooks hooks;

    @Override
    protected void setUp() throws Exception {
        oldFactory = FormulaEngine.getFactory();
        FormulaEngine.setFactory(BaseCustomizableParserTest.TEST_FACTORY);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        FormulaEngine.setFactory(oldFactory);
        super.tearDown();
    }
    
    
    protected FormulaDataType getSingleType() {
        return singleType;
    }
    
    protected Object getSingleValue() {
        return singleValue;
    }
    
    public void testStringValue() throws FormulaException {
        singleValue = "Hello";
        singleType = MockFormulaDataType.TEXT;
        assertTrue(evaluateBoolean("Value != NULL"));
        assertFalse(evaluateBoolean("Value = NULL"));
        assertTrue(evaluateBoolean("LEN(Value)>4"));
   }
        
        
    @Override
    protected MockFormulaType getFormulaType() {
        return MockFormulaType.TEMPLATE;
    }

    @Override
    protected FormulaRuntimeContext setupMockContext(FormulaDataType dataType) {
        return new SingleValueFormulaContext<Object>(getFormulaType(),
                ()->dataType, getSingleType(), getSingleValue());
    }

}
