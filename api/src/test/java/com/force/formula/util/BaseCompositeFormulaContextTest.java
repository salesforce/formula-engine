/**
 * 
 */
package com.force.formula.util;

import org.junit.Assert;
import org.junit.Test;

import com.force.formula.FormulaContext;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaReturnType;
import com.force.formula.FormulaRuntimeContext;
import com.force.formula.FormulaTypeSpec;
import com.force.formula.InvalidFieldReferenceException;
import com.force.formula.UnsupportedTypeException;
import com.force.i18n.HumanLanguage;
import com.force.i18n.LanguageLabelSetDescriptor.GrammaticalLabelSetDescriptor;
import com.force.i18n.LanguageProviderFactory;
import com.force.i18n.LocalizerFactory;
import com.force.i18n.grammar.GrammaticalLocalizerFactory;
import com.force.i18n.grammar.parser.GrammaticalLabelSetLoader;
import com.google.common.collect.ImmutableMap;

/**
 * @author stamm
 *
 */
public class BaseCompositeFormulaContextTest {	
	@Test
	public void testBaseCompositeFormulaContext() throws Exception {
		setLocalizer();
		final BaseCompositeFormulaContext test = new TestCompositeFormulaContext();
		try {
			test.fromDurableName("foo");
			Assert.fail();
		} catch (InvalidFieldReferenceException x) {
		}
		
		try {
			test.toDurableName("foo");
			Assert.fail();
		} catch (UnsupportedTypeException x) {
		}
		
		// These should hit NullFormulaContext
		try {
			test.getBoolean("foo");
			Assert.fail();
		} catch (UnsupportedOperationException x) {
			
		}
		try {
			test.getObject("foo");
			Assert.fail();
		} catch (UnsupportedOperationException x) {
			
		}
		try {
			test.getString("foo", false);
			Assert.fail();
		} catch (UnsupportedOperationException x) {
		}
	}
	

	@Test
	public void testDefaultMethods() throws Exception {
		setLocalizer();
		BaseCompositeFormulaContext test = new TestCompositeFormulaContext();
		Assert.assertEquals("name", test.getName());
		Assert.assertEquals("name", test.getJavascriptReference());
		Assert.assertEquals(null, test.getSqlStyle());
		Assert.assertTrue(test.jsDatesAreStrings());
		Assert.assertFalse(test.useHighPrecisionJs());
		Assert.assertFalse(test.isCheckingSqlLengthLimit());
		Assert.assertFalse(test.isNew());
		Assert.assertFalse(test.isClone());
		Assert.assertFalse(test.isUIDeprecated());
		Assert.assertFalse(test.convertIdto18Digits());
		Assert.assertEquals("display", test.getGlobalProperties().getFormulaType().getDisplay());
		Assert.assertEquals("display", test.getGlobalProperties().getTopLevelFormulaType().getDisplay());
		// Property Methods
		Assert.assertNull(test.getProperty("hi"));
		test.setProperty("hi", Boolean.TRUE);
		Assert.assertTrue(test.getProperty("hi"));
		
		// MetaInfo for debugging
		Assert.assertEquals(ImmutableMap.of(
				"FormulaContext_ContextName", "TestCompositeFormulaContext",
				"FormulaContext_ReturnType", "text",
				"CompositeFormulaContext_TopLevelFormulaType", "display",
				"CompositeFormulaContext_DefaultContext", "{FormulaContext_ContextName = TestGlobalFormulaContext\n"
						+ "FormulaContext_ReturnType = text\n"
						+ "}"
				), test.getMetaInformation());
		
		Assert.assertEquals(test.getDefaultContext(), test.getFinalContext("foo"));
		Assert.assertNull(test.getAdditionalContexts());
		Assert.assertFalse(test.getGlobalProperties().getFormulaType().isTemplate());
	}
	
	static class TestCompositeFormulaContext extends BaseCompositeFormulaContext {
		public TestCompositeFormulaContext() {
			this(new TestGlobalFormulaContext(null), new FormulaTypeSpec() {
				@Override
				public int getMaxLength() {
					return 0;
				}
				@Override
				public String getDisplay() {
					return "display";
				}
				@Override
				public FormulaProperties getDefaultProperties() {
					return new FormulaProperties();
				}
			});
		}
		public TestCompositeFormulaContext(FormulaRuntimeContext defaultContext, FormulaTypeSpec topLevelFormulaType) {
			super(defaultContext, topLevelFormulaType);
		}
		@Override
		public String getName() {
			return "name";
		}
	}
	
	static class TestGlobalFormulaContext extends NullFormulaContext {
		public TestGlobalFormulaContext(FormulaContext outerContext) {
			super(outerContext);
		}
		@Override
		public FormulaReturnType getFormulaReturnType() {
			return getReturnType();
		}
		@Override
		public String getName() {
			return "global";
		}
		@Override
		public String fromDurableName(String reference)
				throws InvalidFieldReferenceException, UnsupportedTypeException {
			throw new InvalidFieldReferenceException("reference", "test");
		}
		@Override
		public String toDurableName(String name) throws InvalidFieldReferenceException, UnsupportedTypeException {
			throw new UnsupportedTypeException(name, getReturnType().getDataType());
		}
	}

	
	static FormulaReturnType getReturnType() {
		return new FormulaReturnType() {
			@Override
			public int getScale() {
				return 0;
			}
			@Override
			public String getName() {
				return "name";
			}
			@Override
			public FormulaDataType getDataType() {
				return new FormulaDataType() {
					@Override
					public boolean isSimpleText() {
						return true;
					}
					@Override
					public boolean isPercent() {
						return false;
					}
					@Override
					public boolean isInteger() {
						return false;
					}
					@Override
					public boolean isDecimal() {
						return false;
					}
					@Override
					public boolean isDateTime() {
						return false;
					}
					@Override
					public boolean isDateOnly() {
						return false;
					}
					@Override
					public boolean isBoolean() {
						return false;
					}
					@Override
					public String getName() {
						return "text";
					}
					@Override
					public String getLabel() {
						return "Text";
					}
					@Override
					public String getJavaName() {
						return "String";
					}
				};
			}
		};
	}


	public static void setLocalizer() {
        HumanLanguage language = LanguageProviderFactory.get().getBaseLanguage();
        GrammaticalLabelSetDescriptor desc = FormulaI18nUtils.getFormulaEngineLabelsDesc(language);
        LocalizerFactory.set(new GrammaticalLocalizerFactory(new GrammaticalLabelSetLoader(desc)));
	}
	
}
