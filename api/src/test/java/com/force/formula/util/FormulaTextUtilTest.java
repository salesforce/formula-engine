/*
 * Copyright 1999-2003, salesforce.com All rights reserved Company confidential Created on Jul 28, 2003
 */
package com.force.formula.util;

import java.util.*;

import org.junit.Assert;

import com.force.formula.util.FormulaTextUtil.TrieMatcher;

import junit.framework.TestCase;

/**
 * Unit tests for FormulaTextUtil.
 *
 * @author davem
 * @since 132
 */
public class FormulaTextUtilTest extends TestCase {
    private final static String BASICLATIN = "This is F0obar";
    private final static String NONLATIN = "\u3042\u3044\u3046\u65E5\u672C\u8A9E";
    private final static String SUPPLEMENTARY = "\uD844\uDE3D\uD842\uDFB7\uD83D\uDD25\uD83D\uDCE4\uD83C\uDDEF\uD83C\uDDF5";
    private final static String SUPPLEMENTARY_DEC = "&#135741;&#134071;&#128293;&#128228;&#127471;&#127477;";
    private final static String MIXED = BASICLATIN + NONLATIN + SUPPLEMENTARY;

    public FormulaTextUtilTest(String name) {
        super(name);
    }

    public void testEscapeInput() {
        Assert.assertEquals("This &lt;&gt;&amp;&quot;&copy;", FormulaTextUtil.escapeToHtml("This <>&\"©", false));
        Assert.assertEquals("This &amp;amp;", FormulaTextUtil.escapeToHtml("This &amp;", false));
    }


    public void testEscapeOutput() {
        Assert.assertEquals("This &lt;&gt;&amp;&quot;&#39;&copy;\n", FormulaTextUtil.escapeToHtml("This <>&\"'©\n", false));
        Assert.assertEquals("This &lt;&gt;&amp;&quot;&#39;&copy;<br>", FormulaTextUtil.escapeToHtml("This <>&\"'©\n", true));
    }




    public void testReplaceSimple() {
        Assert.assertEquals("Some 123Xata with XX'sX", FormulaTextUtil.replaceSimple("Some 123data with dd'sd", "d", "X"));
        Assert.assertEquals("Some 123data with X's", FormulaTextUtil.replaceSimple("Some 123data with dd's", "dd", "X"));
        Assert.assertEquals("Some 123XYtY with Z's", FormulaTextUtil.replaceSimple("Some 123data with dd's", new String[] {
            "dd", "d", "a" }, new String[] { "Z", "X", "Y" }));
        Assert.assertEquals("some 123data with dd'S", FormulaTextUtil.replaceSimple("Some 123data with dd's", new String[] {
            "S", "s" }, new String[] { "s", "S" }));
        Assert.assertEquals("Something completely different", FormulaTextUtil.replaceSimple("Some 123data with dd's",
            new String[] { "Some 123data with dd's" }, new String[] { "Something completely different" }));
        Assert.assertEquals("", FormulaTextUtil.replaceSimple("", "dd", "X"));
        Assert.assertNull(FormulaTextUtil.replaceSimple(null, "dd", "X"));
    }

    public void testReplaceSimple_WithArrays() {
        Assert.assertEquals("Some 123XYtY with Z's", FormulaTextUtil.replaceMultiple("Some 123data with dd's", TrieMatcher
            .compile(new String[] { "dd", "d", "a" }, new String[] { "Z", "X", "Y" })));
        Assert.assertEquals("some 123data with dd'S", FormulaTextUtil.replaceMultiple("Some 123data with dd's", TrieMatcher
            .compile(new String[] { "S", "s" }, new String[] { "s", "S" })));
        Assert.assertEquals("Something completely different", FormulaTextUtil.replaceMultiple("Some 123data with dd's",
            TrieMatcher.compile(new String[] { "Some 123data with dd's" },
                new String[] { "Something completely different" })));
        Assert.assertEquals("", FormulaTextUtil.replaceMultiple("", TrieMatcher.compile(new String[] { "dd", "xxx" },
            new String[] { "X", "Y" })));
        Assert.assertNull(FormulaTextUtil.replaceMultiple(null, TrieMatcher.compile(new String[] { "dd", "xxx" },
            new String[] { "X", "Y" })));

        Assert.assertEquals(FormulaTextUtil.replaceSimple("KOliver is a test nazi", "nazi", "dork"), FormulaTextUtil.replaceMultiple(
            "KOliver is a test nazi", TrieMatcher.compile(new String[] { "nazi" }, new String[] { "dork" })));
    }

