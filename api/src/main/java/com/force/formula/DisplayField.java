/**
 * DisplayField Class
 * 
 * @author dchasman
 * @since 144
 */

package com.force.formula;

public class DisplayField {
    public DisplayField(String categoryName, String categoryLabel, FormulaFieldInfo formulaFieldInfo) {
        this.categoryName = categoryName;
        this.categoryLabel = categoryLabel;
        this.formulaFieldInfo = formulaFieldInfo;
    }

    public String getCategoryLabel() {
        return this.categoryLabel;
    }
    public String getCategoryName() {
        return this.categoryName;
    }
    public FormulaFieldInfo getFormulaFieldInfo() {
        return this.formulaFieldInfo;
    } 
    
    private final String categoryName;
    private final String categoryLabel;
    private final FormulaFieldInfo formulaFieldInfo;
}
