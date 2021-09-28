/**
 * 
 */
package com.force.formula.template.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import com.force.formula.*;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.commands.FunctionFormat;
import com.force.formula.impl.*;

/**
 * TODO Describe your class here.
 *
 * @author stamm
 * @since <release>
 */
public class TemplateFunctionsTest extends ParserTestBase {

    public TemplateFunctionsTest(String name) {
        super(name);
    }
    
    @Override
    protected MockFormulaType getFormulaType() {
        return MockFormulaType.TEMPLATE;
    }

    private FormulaEngineHooks oldHooks = null;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        oldHooks = FormulaEngine.getHooks();
        FormulaEngine.setHooks(getHooksOverrideLocalizer(oldHooks, GMT_LOCALIZER));
    }

    @Override
    protected void tearDown() throws Exception {
        FormulaEngine.setHooks(oldHooks);
    }

    

    static final FormulaFactoryImpl TEST_FACTORY;
    static {
        List<FormulaCommandInfo> types = new ArrayList<>(FormulaCommandTypeRegistryImpl.getDefaultCommands());
        // Test format and template parsing
        types.add(new FunctionFormat());
        types.add(new FunctionTemplate());
        TEST_FACTORY = new FormulaFactoryImpl(new FormulaCommandTypeRegistryImpl(types));
    }

    
    public void testFormat() throws Exception {
        // Not available by default
        try {
            assertFalse( evaluateBoolean("\"HiHo\"=FORMAT(\"{0}{1}\",\"Hi\",\"Ho\")")); 
            fail("Format isn't a default method"); 
        } catch (InvalidFunctionReferenceException x) {
            assertEquals("Unknown function FORMAT. Check spelling.", x.getMessage());
        }

        FormulaFactory oldFactory = FormulaEngine.getFactory();
        try {
            String LEAP = "2016-02-29 13:15:10";
            FormulaEngine.setFactory(TEST_FACTORY);
            // Test String
            assertEquals("HiHo",  evaluateString("FORMAT(\"{0}{1}\",\"Hi\",\"Ho\")")); 
            // Test Date/Number/Time format
            assertEquals("2/29/2016, 1:15 PM",  evaluateString("FORMAT(DATETIMEVALUE(\"" + LEAP + "\"))")); 
            assertEquals("1/3/2005",  evaluateString("FORMAT(date(2005,1,3))")); 
            assertEquals("8:34 AM",  evaluateString("FORMAT(timeValue(\"08:34:56.789\"))")); 
             
            // Try various formats
            assertEquals("1:15 PM",  evaluateString("FORMAT(DATETIMEVALUE(\"" + LEAP + "\"),\"time\")")); 
            assertEquals("1:15 PM",  evaluateString("FORMAT(DATETIMEVALUE(\"" + LEAP + "\"),\"time_correct\")")); 
            assertEquals("1/3/2005",  evaluateString("FORMAT(date(2005,1,3),\"date\")")); 
            assertEquals("January 3, 2005",  evaluateString("FORMAT(date(2005,1,3),\"ldate\")")); 
            assertEquals("2/29/2016",  evaluateString("FORMAT(DATETIMEVALUE(\"" + LEAP + "\"),\"date\")")); 
            assertEquals("February 29, 2016",  evaluateString("FORMAT(DATETIMEVALUE(\"" + LEAP + "\"),\"ldate\")")); 

            // Use TEXT as a comparsion where the TimeZone isn't used.
            assertEquals("2016-02-29 13:15:10Z",  evaluateString("TEXT(DATETIMEVALUE(\"" + LEAP + "\"))")); 
            assertEquals("2005-01-03",  evaluateString("TEXT(date(2005,1,3))")); 
            assertEquals("08:34:56.789",  evaluateString("TEXT(timeValue(\"08:34:56.789\"))")); 

            FormulaEngine.setHooks(getHooksOverrideLocalizer(oldHooks, PST_LOCALIZER));
            assertEquals("2/29/2016, 5:15 AM",  evaluateString("FORMAT(DATETIMEVALUE(\"" + LEAP + "\"))")); 
            assertEquals("1/3/2005",  evaluateString("FORMAT(date(2005,1,3))")); 
            assertEquals("12:34 AM",  evaluateString("FORMAT(timeValue(\"08:34:56.789\"))")); 
            assertEquals("5:15 AM",  evaluateString("FORMAT(DATETIMEVALUE(\"" + LEAP + "\"),\"time\")")); 
            assertEquals("5:15 AM",  evaluateString("FORMAT(DATETIMEVALUE(\"" + LEAP + "\"),\"time_correct\")")); 
            assertEquals("2/29/2016",  evaluateString("FORMAT(DATETIMEVALUE(\"" + LEAP + "\"),\"date\")")); 
            assertEquals("February 29, 2016",  evaluateString("FORMAT(DATETIMEVALUE(\"" + LEAP + "\"),\"ldate\")")); 
            
            // TODO: This isn't right.  FormulaDate should be formatted in GMT... 
            assertEquals("1/2/2005",  evaluateString("FORMAT(date(2005,1,3),\"date\")")); 
            assertEquals("January 2, 2005",  evaluateString("FORMAT(date(2005,1,3),\"ldate\")")); 

        } finally {
            FormulaEngine.setFactory(oldFactory);
        }
    }
    
    public void testIsNew() throws Exception {
        assertFalse( evaluateBoolean("ISNEW()")); 
        setIsNew = true;
        assertTrue( evaluateBoolean("ISNEW()")); 
    }

    public void testIsClone() throws Exception {
        assertFalse( evaluateBoolean("ISCLONE()")); 
        setIsNew = true;
        assertTrue( evaluateBoolean("ISCLONE()")); 
    }

    public void testJSEncode() throws Exception {
        assertEquals("This is a test", evaluateString("JSENCODE(\"This is a test\")"));
        assertEquals("This \\'is a test", evaluateString("JSENCODE(\"This 'is a test\")"));
        assertEquals("This \\\\is a test", evaluateString("JSENCODE(\"This \\\\is a test\")"));
        assertEquals("This \\\"is a test", evaluateString("JSENCODE(\"This \\\"is a test\")"));
        assertEquals("This \\u003Cb\\u003Eis a test", evaluateString("JSENCODE(\"This <b>is a test\")"));
        assertEquals("This is a \\/\\*test\\*\\/", evaluateString("JSENCODE(\"This is a /*test*/\")"));
        assertEquals("This is a \\*test\\*", evaluateString("JSENCODE(\"This is a *test*\")"));
    }

    public void testHtmlEncode() throws Exception {
        assertEquals("&lt;", evaluateString("HTMLENCODE(\"<\")"));
        assertEquals("&gt;", evaluateString("HTMLENCODE(\">\")"));
        assertEquals("&amp;", evaluateString("HTMLENCODE(\"&\")"));
        assertEquals("&quot;", evaluateString("HTMLENCODE(\"\\\"\")"));
        assertEquals("&#39;", evaluateString("HTMLENCODE(\"\'\")"));
        assertEquals("<br>", evaluateString("HTMLENCODE(\"\u2028\")"));
        assertEquals("<p>", evaluateString("HTMLENCODE(\"\u2029\")"));
        assertEquals("&copy;", evaluateString("HTMLENCODE(\"\u00a9\")"));
    }
    
    public void testRegex() throws Exception {
        assertTrue(evaluateBoolean("REGEX(\"867-5309\",\"^[0-9]{3}-[0-9]{4}$\")"));
        assertFalse(evaluateBoolean("REGEX(\"94102\",\"^[0-9]{3}-[0-9]{4}$\")"));

        try {
            evaluateBoolean("REGEX(\"94102\",\"^[\")");
            fail("Invalid regex");
        } catch (PatternSyntaxException ex) {
            assertTrue(true);
        }

    }
    
    

    
    private boolean setIsNew = false;
    @Override
    protected FormulaRuntimeContext setupMockContext(FormulaDataType columnType) {
        FormulaRuntimeContext result = super.setupMockContext(columnType);
        if (setIsNew) {
            result.setProperty("test.ISNEW", Boolean.TRUE);
            result.setProperty("test.ISCLONE", Boolean.TRUE);
        }
        return result;
    }
    

    /**
     * Verify various formats for number formula type ( valid and invalid)
     * @priority Medium
     * @hierarchy Declarative App Builder.Formula Fields.Format
     * @userStory Annotation Debt
     *
     */
    public void testTemplateFormatNumber() throws Exception {
        FormulaFactory oldFactory = FormulaEngine.getFactory();
        try {
            FormulaEngine.setFactory(TEST_FACTORY);
            String expression = "This is a test of format {!format(100.0)}";
            assertTemplateFormula("This is a test of format 100", expression);
    
            expression = "This is a test of format {!format(10000.05)}";
            assertTemplateFormula("This is a test of format 10,000.05", expression);
    
            expression = "This is a test of format {!format(100.0,\"0.0#'%'\")}";
            assertTemplateFormula("This is a test of format 100.0%", expression);
    
            expression = "This is a test of format {!format(100.0,\"INVALID\")}";
            assertTemplateFormula("This is a test of format INVALID100", expression);

            expression = "This is a test of format {!100.0}";
            assertTemplateFormula("This is a test of format 100.0", expression);

        } finally {
            FormulaEngine.setFactory(oldFactory);
        }
    }

    /**
     * Verify various formats for Date formula type ( valid and invalid)
     * @priority Medium
     * @hierarchy Declarative App Builder.Formula Fields.Format
     * @userStory Annotation Debt
     *
     */
    public void testTemplateFormatDate() throws Exception {
        FormulaFactory oldFactory = FormulaEngine.getFactory();
        try {
            FormulaEngine.setFactory(TEST_FACTORY);
             String expression = "This is a test of format {!format(DATE(2007,7,26))}";
            assertTemplateFormula("This is a test of format 7/26/2007", expression);
    
            expression = "This is a test of format {!format(DATE(2007,7,26),\"yyyy-MM-dd\")}";
            assertTemplateFormula("This is a test of format 2007-07-26", expression);
    
    
            expression = "This is a test of format {!format(DATE(2007,7,26),\"INVALID\")}";
            try {
                assertTemplateFormula(null, expression);
                fail();
            } catch (FormulaEvaluationException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Illegal pattern character"));
            }
            
            // Assume en_US as default locale
            expression = "This is a test of format {!DATE(2007,7,26)}";
            assertTemplateFormula("This is a test of format 7/26/2007", expression);
            String LEAP = "2016-02-29 13:15:10";
            expression = "This is a test of format {!DATETIMEVALUE('"+LEAP+"')}";
            assertTemplateFormula("This is a test of format 2/29/2016, 1:15 PM", expression);
    
        } finally {
            FormulaEngine.setFactory(oldFactory);
        }
    }



    /**
     * Verify various formats for Text formula type ( valid and invalid)
     * @priority Medium
     * @hierarchy Declarative App Builder.Formula Fields.Format
     * @userStory Annotation Debt
     *
     */
    public void testTemplateFormatText() throws Exception {
        FormulaFactory oldFactory = FormulaEngine.getFactory();
        try {
            FormulaEngine.setFactory(TEST_FACTORY);
            String expression = "This is a test of format {!format()}";
            try {
                assertTemplateFormula(null, expression);
                fail();
            } catch (WrongNumberOfArgumentsException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Incorrect number of parameters"));
            }
    
            expression = "This is a test of format {!format(\"USD\")}";
            assertTemplateFormula("This is a test of format USD", expression);
    
            expression = "This is a test of format {!format(\"USD\",\"USD\")}";
            assertTemplateFormula("This is a test of format USD", expression);
    
            expression = "This is a test of format {!format(\"Test {1}\",\"USD\",\"USD\")}";
            assertTemplateFormula("This is a test of format Test USD", expression);
    
            expression = "This is a test of format {!format(\"Test {0}\",\"USD\",\"USD\")}";
            assertTemplateFormula("This is a test of format Test USD", expression);
    
            expression = "This is a test of format {!format(\"Test {0}\",null,\"USD\")}";
            assertTemplateFormula("This is a test of format Test ", expression);
    
            try {
                expression = "This is a test of format {!format(\"Test {0}\",10.0,\"USD\")}";
                assertTemplateFormula("This is a test of format Test null", expression);
                fail();
            }
            catch (WrongArgumentTypeException ex) {
                assertTrue(ex.getMessage(), ex.getMessage().contains("Incorrect parameter type"));
            }
    
            expression = "This is a test of format {!format(\"Test {3}\",\"USD\",\"USD\")}";
            assertTemplateFormula("This is a test of format Test {3}", expression);
        } finally {
            FormulaEngine.setFactory(oldFactory);
        }
    }



}
