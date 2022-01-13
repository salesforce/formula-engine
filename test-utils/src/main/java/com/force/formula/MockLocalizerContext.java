package com.force.formula;

import java.util.Locale;
import java.util.TimeZone;

import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.HumanLanguage;
import com.force.i18n.LanguageLabelSetDescriptor.GrammaticalLabelSetDescriptor;
import com.force.i18n.LanguageProviderFactory;
import com.force.i18n.LocalizerFactory;
import com.force.i18n.grammar.GrammaticalLabelSet;
import com.force.i18n.grammar.GrammaticalLocalizer;
import com.force.i18n.grammar.GrammaticalLocalizerFactory;
import com.force.i18n.grammar.parser.GrammaticalLabelSetLoader;

/**
 * 
 * @author stamm
 *
 */
public final class MockLocalizerContext {
    private MockLocalizerContext() {}

    public static final class MockLocalizer extends GrammaticalLocalizer {
    	public MockLocalizer() {
    		this(Locale.US, Locale.US, TimeZone.getTimeZone("GMT"), LanguageProviderFactory.get().getBaseLanguage(), getLabels());
    	}
        public MockLocalizer(Locale locale, Locale currencyLocale, TimeZone timeZone, HumanLanguage language,
                GrammaticalLabelSet labelSet) {
            super(locale, currencyLocale, timeZone, language, labelSet);
        }
    }
    	
    public static GrammaticalLabelSet getLabels() {
        HumanLanguage language = LanguageProviderFactory.get().getBaseLanguage();
        return FormulaI18nUtils.getFormulaEngineLabelsProvider(null).getSet(language);
    }

    public static void establishMock() {
        HumanLanguage language = LanguageProviderFactory.get().getBaseLanguage();
        GrammaticalLabelSetDescriptor desc = FormulaI18nUtils.getFormulaEngineLabelsDesc(language);
        LocalizerFactory.set(new GrammaticalLocalizerFactory(new GrammaticalLabelSetLoader(desc)));
    }
}