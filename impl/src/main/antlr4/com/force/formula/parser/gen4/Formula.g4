grammar Formula;

import LexerRules;

tokens { ROOT, TEMPLATE, UNARY_MINUS, UNARY_PLUS, FUNCTION_CALL, NOUNESCAPE_STRING_LITERAL,
       TEMPLATE_STRING_LITERAL, DYNAMIC_REF, DYNAMIC_REF_ROOT, DYNAMIC_REF_IDENT, OR, AND }

@members {
private boolean allowSubscripts = false;

public void setAllowSubscripts(boolean subscripts) {
    this.allowSubscripts = subscripts;
}

//TODO(arman): W-6454170
//Can remove once ANTLR4 is fully GA and stable
boolean hasSpecialCharacters(String input) {
    for(int i = 0; i < input.length(); i++) {
        char c = input.charAt(i);
        if(c == '\n' || c == '\r') {
            return true;
        }
    }

    return false;
}
}

formula
    : expression  EOF
    ;

fieldReferenceLiteral
    : IDENT
    ;

constant
    : NUMBER
    | STRING_LITERAL { !hasSpecialCharacters($STRING_LITERAL.getText()) }?
    | map
    ;

map
    : map_content
    ;

map_content
    : LSQUAREBRACKET IDENT EQUAL expression ( COMMA IDENT EQUAL expression )* RSQUAREBRACKET                                                                                                                            #map_content_ident
    | LSQUAREBRACKET s1=STRING_LITERAL { !hasSpecialCharacters($s1.getText()) }? EQUAL expression (COMMA s2=STRING_LITERAL { !hasSpecialCharacters($s2.getText()) }? EQUAL expression )* RSQUAREBRACKET     #map_content_string_literal
    ;

expression
    : logicalOrExpression
    ;

//comma version of OR falls under functionCall
logicalOrExpression
    : logicalAndExpression ( INFIX_OR  logicalAndExpression )*
    ;

//comma version of AND falls under functionCall
logicalAndExpression
    : equalityExpression ( INFIX_AND equalityExpression )*
    ;

equalityExpression
    : relationalExpression ( ( NOT_EQUAL | EQUAL |  NOT_EQUAL2 | EQUAL2 ) relationalExpression )*
    ;

relationalExpression
    : additiveExpression ( ( LT | GT | LE | GE ) additiveExpression )*
    ;

additiveExpression
    : multiplicativeExpression ( (PLUS | MINUS | CONCAT ) multiplicativeExpression )*
    ;

multiplicativeExpression
    : exponentExpression ( EXPONENT exponentExpression )*
    ;

exponentExpression
    : unaryExpression ( (STAR | DIV ) unaryExpression )*
    ;

unaryExpression
    : primaryExpression       #unaryExpression_primary
    | PLUS unaryExpression    #unaryExpression_plus
    | MINUS unaryExpression   #unaryExpression_minus
    | NOT unaryExpression     #unaryExpression_not
    | BANG unaryExpression    #unaryExpression_bang
    ;

// the basic element of an expression
primaryExpression
    : fieldReference
    | constant
    | TRUE
    | FALSE
    | NULL
    | LPAREN logicalOrExpression RPAREN
    | functionCall
    ;

//"OR(...)" and "AND(...)" fall under functionCall
functionCall
    : IDENT LPAREN (expression ( COMMA expression )* )? RPAREN
    ;

// This is root goal for retrieving an "lvalue"; i.e. an expression for a reference that can be assigned to
fieldReferenceRoot
    : fieldReference EOF
    ;

// Currently we don't actually go through this for fieldReferenceLiterals because they don't use the evaluator to get the reference.
fieldReference
    : fieldReferenceLiteral
    | {this.allowSubscripts}? subscriptedExpression
    ;

subscriptedExpression
    : fieldReferenceLiteral ( fieldSelector )+
    ;

fieldSelector
    : LSQUAREBRACKET expression RSQUAREBRACKET      #fieldSelector_expression
    | DOT IDENT                                     #fieldSelector_ident
    ;
