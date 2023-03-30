package com.force.formula.v2.data;

import com.force.formula.FormulaDataType;
import com.force.formula.v2.Utils;
import org.apache.commons.lang3.Validate;

import java.math.BigDecimal;
import java.util.List;

public class FormulaFieldDefinition {

    private String fieldName;

    private FormulaDataType dataType;

    private String formula;

    private List<FormulaFieldDefinition> referenceFields;

    public FormulaFieldDefinition(String fieldName, FormulaDataType dataType, String formula, List<FormulaFieldDefinition> referenceFields) {
        Validate.notEmpty(fieldName,"fieldName cannot be null or empty");
        Validate.notNull(dataType,"each field needs to have a dataType and it cannot be null");
        //formula for a custom reference field can be null or empty
        //referenceFields can be null or empty if the formula does not depend on any referenceField
        this.fieldName = fieldName;
        this.dataType = dataType;
        this.formula = formula;
        this.referenceFields = referenceFields;
    }

    public String getFieldName() {
        return this.fieldName + "__c";
    }

    public FormulaDataType getDataType() {
        return dataType;
    }

    public String getFormula() {
        return formula;
    }

    public List<FormulaFieldDefinition> getReferenceFields() {
        return referenceFields;
    }

    public Object createObjectWithGivenValue(String fieldValue){
        if (this.dataType.isNumber()) {
            if ("NULL".equalsIgnoreCase(fieldValue)) {
                return null;
            }
            return new BigDecimal(fieldValue.trim());
        } else if (this.dataType.isDate()) {
            return Utils.getDateObject(fieldValue.trim(), this.dataType.isDateOnly());
        } else if (this.dataType.isBoolean()) {
            return Boolean.valueOf(fieldValue.trim());
        } else {
            return fieldValue;
        }
    }

}

