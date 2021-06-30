/*
 * Copyright, 1999-2015, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package sfdc.formula.impl;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import javax.script.*;

import com.google.common.base.Objects;

import sfdc.formula.*;
import sfdc.formula.commands.ConstantNull;
import sfdc.formula.commands.RuntimeType;

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

    

    /**
     * Convert an object from a nashorn type to a formula type.  Mostly around date & number convertion.
     * @param obj an object returned from nashorn
     * @return a formula engine useful version.
     */
    @SuppressWarnings("restriction")
    public static Object convertFromNashorn(ScriptEngine engine, Object obj) {
        if (obj instanceof Number) {
            if (obj instanceof BigDecimal) {
                return obj;
            } else if (obj instanceof Double) {
                double val = ((Number)obj).doubleValue();
                if (Double.isNaN(val) || Double.isInfinite(val)) return null;
                if (val == 0) return BigDecimal.ZERO;
                if (val == 1) return BigDecimal.ONE;
                return new BigDecimal(val).setScale(7, RoundingMode.HALF_EVEN).stripTrailingZeros(); // NOPMD
            } else {
                return new BigDecimal(((Number)obj).intValue());
            }
        }
        
        if (obj instanceof jdk.nashorn.api.scripting.ScriptObjectMirror) {
            jdk.nashorn.api.scripting.ScriptObjectMirror mirror = (jdk.nashorn.api.scripting.ScriptObjectMirror) obj;
            if (mirror.hasMember("getTime")) {
                // It's a ScriptObjectMirror with 'time'.  Assume date
                JsDateWrapper wrapper = ((Invocable)engine).getInterface(obj, JsDateWrapper.class);
                return new Date(wrapper.getTime());
            } else {
                return new BigDecimal((String)mirror.callMember("toString"));
            }
        }
        if ("".equals(obj)) return null;  // Oracle stupidity
        return obj;
    }
    
    
    /**
     * Convert an object from a Graal type to a formula type.  Mostly around date & number convertion.
     * @param obj an object returned from nashorn
     * @return a formula engine useful version.
     */
    public static Object convertFromGraal(Object obj, FormulaDataType type) {
        if (obj instanceof Number) {
            if (obj instanceof BigDecimal) {
                return obj;
            } else if (obj instanceof Double) {
                double val = ((Number)obj).doubleValue();
                if (Double.isNaN(val) || Double.isInfinite(val)) return null;
                if (val == 0) return BigDecimal.ZERO;
                if (val == 1) return BigDecimal.ONE;
                BigDecimal result = new BigDecimal(val).setScale(7, RoundingMode.HALF_EVEN).stripTrailingZeros();  // NOPMD
                if (result.scale() < 0) {
                    result = result.setScale(0);  // No 3E+2
                }
                return result;
            } else {
                return new BigDecimal(((Number)obj).intValue());
            }
        }
        
        if (obj instanceof Map) {
        	if (type.isDate() || type.isTimeOnly()) {
        		// Get the tostring and parse it
        		try {
					return FormulaDateUtil.parseISO8601(obj.toString());
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}
        	} else if (type.isNumber()) {
        		return new BigDecimal(obj.toString());
        	} 
        }
        if ("".equals(obj)) return null;  // Oracle stupidity
        return obj;
    }    
    
    // 
    public interface JsDateWrapper {
        long getTime();
        int getTimezoneOffset();
    }
    
    
    /**
     * Convert an object from a formula type to a nashorn type.  Mostly to fix dates
     * @param obj an object returned from formulas
     * @return a nashorn useful version.
     */
    public static Object convertToNashorn(ScriptEngine engine, Object obj, FormulaContext context) {
        try {
            if (obj instanceof Date) {
                return engine.eval("new Date("+((Date)obj).getTime()+")");
            } else if (obj instanceof FormulaDateTime) {
                return engine.eval("new Date("+((FormulaDateTime)obj).getTime()+")");
            } else if (obj instanceof Number && context.useHighPrecisionJs()) {
                return engine.eval("new $F.Decimal("+((Number)obj).toString()+")");
            } else if (obj instanceof Map) {
                // This'll help convert deep objects.  We'll do it in place to keep the cost down.
                ((Map<?,Object>)obj).entrySet().stream().forEach((e)->e.setValue(FormulaTypeUtils.convertToNashorn(engine, e.getValue(), context)));
            }
            // TODO: Convert BigDecimal -> Double?
        } catch (ScriptException x) { // NOPMD
            // Ignore it for now
        }
        return obj;
    }
}
