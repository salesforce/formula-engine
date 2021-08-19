/*
 * Copyright, 1999-2007, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.util;

import com.force.formula.*;

/**
 * Base Implementation of FormulaFieldInfo that provides default implementations of most methods.
 *
 * @author aballard
 * @since 150
 */
public abstract class FormulaFieldInfoImpl implements ContextualFormulaFieldInfo {

    public FormulaFieldInfoImpl(Object id, String name, Object label) {
        this.id = id;
        this.name = name;
        this.label = label;
    }

    // Constructor for classes that have their own implementations of id/name/label.
    public FormulaFieldInfoImpl() {
        this(null, null, null);
    }

    @Override
    public String getDbColumn(String standardTablAlias, String customTableAlias) {
        return null;
    }

    @Override
    public FormulaPicklistInfo getEnumInfo() {
        return null;
    }

    @Override
    public String[] getExternalizedIds() {
        return null;
    }

    @Override
    public FormulaSchema.FieldOrColumn getFieldOrColumnInfo() {
        return null;
    }

    @Override
    public Object getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getScale() {
        return 0;
    }

    @Override
    public FormulaDataType getTemplateDataType() {
        return getDataType();
    }

    public FormulaDataType getDbDataType() {
        return getDataType();
    }


    @Override
    public FormulaSchema.Entity[] getFormulaForeignKeyDomains() {
        return null;
    }

    @Override
    public boolean isCustom() {
        return false;
    }

    @Override
    public FormulaContext getFormulaContext() {
        return null;
    }

    @Override
    public boolean isRuntimeType() {
        return false;
    }

    private final Object id;
    private final String name;
    private final Object label;

}
