header {
/*
 * Copyright, 1999-2010, SALESFORCE.com
 * All Rights Reserved
 * Company Confidential
 */
package sfdc.formula.parser.gen;

import antlr.collections.AST;
import sfdc.formula.impl.FormulaAST;

}

class FormulaParser extends Parser;

options {
    k = 2;							// two token lookahead
    exportVocab=SfdcFormula;	    // Call its vocabulary "SfdcFormula"
    codeGenMakeSwitchThreshold = 2;	// Some optimizations
    codeGenBitsetTestThreshold = 3;
    defaultErrorHandler = false;	// Don't generate parser error handlers
    buildAST = true;
}

tokens {
    ROOT; TEMPLATE; UNARY_MINUS; UNARY_PLUS; FUNCTION_CALL; NOUNESCAPE_STRING_LITERAL; TEMPLATE_STRING_LITERAL; TRUE="true"; FALSE="false"; NULL="null"; NOT="not";
    DYNAMIC_REF; DYNAMIC_REF_ROOT; DYNAMIC_REF_IDENT;
}

{
    protected AST handleException(Exception x) throws RecognitionException, TokenStreamException {
        // Do nothing, handled in subclasses
        throw new UnsupportedOperationException(x);
    }

    protected void reparent(FormulaAST parent, FormulaAST firstChild) {
        // Do nothing, handled in subclasses
        throw new UnsupportedOperationException();
    }

     public void setAllowSubscripts(boolean subscripts) {
         this.allowSubscripts = subscripts;
     }

     private boolean allowSubscripts = false;
}


