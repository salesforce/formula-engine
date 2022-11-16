/**
 * 
 */
package com.force.formula.util;

import java.util.HashMap;
import java.util.Map;

import com.force.formula.FormulaCommandType;
import com.force.formula.FormulaReturnType;
import com.force.formula.FormulaTypeSpec;

/**
 * Class for implementing a "base" Formula Context with an "intended" return type.
 * 
 * You should be able to extend this and produce a formula context suitable for the "root"
 *
 * @author stamm
 * @since 0.2.6
 */
public abstract class BaseRootFormulaContext extends BaseCompositeFormulaContext {
    private final FormulaReturnType returnType;

    /**
     * @param topLevelFormulaType the top level formula type
     * @param returnType the type to return for this formula
     */
    public BaseRootFormulaContext(FormulaTypeSpec topLevelFormulaType, FormulaReturnType returnType) {
        super(null, topLevelFormulaType);
        this.returnType = returnType;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public FormulaReturnType getFormulaReturnType() {
        return this.returnType;
    }

    @Override
    public boolean isFunctionSupported(FormulaCommandType function) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P> P getProperty(String name) {
        return (P)properties.get(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        properties.put(name, value);
    }
    

    private final Map<String, Object> properties = new HashMap<>(4);


}