    public void testEscapeForJavascriptString() throws Exception {
        Assert.assertEquals("This is a test", FormulaTextUtil.escapeForJavascriptString("This is a test"));
        Assert.assertEquals("This \\'is a test", FormulaTextUtil.escapeForJavascriptString("This 'is a test"));
        Assert.assertEquals("This \\\\is a test", FormulaTextUtil.escapeForJavascriptString("This \\is a test"));
        Assert.assertEquals("This \\\"is a test", FormulaTextUtil.escapeForJavascriptString("This \"is a test"));
        Assert.assertEquals("This \\nis a test", FormulaTextUtil.escapeForJavascriptString("This \nis a test"));
        Assert.assertEquals("This \\ris a test", FormulaTextUtil.escapeForJavascriptString("This \ris a test"));
        Assert.assertEquals("This \\u003Cb\\u003Eis a test", FormulaTextUtil.escapeForJavascriptString("This <b>is a test"));
        Assert.assertEquals("This is a \\/\\*test\\*\\/", FormulaTextUtil.escapeForJavascriptString("This is a /*test*/"));
        Assert.assertEquals("This is a \\*test\\*", FormulaTextUtil.escapeForJavascriptString("This is a *test*"));
        Assert.assertEquals(BASICLATIN, FormulaTextUtil.escapeForJavascriptString(BASICLATIN));
        Assert.assertEquals(NONLATIN, FormulaTextUtil.escapeForJavascriptString(NONLATIN));
        Assert.assertEquals(SUPPLEMENTARY, FormulaTextUtil.escapeForJavascriptString(SUPPLEMENTARY));
        Assert.assertEquals(MIXED, FormulaTextUtil.escapeForJavascriptString(MIXED));
    }

    public void testIsNullEmptyOrWhitespace() throws Exception {
        Assert.assertTrue(FormulaTextUtil.isNullEmptyOrWhitespace(null));
        Assert.assertTrue(FormulaTextUtil.isNullEmptyOrWhitespace(""));
        Assert.assertTrue(FormulaTextUtil.isNullEmptyOrWhitespace(" "));
        Assert.assertTrue(FormulaTextUtil.isNullEmptyOrWhitespace("\u3000\u3000\u3000"));
        Assert.assertTrue(FormulaTextUtil.isNullEmptyOrWhitespace("\t\r\n \u3000 \u3000  "));

        Assert.assertFalse(FormulaTextUtil.isNullEmptyOrWhitespace("   a  a"));
        Assert.assertFalse(FormulaTextUtil.isNullEmptyOrWhitespace(" \u3000A A\r\n\t\u3000"));
    }

    public void testPrettyPrintMap() {
        Map<String, String> m = new TreeMap<String, String>();
        m.put("x", "y");
        m.put("a", "b");
        Assert.assertEquals("a = b\nx = y\n", FormulaTextUtil.prettyPrintMap(m));
        Assert.assertNull(FormulaTextUtil.prettyPrintMap(null));
        Assert.assertEquals("", FormulaTextUtil.prettyPrintMap(new HashMap<>()));
    }

    public void testCollectionToStringEnclosed() {
        List<String> s1 = new ArrayList<String>();
        s1.add("a");
        List<String> s2 = new ArrayList<String>();
        s2.add("a");
        s2.add("b");
        List<String> s3 = new ArrayList<String>();
        s3.add("a");
        s3.add("b");
        s3.add("c");

        Assert.assertNull(FormulaTextUtil.collectionToStringEnclosed(null, null, null, null));
        Assert.assertNull(FormulaTextUtil.collectionToStringEnclosed(null, "", "", ""));

        Assert.assertEquals("", FormulaTextUtil.collectionToStringEnclosed(new HashSet<>(), null, null, null));

        Assert.assertEquals("[a]", FormulaTextUtil.collectionToStringEnclosed(s1, ",", "[", "]"));
        Assert.assertEquals("\"a\"", FormulaTextUtil.collectionToStringEnclosed(s1, ",", "\"", "\""));
        Assert.assertEquals("'a'", FormulaTextUtil.collectionToStringEnclosed(s1, ",", "'", "'"));
        Assert.assertEquals("<x>a</x>", FormulaTextUtil.collectionToStringEnclosed(s1, "", "<x>", "</x>"));
        Assert.assertEquals("_a", FormulaTextUtil.collectionToStringEnclosed(s1, "_and", "_", ""));

        Assert.assertEquals("[a],[b]", FormulaTextUtil.collectionToStringEnclosed(s2, ",", "[", "]"));
        Assert.assertEquals("\"a\",\"b\"", FormulaTextUtil.collectionToStringEnclosed(s2, ",", "\"", "\""));
        Assert.assertEquals("'a','b'", FormulaTextUtil.collectionToStringEnclosed(s2, ",", "'", "'"));
        Assert.assertEquals("<x>a</x><x>b</x>", FormulaTextUtil.collectionToStringEnclosed(s2, "", "<x>", "</x>"));
        Assert.assertEquals("_a_and_b", FormulaTextUtil.collectionToStringEnclosed(s2, "_and", "_", ""));

        Assert.assertEquals("[a],[b],[c]", FormulaTextUtil.collectionToStringEnclosed(s3, ",", "[", "]"));
        Assert.assertEquals("\"a\",\"b\",\"c\"", FormulaTextUtil.collectionToStringEnclosed(s3, ",", "\"", "\""));
        Assert.assertEquals("'a','b','c'", FormulaTextUtil.collectionToStringEnclosed(s3, ",", "'", "'"));
        Assert.assertEquals("<x>a</x><x>b</x><x>c</x>", FormulaTextUtil.collectionToStringEnclosed(s3, "", "<x>", "</x>"));
        Assert.assertEquals("_a_and_b_and_c", FormulaTextUtil.collectionToStringEnclosed(s3, "_and", "_", ""));
    }

