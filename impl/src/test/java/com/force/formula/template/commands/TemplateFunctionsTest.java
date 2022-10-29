/**
 * 
 */
package com.force.formula.template.commands;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.PatternSyntaxException;

import com.force.formula.FormulaDataType;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaEngineHooks;
import com.force.formula.FormulaEvaluationException;
import com.force.formula.FormulaFactory;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.MockFormulaType;
import com.force.formula.MockLocalizerContext;
import com.force.formula.MockLocalizerContext.MockLocalizer;
import com.force.formula.ParserTestBase;
import com.force.formula.commands.FieldReferenceCommandInfo;
import com.force.formula.commands.FormulaCommandInfo;
import com.force.formula.commands.FunctionFormat;
import com.force.formula.commands.FunctionFormatCurrency;
import com.force.formula.commands.FunctionIsChanged;
import com.force.formula.commands.FunctionLabel;
import com.force.formula.commands.FunctionPriorValue;
import com.force.formula.impl.FormulaCommandTypeRegistryImpl;
import com.force.formula.impl.FormulaFactoryImpl;
import com.force.formula.impl.FormulaParseException;
import com.force.formula.impl.FormulaValidationHooks;
import com.force.formula.impl.FormulaValidationHooks.ParseOption;
import com.force.formula.impl.InvalidFunctionReferenceException;
import com.force.formula.impl.JvmMetrics;
import com.force.formula.impl.RegexTooComplicatedException;
import com.force.formula.impl.WrongNumberOfArgumentsException;
import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.BaseLocalizer;
import com.force.i18n.HumanLanguage;
import com.force.i18n.LabelSetDescriptorImpl;
import com.force.i18n.LanguageLabelSetDescriptor.GrammaticalLabelSetDescriptor;
import com.force.i18n.LanguageProviderFactory;
import com.force.i18n.grammar.GrammaticalLabelSetProvider;
import com.force.i18n.grammar.GrammaticalLocalizer;
import com.force.i18n.grammar.GrammaticalLocalizerFactory;
import com.force.i18n.grammar.LanguageArticle;
import com.force.i18n.grammar.LanguageDeclension;
import com.force.i18n.grammar.LanguageGender;
import com.force.i18n.grammar.LanguageNumber;
import com.force.i18n.grammar.LanguageStartsWith;
import com.force.i18n.grammar.Noun;
import com.force.i18n.grammar.Noun.NounType;
import com.force.i18n.grammar.RenamingProvider;
import com.force.i18n.grammar.RenamingProviderFactory;
import com.force.i18n.grammar.impl.LanguageDeclensionFactory;
import com.google.common.collect.ImmutableMap;

