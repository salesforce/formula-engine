package com.force.formula.impl;

import java.util.Objects;

import com.force.formula.ContextualFormulaFieldInfo;
import com.force.formula.Formula;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaException;
import com.force.formula.FormulaFieldInfo;
import com.force.formula.FormulaPicklistInfo;
import com.force.formula.FormulaProvider;
import com.force.formula.FormulaSchema;
import com.force.formula.FormulaTypeSpec;
import com.force.formula.sql.FormulaSQLProvider;
import com.force.formula.util.FormulaFieldInfoImpl;

/**
 * Simple base for formula fields that aren't dynamic.  See the tests for an example
 * @author stamm
 * @since 0.2
 */
public abstract class FormulaFieldInfoAdapter extends FormulaFieldInfoImpl implements FormulaSQLProvider, FormulaProvider {

    public FormulaFieldInfoAdapter(String namespace, FormulaSchema.FieldOrColumn fieldOrColumnInfo, ContextualFormulaFieldInfo info) {
        this.namespace = namespace;
        this.fieldOrColumnInfo = fieldOrColumnInfo;
        this.info = info;
    }

    @Override
    public Object getId() {
        return info.getId();
    }

    @Override
    public String[] getExternalizedIds() {
        return info.getExternalizedIds();
    }

    @Override
    public String getName() {
        return info.getName();
    }

    @Override
    public String getLabel() {
        return info.getLabel();
    }

    @Override
    public FormulaDataType getDataType() {
        return info.getDataType();
    }

    @Override
    public int getScale() {
        return info.getScale();
    }

    @Override
    public boolean isCustom() {
        return info.isCustom();
    }

    protected String getNamespace() {
        return namespace;
    }

    protected FormulaSchema.Field getFieldInfo() {
        return fieldOrColumnInfo.getFieldInfo();
    }

    @Override
    public FormulaSchema.FieldOrColumn getFieldOrColumnInfo() {
        return fieldOrColumnInfo;
    }

    protected FormulaFieldInfo getInfo() {
        return info;
    }

    @Override
    public String getDbColumn(String standardTablAlias, String customTableAlias) {
        return info.getDbColumn(standardTablAlias, customTableAlias);
    }

    @Override
    public FormulaPicklistInfo getEnumInfo() {
        return info.getEnumInfo();
    }

    @Override
    public FormulaSchema.Entity[] getFormulaForeignKeyDomains() {
        if (fieldOrColumnInfo.getFieldInfo().getFieldType().isPrimaryKey()) {
            return new FormulaSchema.Entity[] {fieldOrColumnInfo.getEntityInfo()};
        } else {
            return info.getFormulaForeignKeyDomains();
        }
    }
    
    /**
     * @return the default formula type to use when the ContextualFormulaFieldInfo isn't a FormulaProvider
     */
    protected abstract FormulaTypeSpec getDefaultFormulaType(); 
    
    @Override
    public FormulaTypeSpec getFormulaType() {
        return (info instanceof FormulaProvider) ? ((FormulaProvider)info).getFormulaType() : getDefaultFormulaType();
    }

    @Override
    public Formula getFormula() throws FormulaException {
        return (info instanceof FormulaProvider) ? ((FormulaProvider)info).getFormula() : null;
    }

    @Override
    public String getSource() throws FormulaException {
        return (info instanceof FormulaProvider) ? ((FormulaProvider)info).getSource() : null;
    }

    @Override
    public FormulaContext getFormulaContext() {
        return info.getFormulaContext();
    }
    
    @Override
    public int hashCode() {
        return super.hashCode() * Objects.hash(this.namespace, this.fieldOrColumnInfo, this.info);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        FormulaFieldInfoAdapter other = (FormulaFieldInfoAdapter) obj;
        return Objects.equals(this.namespace, other.namespace)
                && Objects.equals(this.fieldOrColumnInfo, other.fieldOrColumnInfo)
                && Objects.equals(this.info, other.info);
    }

    private final String namespace;
    private final FormulaSchema.FieldOrColumn fieldOrColumnInfo;
    private final ContextualFormulaFieldInfo info;
}
