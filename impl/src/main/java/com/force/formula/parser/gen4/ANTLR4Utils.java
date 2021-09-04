package com.force.formula.parser.gen4;

import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.misc.Interval;

import com.force.formula.parser.gen.FormulaTokenTypes;

import antlr.*;

/**
 * @author ashanjani
 * @since 220
 */
public class ANTLR4Utils {

    public static Map<Integer, Integer> getNumberOfCharactersBeforeEachLine(String formula) {
        Map<Integer, Integer> numberOfCharactersBeforeLine = new HashMap<>();
        numberOfCharactersBeforeLine.put(1, 0);

        int nextLineNumber = 2;
        for(int i = 0; i < formula.length(); i++) {
            if(formula.charAt(i) == '\n') {
                numberOfCharactersBeforeLine.put(nextLineNumber, i+1);
                nextLineNumber++;
            }
        }

        return numberOfCharactersBeforeLine;
    }

    static int getANTLR2Column(int startIndex, int numberOfCharactersBeforeLine, int antlr4Column) {
        return startIndex + numberOfCharactersBeforeLine + antlr4Column + 1;
    }

    static Token convertAntlr4TokenToAntlr2Token(org.antlr.v4.runtime.Token antlr4Token, int antlr2Column) {
        CommonToken antlr2Token = new CommonToken();
        antlr2Token.setText(antlr4Token.getText());
        antlr2Token.setType(convertANTLR4TypeToANTLR2Type(antlr4Token.getType()));
        antlr2Token.setLine(1);
        antlr2Token.setColumn(antlr2Column);
        return antlr2Token;
    }

