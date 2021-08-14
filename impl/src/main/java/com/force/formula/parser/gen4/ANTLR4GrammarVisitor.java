package com.force.formula.parser.gen4;

import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.force.formula.impl.FormulaAST;
import com.force.formula.parser.gen.SfdcFormulaTokenTypes;

import antlr.CommonToken;
import antlr.Token;

/**
 * @author ashanjani
 * @since 220
 */
public class ANTLR4GrammarVisitor extends FormulaBaseVisitor<FormulaAST> {

    private final FormulaParser parser;

    /**
     * startIndex is used to set column values correctly, based on the starting index of this formula in a higher level input.
     * This is particularly used in templates where only the inner formulas are parsed through ANTLR4.
     */
    private int startIndex;

    /**
     * numberOfCharactersBeforeLine tracks the number of characters that come before a given line number.
     * Key = line number, Value = number of characters before the given line number
     * This information is used when converting line and column values for ANTLR4 tokens to ANTLR2 tokens.
     * This conversion is needed because ANTLR4 properly sets line and column values but in ANTLR2, line value is
     * always set to 1 and column value tracks the number of characters, as if the formula is a normal string.
     *
     * Let's look at an example:
     * Example formula: "abc && def &&\nghi && jkl"
     * In ANTLR4, token "jkl" will have line = 2, column = 7 (lines start at 1, columns start at 0).
     * In ANTLR2, token "jkl" will have line = 1, column = 22 (line is always 1, columns start at 1).
     */
    private Map<Integer, Integer> numberOfCharactersBeforeLine;

    public ANTLR4GrammarVisitor(FormulaParser parser, int startIndex, Map<Integer, Integer> numberOfCharactersBeforeLine) {
        this(parser, null, startIndex, numberOfCharactersBeforeLine);
    }

    @Deprecated
    public ANTLR4GrammarVisitor(FormulaParser parser, String formula, int startIndex, Map<Integer, Integer> numberOfCharactersBeforeLine) {  // NOPMD
        this.parser = parser;
        this.startIndex = startIndex;
        this.numberOfCharactersBeforeLine = numberOfCharactersBeforeLine;
    }

    @Override
    public FormulaAST visitFormula(FormulaParser.FormulaContext ctx) {
        FormulaAST ast = new FormulaAST();
        ast.setText("root");
        ast.setType(SfdcFormulaTokenTypes.ROOT);
        ast.setFirstChild(visit(ctx.expression()));
        return ast;
    }

    @Override
    public FormulaAST visitFieldReferenceLiteral(FormulaParser.FieldReferenceLiteralContext ctx) {
        return visit(ctx.IDENT());
    }

