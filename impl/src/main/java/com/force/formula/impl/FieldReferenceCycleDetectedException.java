/*
 * Created on Dec 8, 2004 
 */
package com.force.formula.impl;

import com.force.formula.FormulaException;
import com.force.formula.util.FormulaI18nUtils;

/**
 * Thrown when a reference is encoutered to a field that is not visible in the current formula context
 * 
 * @author dchasman
 * @since 140
 */
public class FieldReferenceCycleDetectedException extends FormulaException {
    private static final long serialVersionUID = 1L;

    public FieldReferenceCycleDetectedException(String name, String[] path) {
        super(FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionMessages", "FieldReferenceCycleDetectedException", name,
            formatPath(path, name)));

        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String[] getPath() {
        return path;
    }

    public String formatPath() {
        return formatPath(path, name);
    }
    
    private static String formatPath(String[] path, String name) {
        StringBuffer formattedPath = new StringBuffer();
        formattedPath.append(name);

        for (int n = 1; n < path.length && !name.equalsIgnoreCase(path[n]); n++) {
            formattedPath.append(" -> ");
            formattedPath.append(path[n]);
        }

        formattedPath.append(" -> " + name);

        return formattedPath.toString();
    }

    private final String name;
    private final String[] path;
}
