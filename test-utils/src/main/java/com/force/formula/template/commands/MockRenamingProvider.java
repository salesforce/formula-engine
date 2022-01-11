package com.force.formula.template.commands;

import java.util.HashMap;
import java.util.Map;

import com.force.i18n.HumanLanguage;
import com.force.i18n.LanguageProviderFactory;
import com.force.i18n.Renameable;
import com.force.i18n.Renameable.StandardField;
import com.force.i18n.grammar.LanguageDictionary;
import com.force.i18n.grammar.Noun;
import com.force.i18n.grammar.RenamingProvider;
import com.google.common.collect.ImmutableMap;

/**
 * A mock renaming provider, where you provide it with a series of nouns, and it will return those nouns as the
 * "renamed" values when you specify it. Make sure you use this in a try finally
 *
 * <pre>
 * RenamingProvider curProvider = RenamingProviderFactory.get().getProvider();
 * try {
 *     MockRenamingProvider newProvider = new MockRenamingProvider(makeEnglishNoun("account", NounType.ENTITY,
 *             LanguageStartsWith.CONSONANT, "Client or Person", "Clients & People"));
 * } finally {
 *     RenamingProviderFactory.get().setProvider(curProvider);
 * }
 * </pre>
 *
 * @author stamm
 */
public class MockRenamingProvider implements RenamingProvider {
    private static final String CUSTOM_OBJECT_PREFIX = "01N";  // Needed because we can't refer to the udd here
    private final Map<? extends HumanLanguage, Map<String, Noun>> nounMap;
    private boolean useRenamedNouns = true;

    public MockRenamingProvider(Map<? extends HumanLanguage, Map<String, Noun>> nounMap) {
        assert nounMap != null;
        this.nounMap = nounMap;
    }

    /**
     * Construct a mock renaming provider based on the given nouns
     * 
     * @param nouns
     *            the nouns to use for this mock provider
     */
    public MockRenamingProvider(Noun... nouns) {
        this(makeNounMap(nouns));
    }

    /**
     * Convert a series of nouns into a noun map suitable for using in constructing a MockRenameable
     * 
     * @param nouns
     *            the test nouns
     * @return a noun map from the given nouns sorted by the language of those nouns
     */
    static Map<HumanLanguage, Map<String, Noun>> makeNounMap(Noun... nouns) {
        Map<HumanLanguage, Map<String, Noun>> result = LanguageProviderFactory.get().getNewMap();

        for (Noun noun : nouns) {
            HumanLanguage language = noun.getDeclension().getLanguage();
            Map<String, Noun> map = result.get(language);
            if (map == null) {
                map = new HashMap<String, Noun>(4);
                result.put(language, map);
            }
            map.put(noun.getName().toLowerCase(), noun);
        }
        return result;
    }

    @Override
    public Noun getRenamedNoun(HumanLanguage language, String key) {
        Map<String, Noun> m = nounMap.get(language);
        return m != null ? m.get(key.toLowerCase()) : null;
    }

    @Override
    public Noun getPackagedNoun(HumanLanguage language, String key) {
        return null; // This only applies to packaged entities
    }

    @Override
    public Noun getNoun(HumanLanguage language, Renameable key) {
        if (useRenamedNouns()) {
            Noun n = getRenamedNoun(language, key.getEntitySpecificDbLabelKey(Renameable.ENTITY_NAME));
            if (n != null) { return n; }
        }
        return key.getStandardNoun(language);
    }

    @Override
    public boolean isCustomKey(String key) {
        return key.startsWith(CUSTOM_OBJECT_PREFIX);
    }

    @Override
    public boolean isRenamed(HumanLanguage language, String key) {
        Map<String, Noun> m = nounMap.get(language);
        return m != null && m.containsKey(key);
    }

    @Override
    public boolean useRenamedNouns() {
        return useRenamedNouns;
    }

    public void setUseRenamedNouns(boolean useRenamedNouns) {
        this.useRenamedNouns = useRenamedNouns;
    }

    @Override
    public boolean supportOldGrammarEngine() {
        return false;
    }

    @Override
    public boolean displayMiddleNameInCalculatedPersonName() {
        return false;
    }

    @Override
    public boolean displaySuffixInCalculatedPersonName() {
        return false;
    }

