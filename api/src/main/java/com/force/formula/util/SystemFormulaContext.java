/*
 * Copyright, 2006, salesforce.com
 */

package com.force.formula.util;

import java.util.Calendar;

import com.force.formula.ContextualFormulaFieldInfo;
import com.force.formula.DisplayField;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaReturnType;
import com.force.formula.FormulaSchema;
import com.force.formula.InvalidFieldReferenceException;
import com.force.formula.UnsupportedTypeException;
import com.force.formula.sql.FormulaSQLProvider;
import com.force.formula.sql.FormulaSqlStyle;
import com.force.formula.sql.ITableAliasRegistry;
import com.force.formula.sql.SQLPair;
import com.force.i18n.BaseLocalizer;

/**
 * Example of a formula context that returns some fixed constants.  
 * @author dchasman
 * @since 146
 */
public class SystemFormulaContext extends NullFormulaContext {

    public SystemFormulaContext(FormulaContext outerContext){
        super(outerContext);
    }

    @Override
    public FormulaDateTime getDateTime(String fieldName) throws InvalidFieldReferenceException,
        UnsupportedTypeException {
        if (fieldName.equalsIgnoreCase(ORIGIN_DATE_TIME)) {
            return beginingOfTime;
        } else {
            return null;
        }
    }

    @Override
    public String getString(String fieldName, boolean useNative) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaDateTime dateTime = getDateTime(fieldName);
        return dateTime == null ? null : dateTime.toString();
    }

    @Override
    public DisplayField[] getDisplayFields(FormulaSchema.Entity entityInfo) {
        return displayFields;
    }

    @Override
    public String getName() {
        return SYSTEM_NAMESPACE;
    }

    @Override
    public FormulaReturnType getFormulaReturnType() {
        return null;
    }

    @Override
    public ContextualFormulaFieldInfo lookup(String name, boolean isDynamicRefBase) throws InvalidFieldReferenceException, UnsupportedTypeException {
        if (ORIGIN_DATE_TIME.equalsIgnoreCase(name)) {
        	FormulaSqlStyle style = FormulaEngine.getHooks().getSqlStyle();
        	if (style != null && style.isMysqlStyle()) {
        		return originDateTime_MYSQL;
        	} else if (style != null && style.isTransactSqlStyle()) {
        		return originDateTime_TSQL;
            } else if (style != null && style.isGoogleStyle()) {
                return originDateTime_GOOGLE;
        	}
            return originDateTime;
        } else {
            throw new InvalidFieldReferenceException(name, "Unknown field");
        }
    }

    @Override
    public String toDurableName(String name) throws InvalidFieldReferenceException, UnsupportedTypeException {
        return name;
    }

    @Override
    public String toJavascriptName(String name) throws InvalidFieldReferenceException {
        if (ORIGIN_DATE_TIME.equalsIgnoreCase(name)) {
            return ORIGIN_DATE_TIME;
        }
        return name;
    }

    
    @Override
    public String fromDurableName(String reference) throws InvalidFieldReferenceException, UnsupportedTypeException {
        return reference;
    }

    static class SystemFormulaFieldInfo extends FormulaFieldInfoImpl implements FormulaSQLProvider {

        public SystemFormulaFieldInfo(String name, FormulaDataType columnType, SQLPair sql) {
            super(name, name, name);
            this.columnType = columnType;
            this.sql = sql;
        }

        @Override
        public FormulaDataType getDataType() {
            return columnType;
        }

        @Override
        public String getDbColumn(String standardTablAlias, String customTableAlias) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SQLPair getSQL(ITableAliasRegistry registry) {
            return sql;
        }

        private final FormulaDataType columnType;
        private final SQLPair sql;
    }


    private static final String SYSTEM_NAMESPACE = "$System";
    private static final String ORIGIN_DATE_TIME = "OriginDateTime";
    private static final FormulaDateTime beginingOfTime;
    private static final SystemFormulaFieldInfo originDateTime = new SystemFormulaFieldInfo(ORIGIN_DATE_TIME,
        FormulaEngine.getHooks().getDataTypeByName("DateTime"), new SQLPair("TO_DATE('01-01-1900', 'DD-MM-YYYY')", null));
    private static final SystemFormulaFieldInfo originDateTime_MYSQL = new SystemFormulaFieldInfo(ORIGIN_DATE_TIME,
            FormulaEngine.getHooks().getDataTypeByName("DateTime"), new SQLPair("DATE('1900-01-01')", null));
    private static final SystemFormulaFieldInfo originDateTime_TSQL = new SystemFormulaFieldInfo(ORIGIN_DATE_TIME,
            FormulaEngine.getHooks().getDataTypeByName("DateTime"), new SQLPair("DATEFROMPARTS(1900,1,1)", null));
    private static final SystemFormulaFieldInfo originDateTime_GOOGLE = new SystemFormulaFieldInfo(ORIGIN_DATE_TIME,
            FormulaEngine.getHooks().getDataTypeByName("DateTime"), new SQLPair("DATE(1900,1,1)", null));
    private static DisplayField[] displayFields = new DisplayField[] { new DisplayField(SYSTEM_NAMESPACE, SYSTEM_NAMESPACE, originDateTime) };

    static {
        Calendar c = FormulaI18nUtils.getLocalizer().getCalendar(BaseLocalizer.GMT);
        c.clear();
        c.set(1900, 0, 1); // Months are zero-based in Java Calenders

        beginingOfTime = new FormulaDateTime(c.getTime());
    }
}
