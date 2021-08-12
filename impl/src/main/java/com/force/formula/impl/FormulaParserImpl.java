/*
 * Copyright, 2006, SALESFORCE.com All Rights Reserved Company Confidential
 */

package com.force.formula.impl;

import antlr.*;
import antlr.collections.AST;
import com.force.formula.parser.gen.FormulaLexer;
import com.force.formula.parser.gen.FormulaParser;

/**
 * @author dchasman
 * @since 144
 */
public class FormulaParserImpl extends FormulaParser {

    public FormulaParserImpl(TokenStream lexer) {
        super(lexer);

        ASTFactory astFactory = new ASTFactory() {
            @Override
            public AST create(Token token) {
                FormulaAST ast = (FormulaAST)super.create(token);

                // Attach the actual parser token to the AST node (needed for
                // error handling/highlighting later
                ast.setToken(token);

                return ast;
            }
        };

        astFactory.setASTNodeClass(FormulaAST.class.getName());
        setASTFactory(astFactory);
    }

    public void setFormulaLexer(FormulaLexer formulaLexer) {
        this.formulaLexer = formulaLexer;
    }

    public void setFailOnEmbeddedFormulaExceptions(boolean failOnEmbeddedFormulaExceptions) {
        this.failOnEmbeddedFormulaExceptions = failOnEmbeddedFormulaExceptions;
    }

    @Override
    protected void reparent(FormulaAST parent, FormulaAST firstChild) {
        if (firstChild != null) {
            FormulaAST child = firstChild;
            while (child != null) {
                if (child.getParent() == null) {
                    child.setParent(parent);
                }

                child = (FormulaAST)child.getNextSibling();
            }
        }
    }

    @Override
    protected AST handleException(Exception x) throws RecognitionException, TokenStreamException {
        if (failOnEmbeddedFormulaExceptions) {
            if (x instanceof RecognitionException) {
                throw (RecognitionException)x;
            } else if (x instanceof TokenStreamException) {
                throw (TokenStreamException)x;
            } else {
            	// throw new SalesforceGenericException(x);
            	throw new RuntimeException(x);  // TODO SLT: This might cause regressions.
            }
        }

        // Just silently ignore this entire embedded formula and return empty string as its replacement value
        formulaLexer.setInFormula(false);
        Token token = LT(1);
        while (token.getType() != Token.EOF_TYPE && !"}".equals(token.getText())) {
            consume();
            token = LT(1);
        }

        if ("}".equals(token.getText())) {
            consume();
        }

        ASTPair currentAST = new ASTPair();
        AST replacement = astFactory.create(STRING_LITERAL, "\"\"");
        astFactory.addASTChild(currentAST, replacement);

        return replacement;
    }

    private boolean failOnEmbeddedFormulaExceptions = true;
    private FormulaLexer formulaLexer;
}
