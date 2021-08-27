/*
 * Copyright, 1999-2015, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula.impl;

import java.lang.reflect.Type;

import com.force.formula.*;
import com.force.formula.commands.ConstantNull;
import com.force.formula.commands.RuntimeType;
import com.force.formula.util.FormulaI18nUtils;
import com.google.common.base.Objects;

/**
 * Utilities for resolving formula types, mostly runtime text/type validations.
 * Extracted from FormulaUtils to make it easier to manager
 *
 * @author stamm
 * @since 200
 */
public class FormulaTypeUtils {
    // From LayoutFieldListUtils
    /**
     * Return the (EntityName) or (EntityName,...) for adding to the end of a lookup type
     * @param infos the domain infos
     * @param shortenFKDomains if it should return ... if there is more than one
     * @return the string to add to the "Lookup" or "Master-Detail" type of the object
     */
    public static String getDomainAddition(FormulaSchema.Entity[] infos, boolean shortenFKDomains) {
        if (infos == null) return "";
        StringBuilder sb = new StringBuilder();
        if (shortenFKDomains && infos.length > 1) {
            sb.append(FormulaI18nUtils.getLocalizer().getLabel("Page_LayoutFieldsEdit", "detailLookupAbbr", infos[0].getLabel(), infos.length - 1));
        } else {
            for (FormulaSchema.Entity info : infos) {
                if (sb.length() != 0) sb.append(',');
                sb.append(info.getLabel());
            }
        }
        return FormulaI18nUtils.getLocalizer().getLabel("Page_LayoutFieldsEdit", "detailLookup", sb.toString());
    }
    
    public static String getTypeName(Type type) {
        if (type instanceof Class) {
            return ((Class<?>)type).getName();
        } else if (type instanceof FormulaTypeWithDomain) {
            return ((FormulaTypeWithDomain)type).getTypeName();
        } else {
            return String.valueOf(type);
        }
    }

    public static String getTypeLabel(Type type) {
        if (type instanceof FormulaTypeWithDomain.IdType) {
            return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionDataTypes", getTypeName(type)) + getDomainAddition(((FormulaTypeWithDomain.IdType)type).getDomains(), false);
        } else {
            return FormulaI18nUtils.getLocalizer().getLabel("FormulaFieldExceptionDataTypes", getTypeName(type));
        }
    }

    public static boolean isTypeText(Type type) {
        if (type == String.class) {
            return true;
        }
        if (type instanceof FormulaTypeWithDomain.IdType) {
            return ((FormulaTypeWithDomain.IdType)type).isTypeText();
        }
        return false;
    }
    
    public static boolean isTypePicklist(Type type) {
    	// TODO SLT FIXME: make this less awful
    	return type.getTypeName().endsWith("PicklistData");
    }

    /**
     * Same as isTypeText, but in cases where we think supporting an ID is ugly.
     * @param type
     * @return
     */
    public static boolean isTypeTextUgly(Type type) {
        return isTypeText(type);
    }
    
    public static boolean isTypeIdList(Type type, FormulaDataType expectedType, FormulaSchema.FieldOrColumn info) {
        if (type == String[].class) {
            return true;
        }
        if (type instanceof FormulaTypeWithDomain.IdType) {
            return (((FormulaTypeWithDomain.IdType)type).isApplicable(info) 
            		&& (FormulaUtils.isTypeSobjectRow(expectedType)) ==  !((FormulaTypeWithDomain.IdType)type).isTypeText());
        } else if (type instanceof FormulaTypeWithDomain) {
        	// Assume this is a list
            return ((((FormulaTypeWithDomain)type).getRawType() == String[].class) && ((FormulaTypeWithDomain)type).isApplicable(info));
        }
        return false;
    }

    
    public static boolean canCastTo(Type lhs, Type rhs) {
        if (Objects.equal(lhs,rhs)) return true;
        if (lhs == RuntimeType.class || rhs == RuntimeType.class) return true;
        return false;
    }

    /**
     * @return whether the values on the left and right hand side have a common super type
     * (i.e. they could be compared)
     *
     * For historical reasons, *all* IdFormulaTypes can be compared even if they have the wrong type
     *
     * @param lhs the left hand side of a comparison/equality
     * @param rhs the right hand side of a comparison/equality
     */
    public static boolean hasCommonSuperType(Type lhs, Type rhs) {
        return lhs == ConstantNull.class || lhs == RuntimeType.class
                || rhs == ConstantNull.class || rhs == RuntimeType.class
                || Objects.equal(lhs,rhs) || (isTypeText(lhs) && isTypeText(rhs));
    }

    /**
     * For objects involved with a comparison, determine if there is a common supertype of
     * the either side, and if so, return it.
     * @param lhs
     * @param rhs
     * @return
     */
    public static Type getCommonSuperType(Type lhs, Type rhs) {
        if (lhs == ConstantNull.class) return rhs;
        if (rhs == ConstantNull.class) return lhs;
        if (lhs == RuntimeType.class || rhs == RuntimeType.class) return RuntimeType.class;
        if (Objects.equal(lhs,rhs)) return lhs;
        if (lhs instanceof FormulaTypeWithDomain.IdType && rhs instanceof FormulaTypeWithDomain.IdType) {
            return ((FormulaTypeWithDomain.IdType)lhs).addToDomain(((FormulaTypeWithDomain.IdType)rhs).getDomains());
        } else if (isTypeText(lhs) && isTypeText(rhs)) {
            return String.class;
        }
        // No common denominator.
        return null;
    }   
    
}
