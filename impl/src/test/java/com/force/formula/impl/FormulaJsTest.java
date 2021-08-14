package com.force.formula.impl;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.force.formula.*;

/**
 * Tests specific to JS code generation
 *
 * @author a.rich
 * @since 0.4.24
 */
public class FormulaJsTest {
    @Before
    public void setUp() {
        MockLocalizerContext.establishMock();
    }
    
    /**
     * Tests that we use a relaxed JS size limit at runtime.
     */
    @Test
    public void testJsTooBigAtRuntime() throws Exception {
        String formula = "CASE(TODAY(), TODAY()";
        for (int i = 0; i < 30; i++) {
            formula += ", TODAY(), TODAY()";
        }
        formula += ")";

        MockFormulaType type = MockFormulaType.JAVASCRIPT;
        FormulaContext context = new MockFormulaContext( type,MockFormulaDataType.DATEONLY);

        boolean isCreateOrEdit = true;
        RuntimeFormulaInfo formulaInfo;
        try {
            formulaInfo = FormulaEngine.getFactory().create(type, context, formula, true, false, isCreateOrEdit);
            fail("Expected a JSTooBigException! JS size: " + formulaInfo.getFormula().toJavascript().length());
        } catch (JSTooBigException e) {
            // expected;
        }

        isCreateOrEdit = false;

        // No failure expected
        formulaInfo = FormulaInfoFactory.create(type, context, formula, true, false, isCreateOrEdit);
        assertTrue("JS Size should be greater than 5000",
                formulaInfo.getFormula().toJavascript().length() > FormulaInfo.MAX_JS_SIZE);

        // Now try with a *very* large formula:
        formula = "CASE(TODAY(), TODAY()";
        for (int i = 0; i < 200; i++) {
            formula += ", TODAY(), TODAY()";
        }
        formula += ")";

        try {
            FormulaEngine.getFactory().create(type, context, formula, true, false, isCreateOrEdit);
            fail("Expected a JSTooBigException! JS size: " + formulaInfo.getFormula().toJavascript().length());
        } catch (JSTooBigException e) {
            // expected;
        }
    }
}
