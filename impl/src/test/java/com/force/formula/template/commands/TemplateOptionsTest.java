/**
 * 
 */
package com.force.formula.template.commands;

import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.InvalidFieldReferenceException;
import com.force.formula.MockFormulaDataType;
import com.force.formula.MockFormulaType;
import com.force.formula.impl.BaseCustomizableParserTest;
import com.force.formula.impl.FormulaInfoFactory;
import com.force.formula.impl.FormulaParseException;
import com.force.formula.sql.RuntimeSqlFormulaInfo;

/**
 * Test of various error handling permutations of template processing.
 *
 * @author stamm
 * @since 0.2.16
 */
public class TemplateOptionsTest extends BaseCustomizableParserTest {

    public TemplateOptionsTest(String name) {
        super(name);
    }
    
    @Override
    protected MockFormulaType getFormulaType() {
        return MockFormulaType.TEMPLATE;
    }
    
    public void testInvalidEmbeddedFormulaExpressions() throws Exception {
        FormulaRuntimeContext context = setupMockContext(MockFormulaDataType.TEXT);

        String message = "{!x} {! \"Some ok \" & \"stuff\"} {! y + x } followed by lexical garbage {!1 % 1 } and finally more garbage {! with another crap expression}";

        FormulaProperties properties = new FormulaProperties();
        properties.setGenerateSQL(false);
        properties.setAllowCycles(false);
        properties.setPolymorphicReturnType(true);
        properties.setParseAsTemplate(true);
        properties.setFailOnEmbeddedFormulaExceptions(true);
        properties.setThrowOnUnavailableField(true);

        try {
            FormulaInfoFactory.create(context, message, properties);
            fail("Should fail");
        } catch (FormulaParseException ex) {
            assertEquals("Syntax error", ex.getMessage());
        }

        // Invalid Field Reference should propogate
        try {
            FormulaInfoFactory.create(context, "Far {!x}", properties);
            fail("Should fail");
        } catch (InvalidFieldReferenceException ex) {
            assertEquals("Field x does not exist. Check spelling.", ex.getMessage());
            assertEquals(7, ex.getLocation());
        }

        properties.setThrowOnUnavailableField(false);
        try {
            FormulaInfoFactory.create(context, message, properties);
            fail("Should fail");
        } catch (FormulaParseException ex) {
            assertEquals("Syntax error", ex.getMessage());
        }
        
        try {
            FormulaInfoFactory.create(context, "{!garbage test}", properties);
            fail("Should fail");
        } catch (FormulaParseException ex) {
            assertEquals("Syntax error. Extra test", ex.getMessage());
        }


        // Turn failing off, and it should work with 'null' for the bad values.
        properties.setFailOnEmbeddedFormulaExceptions(false);

        RuntimeSqlFormulaInfo fi = FormulaInfoFactory.create(context, message, properties);
        assertEquals("null Some ok stuff null followed by lexical garbage null and finally more garbage null", fi.getFormula().evaluate(context));
        
        // Fail on Embedded controls it.
        properties.setThrowOnUnavailableField(true);
        fi = FormulaInfoFactory.create(context, message, properties);
        assertEquals("null Some ok stuff null followed by lexical garbage null and finally more garbage null", fi.getFormula().evaluate(context));
        
        // Test stufff
        fi = FormulaInfoFactory.create(context, "{!garbage test}", properties);
        assertEquals("null", fi.getFormula().evaluate(context));
        fi = FormulaInfoFactory.create(context, "{!1 % 1}", properties);
        assertEquals("null", fi.getFormula().evaluate(context));
        
    }
}
