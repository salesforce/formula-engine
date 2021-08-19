/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import java.lang.reflect.Proxy;

import com.force.formula.*;
import com.force.formula.FormulaSchema.Entity;
import com.force.formula.FormulaSchema.FieldOrColumn;
import com.force.formula.util.FormulaFieldInfoImpl;

/**
 * Simple implementation of ContextualFormulaFieldInfo, that actually has context
 *
 * @author stamm
 * @since 0.2
 */
public class ContextualFormulaFieldInfoImpl extends FormulaFieldInfoImpl implements ContextualFormulaFieldInfo, FormulaProvider {

    private final FormulaContext context;
    private final FormulaSchema.FieldOrColumn field;
    private final String formulaSource;
    private Formula formulaCache;
    public ContextualFormulaFieldInfoImpl(FormulaContext context, FormulaSchema.FieldOrColumn field) {
        this(context, field, null);
    }

    public ContextualFormulaFieldInfoImpl(FormulaContext context, FormulaSchema.FieldOrColumn field, String formulaSource) {
        super(field.getName(), field.getName(), field.getName());
        this.field = field;
        this.context = context;
        this.formulaSource = formulaSource;
    }

    @Override
    public FieldOrColumn getFieldOrColumnInfo() {
        return this.field;
    }

    @Override
    public FormulaDataType getDataType() {
        return field.getDataType();
    }

    @Override
    public Entity[] getFormulaForeignKeyDomains() {
        if (field instanceof FormulaSchema.Field) {
            return ((FormulaSchema.Field)field).getFormulaForeignKeyDomains();
        } else {
            return null;
        }
    }

    @Override
    public FormulaContext getFormulaContext() {
        return this.context;
    }

    @Override
    public Formula getFormula() throws FormulaException {
        if (formulaSource == null) return null;
        if (formulaCache == null) {
            try {
                // For historical reasons the FormulaContext contains the target return type.  Lambda's help
                FormulaContext withNewReturn = (FormulaContext) Proxy.newProxyInstance(ContextualFormulaFieldInfoImpl.class.getClassLoader(), 
                        new Class[] {FormulaContext.class}, 
                        (proxy, method, methodArgs) -> {
                            if ("getFormulaReturnType".equals(method.getName())) {
                                return this;
                            } else {
                                return method.invoke(this.context, methodArgs);
                            }
                        });
                formulaCache = FormulaInfoFactory.create(context.getGlobalProperties().getFormulaType(), withNewReturn, this.formulaSource).getFormula();
            } catch (FormulaException x) {
                formulaCache = new InvalidFormula(new RuntimeException(x));
            }
        }
        return formulaCache;
    }

    @Override
    public FormulaTypeSpec getFormulaType() {
        return this.context != null ? this.context.getGlobalProperties().getFormulaType() : null;
    }

    @Override
    public String getSource() throws FormulaException {
        return formulaSource;
    }    
}
