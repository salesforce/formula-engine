package com.force.formula.impl;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import com.force.formula.*;
import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.parser.gen4.*;
import com.force.formula.sql.FormulaWithSql;
import com.force.formula.sql.InvalidFormula;
import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.commons.text.TextUtil;
import com.google.common.base.Stopwatch;

/**
 * Formula Utils
 */
public class FormulaUtils {
    // Some standard bits to use
    public static final int PRODUCES_SQL_ERROR_COLUMN = 0;   // If you need a guard
    public static final int JS_COULD_BE_NULL = 1;  // 
    public static final int REFERENCES_SUBFORMULA = 2;  // If a formula references another formula (custom formula field)
    public static final int PRODUCES_HTML = 3;  // If a formula produces HTML.
	
    static final Pattern REFERENCE_PATTERN = Pattern.compile("\\{!([^}]+)\\}");
    static final String TREAT_NULL_AS_NULL_ANNOTATION = "[treatNullAsNull]";
    private static final Pattern ANNOTATION_PATTERN = Pattern.compile("(\\{\\!)?(\\[\\w+\\])(.*)", Pattern.DOTALL);
    public static final String DISABLE_ANNOTATION = "[disable]";
    public static final String DOMAIN_DELIMETER = ":"; // used in the names of polymorphic spanning relationships
    public static final Pattern FIELD_REFERENCE_PATTERN = Pattern.compile("([^:]+)(?::(.*))?");


    public static String decode(NameDetokenizer context, String encodedSource, FormulaProperties properties)
            throws FormulaException {
        if (!FormulaInfoFactory.isEncoded(encodedSource)) {
        	return encodedSource;	
        }

        // Pick out just the {!ID:blah} encoded refs
        StringBuffer decodedSource = new StringBuffer();
        String decodeSourcePrefix = decodeSourcePrefix(encodedSource, properties);

        Matcher matcher = FormulaInfo.ENCODED_REFERENCE_PATTERN.matcher(decodeSourcePrefix);
        while (matcher.find()) {
            String reference = matcher.group(1);

            String decodedReference;
            try {
                decodedReference = context.fromDurableName(reference);
            } catch (InvalidFieldReferenceException e) {
                if (properties != null && properties.getThrowOnUnavailableField() && !properties.isDisabled()) {
                    throw new InvalidFieldReferenceException(reference, e.getReason(),
                            FormulaI18nUtils.getLocalizer().getLabel("Formula_General", "InvalidFields", reference)
                                    + " Context: " + context.getClass().getName(),
                            e);
                }
                // Replace the field with reference - not much else we can do...
                decodedReference = reference;
            }

            padIfNeeded(decodedSource, decodeSourcePrefix, matcher, decodedReference);
        }

        matcher.appendTail(decodedSource);

        return decodedSource.toString();
    }

    public static String[] getReferencesFromEncodedSource(String encodedSource) {
        // Uses regex to extract field refs from an encoded formula
        if (!FormulaInfoFactory.isEncoded(encodedSource)) { throw new UnsupportedOperationException(
                "Formula must be in encoded form!"); }
        Set<String> references = extractRefsFromEncodedSource(decodeSourcePrefix(encodedSource, null));

        return references.toArray(new String[references.size()]);
    }

    static FormulaAST getAST(String source, FormulaProperties properties) throws FormulaException {
        return parse(decodeSourcePrefix(source, null), properties);
    }

