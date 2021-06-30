/**
 *
 */
package sfdc.formula.impl;

import java.util.*;

import sfdc.formula.FormulaDataType;
import sfdc.formula.MockFormulaDataType;

/**
 * @author syendluri
 * @since 140
 * This is a template class that holds all the required information related to Custom or Formula field.
 * The constructor needs
 */
public class FieldDefinitionInfo implements Cloneable {

    //    private String name;
    private String entityName;
    private String formula;
    private String template;
    private FormulaDataType returnType;
    private String devName;
    private String labelName;
    private String domainName;
    private int precision;
    private int scale;
    private int length;
    private String autoNumberMask;
    private boolean flsEditable;
    private boolean isFormula;
    private boolean isStandard;
    private String fieldId;
    private List<String> pickListValues = null;
    private List<FieldDefinitionInfo> refFields = null;

    public FieldDefinitionInfo(String entityName, FormulaDataType returnType, String devName, String labelName) {
        super();
        this.entityName = entityName;
        this.formula = null;
        this.template = null;
        this.returnType = returnType;
        this.devName = devName;
        this.labelName = labelName;
        this.precision = 0;
        this.scale = 0;
        this.length = 0;
        this.autoNumberMask = null;
        this.flsEditable = Boolean.TRUE;
        this.isFormula = Boolean.FALSE;
        this.isStandard = Boolean.FALSE;
        this.refFields = new LinkedList<FieldDefinitionInfo>();
    }

    public FieldDefinitionInfo(String entityName, FormulaDataType returnType, String devName, String labelName, int precision,
        int scale, int length, String domain, String code) {
        this(entityName, returnType, devName, labelName);
        this.precision = precision;
        this.scale = scale;
        this.length = length;
        this.formula = code;
        this.returnType = returnType;
        if (this.formula != null && this.formula.length() > 0) {
            this.flsEditable = Boolean.FALSE;
            this.isFormula = Boolean.TRUE;
        } else {
            this.flsEditable = Boolean.TRUE;
            this.isFormula = Boolean.FALSE;
        }
        this.isStandard = Boolean.FALSE;
        this.template = null; // have to create this later.
        this.domainName = domain
;    }

    @Override
    public FieldDefinitionInfo clone() {
        try {
            return (FieldDefinitionInfo)super.clone();
        }
        catch (CloneNotSupportedException x) {
            throw new IllegalArgumentException(x);
        }
    }

    public String getDevName() {
        return this.devName;
    }

    public String getReferenceName() {
        if (this.isStandard)
            return this.devName;
        else
            return this.devName + "__c";  // SFDC Convention
    }

    public boolean isFormula() {
        return this.isFormula;
    }

    public String getEntityName() {
        return this.entityName;
    }


    public String getDomainName() {
        return this.domainName;
    }

    public String getFormula() {
        return this.formula != null ? this.formula : "";
    }

    public String getLabelName() {
        return this.labelName;
    }

    public FormulaDataType getReturnType() {
        return this.returnType;
    }

    public int getPrecision() {
        return this.precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getScale() {
        return this.scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public void setLength(int length) {
        this.length = length;

    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public int getLength() {
        return this.length;
    }

    public String getAutoNumberMask() {
        return this.autoNumberMask;
    }

    public void setAutoNumberMask(String autoNumberMask) {
        this.autoNumberMask = autoNumberMask.trim();
    }

    public void setFormula(String formula) {
        this.formula = formula.trim();
        this.isFormula = Boolean.TRUE;
        this.flsEditable = Boolean.FALSE;
        this.isStandard = Boolean.FALSE;
    }

    public void setReturnType(FormulaDataType returnType) {
        this.returnType = returnType;
    }

    public boolean isFlsEditable() {
        return this.flsEditable;
    }

    public boolean isStandard() {
        return this.isStandard;
    }

    public void setDevName(String devName) {
        this.devName = devName;
    }

    public boolean isNumeric() {
        return ((MockFormulaDataType)getReturnType()).isNumber();
    }

    public void setPickListValues(List<String> values) {
        this.pickListValues = values;
    }

    public void appendPickListValue(String value) {
        if (this.pickListValues == null)
            this.pickListValues = new ArrayList<String>();
        this.pickListValues.add(value);
    }

    public List<String> getPickListValues() {
        return this.pickListValues;
    }

    public void setIsStandard(boolean isStandard) {
        this.isStandard = isStandard;
    }

    public String getUnMaskedEntityName() {

        return this.entityName;
    }

    /**
     * @return Returns the refFields.
     */
    public List<FieldDefinitionInfo> getRefFields() {
        return this.refFields;
    }

    /**
     * @param refFields The refFields to set.
     */
    public void setRefFields(List<FieldDefinitionInfo> refFields) {
        this.refFields = refFields;
    }

    /**
     * @return Returns true if this field has any children (like formula field using other fields)
     */
    public boolean hasChildren() {
        return !this.refFields.isEmpty();
    }

    /**
     * @param oldFieldInfo
     * @return TRUE if the field being compared has same member values
     */
    public boolean compare(FieldDefinitionInfo fieldInfo) {
        if (this.length == fieldInfo.getLength() && this.precision == fieldInfo.getPrecision()
            && this.returnType.equals(fieldInfo.getReturnType()) && this.scale == fieldInfo.getScale())
            return Boolean.TRUE;
        else
            return Boolean.FALSE;
    }

    /**
     * @param fieldId - Store the SFDC ID created for this field at run time.
     */
    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    /**
     * @return Returns the fieldId.
     */
    public String getFieldId() {
        return this.fieldId;
    }

    public String getTemplate() {
        return this.template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

}