template_root
    :   template { #template_root = #([ROOT, "root"], #template_root); } EOF!
    ;

template
    :   c:template_content { #template = #(#([FUNCTION_CALL, "template"], #template), c); reparent((FormulaAST)#template, (FormulaAST)#c); }
    ;

template_content
    :   (plain_text | embedded_formula)*
    ;

plain_text
    {
        StringBuilder result = new StringBuilder("");
    }
    :   (c:PLAIN_TEXT! { result.append(c.getText()); } )+
        { #plain_text = #([TEMPLATE_STRING_LITERAL, result.toString()], #plain_text); }
    ;


embedded_formula
    :   BEGIN_EMBEDDED_FORMULA! expression END_EMBEDDED_FORMULA!
        exception // for rule
            catch [Exception x] {
                embedded_formula_AST = handleException(x);
            }
    ;

formula
    :   expression { #formula = #([ROOT, "root"], #formula); } EOF!
    ;

fieldReferenceLiteral
    :   IDENT
    ;


constant
    :	NUMBER
    |	STRING_LITERAL
    |	map
    ;

map
    :	c: map_content     { #map = #([FUNCTION_CALL, "map"], #map); reparent((FormulaAST)#map, (FormulaAST)#c);}
    ;

map_content
    : 	LSQUAREBRACKET! i:IDENT! { astFactory.addASTChild(currentAST, astFactory.create(NOUNESCAPE_STRING_LITERAL, i.getText())); } EQUAL! expression
        (COMMA! i2:IDENT! { astFactory.addASTChild(currentAST, astFactory.create(NOUNESCAPE_STRING_LITERAL, i2.getText() )); } EQUAL! expression)* RSQUAREBRACKET!
    |	LSQUAREBRACKET! STRING_LITERAL EQUAL! expression
        (COMMA! STRING_LITERAL EQUAL! expression)* RSQUAREBRACKET!
    ;

// expressions
// Note that most of these expressions follow the pattern
//   thisLevelExpression :
//	   nextHigherPrecedenceExpression
//		   (OPERATOR nextHigherPrecedenceExpression)*
// which is a standard recursive definition for a parsing an expression.
// The operators in java have the following precedences:
//	lowest  (7)  OR
//			(6)  AND
//			(5)  = !=
//			(4)  < <= > >=
//			(3)  +(binary) -(binary)
//			(2)  * / %
//			(1)  +(unary) -(unary)  not(type)
//
// Note that the above precedence levels map to the rules below...
// Once you have a precedence chart, writing the appropriate rules as below
//   is usually very straightforward

// the mother of all expressions
expression
    :	logicalOrExpression
    ;

// logical or
logicalOrExpression
    :	logicalAndExpression (INFIX_OR^ { #INFIX_OR.setText("or"); #INFIX_OR.setType(OR); } logicalAndExpression)*
    | 	OR^ LPAREN! logicalOrExpression (COMMA! logicalOrExpression)+ RPAREN!
    ;


// logical and
logicalAndExpression
    :	equalityExpression (INFIX_AND^ { #INFIX_AND.setText("and"); #INFIX_AND.setType(AND); } equalityExpression)*
    | 	AND^ LPAREN! logicalOrExpression (COMMA! logicalOrExpression)+ RPAREN!
    ;



// equality/inequality (=/!=)
equalityExpression
    :	relationalExpression ((NOT_EQUAL^ | EQUAL^ |
            NOT_EQUAL2^ { #NOT_EQUAL2.setText("<>"); #NOT_EQUAL2.setType(NOT_EQUAL); } |
            EQUAL2^ { #EQUAL2.setText("="); #EQUAL2.setType(EQUAL); })
                relationalExpression)*
    ;


// boolean relational expressions
relationalExpression
    :	additiveExpression
        (	(	(	LT^
                |	GT^
                |	LE^
                |	GE^
                )
                additiveExpression
            )*
        )
    ;

// binary addition/subtraction
additiveExpression
    :	multiplicativeExpression ((PLUS^ | MINUS^ | CONCAT^) multiplicativeExpression)*
    ;


// multiplication/division
multiplicativeExpression
    :	exponentExpression (EXPONENT^ exponentExpression)*
    ;

exponentExpression
    :	unaryExpression ((STAR^ | DIV^ ) unaryExpression)*
    ;


unaryExpression
    :	primaryExpression
    |   MINUS^ { #MINUS.setType(UNARY_MINUS); } unaryExpression
    |	PLUS^ { #PLUS.setType(UNARY_PLUS); } unaryExpression
    |	(NOT^ | BANG^ { #BANG.setText("not"); #BANG.setType(NOT); }) unaryExpression
    ;


// the basic element of an expression
primaryExpression
    :	fieldReference
    |	constant
    |	TRUE
    |	FALSE
    |	NULL
    |	LPAREN! logicalOrExpression RPAREN!
    | 	functionCall
    ;


functionCall
    :	IDENT^ { #IDENT.setType(FUNCTION_CALL); } LPAREN! (expression (COMMA! expression)*)? RPAREN!
    ;


// This is root goal for retrieving an "lvalue"; i.e. an expression for a reference that can be assigned to
fieldReferenceRoot
    :   fieldReference { #fieldReferenceRoot = #([DYNAMIC_REF_ROOT, "referenceroot"], #fieldReferenceRoot); } EOF!
    ;

// Currently we don't actually go through this for fieldReferenceLiterals because they don't use the evaluator to get the reference.
fieldReference
    : fieldReferenceLiteral
    | {this.allowSubscripts}? subscriptedExpression
    ;

subscriptedExpression
    : fieldReferenceLiteral ({#subscriptedExpression = #([DYNAMIC_REF, "[]"], #subscriptedExpression);
                     ((FormulaAST)#subscriptedExpression).setToken(LT(1));}  fieldSelector)+
    ;

fieldSelector
    : LSQUAREBRACKET! expression RSQUAREBRACKET!
    | DOT! IDENT {#IDENT.setType(DYNAMIC_REF_IDENT); }
    ;

class FormulaLexer extends Lexer;

options {
    exportVocab=SfdcFormula;	// call the vocabulary "SfdcFormula"
    testLiterals=false;			// don't automatically test for literals
    k=4;						// four characters of lookahead
    charVocabulary='\u0003'..'\uFFFE';
    codeGenBitsetTestThreshold=20;
    caseSensitive=false;
    caseSensitiveLiterals=false;
}

{
     public boolean getInFormula() {
         return this.inFormula;
     }

     public void setInFormula(boolean inFormula) {
         this.inFormula = inFormula;
     }

     private boolean inFormula = true;
}

// OPERATORS
LPAREN			:	{ this.inFormula }? '('	;
RPAREN			:	{ this.inFormula }? ')'	;
COLON			:	{ this.inFormula }? ':'	;
COMMA			:	{ this.inFormula }? ','	;
EQUAL			:	{ this.inFormula }? "="	;
EQUAL2			:	{ this.inFormula }? "=="	;
BANG			:	{ this.inFormula }? '!'	;
BNOT			:	{ this.inFormula }? '~'	;
NOT_EQUAL		:	{ this.inFormula }? "<>"	;
NOT_EQUAL2		:	{ this.inFormula }? "!="	;
DIV				:	{ this.inFormula }? '/'	;
PLUS			:	{ this.inFormula }? '+'	;
MINUS			:	{ this.inFormula }? '-'	;
STAR			:	{ this.inFormula }?'*'	;
GE				:	{ this.inFormula }? ">="	;
GT				:	{ this.inFormula }? ">"	;
LE				:	{ this.inFormula }? "<="	;
LT				:	{ this.inFormula }? '<'	;
SEMI			:	{ this.inFormula }? ';'	;
DOT				:	{ this.inFormula }? "."	;
CONCAT			:	{ this.inFormula }? "&"	;
EXPONENT		:	{ this.inFormula }? "^"	;
LSQUAREBRACKET	:	{ this.inFormula }? '['	;
RSQUAREBRACKET	:	{ this.inFormula }? ']'	;
MAPTO			:	{ this.inFormula }? "=>"	;
INFIX_OR		:	{ this.inFormula }? "||"	;
INFIX_AND		:	{ this.inFormula }? "&&"	;


// Whitespace -- ignored
WS	:	{ this.inFormula }?
        (	' '
        |	'\t'
        |	'\f'
            // handle newlines
        |	(	options {generateAmbigWarnings=false;}
            :	"\r\n"	// Evil DOS
            |	'\r'	// Macintosh
            |	'\n'	// Unix (the right way)
            )
        )+
        { _ttype = Token.SKIP; }
    ;


// string literals
STRING_LITERAL
    : { this.inFormula }?
    (
        '"' (ESC|~('"'|'\\'|'\n'|'\r'))* '"'
    | 	'\'' (ESC|~('\''|'\\'|'\n'|'\r'))* '\''
    )
    ;

COMMENT
    : { this.inFormula }? "/*" (options {greedy=false;} : '\\' '*' '/' | . )* COMMENT_END
    { _ttype = Token.SKIP; }
    ;

protected
COMMENT_END
    : { this.inFormula }?
        "*/"
    ;

// escape sequence -- note that this is protected; it can only be called
// from another lexer rule -- it will not ever directly return a token to
// the parser
protected
ESC
    :	{ this.inFormula }?
        '\\'
        (	'n'
        |	'r'
        |	't'
        |	'"'
        |	'\''
        |	'\\'
        )
    ;


// a dummy rule to force vocabulary to be all characters (except special
// ones that ANTLR uses internally (0 to 2)
protected
VOCAB
    :	'\3'..'\377'
    ;


// an identifier. Note that testLiterals is set to true! This means
// that after we match the rule, we look in the literals table to see
// if it's a literal or really an identifier
IDENT
    options { testLiterals = true; }
    : { this.inFormula }?	 ('a'..'z'|'$') ('a'..'z'|'_'|'0'..'9'|'$'|':'|'.'|'#')*
    ;


// a numeric literal
NUMBER
    : { this.inFormula }? ('0'..'9')+ (DOT ('0'..'9')+)?
    ;


BEGIN_EMBEDDED_FORMULA
    : { !this.inFormula }? "{!" { this.inFormula = true; }
    ;


END_EMBEDDED_FORMULA
    : { this.inFormula }? "}" { this.inFormula = false; }
    ;


// plain text of a template
PLAIN_TEXT
    : { !this.inFormula }? .
    ;
