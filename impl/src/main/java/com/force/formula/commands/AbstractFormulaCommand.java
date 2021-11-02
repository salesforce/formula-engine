package com.force.formula.commands;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.force.formula.*;
import com.force.formula.impl.FormulaRuntimeTypeException;
import com.force.formula.sql.ITableAliasRegistry;

public abstract class AbstractFormulaCommand implements FormulaCommand {

    private static final long serialVersionUID = 1L;


	public AbstractFormulaCommand(FormulaCommandInfo formulaCommandInfo) {
        this(formulaCommandInfo.getName());
    }

    public AbstractFormulaCommand(String name) {
        this.name = name;
    }

    @Override
    public boolean isDeterministic(FormulaContext formulaContext) {
        return true;
    }

    @Override
    public boolean isCustomIndexable(FormulaContext formulaContext) {
        return isDeterministic(formulaContext);
    }

    @Override
    public boolean isFlexIndexable(FormulaContext formulaContext) {
        return isCustomIndexable(formulaContext);
    }

    @Override
    public boolean isPostSaveIndexUpdated(FormulaContext formulaContext, FormulaDmlType dmlType) {
        return false;
    }

    @Override
    public boolean isStale(FormulaContext formulaContext) {
        return false;
    }

    @Override
    public FormulaException validateMergeFieldsForFormulaType(FormulaContext formulaContext) {
        return null;
    }

    @Override
    public void preExecuteInBulk(List<FormulaRuntimeContext> contexts) throws FormulaException {
    }

    @Override
    public void visit(FormulaCommandVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public final String getName() {
        return name;
    }

    protected final BigDecimal checkNumberType(Object obj) {
        obj = fixRuntimeAdapter(obj);
        if (obj instanceof BigDecimal || obj == null) {
            return (BigDecimal)obj;
        }
        if (obj instanceof Number) {
            return new BigDecimal(obj.toString());
        }
        throw new FormulaRuntimeTypeException(getName(), BigDecimal.class, obj);

    }

    protected final Boolean checkBooleanType(Object obj) {
        obj = fixRuntimeAdapter(obj);
        if (obj instanceof Boolean || obj == null) {
            return (Boolean)obj;
        }
        throw new FormulaRuntimeTypeException(getName(), Boolean.class, obj);

    }

    // Perform conversions for types to be handled as String.
    // Anything else returned unchanged.
    protected final Object fixString(Object obj) {
        obj = fixRuntimeAdapter(obj);
        if (obj instanceof ConstantString.StringWrapper) {
            return obj.toString();
        } else if (obj instanceof FieldSetMemberInfo) {
            return ((FieldSetMemberInfo)obj).getFieldPath();
        } else if (obj instanceof String ) {
        	return obj;
        }
        return FormulaEngine.getHooks().convertToString(obj);
    }

    protected final String checkStringType(Object obj) {
        obj = fixString(obj);
        if (obj instanceof String || obj == null) {
            return (String)obj;
        }
        throw new FormulaRuntimeTypeException(getName(), String.class, obj);
    }

    protected final FormulaCurrencyData checkCurrencyDataType(Object obj) {
        obj = fixRuntimeAdapter(obj);
        if (obj instanceof FormulaCurrencyData || obj == null) {
            return (FormulaCurrencyData)obj;
        }
        throw new FormulaRuntimeTypeException(getName(), FormulaCurrencyData.class, obj);
    }

    protected final FormulaGeolocation checkGeoLocationType(Object obj) {
        obj = fixRuntimeAdapter(obj);
        if (obj instanceof FormulaGeolocation || obj == null) {
            return (FormulaGeolocation)obj;
        }
        throw new FormulaRuntimeTypeException(getName(), FormulaGeolocation.class, obj);
    }

    protected final Date checkDateType(Object obj) {
        obj = fixRuntimeAdapter(obj);
        if (obj instanceof FormulaDateTime) {
            return ((FormulaDateTime)obj).getDate();
        } else if (obj instanceof Date || obj == null) {
            return (Date)obj;
        }
        throw new FormulaRuntimeTypeException(getName(), Date.class, obj);
    }

    protected final FormulaDateTime checkDateTimeType(Object obj) {
        obj = fixRuntimeAdapter(obj);
        if (obj instanceof FormulaDateTime || obj == null) {
            return (FormulaDateTime)obj;
        }
        throw new FormulaRuntimeTypeException(getName(), FormulaDateTime.class, obj);
    }

    @SuppressWarnings("unchecked")
    protected final Comparable<Object> checkComparableType(Object obj) {
        obj = fixRuntimeAdapter(obj);
        if (obj instanceof Comparable || obj == null) {
            return (Comparable<Object>)obj;
        }
        throw new FormulaRuntimeTypeException(getName(), Comparable.class, obj);
    }

    protected boolean shouldReturnBoolResult(FormulaRuntimeContext context) {
        // determines if T/F is returned for null values or if three-value semantics is followed
    	return FormulaEngine.getHooks().shouldReturnBoolResult(context);
    }

    // It is possible to get a RuntimeTypeMetadataELAdapter during a VF compile-time evaluation
    // (because of apex:variable or iterator definitions of variables).  These have to be treated
    // as null.
    private Object fixRuntimeAdapter(Object obj) {
    	if (FormulaEngine.getHooks().treatObjectAsNull(obj)) {
    		return null;
    	}
        return obj;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
    }

    private final String name;


    @Override
    public List<FormulaFieldReferenceInfo> getDirectReference(FormulaContext formulaContext, ITableAliasRegistry reg,
                boolean zeroExcluded, boolean allowDateValue, AtomicBoolean caseSafeIdUsed, FormulaDataType formulaResultDataType) throws UnsupportedTypeException, InvalidFieldReferenceException {
        return null;
    }
}
