package com.force.formula;

import org.junit.Assert;

import com.force.formula.impl.FormulaUtils;

import antlr.collections.AST;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public abstract class ParserTestBase extends FormulaTestBase {

    public ParserTestBase(String name) {
        super(name);
    }

    protected void parseTest(String expression, String expected) throws FormulaException {
        AST ast;

        ast = parse(expression, false);
        Assert.assertEquals("Test failed for ANTLR2", expected, ast.toStringTree());

        ast = parse(expression, true);
        Assert.assertEquals("Test failed for ANTLR4", expected, ast.toStringTree());
    }

    protected AST parse(String expression, boolean useANTLR4) throws FormulaException {
        return useANTLR4 ? parseWithANTLR4(expression) : parseWithANTLR2(expression);
    }

    protected AST parseWithANTLR2(String expression) throws FormulaException {
        FormulaProperties properties = new FormulaProperties();
        return FormulaUtils.parseWithANTLR2(expression, properties).getFirstChild();
    }

    protected AST parseWithANTLR4(String expression) throws FormulaException {
        FormulaProperties properties = new FormulaProperties();
        return FormulaUtils.parseWithANTLR4(expression, properties).getFirstChild();
    }
}
