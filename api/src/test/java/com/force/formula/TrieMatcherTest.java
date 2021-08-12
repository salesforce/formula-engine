/*
 * Copyright, 1999-2008, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula;

import java.util.*;

import com.force.formula.FormulaTextUtil.TrieMatch;
import com.force.formula.FormulaTextUtil.TrieMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

/**
 * @author koliver
 * @since 154
 */
public class TrieMatcherTest extends TestCase {

    public TrieMatcherTest(String name) {
        super(name);
    }

    public void testConstructor() throws Exception {
        try {
            TrieMatcher.compile((String[])null, new String[0]);
            fail();
        } catch (NullPointerException expected) {
            // expected
        }

        try {
            TrieMatcher.compile(new String[0], (String[])null);
            fail();
        } catch (NullPointerException expected) {
            // expected
        }

        try {
            TrieMatcher.compile(new String[0], new String[] { "" });
            fail();
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    public void testMatch_NoOffset() throws Exception {
        TrieMatcher trie = TrieMatcher.compile(new String[]{"abcd", "abe", "bad"},
            new String[] { "xyz", "123", "ummmmmm"});
        TrieMatch match;

        match = trie.match("labad");
        assertNotNull(match);
        assertEquals("bad", match.getWord());
        assertEquals(2, match.getPosition());
        assertEquals("ummmmmm", match.getReplacement());

        match = trie.match("abc");
        assertNull(match);

        match = trie.match("abce");
        assertNull(match);

        match = trie.match("abcdxxx");
        assertNotNull(match);
        assertEquals("abcd", match.getWord());
        assertEquals(0, match.getPosition());
        assertEquals("xyz", match.getReplacement());

        match = trie.match("xxxabcd");
        assertNotNull(match);
        assertEquals("abcd", match.getWord());
        assertEquals(3, match.getPosition());
        assertEquals("xyz", match.getReplacement());

        match = trie.match(null);
        assertNull(match);

        match = trie.match("");
        assertNull(match);
    }

    public void testMatch_WithOffset() throws Exception {
        TrieMatcher trie = TrieMatcher.compile(new String[]{"abcd", "abe", "bad"},
            new String[] { "xyz", "123", "ummmmmm"});
        TrieMatch match;

        match = trie.match("labad", 2);
        assertNotNull(match);
        assertEquals("bad", match.getWord());
        assertEquals(2, match.getPosition());
        assertEquals("ummmmmm", match.getReplacement());

        match = trie.match("labad", 3);
        assertNull(match);

        match = trie.match("xxxabcd", 3);
        assertNotNull(match);
        assertEquals("abcd", match.getWord());
        assertEquals(3, match.getPosition());
        assertEquals("xyz", match.getReplacement());

        match = trie.match("xxxabcd", 4);
        assertNull(match);

        match = trie.match(null, 1);
        assertNull(match);

        match = trie.match("", 1);
        assertNull(match);
    }

    public void testMatch_WithOverlapingWords() throws Exception {
        TrieMatcher trie = TrieMatcher.compile(new String[]{"abcd", "abc", "bad"},
            new String[] { "xyz", "123", "ummmmmm"});
        TrieMatch match = trie.match("XXXabcdXXX");
        assertNotNull(match);
        assertEquals("abcd", match.getWord());
        assertEquals(3, match.getPosition());
        assertEquals("xyz", match.getReplacement());
    }

    public void testContainedIn() throws Exception {
        String[] searchTerms = new String[] {"abc", "def"};
        TrieMatcher trie = TrieMatcher.compile(searchTerms, searchTerms);
        assertFalse(trie.containedIn("xxx"));
        assertFalse(trie.containedIn("ab de"));
        assertFalse(trie.containedIn(null));
        assertFalse(trie.containedIn(""));
        assertTrue(trie.containedIn("abc"));
        assertTrue(trie.containedIn("def"));
        assertTrue(trie.containedIn("abcdef"));
    }

    public void testFindIn() throws Exception {
        String[] searchTerms = new String[] {"abc", "def"};
        TrieMatcher trie = TrieMatcher.compile(searchTerms, searchTerms);
        assertNull(trie.findIn("xxx", 0));
        assertNull(trie.findIn("xxx", -1));
        assertNull(trie.findIn("xxx", 999));
        assertNull(trie.findIn("ab de", 0));
        assertNull(trie.findIn(null, 0));
        assertNull(trie.findIn("", 0));

        assertEquals("abc", trie.findIn("abc", 0));
        assertNull(trie.findIn("abc", 1));

        assertEquals("def", trie.findIn("def", 0));

        assertEquals("abc", trie.findIn("abcdef", 0));
        assertEquals("def", trie.findIn("abcdef", 1));
    }

    public void testBegins() throws Exception {
        String[] searchTerms = new String[] {"abc", "def"};
        TrieMatcher trie = TrieMatcher.compile(searchTerms, searchTerms);
        assertFalse(trie.begins("xxx"));
        assertFalse(trie.begins("ab de"));
        assertFalse(trie.begins(null));
        assertFalse(trie.begins(""));

        assertTrue(trie.begins("abc"));
        assertFalse(trie.begins("bc"));

        assertTrue(trie.begins("def"));

        assertTrue(trie.begins("abcdef"));
        assertTrue(trie.begins("defabc"));
        assertFalse(trie.begins("efabc"));
        assertFalse(trie.begins("labcdef"));
        assertFalse(trie.begins("ldefabc"));
    }
    
    private static List<String> cacheablePaths = Lists.newArrayList(
            "/dwnld",
            "/css/",
            "/img",
            "/EXT"
            );

    /**
     * Test method for {@link lib.text.TrieMatcher#compile(java.lang.String[], java.lang.String[])}.
     */
    public void testCompileStringArrayStringArray() {
        String[] empty = new String[0];

        try {
            TrieMatcher.compile(cacheablePaths.toArray(empty), cacheablePaths.toArray(empty));
        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Test method for {@link lib.text.TrieMatcher#compile(java.util.List, java.util.List)}.
     */
    public void testCompileListOfStringListOfString() {
        try {
            TrieMatcher.compile(cacheablePaths, cacheablePaths);
        } catch (Exception e) {
            fail();
        }
    }

    static final String[] OZYMANDIAS = { // Percy Bysse Shelley
        "I met a traveller from an antique land",
        "Who said: \"Two vast and trunkless legs of stone",
        "Stand in the desert. Near them on the sand,",
        "Half sunk, a shattered visage lies, whose frown",
        "And wrinkled lip and sneer of cold command",
        "Tell that its sculptor well those passions read",
        "Which yet survive, stamped on these lifeless things,",
        "The hand that mocked them and the heart that fed.",
        "And on the pedestal these words appear:",
        "`My name is Ozymandias, King of Kings:",
        "Look on my works, ye mighty, and despair!'",
        "Nothing beside remains. Round the decay",
        "Of that colossal wreck, boundless and bare,",
        "The lone and level sands stretch far away\"."
    };

    static final String[] KEYWORDS = {"traveller", "vast", "desert",
            "visage", "sneer", "sculptor", "survive", "fed",
            "pedestal", "name", "despair", "decay", "wreck", "stretch"};

    /**
     * Compile a matcher from a set of string->string pairs
     */
    @SafeVarargs
    private static TrieMatcher makeFromPairs(String ... subsArray) {
        List<String> words = new ArrayList<>(subsArray.length / 2);
        List<String> replacements =  new ArrayList<>(subsArray.length / 2);
        for (int i = 0; i < subsArray.length; i++) {
            (i % 2 == 0 ? words : replacements).add(subsArray[i]);
        }
        return TrieMatcher.compile(words, replacements);
    }

    private static TrieMatcher make(String...wordsArray) {
        List<String> words = Arrays.asList(wordsArray);
        return TrieMatcher.compile(words, words);
    }

    /**
     * Test method for {@link lib.text.TrieMatcher#replaceMultiple(java.lang.String, lib.text.TrieMatcher)}.
     */
    public void testReplaceMultiple() {
        TrieMatcher matcher = makeFromPairs(
                "away", "about",
                "a", "one",
                "Ozymandias", "LarryMoe",
                "colossal", "oracular"
                );
        String expected = "I met one troneveller from onen onentique lonend"+"\n"
                + "Who soneid: \"Two vonest onend trunkless legs of stone";
        String result = TrieMatcher.replaceMultiple(Joiner.on("\n").join(Arrays.copyOfRange(OZYMANDIAS,0,2)), matcher);
        assertEquals(expected, result);
    }

    /**
     * Test method for {@link lib.text.TrieMatcher#containedIn(java.lang.String)}.
     */
    public void testContainedInText() {
        TrieMatcher matcher = make(KEYWORDS);

        for (String line : OZYMANDIAS) {
            assertTrue(matcher.containedIn(line));
        }

        assertFalse(matcher.containedIn("Seven songs for seven seals"));
        assertFalse(matcher.containedIn("decadence may sculpt decline"));
    }

    private List<String> runOfChars(char from, char to) {
        ArrayList<String> chars = new ArrayList<String>();
        assertTrue("'"+ from + "' > '"+to+"'", from <= to);
        for (char c = from; c <= to; c++) {
            chars.add(Character.toString(c));
        }
        return chars;
    }

    /**
     * Test method for {@link lib.text.TrieMatcher#begins(java.lang.String)}.
     */
    public void testBeginsText() {
        List<String> chars = runOfChars('A', 'Z');
        TrieMatcher matcher = TrieMatcher.compile(chars, chars);

        for (int i=0; i<OZYMANDIAS.length; i++) {
            if (i==9) {
                assertFalse(matcher.begins(OZYMANDIAS[i]));
            } else {
                assertTrue(matcher.begins(OZYMANDIAS[i]));
            }
        }
    }

    /**
     * Test method for {@link lib.text.TrieMatcher#findIn(java.lang.String, int)}.
     */
    public void testFindInText() {
        TrieMatcher matcher = make(KEYWORDS);

        for (int i=0; i<OZYMANDIAS.length; i++) {
            String result = matcher.findIn(OZYMANDIAS[i], 0);
            assertNotNull(result);
            assertEquals(KEYWORDS[i], result);
        }
    }

    /**
     * Test method for {@link lib.text.TrieMatcher#match(java.lang.String, int)}.
     */
    public void testMatchStringInt() {
        TrieMatcher matcher = TrieMatcher.compile(KEYWORDS, KEYWORDS);

        for (int i=0; i<OZYMANDIAS.length; i++) {
            final String line = OZYMANDIAS[i];
            final String word = KEYWORDS[i];
            final int position = line.indexOf(word);
            TrieMatch match = matcher.match(line);
            assertEquals(word, match.getWord());
            assertEquals(word, match.getReplacement());
            assertEquals(position, match.getPosition());

            for (int p=1; p<line.length(); p++) {
                match = matcher.match(line, p);
                if (p<=position) {
                    assertEquals(word, match.getWord());
                    assertEquals(word, match.getReplacement());
                    assertEquals(position, match.getPosition());
                } else {
                    assertNull(match);
                }
            }
        }
    }
}