    @Override
    public FormulaAST visitConstant(FormulaParser.ConstantContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override
    public FormulaAST visitMap(FormulaParser.MapContext ctx) {
        return visit(ctx.map_content());
    }

    @Override
    public FormulaAST visitMap_content_ident(FormulaParser.Map_content_identContext ctx) {
        FormulaAST ast = new FormulaAST();
        ast.setText("map");
        ast.setType(SfdcFormulaTokenTypes.FUNCTION_CALL);

        List<TerminalNode> idents = ctx.IDENT();
        List<FormulaParser.ExpressionContext> expressions = ctx.expression();
        for(int i = 0; i < idents.size(); i++) {
            FormulaAST identAST = visit(idents.get(i));
            identAST.setType(SfdcFormulaTokenTypes.NOUNESCAPE_STRING_LITERAL);
            identAST.setToken(null);
            ast.addChild(identAST);
            ast.addChild(visit(expressions.get(i)));
        }

        return ast;
    }

    @Override
    public FormulaAST visitMap_content_string_literal(FormulaParser.Map_content_string_literalContext ctx) {
        FormulaAST ast = new FormulaAST();
        ast.setText("map");
        ast.setType(SfdcFormulaTokenTypes.FUNCTION_CALL);

        List<TerminalNode> stringLiterals = ctx.STRING_LITERAL();
        List<FormulaParser.ExpressionContext> expressions = ctx.expression();
        for(int i = 0; i < stringLiterals.size(); i++) {
            ast.addChild(visit(stringLiterals.get(i)));
            ast.addChild(visit(expressions.get(i)));
        }

        return ast;
    }

    @Override
    public FormulaAST visitExpression(FormulaParser.ExpressionContext ctx) {
        return visit(ctx.logicalOrExpression());
    }

    @Override
    public FormulaAST visitLogicalOrExpression(FormulaParser.LogicalOrExpressionContext ctx) {
        FormulaAST curr = visit(ctx.getChild(0));

        for(int i = 1; i < ctx.getChildCount(); i++) {
            if(ctx.getChild(i) instanceof TerminalNode) { //infix_or operator
                FormulaAST operatorAST = visit(ctx.getChild(i));
                operatorAST.setText("or");
                operatorAST.setType(SfdcFormulaTokenTypes.OR);
                operatorAST.addChild(curr);
                curr = operatorAST;
            }
            else {
                curr.addChild(visit(ctx.getChild(i)));
            }
        }

        return curr;
    }

    @Override
    public FormulaAST visitLogicalAndExpression(FormulaParser.LogicalAndExpressionContext ctx) {
        FormulaAST curr = visit(ctx.getChild(0));

        for(int i = 1; i < ctx.getChildCount(); i++) {
            if(ctx.getChild(i) instanceof TerminalNode) { //infix_or operator
                FormulaAST operatorAST = visit(ctx.getChild(i));
                operatorAST.setText("and");
                operatorAST.setType(SfdcFormulaTokenTypes.AND);
                operatorAST.addChild(curr);
                curr = operatorAST;
            }
            else {
                curr.addChild(visit(ctx.getChild(i)));
            }
        }

        return curr;
    }

    @Override
    public FormulaAST visitEqualityExpression(FormulaParser.EqualityExpressionContext ctx) {
        FormulaAST curr = visit(ctx.getChild(0));

        for(int i = 1; i < ctx.getChildCount(); i++) {
            if(ctx.getChild(i) instanceof TerminalNode) { //operator
                FormulaAST operatorAST = visit(ctx.getChild(i));
                operatorAST.addChild(curr);
                curr = operatorAST;

                //convert NOT_EQUAL2 to NOT_EQUAL and EQUAL2 to EQUAL so consumers of FormulaAST don't have to care which one was used
                if(operatorAST.getType() == SfdcFormulaTokenTypes.NOT_EQUAL2) {
                    operatorAST.setType(SfdcFormulaTokenTypes.NOT_EQUAL);
                    operatorAST.setText("<>");
                }
                else if(operatorAST.getType() == SfdcFormulaTokenTypes.EQUAL2) {
                    operatorAST.setType(SfdcFormulaTokenTypes.EQUAL);
                    operatorAST.setText("=");
                }
            }
            else {
                curr.addChild(visit(ctx.getChild(i)));
            }
        }

        return curr;
    }

    @Override
    public FormulaAST visitRelationalExpression(FormulaParser.RelationalExpressionContext ctx) {
        return visitCommonExpressionHelper(ctx);
    }

    @Override
    public FormulaAST visitAdditiveExpression(FormulaParser.AdditiveExpressionContext ctx) {
        return visitCommonExpressionHelper(ctx);
    }

    @Override
    public FormulaAST visitMultiplicativeExpression(FormulaParser.MultiplicativeExpressionContext ctx) {
        return visitCommonExpressionHelper(ctx);
    }

    @Override
    public FormulaAST visitExponentExpression(FormulaParser.ExponentExpressionContext ctx) {
        return visitCommonExpressionHelper(ctx);
    }

    private FormulaAST visitCommonExpressionHelper(ParserRuleContext ctx) {
        FormulaAST curr = visit(ctx.getChild(0));

        for(int i = 1; i < ctx.getChildCount(); i++) {
            if(ctx.getChild(i) instanceof TerminalNode) { //operator
                FormulaAST operatorAST = visit(ctx.getChild(i));
                operatorAST.addChild(curr);
                curr = operatorAST;
            }
            else {
                curr.addChild(visit(ctx.getChild(i)));
            }
        }

        return curr;
    }

    @Override
    public FormulaAST visitUnaryExpression_primary(FormulaParser.UnaryExpression_primaryContext ctx) {
        return visit(ctx.primaryExpression());
    }

    @Override
    public FormulaAST visitUnaryExpression_plus(FormulaParser.UnaryExpression_plusContext ctx) {
        FormulaAST plusAST = visit(ctx.PLUS());
        plusAST.setType(SfdcFormulaTokenTypes.UNARY_PLUS);
        plusAST.addChild(visit(ctx.unaryExpression()));
        return plusAST;
    }

    @Override
    public FormulaAST visitUnaryExpression_minus(FormulaParser.UnaryExpression_minusContext ctx) {
        FormulaAST minusAST = visit(ctx.MINUS());
        minusAST.setType(SfdcFormulaTokenTypes.UNARY_MINUS);
        minusAST.addChild(visit(ctx.unaryExpression()));
        return minusAST;
    }

    @Override
    public FormulaAST visitUnaryExpression_not(FormulaParser.UnaryExpression_notContext ctx) {
        FormulaAST notAST = visit(ctx.NOT());
        notAST.addChild(visit(ctx.unaryExpression()));
        return notAST;
    }

    @Override
    public FormulaAST visitUnaryExpression_bang(FormulaParser.UnaryExpression_bangContext ctx) {
        FormulaAST bangAST = visit(ctx.BANG());
        bangAST.setText("not");
        bangAST.setType(SfdcFormulaTokenTypes.NOT);
        bangAST.addChild(visit(ctx.unaryExpression()));
        return bangAST;
    }

    @Override
    public FormulaAST visitPrimaryExpression(FormulaParser.PrimaryExpressionContext ctx) {
        if(ctx.LPAREN() != null) { //visiting logicalOrExpression
            return visit(ctx.logicalOrExpression());
        }

        return visit(ctx.getChild(0));
    }

    @Override
    public FormulaAST visitFunctionCall(FormulaParser.FunctionCallContext ctx) {
        FormulaAST ast = visit(ctx.IDENT()); //function name
        ast.getToken().setType(SfdcFormulaTokenTypes.IDENT);
        ast.setType(SfdcFormulaTokenTypes.FUNCTION_CALL);

        List<FormulaParser.ExpressionContext> expressions = ctx.expression();
        for(int i = 0; i < expressions.size(); i++) {
            ast.addChild(visit(expressions.get(i)));
        }

        return ast;
    }

    @Override
    public FormulaAST visitFieldReferenceRoot(FormulaParser.FieldReferenceRootContext ctx) {
        FormulaAST ast = new FormulaAST();
        ast.setText("referenceroot");
        ast.setType(SfdcFormulaTokenTypes.DYNAMIC_REF_ROOT);
        ast.addChild(visit(ctx.fieldReference()));
        return ast;
    }

    @Override
    public FormulaAST visitFieldReference(FormulaParser.FieldReferenceContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override
    public FormulaAST visitSubscriptedExpression(FormulaParser.SubscriptedExpressionContext ctx) {
        FormulaAST currentAST = visit(ctx.fieldReferenceLiteral());

        List<FormulaParser.FieldSelectorContext> fieldSelectors = ctx.fieldSelector();
        for(int i = 0; i < fieldSelectors.size(); i++) {
            FormulaAST fieldSelectorAST = visit(fieldSelectors.get(i));
            FormulaAST fieldSelectorExpressionAST = (FormulaAST) fieldSelectorAST.getFirstChild();
            fieldSelectorAST.setFirstChild(currentAST);
            fieldSelectorAST.addChild(fieldSelectorExpressionAST);
            currentAST = fieldSelectorAST;
        }

        return currentAST;
    }

    @Override
    public FormulaAST visitFieldSelector_expression(FormulaParser.FieldSelector_expressionContext ctx) {
        FormulaAST ast = getDynamicRefAST(ctx);
        ast.addChild(visit(ctx.expression()));
        return ast;
    }

    @Override
    public FormulaAST visitFieldSelector_ident(FormulaParser.FieldSelector_identContext ctx) {
        FormulaAST ast = getDynamicRefAST(ctx);

        FormulaAST identAST = visit(ctx.IDENT());
        identAST.setType(SfdcFormulaTokenTypes.DYNAMIC_REF_IDENT);
        ast.addChild(identAST);

        return ast;
    }

    private FormulaAST getDynamicRefAST(FormulaParser.FieldSelectorContext ctx) {
        org.antlr.v4.runtime.Token antlr4Token = this.parser.getTokenStream().get(ctx.getStart().getTokenIndex());
        Token antlr2Token = ANTLR4Utils.convertAntlr4TokenToAntlr2Token(antlr4Token, ANTLR4Utils.getANTLR2Column(startIndex, numberOfCharactersBeforeLine.get(antlr4Token.getLine()), antlr4Token.getCharPositionInLine()));

        FormulaAST ast = new FormulaAST();
        ast.setToken(antlr2Token);
        ast.setText("[]");
        ast.setType(SfdcFormulaTokenTypes.DYNAMIC_REF);
        return ast;
    }

    @Override
    public FormulaAST visitTerminal(TerminalNode node) {
        int type = ANTLR4Utils.convertANTLR4TypeToANTLR2Type(node.getSymbol().getType());

        CommonToken token = new CommonToken();
        token.setText(node.getText());
        token.setType(type);
        token.setLine(1);
        token.setColumn(ANTLR4Utils.getANTLR2Column(startIndex, numberOfCharactersBeforeLine.get(node.getSymbol().getLine()), node.getSymbol().getCharPositionInLine()));

        FormulaAST ast = new FormulaAST();
        ast.setToken(token);
        ast.setText(node.getText());
        ast.setType(type);
        return ast;
    }
}
