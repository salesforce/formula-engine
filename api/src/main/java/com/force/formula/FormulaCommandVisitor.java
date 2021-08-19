package com.force.formula;

/**
 * This will visit all FormulaCommands in a formula and provide a way to inspect each one for
 * validation and integrity checks.
 *
 * @author wmacklem
 * @since 156
 */
public interface FormulaCommandVisitor {
     void visit(FormulaCommand formulaCommand);
     
     /**
      * @return the formula context associated with the visitor (so it can be passed into the commands)
      */
     default FormulaContext getFormulaContext() {
         return null;
     }

     /**
      * Indicate that we're are in a nested formula.  This should always be called
      * with popNestedFormula() in a finally block.
      */
     default void pushNestedFormula(String formulaName) {
     }

     default void popNestedFormula() {
         
     }

}
