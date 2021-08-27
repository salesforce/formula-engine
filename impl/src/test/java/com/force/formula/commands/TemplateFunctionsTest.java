/**
 * 
 */
package com.force.formula.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import com.force.formula.*;
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
        FormulaEngine.setHooks(BuiltinFunctionsTest.GMT_LOCALIZED_HOOKS);
    }

    @Override
    protected void tearDown() throws Exception {
        FormulaEngine.setHooks(oldHooks);
    }


    static final FormulaFactoryImpl TEST_FACTORY;
    static {
        List<FormulaCommandInfo> types = new ArrayList<>(FormulaCommandTypeRegistryImpl.getDefaultCommands());
        types.add(new FunctionFormat());
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

            FormulaEngine.setHooks(BuiltinFunctionsTest.PST_LOCALIZED_HOOKS);
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
}