/**
 * Unit tests for functions normally used in templates for formatting values.
 *
 * @author stamm
 * @since 0.1.0
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
        types.add(new FunctionFormatCurrency());
        types.add(new FunctionTemplate());
        types.add(new FunctionPriorValue());
        types.add(new FunctionIsChanged());
        types.add(new FunctionLabel());
        types.add(new FieldReferenceCommandInfo());

        types.add(new FunctionRenameable());  // TEST FUNCTION ONLY
        TEST_FACTORY = new FormulaFactoryImpl(new FormulaCommandTypeRegistryImpl(types));
    }

    
    public void testFormat() throws Exception {
        FormulaFactory oldFactory = FormulaEngine.getFactory();

        // Not available by default
        try {
            FormulaEngine.setFactory(new FormulaFactoryImpl());
            assertFalse( evaluateBoolean("\"HiHo\"=FORMAT(\"{0}{1}\",\"Hi\",\"Ho\")")); 
            fail("Format isn't a default method"); 
        } catch (InvalidFunctionReferenceException x) {
            assertEquals("Unknown function FORMAT. Check spelling.", x.getMessage());
        }

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
    

    /**
     * Verify that REGEX() validations are bounded.
     */
    public void testBoundedRegex() throws Exception {
        
        int oldLimit = FunctionRegex.FORMULA_LIMIT;
        try {
            assertFalse(evaluateBoolean("REGEX(\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaac\",\"(a*)+b\")"));
            FunctionRegex.FORMULA_LIMIT = 100;  // Allow 100 characters
            try {
                evaluateBoolean("REGEX(\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaac\",\"(a*)+b\")");
                fail("Should fail with too complicated");
            } catch (RegexTooComplicatedException ex) {
                assert ex.getMessage().contains("(a*)+b");
            }
        } finally {
            FunctionRegex.FORMULA_LIMIT = oldLimit;
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
     */
    public void testTemplateFormatText() throws Exception {
        FormulaFactory oldFactory = FormulaEngine.getFactory();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
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
    
            expression = "This is a test of format {!format(\"Test {0}\",10.0,\"USD\")}";
            assertTemplateFormula("This is a test of format Test 10", expression);

            expression = "This is a test of format {!format(\"Test {0}\",10.0,\"USD\")}";
            assertTemplateFormula("This is a test of format Test 10", expression);

            expression = "This is a test of format {!format(\"Test {3}\",\"USD\",\"USD\")}";
            assertTemplateFormula("This is a test of format Test {3}", expression);
            
            expression = "This is a test of format {!format(\"Test {0,date}\",DATE(2015,11,2))}";
            assertTemplateFormula("This is a test of format Test Nov 2, 2015", expression);

            expression = "This is a test of format {!format(\"Test {0,time}\",TIMEVALUE(\"12:00:00.000\"))}";
            assertTemplateFormula("This is a test of format Test 12:00:00 PM", expression);

        } finally {
            FormulaEngine.setFactory(oldFactory);
        }
    }


    /**
     * Added to formula engine but not exposed in regular formulas
     */
    public void testFunctionIsChanged() throws Exception {
        String expression1 = "This is a test 1 \r\n This is a test 2";
        String expression2 = "This is a test 1 \n This is a test 2";

        assertFalse("CR+LF should be treated as just LF", FunctionIsChanged.isChanged(expression1, expression2));

        expression1 = "This is a test 1 \r\n\r\n\r\n This is a test 2";
        expression2 = "This is a test 1 \n\n\n This is a test 2";

        assertFalse("CR+LF should be treated as just LF", FunctionIsChanged.isChanged(expression1, expression2));

        expression1 = "This is a test 1 \r\n This is a test 2";
        expression2 = "This is a test 1 This is a test 2";

        assertTrue("No LF in the 2nd expression", FunctionIsChanged.isChanged(expression1, expression2));

        expression1 = "This is a test 1 \\r\n This is a test 2";
        expression2 = "This is a test 1 \\n This is a test 2";

        assertTrue("CR isn't a char because it's escaped", FunctionIsChanged.isChanged(expression1, expression2));

        expression1 = "This is a test 1 \r This is a test 2";
        expression2 = "This is a test 1 \n This is a test 2";

        assertTrue("CR isn't the same as LF", FunctionIsChanged.isChanged(expression1, expression2));
    }
    

    /**
     * Verify various formats for Currency formula type
     */
    public void testFormatCurrency() throws Exception {
        FormulaFactory oldFactory = FormulaEngine.getFactory();
        try {
            FormulaEngine.setFactory(TEST_FACTORY);
	        String expression = "This is a test of format currency {!formatcurrency(\"USD\", 100.0)}";
	        assertTemplateFormula("This is a test of format currency USD 100.00", expression);
	
	        expression = "This is a test of format currency {!formatcurrency(\"USD\", null)}";
	        assertTemplateFormula("This is a test of format currency ", expression);
	
	        expression = "This is a test of format currency {!formatcurrency(null, 0)}";
	        assertTemplateFormula("This is a test of format currency  0.00", expression);
	
	        expression = "This is a test of format currency {!formatcurrency(null, null)}";
	        assertTemplateFormula("This is a test of format currency ", expression);
	
	        expression = "This is a test of format currency {!formatcurrency(\"USD\", 0)}";
	        assertTemplateFormula("This is a test of format currency USD 0.00", expression);
	
	        expression = "This is a test of format currency {!formatcurrency(\"USD\", 1)}";
	        assertTemplateFormula("This is a test of format currency USD 1.00", expression);
	
	        expression = "This is a test of format currency {!formatcurrency(\"USD\", 1234.567)}";
	        assertTemplateFormula("This is a test of format currency USD 1,234.57", expression);
	
	        expression = "This is a test of format currency {!formatcurrency(\"USD\", -1234567)}";
	        assertTemplateFormula("This is a test of format currency USD -1,234,567.00", expression);
	
	        expression = "This is a test of format currency {!formatcurrency(\"JPY\", 1234.567)}";
	        assertTemplateFormula("This is a test of format currency JPY 1,235", expression);
	
	        expression = "This is a test of format currency {!formatcurrency(\"dog\", 1)}";
	        assertTemplateFormula("This is a test of format currency dog 1.00", expression);
        } finally {
            FormulaEngine.setFactory(oldFactory);
        }
    }
     
    public void testLabel() throws Exception { 
        // Get some sample labels from grammaticus tests that exercize the various functions
        RenamingProvider curProvider = RenamingProviderFactory.get().getProvider();
        URL url = TemplateFunctionsTest.class.getResource("/com/force/formula/impl/labels/labels.xml");
        HumanLanguage language = LanguageProviderFactory.get().getBaseLanguage();
        GrammaticalLabelSetDescriptor desc = new LabelSetDescriptorImpl(url, language, "labels", "labels.xml", "names.xml");
        GrammaticalLabelSetProvider provider = FormulaI18nUtils.getFormulaEngineLabelsProvider(null);
        
        GrammaticalLocalizer localizer = new MockLocalizer(Locale.US, Locale.US, TimeZone.getTimeZone("GMT"), language,
                GrammaticalLocalizerFactory.getLoader(desc, provider).getSet(language));
        FormulaEngine.setHooks(getHooksOverrideLocalizer(oldHooks, localizer));
        
        FormulaFactory oldFactory = FormulaEngine.getFactory();
        try {
            FormulaEngine.setFactory(TEST_FACTORY); 
            
            
            // Use the "standard" formula labels for some testing
            assertEquals("subscript", evaluateString("LABEL(\"FormulaFieldExceptionMessages\",\"subscript\")"));
            assertEquals("Close Parenthesis", evaluateString("LABEL(\"FormulaFieldOperators\",\"RPAREN\")"));
            // Test use in format
            assertEquals("Close Parenthesis", evaluateString("FORMAT(LABEL(\"FormulaFieldOperators\",\"RPAREN\"))"));
            // Test use in functions
            assertEquals("su", evaluateString("LEFT(LABEL(\"FormulaFieldExceptionMessages\",\"subscript\"),2)"));
            // Missing keys generate Missing Labels, but don't error out by default.
            assertEquals("__MISSING LABEL__", evaluateString("LEFT(LABEL(\"FormulaFieldExceptionMessages\",\"unknown\"),17)"));            
            try {
                evaluateString("LEFT(LABEL(\"unknown\",\"unknown\"),17)");
                fail("Invalid section names cause exceptions");
            } catch (FormulaEvaluationException ex) {
                assertEquals("PropertyFile - section unknown not found.", ex.getCause().getMessage());
            }
            
            // Test arguments
            assertEquals("Show 5 items", evaluateString("LABEL(\"Page_Overviews\",\"hotlist_toggle_list_length\", 5.2)"));
            assertEquals("Before: 5.2", evaluateString("LABEL(\"List\",\"last_detail\", \"Before\", 5.2)"));
            assertEquals("An Activity", evaluateString("FORMAT(LABEL(\"Global\",\"aentity\"),RENAMEABLE(\"Activity\"))"));
            assertEquals("A User", evaluateString("FORMAT(LABEL(\"Global\",\"aentity\"),RENAMEABLE(\"User\"))"));
            assertEquals("Back to Activity: List View", evaluateString("FORMAT(LABEL(\"List\",\"back_detail\",\"List View\"),RENAMEABLE(\"Activity\"))"));

            // Rename activity and make sure the labels include the right ones.
            MockRenamingProvider newProvider = new MockRenamingProvider(makeEnglishNoun("Activity", NounType.ENTITY, LanguageStartsWith.CONSONANT,
                    "Task", "Task"));
            RenamingProviderFactory.get().setProvider(newProvider);
            assertEquals("A Task", evaluateString("FORMAT(LABEL(\"Global\",\"aentity\"),RENAMEABLE(\"Activity\"))"));
            assertEquals("A User", evaluateString("FORMAT(LABEL(\"Global\",\"aentity\"),RENAMEABLE(\"User\"))"));
            assertEquals("Back to Task: List View", evaluateString("FORMAT(LABEL(\"List\",\"back_detail\",\"List View\"),RENAMEABLE(\"Activity\"))"));

            // Now use german and try the same thing
            language = LanguageProviderFactory.get().getLanguage(Locale.GERMAN);
            localizer = new MockLocalizer(Locale.GERMANY, Locale.GERMANY, TimeZone.getTimeZone("GMT"), language,
                    GrammaticalLocalizerFactory.getLoader(desc, provider).getSet(language));
            FormulaEngine.setHooks(getHooksOverrideLocalizer(oldHooks, localizer));
            
            assertEquals("5 Elemente anzeigen", evaluateString("LABEL(\"Page_Overviews\",\"hotlist_toggle_list_length\", 5.2)"));
            assertEquals("Before: 5,2", evaluateString("LABEL(\"List\",\"last_detail\", \"Before\", 5.2)"));  // Yes, comma
            assertEquals("Eine Aktivität", evaluateString("FORMAT(LABEL(\"Global\",\"aentity\"),RENAMEABLE(\"Activity\"))"));
            assertEquals("Ein Benutzer", evaluateString("FORMAT(LABEL(\"Global\",\"aentity\"),RENAMEABLE(\"User\"))"));
            assertEquals("Zurück zu Aktivität: Listenansicht", evaluateString("FORMAT(LABEL(\"List\",\"back_detail\",\"Listenansicht\"),RENAMEABLE(\"Activity\"))"));

            language = LanguageProviderFactory.get().getLanguage(Locale.JAPANESE);
            localizer = new MockLocalizer(Locale.JAPAN, Locale.JAPAN, TimeZone.getTimeZone("GMT"), language,
                    GrammaticalLocalizerFactory.getLoader(desc, provider).getSet(language));
            FormulaEngine.setHooks(getHooksOverrideLocalizer(oldHooks, localizer));

            assertEquals("5 件表示する", evaluateString("LABEL(\"Page_Overviews\",\"hotlist_toggle_list_length\", 5.2)"));
            assertEquals("Before: 5.2", evaluateString("LABEL(\"List\",\"last_detail\", \"Before\", 5.2)"));
            assertEquals("活動: リストビュー に戻る", evaluateString("FORMAT(LABEL(\"List\",\"back_detail\",\"リストビュー\"),RENAMEABLE(\"Activity\"))"));

        } finally {
            FormulaEngine.setFactory(oldFactory);
            RenamingProviderFactory.get().setProvider(curProvider);
        }
    }

    protected Noun makeEnglishNoun(String name, NounType type, LanguageStartsWith startsWith, String singular, String plural) {
        LanguageDeclension decl = LanguageDeclensionFactory.get().getDeclension(LanguageProviderFactory.get().getBaseLanguage());
        return decl.createNoun(name, type, null, startsWith, LanguageGender.NEUTER,
                ImmutableMap.of(decl.getNounForm(LanguageNumber.SINGULAR, LanguageArticle.ZERO), singular,
                        decl.getNounForm(LanguageNumber.PLURAL, LanguageArticle.ZERO), plural));
    }
    
    /**
     * Test parsing with other parse options to make sure it returns the right errors
     * @throws Exception
     */
    public void testParseTemplateWithBoth() throws Exception {
    	final AtomicReference<ParseOption> OPTION = new AtomicReference<>(ParseOption.PARSE_USING_BOTH_BUT_RETURN_ANTLR2);
        FormulaEngine.setHooks(new FormulaValidationHooks() {
			@Override
			public BaseLocalizer getLocalizer() {
				return new MockLocalizerContext.MockLocalizer();
			}

			@Override
			public ParseOption parseHook_getParseOption(String formula, FormulaProperties properties) {
				return OPTION.get();
			}

			@Override
			public void parseHook_logParsingMetrics(String formula, FormulaProperties properties,
					ParsingMetrics parsingMetrics) {
				FormulaValidationHooks.super.parseHook_logParsingMetrics(formula, properties, parsingMetrics);
				assertEquals(OPTION.get(), parsingMetrics.parseOption);
				String diff = JvmMetrics.diff(parsingMetrics.before, parsingMetrics.after).toString();
				assertNotNull(diff);
				assertTrue(diff.contains("threadAllocatedBytes=0"));
			}
		});

        FormulaFactory oldFactory = FormulaEngine.getFactory();
        try {
            FormulaEngine.setFactory(TEST_FACTORY);
            assertTrue( evaluateBoolean("\"HiHo\"=FORMAT(\"{0}{1}\",\"Hi\",\"Ho\")")); 
	        String expression = "This is a test of format currency {!formatcurrency(\"USD\", 100.0)}";
	        assertTemplateFormula("This is a test of format currency USD 100.00", expression);

	        // Do the parse exception and make sure it works.
	        try {
	        	evaluateBoolean("\"HiHo\"="); 
	        } catch (FormulaParseException ex) {
	        	assertEquals("Syntax error.  Found 'end of formula'", ex.getMessage());
	        }
	        OPTION.set(ParseOption.PARSE_USING_BOTH_BUT_RETURN_ANTLR4);
	        try {
	        	evaluateBoolean("\"HiHo\"="); 
	        } catch (FormulaParseException ex) {
	        	assertEquals("Syntax error.  Found 'end of formula'", ex.getMessage());
	        }
	        OPTION.set(ParseOption.PARSE_USING_ANTLR2_ONLY);
	        try {
	        	evaluateBoolean("\"HiHo\"="); 
	        } catch (FormulaParseException ex) {
	        	assertEquals("Syntax error.  Found 'end of formula'", ex.getMessage());
	        }

        } finally {
            FormulaEngine.setFactory(oldFactory);
        }
    }
}
