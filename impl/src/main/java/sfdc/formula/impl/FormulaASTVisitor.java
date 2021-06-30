/*
 * Created on Dec 21, 2004
 *
 */
package sfdc.formula.impl;

import sfdc.formula.FormulaException;

/**
 * Represents visitor pattern for traversal of FormulaAST syntax tree.
 *
 * @author dchasman
 * @since 140
 */
public interface FormulaASTVisitor {
    void visit(FormulaAST node) throws FormulaException;
}
