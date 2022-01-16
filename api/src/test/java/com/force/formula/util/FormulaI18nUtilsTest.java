package com.force.formula.util;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.force.formula.FormulaEngine;
import com.force.formula.FormulaEngineHooks;
import com.force.i18n.HumanLanguage;
import com.force.i18n.LabelRef;
import com.force.i18n.LanguageLabelSetDescriptor.GrammaticalLabelSetDescriptor;
import com.force.i18n.LanguageProviderFactory;
import com.force.i18n.LocalizerFactory;
import com.force.i18n.grammar.GrammaticalLocalizer;
import com.force.i18n.grammar.GrammaticalLocalizerFactory;
import com.force.i18n.grammar.parser.GrammaticalLabelSetLoader;


/**
 * Test of FormulaI18nUtils.
 * @author stamm
 * @since 0.2
 */
public class FormulaI18nUtilsTest {
    /**
     * Validate that the default formula-engine labels are translated into German and Japanese
     */
    @Test
    public void testTranslatedLabels() {
        HumanLanguage language = LanguageProviderFactory.get().getBaseLanguage();
        GrammaticalLabelSetDescriptor desc = FormulaI18nUtils.getFormulaEngineLabelsDesc(language);
        LocalizerFactory.set(new GrammaticalLocalizerFactory(new GrammaticalLabelSetLoader(desc)));
        Assert.assertEquals("Text",  FormulaI18nUtils.renderLabelReference(new LabelRef("FormulaFieldExceptionDataTypes", "java.lang.String")));
        Assert.assertEquals("Number",  FormulaI18nUtils.renderLabelReference(new LabelRef("FormulaFieldExceptionDataTypes", "java.math.BigDecimal")));

        FormulaEngineHooks oldHooks = FormulaEngine.getHooks();
        FormulaEngineHooks def = new FormulaEngineHooks() {
            @Override
            public GrammaticalLocalizer getLocalizer() {
                return (GrammaticalLocalizer) LocalizerFactory.get().getLocalizer(Locale.GERMAN);
            }
        };
        try {
            FormulaEngine.setHooks(def);            
            Assert.assertEquals("Text",  FormulaI18nUtils.renderLabelReference(new LabelRef("FormulaFieldExceptionDataTypes", "java.lang.String")));
            Assert.assertEquals("Zahl",  FormulaI18nUtils.renderLabelReference(new LabelRef("FormulaFieldExceptionDataTypes", "java.math.BigDecimal")));
        } finally {
            FormulaEngine.setHooks(oldHooks);            
        }
        
        def = new FormulaEngineHooks() {
            @Override
            public GrammaticalLocalizer getLocalizer() {
                return (GrammaticalLocalizer) LocalizerFactory.get().getLocalizer(Locale.JAPANESE);
            }
        };
        try {
            FormulaEngine.setHooks(def);            
            Assert.assertEquals("テキスト",  FormulaI18nUtils.renderLabelReference(new LabelRef("FormulaFieldExceptionDataTypes", "java.lang.String")));
            Assert.assertEquals("数字",  FormulaI18nUtils.renderLabelReference(new LabelRef("FormulaFieldExceptionDataTypes", "java.math.BigDecimal")));
        } finally {
            FormulaEngine.setHooks(oldHooks);            
        }
    }

}