    //This needs to be done because ANTLR2 and ANTLR4 are going to live side by side for a while.
    //Formula Engine users might use the FormulaTokenTypes types to do work, but if they are using ANTLR4,
    //those values will not match to ANTLR4's types' values (e.g. will be using an ANTLR2 type on an AST generated
    //by ANTLR4 with ANTLR types' values.
    //TODO(arman): remove this when ANTLR2 is removed completely and ANTLR4 automatically generates FormulaTokenTypes
    static int convertANTLR4TypeToANTLR2Type(int antrl4Type) {
        switch (antrl4Type) {
            case FormulaParser.LPAREN:
                return FormulaTokenTypes.LPAREN;
            case FormulaParser.RPAREN:
                return FormulaTokenTypes.RPAREN;
            case FormulaParser.LSQUAREBRACKET:
                return FormulaTokenTypes.LSQUAREBRACKET;
            case FormulaParser.RSQUAREBRACKET:
                return FormulaTokenTypes.RSQUAREBRACKET;
            case FormulaParser.COLON:
                return FormulaTokenTypes.COLON;
            case FormulaParser.COMMA:
                return FormulaTokenTypes.COMMA;
            case FormulaParser.BANG:
                return FormulaTokenTypes.BANG;
            case FormulaParser.BNOT:
                return FormulaTokenTypes.BNOT;
            case FormulaParser.DIV:
                return FormulaTokenTypes.DIV;
            case FormulaParser.PLUS:
                return FormulaTokenTypes.PLUS;
            case FormulaParser.MINUS:
                return FormulaTokenTypes.MINUS;
            case FormulaParser.STAR:
                return FormulaTokenTypes.STAR;
            case FormulaParser.GE:
                return FormulaTokenTypes.GE;
            case FormulaParser.GT:
                return FormulaTokenTypes.GT;
            case FormulaParser.LE:
                return FormulaTokenTypes.LE;
            case FormulaParser.LT:
                return FormulaTokenTypes.LT;
            case FormulaParser.SEMI:
                return FormulaTokenTypes.SEMI;
            case FormulaParser.DOT:
                return FormulaTokenTypes.DOT;
            case FormulaParser.CONCAT:
                return FormulaTokenTypes.CONCAT;
            case FormulaParser.EXPONENT:
                return FormulaTokenTypes.EXPONENT;
            case FormulaParser.MAPTO:
                return FormulaTokenTypes.MAPTO;
            case FormulaParser.INFIX_OR:
                return FormulaTokenTypes.INFIX_OR;
            case FormulaParser.INFIX_AND:
                return FormulaTokenTypes.INFIX_AND;
            case FormulaParser.EQUAL:
                return FormulaTokenTypes.EQUAL;
            case FormulaParser.EQUAL2:
                return FormulaTokenTypes.EQUAL2;
            case FormulaParser.NOT_EQUAL:
                return FormulaTokenTypes.NOT_EQUAL;
            case FormulaParser.NOT_EQUAL2:
                return FormulaTokenTypes.NOT_EQUAL2;
            case FormulaParser.OR:
                return FormulaTokenTypes.OR;
            case FormulaParser.AND:
                return FormulaTokenTypes.AND;
            case FormulaParser.NOT:
                return FormulaTokenTypes.NOT;
            case FormulaParser.TRUE:
                return FormulaTokenTypes.TRUE;
            case FormulaParser.FALSE:
                return FormulaTokenTypes.FALSE;
            case FormulaParser.NULL:
                return FormulaTokenTypes.NULL;
            case FormulaParser.WS:
                return FormulaTokenTypes.WS;
            case FormulaParser.STRING_LITERAL:
                return FormulaTokenTypes.STRING_LITERAL;
            case FormulaParser.COMMENT:
                return FormulaTokenTypes.COMMENT;
            case FormulaParser.IDENT:
                return FormulaTokenTypes.IDENT;
            case FormulaParser.NUMBER:
                return FormulaTokenTypes.NUMBER;
            case FormulaParser.ROOT:
                return FormulaTokenTypes.ROOT;
            case FormulaParser.TEMPLATE:
                return FormulaTokenTypes.TEMPLATE;
            case FormulaParser.UNARY_MINUS:
                return FormulaTokenTypes.UNARY_MINUS;
            case FormulaParser.UNARY_PLUS:
                return FormulaTokenTypes.UNARY_PLUS;
            case FormulaParser.FUNCTION_CALL:
                return FormulaTokenTypes.FUNCTION_CALL;
            case FormulaParser.NOUNESCAPE_STRING_LITERAL:
                return FormulaTokenTypes.NOUNESCAPE_STRING_LITERAL;
            case FormulaParser.TEMPLATE_STRING_LITERAL:
                return FormulaTokenTypes.TEMPLATE_STRING_LITERAL;
            case FormulaParser.DYNAMIC_REF:
                return FormulaTokenTypes.DYNAMIC_REF;
            case FormulaParser.DYNAMIC_REF_ROOT:
                return FormulaTokenTypes.DYNAMIC_REF_ROOT;
            case FormulaParser.DYNAMIC_REF_IDENT:
                return FormulaTokenTypes.DYNAMIC_REF_IDENT;
            case FormulaParser.EOF:
                return FormulaTokenTypes.EOF;
            //default should never happen, unless a new token is added to ANTLR4 grammar but not ANTLR2 grammar and also not added to the above list
            default:
                throw new RuntimeException("Failed to convert ANTLR4 token type to ANTLR2 token type.");
        }
    }

    public static ANTLRException convertANTLR4ExceptionToANTLR2(org.antlr.v4.runtime.RecognitionException e, int startIndex, Map<Integer, Integer> numberOfCharactersBeforeLine) {
        if(e instanceof LexerNoViableAltException) {
            LexerNoViableAltException lexerException = (LexerNoViableAltException)e;
            char c = lexerException.getInputStream().getText(Interval.of(lexerException.getStartIndex(), lexerException.getStartIndex())).charAt(0);
            return new NoViableAltForCharException(c, null, 1, lexerException.getStartIndex() + startIndex + 1);
        }

        org.antlr.v4.runtime.Token antlr4Token = e.getOffendingToken();
        int antlr2Column = getANTLR2Column(startIndex, numberOfCharactersBeforeLine.get(antlr4Token.getLine()), antlr4Token.getCharPositionInLine());
        Token antlr2Token = convertAntlr4TokenToAntlr2Token(antlr4Token, antlr2Column);

        if(e instanceof org.antlr.v4.runtime.NoViableAltException) {
            return new NoViableAltException(antlr2Token, null);
        }
        else if(e instanceof org.antlr.v4.runtime.InputMismatchException) {
            return new MismatchedTokenException(null, antlr2Token, convertANTLR4TypeToANTLR2Type(e.getExpectedTokens().get(0)), false, null);
        } else { //should not get here
            return new RecognitionException(e.getMessage(), null, antlr2Token.getLine(), antlr2Token.getColumn());
        }
    }
}
