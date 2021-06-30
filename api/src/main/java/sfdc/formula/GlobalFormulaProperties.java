package sfdc.formula;

import java.util.LinkedList;

/**
 * Global properties for Formula evaluation
 * @author stamm
 * @since 200 (Extracted from CompositeFormulaContext)
 */
public class GlobalFormulaProperties {
    private final LinkedList<FormulaTypeSpec> formulaTypes;
    private boolean shouldIgnoreFls = false;
    private boolean shouldThrowOnEmptyFieldValue = false;

    public GlobalFormulaProperties(FormulaTypeSpec topLevelType){
        formulaTypes = new LinkedList<FormulaTypeSpec>();
        formulaTypes.add(topLevelType);
    }

    public FormulaTypeSpec getFormulaType(){
        return formulaTypes.getLast();
    }

    public void pushFormulaType(FormulaTypeSpec type){
        formulaTypes.add(type);
    }

    public boolean shouldIgnoreFls(){
        return shouldIgnoreFls;
    }

    public boolean shouldThrowOnEmptyFieldValue() {
        return shouldThrowOnEmptyFieldValue;
    }
    
    public FormulaTypeSpec popFormulaType(){
        assert formulaTypes.size() > 1 : "Tried to remove only FormulaType on stack";
        return formulaTypes.removeLast();
    }

    public FormulaTypeSpec getTopLevelFormulaType(){
        return formulaTypes.getFirst();
    }

    public void setShouldIgnoreFls(boolean ignore){
        shouldIgnoreFls = ignore;
    }

    public void setShouldThrowOnEmptyFieldValue(boolean newValue) {
        shouldThrowOnEmptyFieldValue = newValue;
    }
}