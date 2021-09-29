/**
 * 
 */
package com.force.formula.util;

import java.net.URL;
import java.util.*;

import org.junit.Assert;
import org.junit.Test;

import com.force.formula.*;
import com.force.i18n.*;
import com.force.i18n.grammar.GrammaticalLocalizer;
import com.google.common.collect.ImmutableMap;

/**
 * @author stamm
 *
 */
public class BaseCompositeFormulaContextTest {	
	@Test
	public void testBaseCompositeFormulaContext() throws Exception {
		setLocalizer();
		BaseCompositeFormulaContext test = new BaseCompositeFormulaContext(null, null) {
		};
		try {
			test.fromDurableName("foo");
			Assert.fail();
		} catch (InvalidFieldReferenceException x) {
			
		}
		try {
			test.getBoolean("foo");
			Assert.fail();
		} catch (FormulaEvaluationException x) {
			
		}
		try {
			test.getObject("foo");
			Assert.fail();
		} catch (FormulaEvaluationException x) {
			
		}
		try {
			test.getString("foo", false);
			Assert.fail();
		} catch (FormulaEvaluationException x) {
			
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
	}
	
	class TestCompositeFormulaContext extends BaseCompositeFormulaContext {
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
	
	class TestGlobalFormulaContext extends NullFormulaContext {
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
	}

	
	FormulaReturnType getReturnType() {
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
	
	// Set a localizer that returns "section.name" for every label.
	void setLocalizer() {
		final GrammaticalLocalizer localizer = new GrammaticalLocalizer(null, null, null, null, null) {
			@Override
			public String getLabel(String section, String name) {
				return section + "." + name;
			}
			@Override
			public String getLabel(String section, String key, Object... args) {
				return section + "." + key;
			}
			
		};
		LocalizerFactory.set(new LocalizerProvider() {
			@Override
			public BaseLocalizer getLocalizer(Locale locale, Locale currencyLocale, HumanLanguage language, TimeZone timeZone) {
				return localizer;
			}
			@Override
			public BaseLocalizer getLocalizer(HumanLanguage language) {
				return localizer;
			}
			@Override
			public BaseLocalizer getLocalizer(Locale langLocale) {
				return localizer;
			}
			@Override
			public URL getLabelsDirectory() {
				return null;
			}
			@Override
			public BaseLocalizer getLabelLocalizer(HumanLanguage language) {
				return localizer;
			}
			@Override
			public BaseLocalizer getEnglishLocalizer() {
				return localizer;
			}
			@Override
			public BaseLocalizer getDefaultLocalizer() {
				return localizer;
			}
			@Override
			public LabelSet findLabelSet(HumanLanguage language) {
				return null;
			}
		});		
	}
	
}
