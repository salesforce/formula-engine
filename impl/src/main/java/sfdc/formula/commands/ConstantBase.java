/*
 * Copyright, 1999-2017, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package sfdc.formula.commands;

import java.lang.reflect.Type;

import sfdc.formula.FormulaCommandType;
import sfdc.formula.FormulaCommandType.AllowedContext;
import sfdc.formula.FormulaCommandType.SelectorSection;

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
