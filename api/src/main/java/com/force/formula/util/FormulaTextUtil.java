package com.force.formula.util;

import java.util.*;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;

import com.force.i18n.commons.text.DeferredStringBuilder;
import com.force.i18n.commons.util.collection.IntHashMap;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

/**
 * A package of generic text utility functions.
 * 
 * This is old enough that it was mostly implemented at salesforce, but replacements have been found
 *
 * @author davem,pnakada,jjordano,et. al.
 * @since the beginning
 */
public final class FormulaTextUtil {

	private FormulaTextUtil() {}

	
    private static final Escaper FORMULA_SEARCH_REPLACE = Escapers.builder()
    		.addEscape('\\', "\\\\")
    		.addEscape('\"', "\\\"")
    		.addEscape('\r', "\\n")
    		.addEscape('\n', "\\n")
    		.addEscape('\t', "\\t")
    		.build();
    		
    public static String escapeForFormulaString(String in) {
    	return FORMULA_SEARCH_REPLACE.escape(in);
    }
    
    
    /**
     * Escape output being sent to the user to be safe in HTML. Replaces &lt; &gt; &amp; &quot; etc. with their HTML escape
     * sequences. Does not translate \n's. Returns empty string if <code>value<code> is null.
     * <br><br>
     * Unless you are writing an element class or writing something that doesn't use elements,
     * <b>you probably shouldn't call this method</b>.
     * <br><br>
     * Calling this method in conjunction with an element is an error and will result in double-escaping.
     * <br><br>
     * The convention in the app is that all escaping is done at output time by Elements
     * and output should go through elements when possible. If you are using this method, you should think
     * carefully about what you are doing and decide if it's truly necessary to bypass elements.
     */
    public static String escapeToHtmlNoNulls(String value) {
        return value != null ? escapeToHtml(value) : "";
    }

    /**
     * Escape output being sent to the user to be safe in HTML. Replaces &lt; &gt; &amp; &quot; etc. with their HTML escape
     * sequences. Does not translate \n's.
     * <br><br>
     * Unless you are writing an element class or writing something that doesn't use elements,
     * <b>you probably shouldn't call this method</b>.
     * <br><br>
     * Calling this method in conjunction with an element is an error and will result in double-escaping.
     * <br><br>
     * The convention in the app is that all escaping is done at output time by Elements
     * and output should go through elements when possible. If you are using this method, you should think
     * carefully about what you are doing and decide if it's truly necessary to bypass elements.
     */
    public static String escapeToHtml(String value) {
        return escapeToHtml(value, false);
    }