    /**
     * Abstract renameable object. Since custom objects and standard objects are quite different; this handles the
     * commonality in getEntitySpecificDbLabelKey.
     * 
     * @author stamm
     */
    public abstract static class MockRenameable implements Renameable {
        private final String name;

        public MockRenameable(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getEntitySpecificDbLabelKey(String labelKey) {
            boolean isEntity = Renameable.ENTITY_NAME.equalsIgnoreCase(labelKey) || labelKey.equals(name);
            if (name.startsWith(CUSTOM_OBJECT_PREFIX)) {
                // for custom entity, return as custom entity ID for <Entity> tag,
                // and ID_name format
                return isEntity ? name : name + "_" + labelKey;
            } else if (isEntity) {
                // for standard entity name, always returns as lower case - "account"
                return name.toLowerCase();
            }

            // if this is custom compound noun such as <Entity_Record_Type> for standard entity,
            // retranslate them to the right name: For Account, translate to the account_record_type
            String namePrefix = labelKey.toLowerCase();
            return namePrefix.startsWith(Renameable.ENTITY_NAME_PREFIX)
                    ? name.toLowerCase() + "_" + namePrefix.substring(Renameable.ENTITY_NAME_PREFIX.length())
                    : namePrefix;
        }

        @Override
        public StandardField getRenameableFieldForKey(String labelKey) {
            if (Renameable.ENTITY_NAME_PREFIX.equals(labelKey)) { return MockStandardField.NAME; }
            return null;
        }

        @Override
        public boolean hasStandardLabel() {
            return !name.startsWith(CUSTOM_OBJECT_PREFIX);
        }
    }

    /**
     * Mock Custom Renameable Entity
     * 
     * @author stamm
     */
    public static class MockCustomRenameable extends MockRenameable {
        private final Noun baseNoun;
        private final Map<HumanLanguage, Noun> standardNouns;

        public MockCustomRenameable(String name, Noun englishNoun) {
            this(name, ImmutableMap.of(LanguageProviderFactory.get().getBaseLanguage(), englishNoun));
        }

        public MockCustomRenameable(String name, Map<HumanLanguage, Noun> standardNouns) {
            super(name);
            assert standardNouns != null && standardNouns.size() > 0 : "You must provide a standard noun";
            this.standardNouns = standardNouns;
            this.baseNoun = standardNouns.get(LanguageProviderFactory.get().getBaseLanguage());
            assert this.baseNoun != null : "You must provide a standard english noun";
        }

        @Override
        public String getLabel() {
            return baseNoun.getDefaultString(false);
        }

        @Override
        public String getLabelPlural() {
            return baseNoun.getDefaultString(true);
        }

        @Override
        public String getStandardFieldLabel(HumanLanguage language, StandardField field) {
            return baseNoun + ((MockStandardField)field).getKey();
        }

        @Override
        public Noun getStandardNoun(HumanLanguage language) {
            Noun n = standardNouns.get(language);
            return n != null ? n : baseNoun;
        }
    }

    /**
     * Mock Standard Renameable Entity
     * 
     * @author stamm
     */
    public static class MockExistingRenameable extends MockRenameable {
        private final Noun baseNoun;
        private final LanguageDictionary dict;

        public MockExistingRenameable(String name, LanguageDictionary dict) {
            super(name);
            this.dict = dict;
            baseNoun = dict.getNoun(name.toLowerCase(), false);
            assert this.baseNoun != null : "You must provide a standard english noun";
        }

        @Override
        public String getLabel() {
            return baseNoun.getDefaultString(false);
        }

        @Override
        public String getLabelPlural() {
            return baseNoun.getDefaultString(true);
        }

        @Override
        public String getStandardFieldLabel(HumanLanguage language, StandardField field) {
            return baseNoun + ((MockStandardField)field).getKey();
        }

        @Override
        public Noun getStandardNoun(HumanLanguage language) {
            if (language == LanguageProviderFactory.get().getBaseLanguage()) { return baseNoun; }
            Noun n = dict.getNoun(getName(), false);
            return n != null ? n : baseNoun;
        }
    }

    /**
     * The set of "standard" translateable fields
     * 
     * @author stamm
     */
    public enum MockStandardField implements StandardField {
        NAME(" Name"), FIRST_NAME(" FirstName"), LAST_NAME(" LastName"),;

        private final String key;

        MockStandardField(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }
}