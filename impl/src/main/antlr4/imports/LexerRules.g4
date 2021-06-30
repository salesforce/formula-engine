lexer grammar LexerRules;

// operators

LPAREN              : '('   ;
RPAREN              : ')'   ;

LSQUAREBRACKET      : '['   ;
RSQUAREBRACKET      : ']'   ;

COLON               : ':'   ;
COMMA               : ','   ;
BANG                : '!'   ;
BNOT                : '~'   ;

DIV                 : '/'   ;
PLUS                : '+'   ;
MINUS               : '-'   ;
STAR                : '*'   ;

GE                  : '>='    ;
GT                  : '>'   ;
LE                  : '<='    ;
LT                  : '<'   ;

SEMI                : ';'   ;
DOT                 : '.'   ;
CONCAT              : '&'   ;
EXPONENT            : '^'   ;

MAPTO               : '=>'    ;

INFIX_OR            : '||'    ;
INFIX_AND           : '&&'    ;

EQUAL               : '='   ;
EQUAL2              : '=='    ;
NOT_EQUAL           : '<>'    ;
NOT_EQUAL2          : '!='    ;

// these operators are done like this instead of literals to allow any case scenario, even "NuLl", since ANTLR2 allows it
NOT                 : N O T       ;
TRUE                : T R U E     ;
FALSE               : F A L S E   ;
NULL                : N U L L     ;

// Whitespace -- ignored
WS  :
        ( ' '
        | '\t'
        | '\f'
        | // handle newlines:(options {generateAmbigWarnings=false;}
            ( '\r\n'    // Evil DOS
            | '\r'      // Macintosh
            | '\n'      // Unix (the right way)
            )
        )+ -> skip
    ;

//TODO(arman): W-6454170
//STRING_LITERAL can be updated to the following once ANTLR4 is fully GA and stable:
//STRING_LITERAL
//    :
//    (   '"' ( ESC | . )*? '"'
//    |   '\'' ( ESC | . )*? '\''
//    )
//    ;

// the question mark in *? makes this non-greedy
STRING_LITERAL
    :
    (   '"' ( ESC | CAPITALIZED_ESC | ~('"'|'\\') )*? '"'
    |   '\'' ( ESC | CAPITALIZED_ESC | ~('\''|'\\') )*? '\''
    )
    ;

fragment
CAPITALIZED_ESC
    : '\\'
        ( 'N'
        | 'R'
        | 'T'
        )
    ;

// the question mark in )*? makes this non-greedy
COMMENT
    :  '/*' ( '\\' '*' '/' | . )*? '*/' -> skip
    ;

// escape sequence -- note that this is a fragment (formerly protected);
// it can only be called from another lexer rule -- it will not ever
// directly return a token to the parser
fragment
ESC
    : '\\'
        ( 'n'
        | 'r'
        | 't'
        | '"'
        | '\''
        | '\\'
        )
    ;

// a dummy rule to force vocabulary to be all characters (except special
// ones that ANTLR uses internally (0 to 2)
fragment
VOCAB
    : '\u0003'..'\u0377'
    ;

// an identifier. Note that testLiterals is set to true! This means
// that after we match the rule, we look in the literals table to see
// if it's a literal or really an identifier
IDENT
    :    IDENT_START_CHAR IDENT_CHAR*
    ;

fragment
IDENT_START_CHAR
    :  'a'..'z'
    |  'A'..'Z'
    |  '$'
    // Unicode characters ANTLR2 allows
    |  '\u0130'
    |  '\u212A'
    ;

fragment
IDENT_CHAR
    :  'a'..'z'
    |  'A'..'Z'
    |  '0'..'9'
    |  '$'
    |  '_'
    |  ':'
    |  '.'
    |  '#'
    // Unicode characters ANTLR2 allows
    |  '\u0130'
    |  '\u212A'
    ;

NUMBER
    :  ('0'..'9')+ (DOT ('0'..'9')+)?
    ;

// allowing either case
fragment A : [aA]; // match either an 'a' or 'A'
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];