    /**
     * Escape output being sent to the user to be safe in HTML. Replaces &lt; &gt; &amp; &quot; etc. with their HTML escape
     * sequences. Also translates '\n' to &lt;br&gt; if <code>escapeNewline</code> is <code>true</code>.
     * <br><br>
     * Unless you are writing an element class or writing something that doesn't use elements,
     * <b>you probably shouldn't call this method</b>.
     * <br><br>
     * Calling this method in conjunction with an element is an error and will result in double-escaping.
     * <br><br>
     * The convention in the app is that all escaping is done at output time by Elements
     * and output should go through elements when possible. If you are using this method, you should think
     * carefully about what you are doing and decide if it's truly necessary to bypass elements.
     */
    public static String escapeToHtml(String value, boolean escapeNewline) {
        if (value == null || value.length() == 0) {
            return value;
        }
        DeferredStringBuilder buf = new DeferredStringBuilder(value);
        // Optimized version of appendEscapedOutput where we can use appendQuicklyForEscapingWithoutSkips
        final int length = value.length();
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            // TODO: Is this switch statement faster than an IntHashMap or CharEscaper?  I'm guessing it is.
            switch (c) {
            case '\n':  if (escapeNewline) {
                buf.append("<br>");
            } else {
                buf.appendQuicklyForEscapingWithoutSkips(c);
            } break;
            case '<':   buf.append("&lt;"); break;
            case '>':   buf.append("&gt;"); break;
            case '&':   buf.appendAsDifferent("&amp;"); break;
            case '"':   buf.append("&quot;"); break;
            case '\'':   buf.append("&#39;"); break;
            case '\u2028':   buf.append("<br>"); break;
            case '\u2029':   buf.append("<p>"); break;
            case '\u00a9':   buf.append("&copy;"); break;  // ©
            default:                 
	            if (Character.isHighSurrogate(c) && (i + 1) < length && Character.isSurrogatePair(c, value.charAt(i + 1))) {
	                // Old browsers prefer decimal entity
	                buf.append(codepointToCharRefDec(Character.codePointAt(value, i)));
	                i++;
	            } else {
	                buf.appendQuicklyForEscapingWithoutSkips(c);
	            }
            }
        }
        return buf.toString();
    }
   
    
    /**
     * Generate the decimal XML character reference for specified Unicode code point.
     * Returns empty string for invalid code points.
     * Generated character reference will not be zero padded.
     *
     * @param codepoint Code point
     * @return Character reference in decimal format
     */
    public static String codepointToCharRefDec(int codepoint) {
        if (Character.isValidCodePoint(codepoint)) {
            return "&#" + Integer.toUnsignedString(codepoint, 10) + ";";
        }
        return ""; // TODO: Should we throw new IllegalArgumentException("Invalid codepoint"); ?
    }
   
    
    /**
     * @param c
     * @param delim
     *        what delimiter to use in between collection elements.
     * @param start
     *        what text to use at the beginning of each element
     * @param end
     *        what text to use at the end of each element
     * @return a <delim>-separated string from the contents of the given collection where each member is enclosed
     *         between <beginChar> and <endChar>
     */
    public static String collectionToStringEnclosed(Iterable<?> c, String delim, String start, String end) {
        if (c == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        String mid = new StringBuilder().append(end).append(delim).append(start).toString();
        int count = 0;
        for (Object o : c) {
            sb.append(sb.length() == 0 ? start : mid).append(o);
            count++;
        }
        if (count > 0) {
            sb.append(end);
        }
        return sb.toString();
    }
    
    /**
     * Note, if you are going to search/replace for the same set of source and target many times, you can get a
     * performance win by using the form of this call that takes a TrieMatcher instead.
     *
     * @return the replacement of all occurrences of src[i] with target[i] in s. Src and target are not regex's so this
     *         uses simple searching with indexOf()
     * @see #replaceMultiple(String, TrieMatcher)
     * @see #replaceChar(String, char, CharSequence)
     * @see #replace(String, String[], String[])
     */
    public static String replaceSimple(String s, String[] src, String[] target) {
        assert src != null && target != null && src.length > 0 && src.length == target.length;
        if (src.length == 1 && src[0].length() == 1) {
            return CharMatcher.is(src[0].charAt(0)).replaceFrom(s, target[0]);
        }
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int pos = 0;
        int limit = s.length();
        int lastMatch = 0;
        while (pos < limit) {
            boolean matched = false;
            for (int i = 0; i < src.length; i++) {
                if (s.startsWith(src[i], pos) && src[i].length() > 0) {
                    // we found a matching pattern - append the acculumation plus the replacement
                    sb.append(s.substring(lastMatch, pos)).append(target[i]);
                    pos += src[i].length();
                    lastMatch = pos;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                // we didn't match any patterns, so move forward 1 character
                pos++;
            }
        }
        // see if we found any matches
        if (lastMatch == 0) {
            // we didn't match anything, so return the source string
            return s;
        }

        // apppend the trailing portion
        sb.append(s.substring(lastMatch));

        return sb.toString();
    }
    
    /**
     * @return the replacement of src with target in s, using simple string replacement
     */
    //@edu.umd.cs.findbugs.annotations.SuppressWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
    public static String replaceSimple(String s, String src, String target) {
        assert src != null && src.length() > 0;
        if (s == null || s.length() == 0) {
            return s;
        }

        // Even though this may look redundant after the assertion above (which is disabled in production)
        // this precondition check is necessary to prevent infinite loop leading to catastrophic OOM (W-1140719)
        if (src == null || src.length() == 0) {
            // Need to check for null again to create proper message. Suppressed RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE
            String message = (src == null ? "Null" : "Empty") + " string pattern to replace";
            throw new IllegalArgumentException(message);
        }

        if (target == null)
         {
            target = "null";  // gag - but this is the way the replaceRegex works, so, I guess we need to be compatible
        }

        int pos = s.indexOf(src);
        if (pos == -1)
         {
            return s; // no match
        }

        int limit = s.length();
        StringBuilder buf = new StringBuilder(limit);
        buf.append(s.substring(0, pos)).append(target); // replace the first instance
        pos += src.length();
        while (pos < limit) { // and keep looking for more
            int p = s.indexOf(src, pos);
            if (p == -1)
             {
                break; // no more
            }
            buf.append(s.substring(pos, p)).append(target);
            pos = p + src.length();
        }
        if (pos < limit)
         {
            buf.append(s.substring(pos)); // append the tail
        }
        return buf.toString();
    }

    
    private static final String[] JS_IN = new String[] { "\\", "'", "\n", "\r", "\"", "!--", "<", ">", "\u2028", "\u2029", "\u0000", "/*", "*/", "*", "&#39;" };
    private static final String[] JS_OUT = new String[] { "\\\\", "\\'", "\\n", "\\r", "\\\"", "\\!--", "\\u003C", "\\u003E", "\\n", "\\u2029", "", "\\/\\*", "\\*\\/", "\\*", "\\&#39;" };

    private static final TrieMatcher JS_SEARCH_REPLACE = TrieMatcher.compile(JS_IN, JS_OUT);

    /**
     * Properly escapes strings to be displayed in Javascript Strings. This means that backslashes and single quotes are
     * escaped. Double quotes also since javascript string may use either single or double. And HTML comment start,
     * because IE recognizes it even in a javascript string. It is escaped by embedding backslash in it, which JS will
     * ignore, but breaks the pattern for the browser comment recognizer.
     * <br><br>
     * The Javascript escaping methods are the only methods you should call outside of Element classes.
     * Whenever you are passing Javascript into elements, you should escape any potentially dangerous portions
     * of the script.
     */
    public static String escapeForJavascriptString(String in) {
        return TrieMatcher.replaceMultiple(in, JS_SEARCH_REPLACE);
    }
    
    /**
     * Search and replace multiple strings in <code>s</code> given the the words and replacements given in
     * <code>TrieMatcher</code>.
     * <p>
     * Note, using a Trie for matching multiple strings can be much faster than the using
     * {@link #replace(String, String[], String[])}, however, due to the cost of creating the Trie, this is best used
     * when 1) you will reuse the Trie many times 2) you have a large set of strings your are searching on
     * <p>
     * Note, regexes aren't supported by this, see {@link #replace(String, String[], String[])}.
     *
     * @param s
     *        the text you are searching in
     * @param trieMatcher
     *        the trie representing the words to search and replace for
     * @return the text with the search words swapped by the replacements
     */
    public static final String replaceMultiple(String s, TrieMatcher trieMatcher) {
        if (s == null || trieMatcher == null || s.length() == 0) {
            return s;
        }

        // we don't use a DeferredStringBuilder because we don't expect to
        // reuse much of the original string. it's likely all or nothing.
        // Don't allocate the buffer until it's needed.
        StringBuilder dsb = null;

        int pos = 0;
        int length = s.length();
        boolean foundMatch = false;
        while (pos < length) {
            TrieMatch match = trieMatcher.match(s, pos);
            if (match == null) {
                if (!foundMatch) {
                    return s;
                } else {
                    // No more matches, so copy the rest and get gone
                    dsb.append(s, pos, s.length());
                    break;
                }
            }
            foundMatch = true;
            if (dsb == null) {
            	dsb = new StringBuilder(s.length() + 16);
            }

            // Copy up to the match position
            if (match.getPosition() > pos) {
                dsb.append(s, pos, match.getPosition());
            }

            // Append the replacement
            dsb.append(match.getReplacement());

            // Advance our current position
            pos = match.getPosition() + match.getWord().length();
        }
        return dsb.toString();
    }

    /**
     * Used by Formula Field: TRIM() function
     */
    public static String formulaTrim(String arg) {
        int left = 0;
        while ((left < arg.length()) && (arg.charAt(left) == ' ')) {
            left++;
        }
        int right = arg.length() - 1;
        while ((right >= 0) && (arg.charAt(right) == ' ')) {
            right--;
        }
        if (left > right) {
            return null;
        } else {
            return arg.substring(left, right + 1);
        }
    }

    public static boolean isNullEmptyOrWhitespace(CharSequence str) {
        if (str == null) {
            return true;
        }

        return isEmptyOrWhitespace(str);
    }

    
    public static boolean isEmptyOrWhitespace(CharSequence str) {
        int end = str.length();
        char c;
        for (int i = 0; i < end; i++) {
            if (!((c = str.charAt(i)) <= ' ' || Character.isWhitespace(c))) {
                return false;
            }
        }
        return true;
    }
    
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("[-+]?\\d*\\.?\\d+");
    /**
     * @return whether the 
     * @param s
     */
    public static boolean isNumeric(String s) {
    	if (s == null) return false;
    	return NUMERIC_PATTERN.matcher(s).matches();
    }
    
    /**
     * @return a debug string for a map, one entry per line
     */
    public static String prettyPrintMap(Map<?, ?> map) {
        if (map == null) {
            return null;
        } else if (map.size() == 0) {
        	return "";
        }
        return Joiner.on("\n").withKeyValueSeparator(" = ").join(map) + "\n";
    }
    
    /**
     * Escapes <code>String</code>s into valid xml. Similar to <code>escapeInput</code> except that it will also
     * replace control characters with spaces.
     * <br><br>
     * Unless you are writing an element class or writing something that doesn't use elements,
     * <b>you probably shouldn't call this method</b>.
     * <br><br>
     * Calling this method in conjunction with an element is an error and will result in double-escaping.
     * <br><br>
     * The convention in the app is that all escaping is done at output time by Elements
     * and output should go through elements when possible. If you are using this method, you should think
     * carefully about what you are doing and decide if it's truly necessary to bypass elements.
     */
    public static String escapeToXml(CharSequence input) {
        return escapeToXml(input, false, false);
    }

    public static String escapeToXml(CharSequence input, boolean allowNewLines, boolean convertNulls) {
        return escapeToXml(input, allowNewLines, convertNulls, false);
    }

    public static String escapeToXml(CharSequence input, boolean allowNewLines, boolean convertNulls,
            boolean escapeApos) {
        return escapeToXml(input, allowNewLines, convertNulls, escapeApos, false);
    }

    /**
     * Escapes <code>String</code>s into valid xml. Similar to <code>escapeInput</code> except that it will also
     * replace control characters with spaces.
     * <br><br>
     * Unless you are writing an element class or writing something that doesn't use elements,
     * <b>you probably shouldn't call this method</b>.
     * <br><br>
     * Calling this method in conjunction with an element is an error and will result in double-escaping.
     * <br><br>
     * The convention in the app is that all escaping is done at output time by Elements
     * and output should go through elements when possible. If you are using this method, you should think
     * carefully about what you are doing and decide if it's truly necessary to bypass elements.
     * @param input
     *        the text to escape
     * @param allowNewLines
     *        if false, newlines (\r or \n) are converted to spaces instead
     * @param convertNulls
     *        convert nulls to the empty string if treu
     * @param escapeApos
     *        Add a backslash in front of apostrophes to deal with MSXML's nonsense
     * @param preserveWhitespace
     *        if true, whitespace chars (as defined by {@link Character#isWhitespace(char)}) are not converted to
     *        spaces. If false, they may be converted to spaces if they are control characters. This argument is weaker
     *        than {@code allowNewLines}, so if {@code allowNewLines} is true but this argument is false, newlines will
     *        be preserved anyway. Since newlines are also whitespace, if {@code allowNewLines} is false but this
     *        argument is true, then newlines will still be preserved.
     */
    public static String escapeToXml(CharSequence input, boolean allowNewLines, boolean convertNulls,
            boolean escapeApos, boolean preserveWhitespace) {
        if (input == null || input.length() == 0) {
            return convertNulls ? "" : input == null ? null : "";
        }

        int limit = input.length();
        DeferredStringBuilder buf = new DeferredStringBuilder(input);
        for (int i = 0; i < limit; i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\n':
                    buf.append(allowNewLines ? '\n' : ' ');
                    break;
                case '\r':
                    buf.append(allowNewLines ? '\r' : ' ');
                    break;
                case '<':
                    buf.append("&lt;");
                    break;
                case '>':
                    buf.append("&gt;");
                    break;
                case '&':
                    buf.append("&amp;");
                    break;
                case '"':
                    buf.append("&quot;");
                    break;
                case '\'':
                    buf.append(escapeApos ? "&apos;" : "\'");
                    break;
                default:
                    if (Character.isHighSurrogate(c) && (i + 1) < limit && Character.isLowSurrogate(input.charAt(i + 1))) {
                        // Old browsers prefer decimal entity
                        buf.append(codepointToCharRefDec(Character.codePointAt(input, i)));
                        i++;
                    } else if (!(preserveWhitespace && Character.isWhitespace(c)) && isIsoControlOrOddUnicode(c)) {
                        buf.append(' ');
                    } else {
                        buf.append(c);
                    }
                    break;
            }
        }
        return buf.toString();
    }

    /**
     * Determines if the given input char is an iso-control character, undefined, or in an unusable Unicode block.
     */
    public static boolean isIsoControlOrOddUnicode(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return (Character.isISOControl(c) || !Character.isDefined(c) || block == Character.UnicodeBlock.HIGH_SURROGATES
                || block == Character.UnicodeBlock.HIGH_PRIVATE_USE_SURROGATES || block == Character.UnicodeBlock.LOW_SURROGATES);
    }

    // These are from Salesforce's commons-text, not yet open source.  The grammaticus version was rather stripped down, but we need the full one here.
    /**
     * An immutable trie used for fast multiple string search and replace.
     *
     * It's set of words and replacements are populated at initialization,
     * and the data structure creation is not the cheapest of operations,
     * so it is best used when the object will be used multiple times.
     *
     * @author koliver
     * @since 154
     * @see TrieMatcher#replaceMultiple(String, TrieMatcher)
     */
    @Immutable
    public static class TrieMatcher {

        private static final int DEFAULT_CAPACITY = 1; // trading initialization time for a small memory footprint

        /**
         * This is not the cheapest of operations.
         *
         * @param strings this is the list of words that make up the Trie.
         *      It is assumed that the lists are not modified once passed into the Trie
         * @param replacements the list of words that can be used to replace those words.
         *      It is assumed that the lists are not modified once passed into the Trie
         */
        public static TrieMatcher compile(String[] strings, String[] replacements) {
            return TrieMatcher.compile(Arrays.asList(strings), Arrays.asList(replacements));
        }

        /**
         * This is not the cheapest of operations.
         *
         * @param strings this is the list of words that make up the Trie.
         *      It is assumed that the lists are not modified once passed into the Trie
         * @param replacements the list of words that can be used to replace those words.
         *      It is assumed that the lists are not modified once passed into the Trie
         */
        public static TrieMatcher compile(List<String> strings, List<String> replacements) {
            return new TrieMatcher(strings, replacements);
        }

        /**
         * Search and replace multiple strings in <code>s</code> given the the words and replacements given in
         * <code>TrieMatcher</code>.
         * <p>
         * Note, using a Trie for matching multiple strings can be much faster than the using
         * {@link #replace(String, String[], String[])}, however, due to the cost of creating the Trie, this is best used
         * when 1) you will reuse the Trie many times 2) you have a large set of strings your are searching on
         * <p>
         * Note, regexes aren't supported by this, see {@link #replace(String, String[], String[])}.
         *
         * @param s
         *        the text you are searching in
         * @param trieMatcher
         *        the trie representing the words to search and replace for
         * @return the text with the search words swapped by the replacements
         */
        public static final String replaceMultiple(String s, TrieMatcher trieMatcher) {
            if (s == null || trieMatcher == null || s.length() == 0)
                return s;

            // we don't use a DeferredStringBuilder because we don't expect to
            // reuse much of the original string. it's likely all or nothing.
            // Don't allocate the buffer until it's needed.
            StringBuilder dsb = null;

            int pos = 0;
            int length = s.length();
            boolean foundMatch = false;
            while (pos < length) {
                TrieMatch match = trieMatcher.match(s, pos);
                if (match == null) {
                    if (!foundMatch) {
                        return s;
                    } else {
                        // No more matches, so copy the rest and get gone
                        dsb.append(s, pos, s.length());
                        break;
                    }
                }
                foundMatch = true;
                if (dsb == null) dsb = new StringBuilder(s.length() + 16);

                // Copy up to the match position
                if (match.getPosition() > pos)
                    dsb.append(s, pos, match.getPosition());

                // Append the replacement
                dsb.append(match.getReplacement());

                // Advance our current position
                pos = match.getPosition() + match.getWord().length();
            }
            return dsb.toString();
        }

        /**
         * @param s the term to search for the terms of the trie in
         * @return true if the any of the terms are contained in <code>s</code>
         */
        public boolean containedIn(CharSequence s) {
            TrieMatch match = match(s);
            return match != null;
        }

        /**
         * @param s the term to see if it starts with any terms of the trie
         */
        public boolean begins(CharSequence s) {
            TrieData match = begins(s, 0);
            return match != null;
        }

        /**
         * Find the next match in <code>s</code>.
         *
         * @param s the term to search for the terms of the trie in
         * @param start the 0-based position to start the search from.
         * @return null if no match found
         */
        @CheckForNull
        public String findIn(CharSequence s, int start) {
            TrieMatch match = match(s, start);
            if (match == null) return null;
            return match.getWord();
        }

        private static class TrieData {
            String word;
            String replacement;
            final IntHashMap<TrieData> nextChars;

            TrieData(IntHashMap<TrieData> next) {
                this.nextChars = next;
            }
        }

        private final IntHashMap<TrieData> root;
        private final List<String> words;
        private final int minWordLength;

        /**
         * Use the factory {@link #compile()} instead.
         */
        private TrieMatcher(List<String> strings, List<String> replacements) {
            if (strings == null) throw new NullPointerException();
            if (replacements == null) throw new NullPointerException();

            if (strings.size() != replacements.size()) {
                throw new IllegalArgumentException("Replacements must have same size, "+ replacements.size()
                    + ", as search strings " + strings.size());
            }

            this.words = Collections.unmodifiableList(strings);
            this.root = new IntHashMap<TrieData>(DEFAULT_CAPACITY);

            int minWordLen = Integer.MAX_VALUE;
            int wordIndex = 0;
            for (String s : strings) {
                IntHashMap<TrieData> current = this.root;

                int len = s.length();
                minWordLen = Math.min(minWordLen, len);
                for (int i = 0; i < len; i++) {
                    int ch = s.charAt(i);
                    TrieData next = current.get(ch);
                    if (next == null) {
                        next = new TrieData(new IntHashMap<TrieData>(DEFAULT_CAPACITY));
                        current.put(ch, next);
                    }
                    current = next.nextChars;

                    // if we're at the last char, store it and its replacement...
                    if (i+1 == len) {
                        next.word = s;
                        next.replacement = replacements.get(wordIndex);
                    }
                }
                wordIndex++;
            }

            this.minWordLength = minWordLen;
        }

        /**
         * See if the given string matches any of the given words in the Trie
         *
         * @return null if none are found.
         */
        TrieMatch match(CharSequence s) {
            return match(s, 0);
        }

        /**
         * See if the given string matches any of the given words in the Trie
         *
         * @param offset where to start looking inside of the given String.
         * @return null if none are found.
         */
        public TrieMatch match(CharSequence s, int offset) {
            if (s == null || s.length() == 0 || offset < 0) return null;

            int len = s.length();
            for (int i = offset; i < len; i++) {
                // optimize the case when we don't have enough room left to contain any matches
                if (i + this.minWordLength > len) break;

                TrieData data = contains(s, i);
                if (data != null) return new TrieMatch(i, data.word, data.replacement);
            }

            return null;
        }

        private TrieData begins(CharSequence s, int offset) {
            if (s == null || s.length() == 0 || offset < 0) return null;
            return contains(s, offset);
        }

        /**
         * @return null if not found
         */
        private TrieData contains(CharSequence s, int offset) {
            IntHashMap<TrieData> current = this.root;
            int len = s.length();
            LinkedList<TrieData> matches = null;
            TrieData firstMatch = null;

            for (int i = offset; i < len; i++) {
                int ch = s.charAt(i);
                TrieData nextData = current.get(ch);

                if (nextData == null) break;
                if (nextData.word != null) {
                    if (firstMatch == null){
                        firstMatch = nextData;
                    } else {
                        if (matches == null){
                            matches = new LinkedList<TrieData>();
                            matches.add(firstMatch);
                        }
                        matches.add(nextData);
                    }
                }

                current = nextData.nextChars;
            }

            if (firstMatch != null) {
                // only 1 match, so we know that's the one
                if (matches == null) return firstMatch;

                // else, we need to find the "highest" priority order word
                // as specified by the input to the trie
                for (String word : this.words) {
                    for (TrieData td : matches) {
                        if (word.equals(td.word)) return td;
                    }
                }
            }

            return null;
        }

    }

    /**
     * Struct returned by {@link TrieMatcher#match(CharSequence, int)} to represent a match.
     * 
     * @author koliver
     * @see TrieMatcher
     */    
    public static class TrieMatch {

        private final int position;
        private final String word;
        private final String replacement;

        TrieMatch(int position, String word, String replacement) {
            if (position < 0) throw new IllegalArgumentException(Integer.toString(position));
            if (word == null) throw new NullPointerException();
            if (replacement == null) throw new NullPointerException();
            this.position = position;
            this.word = word;
            this.replacement = replacement;
        }
        
        /**
         * @return position of where the match was in the source.
         * Eg, <pre>
         *    TrieMatcher trie = new TrieMatcher(String[]{"x"}, String[]{"Y"});
         *    TrieMatch match = trie.match("abcxdef");
         *    Assert.assertEquals(3, match.getPosition());
         * </pre>
         */
        public int getPosition() {
            return this.position;
        }

        /**
         * @return word in the trie that matched.
         * Eg, <pre>
         *    TrieMatcher trie = new TrieMatcher(String[]{"x"}, String[]{"Y"});
         *    TrieMatch match = trie.match("abcxdef");
         *    Assert.assertEquals("x", match.getWord());
         * </pre>
         */
        public String getWord() {
            return this.word;
        }
        
        /**
         * @return the replacement for word in the trie that matched.
         * Eg, <pre>
         *    TrieMatcher trie = new TrieMatcher(String[]{"x"}, String[]{"Y"});
         *    TrieMatch match = trie.match("abcxdef");
         *    Assert.assertEquals("Y", match.getReplacement());
         * </pre>
         */
        public String getReplacement() {
            return this.replacement;
        }
        
    }

    
}
