/*
 * Copyright, 2006, SALESFORCE.com All Rights Reserved Company Confidential
 */

package com.force.formula.impl;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.LexerNoViableAltException;
import com.force.formula.parser.gen4.FormulaLexer;

/**
 * @author ashanjani
 * @since 220
 */
public class FormulaLexerImpl4 extends FormulaLexer {

    public FormulaLexerImpl4(CharStream input) {
        super(input);
    }

    /**
     * Stop parsing as soon as the first lexical error arises
     */
    @Override
    public void recover(LexerNoViableAltException e) {
        throw e;
    }
}
