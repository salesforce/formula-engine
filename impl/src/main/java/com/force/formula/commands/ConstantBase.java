/*
 * Copyright, 1999-2017, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.commands;

import java.lang.reflect.Type;

import com.force.formula.FormulaCommandType;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;

/**
 * Base class for constants in formulas
 * 
 * @author a.rich
 * @since 208
 */
@AllowedContext(section = SelectorSection.MATH, isOffline = true)
public abstract class ConstantBase extends FormulaCommandInfoImpl {

    public ConstantBase(String name) {
        super(name);
    }

    public ConstantBase(String name, Type returnType, Type[] argumentTypes) {
        super(name, returnType, argumentTypes);
    }

    @Override
    protected FormulaCommandType.AllowedContext getDefaultContext() {
        return ConstantBase.class.getAnnotation(FormulaCommandType.AllowedContext.class);
    }
    
}
