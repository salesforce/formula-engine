package com.force.formula.template.commands;

import java.util.ArrayList;
import java.util.List;

import com.force.formula.*;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.commands.FunctionFormat;
import com.force.formula.impl.*;

/**
 * @author david.brady
 * @since 152
 */
public class FormulaUrlEncodeTest extends FormulaTestBase {

    public FormulaUrlEncodeTest(String name) {
        super(name);
    }

    FormulaFactory oldFactory;
    private FormulaEngineHooks oldHooks;
    @Override
	protected void setUp() throws Exception {
    	oldFactory = FormulaEngine.getFactory();
        FormulaEngine.setFactory(TEST_FACTORY);
        oldHooks = FormulaEngine.getHooks();
        FormulaEngine.setHooks(getHooksOverrideLocalizer(oldHooks, GMT_LOCALIZER));
	}

	@Override
	protected void tearDown() throws Exception {
        FormulaEngine.setFactory(oldFactory);
        FormulaEngine.setHooks(oldHooks);
	}

	public void testEncode() throws Exception {
        String expression = "This is a test of urlencode {!urlencode('http://www.cnn.com/junk.cgi?abc=d e f&date=12/7/2007')}";
        assertTemplateFormula("This is a test of urlencode http%3A%2F%2Fwww.cnn.com%2Fjunk.cgi%3Fabc%3Dd+e+f%26date%3D12%2F7%2F2007", expression); 
    }
    
    public void testEncodeEmptyArg() throws Exception {
        String expression = "This is a test of urlencode {!urlencode('')}";
        assertTemplateFormula("This is a test of urlencode ", expression); 
    }
        
    public void testEncodeNoArg() throws Exception {
        String expression = "This is a test of urlencode {!urlencode()}";
        try {
        	evaluateTemplate(expression);
            fail();
        } catch (WrongNumberOfArgumentsException ex) {
            // Correct
        }
    }
        
    public void testEncodeTooManyArgs() throws Exception {
        String expression = "This is a test of urlencode {!urlencode('a','b')}";
        try {
        	evaluateTemplate(expression);
            fail();
        } catch (WrongNumberOfArgumentsException ex) {
            // Correct
        }
    }
        
    public void testEncodeBadArg() throws Exception {
        String expression = "This is a test of urlencode {!urlencode(12345)}";
        try {
        	evaluateTemplate(expression);
            fail();
        } catch (IllegalArgumentTypeException ex) {
            // Correct
        }
    }
    

    static final FormulaFactoryImpl TEST_FACTORY;
    static {
        List<FormulaCommandInfo> types = new ArrayList<>(FormulaCommandTypeRegistryImpl.getDefaultCommands());
        // Test format and template parsing
        types.add(new FunctionFormat());
        types.add(new FunctionTemplate());
        types.add(new FunctionUrlEncode());
        TEST_FACTORY = new FormulaFactoryImpl(new FormulaCommandTypeRegistryImpl(types));
    }

        
}