    // Compile to AST
    public static FormulaAST parse(String source, FormulaProperties properties) throws FormulaException {
        FormulaValidationHooks.ParsingMetrics parsingMetrics = new FormulaValidationHooks.ParsingMetrics();
        parsingMetrics.parseOption = FormulaValidationHooks.get().parseHook_getParseOption(source, properties);

        parsingMetrics.before = FormulaValidationHooks.get().parseHook_getJvmMetrics();
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            switch (parsingMetrics.parseOption) {
                case PARSE_USING_ANTLR4_ONLY:
                    return parseWithANTLR4(source, properties, parsingMetrics);
                case PARSE_USING_BOTH_BUT_RETURN_ANTLR2:
                    return parseWithBoth(source, properties, false, parsingMetrics);
                case PARSE_USING_BOTH_BUT_RETURN_ANTLR4:
                    return parseWithBoth(source, properties, true, parsingMetrics);
                default: //PARSE_USING_ANTLR2_ONLY
                    return parseWithANTLR2(source, properties, parsingMetrics);
            }
        }
        finally {
            parsingMetrics.totalElapsed = stopwatch.stop().elapsed();
            parsingMetrics.after = FormulaValidationHooks.get().parseHook_getJvmMetrics();
            FormulaValidationHooks.get().parseHook_logParsingMetrics(source, properties, parsingMetrics);
        }
    }

    public static FormulaAST parseWithBoth(String source, FormulaProperties properties, boolean returnANTLR4Result,
                                           FormulaValidationHooks.ParsingMetrics parsingMetrics) throws FormulaException {
        FormulaAST antlr2AST = null;
        FormulaAST antlr4AST = null;
        FormulaException antlr2Exception = null;
        FormulaException antlr4Exception = null;

        try {
            antlr2AST = parseWithANTLR2(source, properties, parsingMetrics);
        }
        catch(FormulaException e) {
            antlr2Exception = e;
        }

        if(FormulaValidationHooks.get().parseHook_shouldSkipANTLR4(source, properties, parsingMetrics.antlr2Elapsed)) {
            if(antlr2Exception != null) {
                throw antlr2Exception;
            }
            return antlr2AST;
        }

        try {
            antlr4AST = parseWithANTLR4(source, properties, parsingMetrics);
        }
        catch(FormulaException e) {
            antlr4Exception = e;
        }
        catch(Exception e) {
            FormulaValidationHooks.get().parseHook_antlr2vs4Failure("ANTLR4 threw an unknown exception: " + e.getMessage(), source, properties);
            if(antlr2Exception != null) {
                throw antlr2Exception;
            }
            return antlr2AST;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            if(antlr2Exception != null && antlr4Exception != null) {
                throw returnANTLR4Result ? antlr4Exception : antlr2Exception;
            }

            if(antlr2Exception != null) {
                FormulaValidationHooks.get().parseHook_antlr2vs4Failure("ANTLR2 threw an exception but ANTLR4 did not", source, properties);
                if(returnANTLR4Result) {
                    return antlr4AST;
                }
                else {
                    throw antlr2Exception;
                }
            }

            if(antlr4Exception != null) {
                FormulaValidationHooks.get().parseHook_antlr2vs4Failure("ANTLR4 threw an exception but ANTLR2 did not", source, properties);
                if(returnANTLR4Result) {
                    throw antlr4Exception;
                }
                else {
                    return antlr2AST;
                }
            }

            try {
                compareFormulaASTs(antlr2AST, antlr4AST);
            }
            catch(Exception e) {
                FormulaValidationHooks.get().parseHook_antlr2vs4Failure("ASTs mismatch: " + e.getMessage(), source, properties);
            }
            catch(StackOverflowError e) {
                FormulaValidationHooks.get().parseHook_antlr2vs4Failure("StackOverflow", source, properties);
            }

            return returnANTLR4Result ? antlr4AST : antlr2AST;
        }
        finally {
            parsingMetrics.comparisonElapsed = stopwatch.stop().elapsed();
        }
    }

    public static FormulaAST parseWithANTLR2(String source, FormulaProperties properties,
                                             FormulaValidationHooks.ParsingMetrics parsingMetrics) throws FormulaException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            return parseWithANTLR2(source, properties);
        }
        finally {
            parsingMetrics.antlr2Elapsed = stopwatch.stop().elapsed();
        }
    }

    public static FormulaAST parseWithANTLR2(String source, FormulaProperties properties) throws FormulaException {
        // Compile to AST
        final boolean template = properties.getParseAsTemplate();
        final boolean reference = properties.getParseAsReference();
        FormulaLexerImpl formulaLexer = new FormulaLexerImpl(new StringReader(source), template);
        FormulaParserImpl parser = new FormulaParserImpl(formulaLexer);
        parser.setFormulaLexer(formulaLexer);
        parser.setFailOnEmbeddedFormulaExceptions(properties.getIgnoreFailOnEmbeddedFormulaParserExceptionsForParsing() || properties.getFailOnEmbeddedFormulaExceptions());
        parser.setAllowSubscripts(properties.getAllowSubscripts());

        try {
            if (template) {
                parser.template_root();
            } else if (reference) {
                parser.fieldReferenceRoot();
            } else {
                parser.formula();
            }
        } catch (antlr.ANTLRException e) {
            throw new FormulaParseException(e);
        }

        return (FormulaAST)parser.getAST();
    }

    public static FormulaAST parseWithANTLR4(String source, FormulaProperties properties,
                                             FormulaValidationHooks.ParsingMetrics parsingMetrics) throws FormulaException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            return parseWithANTLR4(source, properties);
        }
        finally {
            parsingMetrics.antlr4Elapsed = stopwatch.stop().elapsed();
        }
    }

    public static FormulaAST parseWithANTLR4(String source, FormulaProperties properties) throws FormulaException {
        try {
            if(properties.getParseAsTemplate()) {
                Antlr4FormulaTemplateParser parser = new Antlr4FormulaTemplateParser(properties);
                parseTemplate(source, parser);
                return parser.getRoot();
            }

            return parseFormulaWithANTLR4(source, properties, 0);
        }
        catch(FormulaException e) {
            throw e;
        }
        catch(Exception e) { //unexpected exception
            FormulaValidationHooks.get().parseHook_logANTLR4Failure(source, properties, e);
            throw e;
        }
        catch(StackOverflowError e) {
            FormulaValidationHooks.get().parseHook_logANTLR4Failure(source, properties, e);
            return parseWithANTLR2(source, properties);
        }
    }
    
    public static interface FormulaTemplateParser {
        void handleFormula(String formula, int start) throws FormulaException ;
        void handleText(String text);
        
    }
    
    static final class Antlr4FormulaTemplateParser implements FormulaTemplateParser {
        private final FormulaProperties properties;
        private final FormulaAST root;
        private final FormulaAST templateAST;
        public Antlr4FormulaTemplateParser(FormulaProperties properties) {
            root = new FormulaAST();
            root.setText("root");
            root.setType(FormulaTokenTypes.ROOT);

            templateAST = new FormulaAST();
            templateAST.setText("template");
            templateAST.setType(FormulaTokenTypes.FUNCTION_CALL);
            root.addChild(templateAST);
            
            this.properties = properties;
        }
        
        @Override
        public void handleFormula(String formula, int start) throws FormulaException {
            try {
                FormulaAST formulaAST = parseFormulaWithANTLR4(formula, properties, start);
                templateAST.addChild(formulaAST.getFirstChild());
            }
            catch(FormulaException e) {
                if(properties.getIgnoreFailOnEmbeddedFormulaParserExceptionsForParsing() || properties.getFailOnEmbeddedFormulaExceptions()) {
                    throw e;
                }
                else {
                    templateAST.addChild(createEmptyStringAST());
                }
            }
            
        }

        @Override
        public void handleText(String text) {
            FormulaAST templateStringAST = createTemplateStringAST(text, 0, text.length());
            templateAST.addChild(templateStringAST);
        }
        
        public FormulaAST getRoot() {
            return root;
        }
        
    }

    public static void parseTemplate(String source, FormulaTemplateParser parser) throws FormulaException {
        //we are differentiating single and double quotes to avoid closing a double quote with a single quote and vice versa ( e.g {!func("someone's text")} )
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean inComment = false;
        boolean inFormula = false;
        int innerFormulaStartIndex = 0;
        int previousIndex = 0;

        for(int i = 0; i < source.length(); i++) {
            if(inFormula && !inComment && !inSingleQuotes && !inDoubleQuotes && source.charAt(i) == '/' && i+1 < source.length() && source.charAt(i+1) == '*') {
                inComment = true;
            }
            else if(inFormula && inComment && source.charAt(i) == '*' && i+1 < source.length() && source.charAt(i+1) == '/') {
                inComment = false;
            }
            //ignoring escaped single/double quotes
            //not worried about i = 0 because we are in formula so i is at least 2
            else if(inFormula && !inComment && !inSingleQuotes && source.charAt(i) == '"' && numberOfPrecedingBackslashes(source, i) % 2 == 0) {
                inDoubleQuotes = !inDoubleQuotes;
            }
            else if(inFormula && !inComment && !inDoubleQuotes && source.charAt(i) == '\'' && numberOfPrecedingBackslashes(source, i) % 2 == 0) {
                inSingleQuotes = !inSingleQuotes;
            }
            else if(inFormula && !inComment && !inSingleQuotes && !inDoubleQuotes && source.charAt(i) == '}') {
                inFormula = false;
                String innerFormula = source.substring(innerFormulaStartIndex+2, i);
                if (innerFormula.length() > 0) {
                    parser.handleFormula(innerFormula, innerFormulaStartIndex+2);
                }
                previousIndex = i + 1;
            }
            else if(!inFormula && source.charAt(i) == '{' && i+1 < source.length() && source.charAt(i+1) == '!') {
                inFormula = true;
                innerFormulaStartIndex = i;
                i = i+1;
                if (previousIndex < innerFormulaStartIndex) {
                    parser.handleText(source.substring(previousIndex, innerFormulaStartIndex));
                }
            }
        }

        if(inFormula) { //never closed embedded formula
            antlr.CommonToken eofToken = new antlr.CommonToken(FormulaTokenTypes.EOF, null);
            eofToken.setLine(1);
            eofToken.setColumn(source.length() + 1);

            antlr.ANTLRException e;
            if(inComment) {
                e = createMismatchCharException('\uFFFF', '*', 1, source.length()); //not "+ 1" because '*' is the second to last character
            }
            else if(inSingleQuotes) {
                e = createMismatchCharException('\uFFFF', '\'', 1, source.length() + 1);
            }
            else if(inDoubleQuotes) {
                e = createMismatchCharException('\uFFFF', '"', 1, source.length() + 1);
            }
            else {
                e = new antlr.MismatchedTokenException(null, eofToken, FormulaTokenTypes.END_EMBEDDED_FORMULA, false, null);
            }

            throw new FormulaParseException(e);
        }
        else if (previousIndex < source.length()) {
            parser.handleText(source.substring(previousIndex));
        }
    }

    /**
     * Counts the number of backslashes that precede the given index (i.e. index is excluded).
     * If index is out of range, 0 is returned.
     */
    static int numberOfPrecedingBackslashes(String str, int index) {
        if(index > str.length()) {
            return 0;
        }

        int count = 0;
        for(int i = index - 1; i >= 0; i--) {
            if(str.charAt(i) == '\\') {
                count++;
            }
            else {
                break;
            }
        }

        return count;
    }

    private static FormulaAST createTemplateStringAST(String source, int startIndex, int endIndex) {
        String str = source.substring(startIndex, endIndex);
        if (str.length() == 0) {
            return null;
        }

        FormulaAST ast = new FormulaAST();
        ast.setText(str);
        ast.setType(FormulaTokenTypes.TEMPLATE_STRING_LITERAL);
        return ast;
    }

    private static FormulaAST createEmptyStringAST() {
        FormulaAST ast = new FormulaAST();
        ast.setText("\"\"");
        ast.setType(FormulaTokenTypes.STRING_LITERAL);
        return ast;
    }

    private static antlr.MismatchedCharException createMismatchCharException(char found, char expecting, int line, int column) {
        antlr.MismatchedCharException e = new antlr.MismatchedCharException();
        e.foundChar = found;
        e.expecting = expecting;
        e.mismatchType = antlr.MismatchedCharException.CHAR;
        e.line = line;
        e.column = column;
        return e;
    }

    private static FormulaAST parseFormulaWithANTLR4(String source, FormulaProperties properties, int startIndex) throws FormulaException {
        if(source == null || source.length() == 0) {
            throw new FormulaParseException(new antlr.ANTLRException(new NullPointerException()));
        }

        ANTLRInputStream stream = new ANTLRInputStream(source);
        FormulaLexerImpl4 lexer = new FormulaLexerImpl4(stream);
        BufferedTokenStream tokens = new BufferedTokenStream(lexer);
        FormulaParserImpl4 parser = new FormulaParserImpl4(tokens);

        Map<Integer, Integer> numberOfCharactersBeforeLine = ANTLR4Utils.getNumberOfCharactersBeforeEachLine(source);
        @SuppressWarnings("deprecation")
		ANTLR4GrammarVisitor visitor = new ANTLR4GrammarVisitor(parser, source, startIndex, numberOfCharactersBeforeLine);

        parser.setAllowSubscripts(properties.getAllowSubscripts());
        parser.setErrorHandler(new ANTLR4ErrorStrategy());
        lexer.removeErrorListeners();

        try {
            ParseTree tree;
            if (properties.getParseAsReference()) {
                tree = parser.fieldReferenceRoot();
            } else {
                tree = parser.formula();
            }

            FormulaAST ast = visitor.visit(tree);
            return ast;
        } catch (RecognitionException e) {
            throw new FormulaParseException(ANTLR4Utils.convertANTLR4ExceptionToANTLR2(e, startIndex, numberOfCharactersBeforeLine));
        }
    }

    public static Set<String> extractRefsFromEncodedSource(String encodedSource) {
        Set<String> references = new HashSet<String>();

        Matcher matcher = FormulaInfo.ENCODED_REFERENCE_PATTERN.matcher(encodedSource);
        while (matcher.find()) {
            references.add(matcher.group(1));
        }

        return references;
    }

    static void padIfNeeded(StringBuffer dest, String source, Matcher matcher, String reference) {
        // Look left of the match to see if we an alphanumeric character and a whitespace as a delimiter if needed
        String padding = "";
        int posToCheck = matcher.start() - 1;
        if (posToCheck >= 0) {
            char onePriorToMatch = source.charAt(posToCheck);
            if (Character.isLetterOrDigit(onePriorToMatch)) {
                padding = " ";
            }
        }

        matcher.appendReplacement(dest, padding);
        dest.append(reference);
    }

    private static String decodeSourcePrefix(String encodedSource, FormulaProperties properties) {
        if (!FormulaInfoFactory.isEncoded(encodedSource)) {
        	return encodedSource;
        }

        // Strip off the encoding prefix
        String decodedSource = encodedSource.substring(FormulaInfoFactory.ENCODED_PREFIX.length());

        boolean treatNullNumberAsZero = true;
        boolean isDisabled = false;
        if (properties == null || !properties.getParseAsTemplate()) {
            while (true) {
                Matcher matcher = ANNOTATION_PATTERN.matcher(decodedSource);
                if (!matcher.matches()) break;
                String annotation = matcher.group(2);
                if (TREAT_NULL_AS_NULL_ANNOTATION.equals(annotation)) {
                    treatNullNumberAsZero = false;
                } else if (DISABLE_ANNOTATION.equals(annotation)) {
                    isDisabled = true;
                } else {
                    break;
                }
                // the rest of the string after the annotation with optional leading bracket
                decodedSource = (matcher.group(1) == null ? "" : matcher.group(1)) + matcher.group(3);
            }
        }

        if (properties != null) {
            properties.setTreatNullNumberAsZero(treatNullNumberAsZero);
            properties.setDisabled(isDisabled);
        }

        return decodedSource;
    }

    /**
     * Returns polymorphic spanning field name consisting of the name of the field and the
     * name of the domain it spans to. The field and domain are separated by a colon delimeter.
     * @param fieldName name of the field on the object, i.e. "Owner"
     * @param domainName name of the entity this field spans to, i.e. "User"
     * @return name of this span, i.e. "Owner:User"
     */
    public static String getPolymorphicFieldName(String fieldName, String domainName) {
        return fieldName + getFormattedDomain(domainName);
    }

    /**
     * Returns the domain name to be appended to the end of a field name for polymorphic fields
     */
    public static String getFormattedDomain(String domain) {
        return DOMAIN_DELIMETER + domain;
    }

    /**
     * For polymorphic spanning fields, returns a 2 item array where the first item is the name
     * of the field (ex. "Owner") and the second item is the name of the domain it spans to
     * (ex. "User"). For non-polymorphic fields, this should return a 1 item array containing
     * the original field name.
     * @param fieldName the extended name of the field, ex. "Owner:User"
     * @return an array containing the field name and optionally the domain of that field, ex. {"Owner","User"}
     */
    public static List<String> getNameAndDomainFromFieldName(String fieldName) {
        List<String> nameAndDomain = TextUtil.splitSimple(DOMAIN_DELIMETER, fieldName);
        if(nameAndDomain != null) assert nameAndDomain.size() <= 2;
        return nameAndDomain;
    }

    public static boolean isTypeSobjectRow(FormulaDataType type) {
    	return type.isId() && "SObjectRow".equals(type.getName());
    }

    @Deprecated
    public static String getTypeName(Type type) {
        return FormulaTypeUtils.getTypeName(type);
    }

    @Deprecated
    public static String getTypeLabel(Type type) {
        return FormulaTypeUtils.getTypeLabel(type);
    }

    @Deprecated
    public static boolean isTypeText(Type type) {
        return FormulaTypeUtils.isTypeText(type);
    }

    /**
     * Same as isTypeText, but in cases where we think supporting an ID is ugly.
     * @param type
     * @return
     */
    @Deprecated
    public static boolean isTypeTextUgly(Type type) {
        return FormulaTypeUtils.isTypeTextUgly(type);
    }

    @Deprecated
    public static boolean canCastTo(Type lhs, Type rhs) {
        return FormulaTypeUtils.canCastTo(lhs, rhs);
    }

    /**
     * @return whether the values on the left and right hand side have a common super type
     * (i.e. they could be compared)
     *
     * For historical reasons, *all* IdFormulaTypes can be compared even if they have the wrong type
     *
     * @param lhs the left hand side of a comparison/equality
     * @param rhs the right hand side of a comparison/equality
     */
    @Deprecated
    public static boolean hasCommonSuperType(Type lhs, Type rhs) {
        return FormulaTypeUtils.hasCommonSuperType(lhs, rhs);
    }

    /**
     * For objects involved with a comparison, determine if there is a common supertype of
     * the either side, and if so, return it.
     * @param lhs
     * @param rhs
     * @return
     */
    @Deprecated
    public static Type getCommonSuperType(Type lhs, Type rhs) {
        return FormulaTypeUtils.getCommonSuperType(lhs, rhs);
    }

    /**
     * Given a formula and context, return the set of references (without "record." prepended)
     *
     * @param form the formula, possibly null or invalid, to convert
     * @param context Formula context
     * @return a sorted tree-set of field references in the formula
     */
    public static Set<String> getFieldReferences(Formula form, FormulaContext context) {
        if (form == null) return null;
        if (form instanceof InvalidFormula) return Collections.emptySet();
        FormulaCommandVisitorImpl.FieldReferenceCommandVisitor visitor = new FormulaCommandVisitorImpl.FieldReferenceCommandVisitor(
                context, false);
        ((FormulaWithSql)form).visitFormulaCommands(visitor);
        return new TreeSet<String>(visitor.getRefValues().keySet());
    }

    public static void compareFormulaASTs(FormulaAST ast1, FormulaAST ast2) throws Exception {
        compareFormulaASTs(ast1, ast2, true);
    }

        public static void compareFormulaASTs(FormulaAST ast1, FormulaAST ast2, boolean compareTokens) throws Exception {
        if(ast1 == null && ast2 == null) {
            return;
        }

        if(ast1 == null) {
            throw new Exception("ast1 is null while ast2 is not null");
        }

        if(ast2 == null) {
            throw new Exception("ast2 is null while ast1 is not null");
        }

        if(compareTokens) {
            compareTokens(ast1.getToken(), ast2.getToken());
        }

        //compare public methods
        if(!ast1.getText().equals(ast2.getText())) {
            throw new Exception("ASTs' texts do not match: " + ast1.getText() + " != " + ast2.getText());
        }

        if(ast1.getType() != ast2.getType()) {
            throw new Exception("ASTs' types do not match: " + ast1.getText() + " != " + ast2.getText());
        }

        if(ast1.getLine() != ast2.getLine()) {
            throw new Exception("ASTs' lines do not match: " + ast1.getLine() + " != " + ast2.getLine());
        }

        if(ast1.getColumn() != ast2.getColumn()) {
            throw new Exception("ASTs' columns do not match: " + ast1.getColumn() + " != " + ast2.getColumn());
        }

        if(ast1.getDataType() != ast2.getDataType()) {
            throw new Exception("ASTs' data types do not match: " + ast1.getDataType() + " != " + ast2.getDataType());
        }

        if(ast1.getColumnType() != ast2.getColumnType()) {
            throw new Exception("ASTs' column types do not match: " + ast1.getColumnType() + " != " + ast2.getColumnType());
        }

        if(ast1.isConstantExpression() != ast2.isConstantExpression()) {
            throw new Exception("ASTs' isConstantExpressions do not match: " + ast1.isConstantExpression() + " != " + ast2.isConstantExpression());
        }

        if(ast1.isDynamicReferenceBase() != ast2.isDynamicReferenceBase()) {
            throw new Exception("ASTs' isDynamicReferenceBases do not match: " + ast1.isDynamicReferenceBase() + " != " + ast2.isDynamicReferenceBase());
        }

        if(ast1.isLiteral() != ast2.isLiteral()) {
            throw new Exception("ASTs' isLiterals do not match: " + ast1.isLiteral() + " != " + ast2.isLiteral());
        }

        if(ast1.getNumberOfChildren() != ast2.getNumberOfChildren()) {
            throw new Exception("ASTs' number of children do not match: " + ast1.getNumberOfChildren() + " != " + ast2.getNumberOfChildren());
        }

        compareFormulaASTs((FormulaAST) ast1.getFirstChild(), (FormulaAST) ast2.getFirstChild(), compareTokens);
        compareFormulaASTs((FormulaAST) ast1.getNextSibling(), (FormulaAST) ast2.getNextSibling(), compareTokens);
    }

    public static void compareTokens(antlr.Token t1, antlr.Token t2) throws Exception {
        if(t1 == null && t2 == null) {
            return;
        }

        if(t1 == null && t2 != null) {
            throw new Exception("ast1's token is null while ast2's token is not null");
        }

        if(t1 != null && t2 == null) {
            throw new Exception("ast2's token is null while ast1's token is not null");
        }

        //compare public methods
        if(!t1.getText().equals(t2.getText())) {
            throw new Exception("ASTs' tokens' texts do not match: " + t1.getText() + " != " + t2.getText());
        }

        if(t1.getType() != t2.getType()) {
            throw new Exception("ASTs' tokens' types do not match: " + t1.getType() + " != " + t2.getType());
        }

        if(t1.getLine() != t2.getLine()) {
            throw new Exception("ASTs' tokens' lines do not match: " + t1.getLine() + " != " + t2.getLine());
        }

        if(t1.getColumn() != t2.getColumn()) {
            throw new Exception("ASTs' tokens' columns do not match: " + t1.getColumn() + " != " + t2.getColumn());
        }

        if(t1.getFilename() != t2.getFilename()) {
            throw new Exception("ASTs' tokens' filenames do not match: " + t1.getFilename() + " != " + t2.getFilename());
        }
    }
}
