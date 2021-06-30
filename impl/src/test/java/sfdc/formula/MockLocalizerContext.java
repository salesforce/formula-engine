package sfdc.formula;

import java.net.URL;
import java.util.Locale;
import java.util.TimeZone;

import com.force.i18n.*;
import com.force.i18n.LanguageLabelSetDescriptor.GrammaticalLabelSetDescriptor;
import com.force.i18n.grammar.*;
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
        URL url = MockLocalizerContext.class.getResource("/sfdc/formula/formulaLabels.xml");
        HumanLanguage language = LanguageProviderFactory.get().getBaseLanguage();
        GrammaticalLabelSetDescriptor desc = new LabelSetDescriptorImpl(url, language, "formulaLabels", "formulaLabels.xml", "formulaNames.xml");
        return GrammaticalLocalizerFactory.getLoader(desc, null).getSet(language);
    }

    public static GrammaticalLabelSetDescriptor getLabelDesc() {
        URL url = MockLocalizerContext.class.getResource("/sfdc/formula/formulaLabels.xml");
        HumanLanguage language = LanguageProviderFactory.get().getBaseLanguage();
        return new LabelSetDescriptorImpl(url, language, "formulaLabels", "formulaLabels.xml", "formulaNames.xml");
    }

    public static void establishMock() {
    	LocalizerFactory.set(new GrammaticalLocalizerFactory(new GrammaticalLabelSetLoader(getLabelDesc())));
    }
}