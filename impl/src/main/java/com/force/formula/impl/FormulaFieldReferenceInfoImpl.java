package com.force.formula.impl;

import com.force.formula.FormulaFieldReferenceInfo;
import com.force.formula.FormulaSchema;

/**
 * Default imlpementation of FormulaFieldReferenceInfo
 * @author stamm
 * @since 0.2
 */
public class FormulaFieldReferenceInfoImpl implements FormulaFieldReferenceInfo {

    private FormulaSchema.FieldOrColumn fieldOrColumnId;
    private FormulaSchema.Entity foreignKeyDomain;

    public FormulaFieldReferenceInfoImpl(FormulaSchema.FieldOrColumn fieldOrColumnId){
        this.fieldOrColumnId = fieldOrColumnId;
    }
    public FormulaFieldReferenceInfoImpl(FormulaSchema.FieldOrColumn fieldOrColumnId, FormulaSchema.Entity foreignKeyDomain){
        this.fieldOrColumnId = fieldOrColumnId;
        this.foreignKeyDomain = foreignKeyDomain;
    }
    @Override
    public FormulaSchema.FieldOrColumn getFieldOrColumn() {
        return fieldOrColumnId;
    }

    @Override
    public FormulaSchema.Entity getForeignKeyDomain() {
        return foreignKeyDomain;
    }

    @Override
    public String toString(){
        return "fid:" + (fieldOrColumnId != null ? fieldOrColumnId.toString() : "" ) +
                "domain: " + (foreignKeyDomain != null ? foreignKeyDomain.toString() : "" );
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.fieldOrColumnId == null) ? 0 : this.fieldOrColumnId.hashCode());
        result = prime * result + ((this.foreignKeyDomain == null) ? 0 :this.foreignKeyDomain.getName().hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        FormulaFieldReferenceInfoImpl other = (FormulaFieldReferenceInfoImpl)obj;
        if (this.fieldOrColumnId == null) {
            if (other.fieldOrColumnId != null) return false;
        } else if (!this.fieldOrColumnId.equals(other.fieldOrColumnId)) return false;
        if (this.foreignKeyDomain == null) {
            if (other.foreignKeyDomain != null) return false;
        } else if (!this.foreignKeyDomain.equals(other.foreignKeyDomain)) return false;
        return true;
    }
}