    public void testEscapeToXml() throws Exception {
        Assert.assertNull(FormulaTextUtil.escapeToXml(null));
        Assert.assertNotNull(FormulaTextUtil.escapeToXml(null, false, true));
        Assert.assertNotNull(FormulaTextUtil.escapeToXml(null, true, true));
        Assert.assertEquals("", FormulaTextUtil.escapeToXml(""));
        Assert.assertEquals(" ", FormulaTextUtil.escapeToXml("\n"));
        Assert.assertEquals("\n", FormulaTextUtil.escapeToXml("\n", true, true));
        Assert.assertEquals("\n", FormulaTextUtil.escapeToXml("\n", true, false));
        Assert.assertEquals("This &lt;&gt;&amp;&quot;", FormulaTextUtil.escapeToXml("This <>&\""));
        Assert.assertEquals("This &lt;&gt;  &amp; &quot; ", FormulaTextUtil.escapeToXml("This <>\r\n&\r\"\n"));
        Assert
            .assertEquals("This &lt;&gt;\r\n&amp;\r&quot;\n", FormulaTextUtil.escapeToXml("This <>\r\n&\r\"\n", true, false));
        Assert.assertEquals("This \'&lt;&gt;\r\n&amp;\r&quot;\n", FormulaTextUtil.escapeToXml("This \'<>\r\n&\r\"\n", true,
            false));
        Assert.assertEquals("This &apos;&lt;&gt;\r\n&amp;\r&quot;\n", FormulaTextUtil.escapeToXml("This \'<>\r\n&\r\"\n",
            true, false, true));
        Assert.assertEquals(BASICLATIN, FormulaTextUtil.escapeToXml(BASICLATIN, true, false));
        Assert.assertEquals(NONLATIN, FormulaTextUtil.escapeToXml(NONLATIN, true, false));
        Assert.assertEquals(SUPPLEMENTARY_DEC, FormulaTextUtil.escapeToXml(SUPPLEMENTARY, true, false));
    }

    public void testIsIsoControlOrOddUnicode() throws Exception {
        Assert.assertFalse(FormulaTextUtil.isIsoControlOrOddUnicode(' '));
        // I'd love to add more tests here, but I don't know what an example
        // high/low surrogate char looks like...
    }

    public void testEscapeToHtml() {
        assertEquals("\n", FormulaTextUtil.escapeToHtml("\n"));
        assertEquals("<br>", FormulaTextUtil.escapeToHtml("\n", true));
        assertEquals("&lt;", FormulaTextUtil.escapeToHtml("<"));
        assertEquals("&gt;", FormulaTextUtil.escapeToHtml(">"));
        assertEquals("&amp;", FormulaTextUtil.escapeToHtml("&"));
        assertEquals("&quot;", FormulaTextUtil.escapeToHtml("\""));
        assertEquals("&#39;", FormulaTextUtil.escapeToHtml("\'"));
        assertEquals("<br>", FormulaTextUtil.escapeToHtml("\u2028"));
        assertEquals("<p>", FormulaTextUtil.escapeToHtml("\u2029"));
        assertEquals("&copy;", FormulaTextUtil.escapeToHtml("\u00a9"));

        assertEquals(BASICLATIN, FormulaTextUtil.escapeToHtml(BASICLATIN)); // not escaped
        assertEquals(NONLATIN, FormulaTextUtil.escapeToHtml(NONLATIN)); // not escaped
        assertEquals(SUPPLEMENTARY_DEC, FormulaTextUtil.escapeToHtml(SUPPLEMENTARY)); //escaped
    }
    
    public void testCodepointToCharRefDec() {
        assertEquals("&#1;", FormulaTextUtil.codepointToCharRefDec(0x1));
        assertEquals("&#15;", FormulaTextUtil.codepointToCharRefDec(0xF));
        assertEquals("&#16;", FormulaTextUtil.codepointToCharRefDec(0x10));
        assertEquals("&#256;", FormulaTextUtil.codepointToCharRefDec(0x100));
        assertEquals("&#4096;", FormulaTextUtil.codepointToCharRefDec(0x1000));
        assertEquals("&#65536;", FormulaTextUtil.codepointToCharRefDec(0x10000));
        assertEquals("&#131071;", FormulaTextUtil.codepointToCharRefDec(0x1FFFF));
        assertEquals("&#1114111;", FormulaTextUtil.codepointToCharRefDec(0x10FFFF));
        assertEquals("", FormulaTextUtil.codepointToCharRefDec(0x110000));
        assertEquals("", FormulaTextUtil.codepointToCharRefDec(-0x1));
    }
}
