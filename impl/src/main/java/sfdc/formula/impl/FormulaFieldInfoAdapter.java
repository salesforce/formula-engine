package sfdc.formula.impl;

import sfdc.formula.*;

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

    private final String namespace;
    private final FormulaSchema.FieldOrColumn fieldOrColumnInfo;
    private final ContextualFormulaFieldInfo info;
}
