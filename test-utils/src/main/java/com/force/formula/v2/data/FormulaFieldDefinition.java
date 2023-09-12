package com.force.formula.v2.data;

import com.force.formula.FormulaDataType;
import com.force.formula.v2.Utils;
import org.apache.commons.lang3.Validate;

import java.math.BigDecimal;
import java.util.List;

/**
 * A class that contains the definition of the formula field or the custom field defined in the testcase
 */
public class FormulaFieldDefinition {

    /**
     * name of the field
     */
    private String fieldName;

    /**
     * dataType of the field
     */
    private FormulaDataType dataType;

    /**
     * If it is a formula field, then this contains the formula code for that field
     */
    private String formula;

    /**
     * If it is a formula field, it contains the ordered list of all reference fields used in the formula
     */
    private List<FormulaFieldDefinition> referenceFields;

    /**
     * scale to be used for the field
     */
    private int scale;

    public FormulaFieldDefinition(String fieldName, FormulaDataType dataType, String formula, List<FormulaFieldDefinition> referenceFields, int scale) {
        Validate.notEmpty(fieldName,"fieldName cannot be null or empty");
        Validate.notNull(dataType,"each field needs to have a dataType and it cannot be null");
        //formula for a custom reference field can be null or empty
        //referenceFields can be null or empty if the formula does not depend on any referenceField
        this.fieldName = fieldName;
        this.dataType = dataType;
        this.formula = formula;
        this.referenceFields = referenceFields;
        this.scale = scale;
    }

    /**
     * Keeping the salesforce convention of appending "__c" to the field name if it is a custom or formula field
     * @return field name used for querying
     */
    public String getFieldName() {
        return this.fieldName + "__c";
    }

    /**
     * Gets the formula data type of the field
     * @return Formula data type of the field
     */
    public FormulaDataType getDataType() {
        return dataType;
    }

    /**
     * Gets the formula for this field
     * @return formula string for the field
     */
    public String getFormula() {
        return formula;
    }

    /**
     * Gets all the reference fields for this field
     * @return ordered list of reference fields for this field
     */
    public List<FormulaFieldDefinition> getReferenceFields() {
        return referenceFields;
    }

    public Integer getScale() {
        return scale;
    }

    /**
     * Given a string field value, this method converts it into appropriate value based on field's data type
     * @param fieldValue that needs to be converted based on the data type of the field
     * @return the field value after converting based on the field's data type
     */
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

