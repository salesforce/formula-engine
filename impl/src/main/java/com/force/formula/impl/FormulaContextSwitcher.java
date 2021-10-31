/*
 * Copyright, 2008, SALESFORCE.com All Rights Reserved Company Confidential
 */

package com.force.formula.impl;
import java.util.*;

import com.force.formula.*;
import com.force.formula.util.BaseCompositeFormulaContext;

/**
 * Switch between FormulaContexts for evaluating formulas on different entities.
 *
 * @author wmacklem
 * @since 154
 */
public class FormulaContextSwitcher {
    private final List<BaseCompositeFormulaContext> compositeContexts;
    private final List<FormulaRuntimeContext> originalFormulaContexts;
    
    public FormulaContextSwitcher(Collection<? extends FormulaContext> contexts) {
        this.compositeContexts = new ArrayList<BaseCompositeFormulaContext>(contexts.size());
        this.originalFormulaContexts = new ArrayList<FormulaRuntimeContext>(contexts.size());
        for(FormulaContext context : contexts) {
            if (context instanceof BaseCompositeFormulaContext) {
                compositeContexts.add((BaseCompositeFormulaContext)context);
                originalFormulaContexts.add(((BaseCompositeFormulaContext)context).getDefaultContext());  
            }
        }
    }

    
    public FormulaContextSwitcher(FormulaContext context) {
        if (context instanceof BaseCompositeFormulaContext) {
            this.compositeContexts = Collections.singletonList((BaseCompositeFormulaContext)context);
            this.originalFormulaContexts = Collections.singletonList(((BaseCompositeFormulaContext)context).getDefaultContext());
        } else {
            this.compositeContexts = Collections.emptyList();
            this.originalFormulaContexts = Collections.emptyList();
        }
    }
    
    /**
     * When evaluating a nested formula, let's switch to the FormulaContext for the entity the formula resides on.
     * 
     * This must be balanced with a call to revertToOriginalFormulaContext() in a try-catch block.
     * @param formulaFieldInfo for the formula field being evaluated
     */
    public void switchFormulaContext(ContextualFormulaFieldInfo formulaFieldInfo) {
        FormulaContext context = formulaFieldInfo.getFormulaContext();
        assert formulaFieldInfo instanceof FormulaProvider : "The FormulaFieldInfo wasn't a FormulaProvider, something's wrong";
        FormulaTypeSpec newFormulaType = ((FormulaProvider)formulaFieldInfo).getFormulaType();
        if (context instanceof FormulaRuntimeContext){
            for (int i =0; i<compositeContexts.size(); i++) {
                if (this.compositeContexts.get(i).getDefaultContext() != this.originalFormulaContexts.get(i)) {
                    throw new AssertionError("Didn't find the expected default FormulaContext");
                }
            
            }
            for (BaseCompositeFormulaContext compositeContext : compositeContexts) {
                compositeContext.setDefaultContext((FormulaRuntimeContext)context);
            }
        }
        for (BaseCompositeFormulaContext compositeContext : compositeContexts) {
            compositeContext.getGlobalProperties().pushFormulaType(newFormulaType);
        }
    }
    
    /**
     * After the formula has been evaluated, revert back to the original FormulaContext.
     * 
     * This must be balanced with a call to switchFormulaContext() in a try-catch block.
     */
    public void revertToOriginalFormulaContext() {
        for (int i =0; i<compositeContexts.size(); i++) {
            this.compositeContexts.get(i).setDefaultContext(this.originalFormulaContexts.get(i));
            this.compositeContexts.get(i).getGlobalProperties().popFormulaType();
        }
    }
}
