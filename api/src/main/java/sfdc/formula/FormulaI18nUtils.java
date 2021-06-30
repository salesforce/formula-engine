package sfdc.formula;

import com.force.i18n.grammar.GrammaticalLocalizer;

/**
 * Class with some sample Internationalization and Localization Utilities
 *
 * @author stamm
 * @since 146
 */
public enum FormulaI18nUtils {
    INSTANCE;  // a modern singleton

    public static GrammaticalLocalizer getLocalizer() {
    	return (GrammaticalLocalizer) FormulaEngine.getHooks().getLocalizer();
    }
}
