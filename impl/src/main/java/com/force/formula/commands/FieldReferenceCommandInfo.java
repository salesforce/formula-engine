/*
 * Created on Dec 10, 2004
 */
package com.force.formula.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.force.formula.ContextualFormulaFieldInfo;
import com.force.formula.Formula;
import com.force.formula.FormulaCommand;
import com.force.formula.FormulaCommandType.AllowedContext;
import com.force.formula.FormulaCommandType.SelectorSection;
import com.force.formula.FormulaContext;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaDateTime;
import com.force.formula.FormulaEngine;
import com.force.formula.FormulaException;
import com.force.formula.FormulaFieldInfo;
import com.force.formula.FormulaFieldReference;
import com.force.formula.FormulaFieldReferenceInfo;
import com.force.formula.FormulaGeolocation;
import com.force.formula.FormulaProperties;
import com.force.formula.FormulaProvider;
import com.force.formula.FormulaSchema;
import com.force.formula.FormulaSchema.FieldOrColumn;
import com.force.formula.FormulaTime;
import com.force.formula.InvalidFieldReferenceException;
import com.force.formula.RuntimeFormulaInfo;
import com.force.formula.UnsupportedTypeException;
import com.force.formula.impl.FieldReferenceCycleDetectedException;
import com.force.formula.impl.FormulaAST;
import com.force.formula.impl.FormulaInfoFactory;
import com.force.formula.impl.FormulaSqlHooks;
import com.force.formula.impl.FormulaValidationHooks;
import com.force.formula.impl.JsValue;
import com.force.formula.impl.NestedFormulaException;
import com.force.formula.impl.TableAliasRegistry;
import com.force.formula.parser.gen.FormulaTokenTypes;
import com.force.formula.sql.FormulaSQLProvider;
import com.force.formula.sql.FormulaWithSql;
import com.force.formula.sql.ITableAliasRegistry;
import com.force.formula.sql.SQLPair;
import com.force.formula.sql.TableSet;
import com.force.formula.util.BaseCompositeFormulaContext;
import com.force.formula.util.FormulaFieldReferenceImpl;
import com.google.common.base.Splitter;

/**
 * Abstract way to retrieve the value for associated field from the context and push onto the stack.
 * 
 * The intent for the developer is to extend this and implement the abstract functions and then register
 * that extension in the {@link FormulaFactory#getTypeRegistry()}
 * 
 * @author dchasman
 * @since 140
 */
@AllowedContext(section = SelectorSection.ADVANCED, isOffline = true)
public class FieldReferenceCommandInfo extends FormulaCommandInfoImpl implements FormulaCommandEnricher {

    public FieldReferenceCommandInfo() {
        super("IDENT", null, new Class[0]);
    }

    @Override
    public Type getReturnType(FormulaAST node, FormulaContext context) throws FormulaException {
        String fieldName = node.getText();
        FormulaFieldInfo referencedFieldInfo = lookup(context, fieldName, node.isDynamicReferenceBase());

        assert referencedFieldInfo != null;

        if (referencedFieldInfo.isRuntimeType()) {
            return RuntimeType.class;
        }

        FormulaDataType dataType = FormulaAST.isTopOfTemplateExpression(node) ? referencedFieldInfo.getTemplateDataType() : referencedFieldInfo.getDataType();

        Type returnType = getFieldReturnType(dataType, node, context, referencedFieldInfo.getFormulaForeignKeyDomains());

        if (returnType == null) {
            throw new UnsupportedTypeException(referencedFieldInfo.getName(), referencedFieldInfo.getDataType());
        }

        node.setColumnType(dataType);

        return returnType;
    }

    public static Type getFieldReturnType(FormulaDataType columnType, FormulaAST node, FormulaContext context, FormulaSchema.FieldOrColumn targetField) {
        if (columnType.isId() && targetField != null) {
            return getFieldReturnType(columnType, node, context, targetField.getFieldInfo().getFieldType().isPrimaryKey() ?
                    new FormulaSchema.Entity[] {targetField.getEntityInfo()} : targetField.getFieldInfo().getFormulaForeignKeyDomains());
        } else {
            return getFieldReturnType(columnType, node, context, (FormulaSchema.Entity[]) null);
        }
    }

    static Type getFieldReturnType(FormulaDataType columnType, FormulaAST node, FormulaContext context, FormulaSchema.Entity[] domain) {

        if(columnType == null) {
            return null;
        }
        Type returnType = FormulaValidationHooks.get().parseHook_getFieldReturnTypeOverride(columnType, node, context, domain);
        if (returnType != null) {
            return returnType;
        }

        if (columnType.isLocation()) {
            returnType = FormulaGeolocation.class;
        } else if (columnType.isCurrency()) {
            returnType = BigDecimal.class;
        } else if (columnType.isNumber()) {
            returnType = BigDecimal.class;
        } else if (columnType.isSimpleTextOrClob() || columnType.isTextEnum() || columnType.isAnyPerson()) {
            returnType = String.class;
        } else if (columnType.isEncrypted() && context.getGlobalProperties().getFormulaType().allowsLegacyEncryptedFields()) {
            returnType = String.class;
        } else if (columnType.isDateOnly()) {
            returnType = Date.class;
        } else if (columnType.isDate()) {
            returnType = FormulaDateTime.class;
        } else if (columnType.isTimeOnly()) {
            returnType = FormulaTime.class;
        } else if (columnType.isBoolean()) {
            returnType = Boolean.class;
        } else if (columnType.isId()) {
            returnType = FormulaValidationHooks.get().constructIdType(domain);
        }

        return returnType;
    }

    static ContextualFormulaFieldInfo lookup(FormulaContext context, String fieldName, boolean isDynamicRef) throws UnsupportedTypeException, InvalidFieldReferenceException {
        return lookup(context, new FormulaFieldReferenceImpl(null, fieldName, false, isDynamicRef));
    }

    static ContextualFormulaFieldInfo lookup(FormulaContext context, FormulaFieldReference fieldReference) throws InvalidFieldReferenceException, UnsupportedTypeException {
        return FormulaEngine.getHooks().lookupFieldReferenceForCompile(context, fieldReference);
    }
    
    
    @Override
    public FormulaAST enrich(FormulaAST node, FormulaContext context, FormulaProperties properties)
        throws FormulaException {
        String fieldName = node.getText();
        FormulaFieldInfo referencedFieldInfo = lookup(context, fieldName, node.isDynamicReferenceBase());

        // The FormulaFieldInfo should always be FormulaProvider in this
        // setting.
        if (!(referencedFieldInfo instanceof FormulaProvider))
            return node;

        FormulaDataType columnType = referencedFieldInfo.getDataType();
        if (columnType.isNumber()) {
            return treatNull(node, properties);
        } else if (columnType.isCurrency()) {
            // Wrap the argument node in an explicit ConvertCurrencyToNumber node
            FormulaAST newParent = new FormulaAST();
            newParent.setType(FormulaTokenTypes.FUNCTION_CALL);
            newParent.setText("ConvertCurrencyToNumber");
            newParent.setDataType(BigDecimal.class);
            node.reparent(newParent);
            return treatNull(newParent, properties);
        } else {
            return node;
        }
    }

    private FormulaAST treatNull(FormulaAST node, FormulaProperties properties) {
        if (properties.getTreatNullNumberAsZero()) {
            // Wrap the argument node in an explicit NVL node
            FormulaAST newParent = new FormulaAST();
            newParent.setType(FormulaTokenTypes.FUNCTION_CALL);
            newParent.setText("NULLVALUE");
            newParent.setDataType(BigDecimal.class);

            FormulaAST zeroNode = new FormulaAST();
            zeroNode.setType(FormulaTokenTypes.NUMBER);
            zeroNode.setText("0");
            zeroNode.setDataType(BigDecimal.class);

            node.reparent(newParent, zeroNode);
            return newParent;
        } else
            return node;
    }

    @Override
    public FormulaCommand getCommand(FormulaAST node, FormulaContext context) throws InvalidFieldReferenceException,
        FieldReferenceCycleDetectedException, NestedFormulaException, UnsupportedTypeException {
        String fieldName = node.getText();

        FormulaFieldInfo referencedFieldInfo = lookup(context, fieldName, node.isDynamicReferenceBase());

        Formula formula = null;
        if (referencedFieldInfo instanceof FormulaProvider) {
            try {
                formula = ((FormulaProvider)referencedFieldInfo).getFormula();
            } catch (Exception e) {
                throw new NestedFormulaException(e, fieldName);
            }
        }

        FormulaCommand command;
        if (formula != null) {
            command = new CompositeCommand(this, fieldName, formula, referencedFieldInfo.getDataType());
        } else {
            command = FormulaValidationHooks.get().parseHook_constructFieldReferenceCommand(getName(), fieldName, FormulaAST.isTopOfTemplateExpression(node),
                FormulaAST.isTopOfReferenceFormula(node), node.isDynamicReferenceBase());
        }

        return command;
    }

    @Override
    public SQLPair getSQL(FormulaAST node, FormulaContext context, String[] args, String[] guards, TableAliasRegistry registry) throws FormulaException {
        String fieldName = node.getText();

        ContextualFormulaFieldInfo formulaFieldInfo = lookup(context, fieldName, node.isDynamicReferenceBase());
        FormulaSchema.FieldOrColumn foci = (formulaFieldInfo!=null)?formulaFieldInfo.getFieldOrColumnInfo():null;
        FormulaSchema.Entity ei = (foci!=null) ? foci.getEntityInfo() : null;
        
        // Call parsehook 
        if (ei != null) {
            FormulaValidationHooks.get().parseHook_validateSqlFieldReference(ei, foci, context);
        }
      
        if (formulaFieldInfo instanceof FormulaSQLProvider) {
            FormulaSQLProvider fsp = (FormulaSQLProvider)formulaFieldInfo;
            SQLPair sqlPair = fsp.getSQL(registry);
            if (sqlPair != null) {
                return sqlPair;
            }
        }

        FormulaWithSql formula = null;
        if (formulaFieldInfo instanceof FormulaProvider) {
            formula =(FormulaWithSql) ((FormulaProvider)formulaFieldInfo).getFormula();
        }

        List<FormulaFieldReferenceInfo> fieldPath = getFieldPath(formulaFieldInfo);

        FormulaSqlHooks sqlHooks = getSqlHooks(context);
        
        // Get SQL
        String sql;
        String guard;
        if (formula == null) {

            TableSet aliases = registry.getTableAliases(fieldPath);
            sql = formulaFieldInfo.getDbColumn(aliases.mainAlias, aliases.cfAlias);
            if (formulaFieldInfo.getDataType().isPercent()) {
                // The actual value of the field is in formatted fashion
                sql = sqlHooks.sqlConvertPercent(sql);
            } else if (formulaFieldInfo.getDataType().canBeEmptyKeyForNullInDb()) {
            	// Hard code internals for salesforce IDs
                sql = "CASE WHEN " + sql + " = '000000000000000' THEN NULL ELSE " + sql + " END";
            }

            guard = null;
        } else {
            // Can't go through getDbColumnForCalculatedField because that tacks stuff on
            sql = formula.getSQLRaw();
            guard = formula.getGuard();

            ITableAliasRegistry nestedRegistry = formula.getTableAliasRegistry();
            sql = registry.translate(sql, nestedRegistry, fieldPath);
            guard = registry.translate(guard, nestedRegistry, fieldPath);
        }        
    
        // Allow overriding of the sql
        sql = FormulaValidationHooks.get().overrideFieldReferenceSql(formula, sql, fieldPath, formulaFieldInfo, registry);
                
        if (formulaFieldInfo.getDataType().isBoolean()) {
            // Only bother with this if we are a database field; a formula's already a boolean so NVL would be a syntax error
            if (formulaFieldInfo.getFieldOrColumnInfo() != null && formulaFieldInfo.getFieldOrColumnInfo().getFieldInfo().isCalculated()) {
                sql = "("+sqlHooks.sqlNvl()+"(CASE WHEN " + sql + " THEN '1' ELSE '0' END, '0') = '1')";
            } else {
                sql = "("+sqlHooks.sqlNvl()+"(" + sql + ", '0') = '1')";
            }
        }

        return new SQLPair(sql, guard);
    }

    @Override
    public JsValue getJavascript(FormulaAST node, FormulaContext context, JsValue[] args) throws FormulaException {
        String fieldName = node.getText();
        ContextualFormulaFieldInfo referencedFieldInfo = lookup(context, fieldName, node.isDynamicReferenceBase());

        Formula formula = null;
        boolean shouldExpand = Boolean.TRUE.equals(context.getProperty(FormulaContext.EXPAND_FORMULA_REFERENCES));

        if (referencedFieldInfo instanceof FormulaProvider && shouldExpand) {
            try {
                formula = ((FormulaProvider)referencedFieldInfo).getFormula();
            } catch (Exception e) {
                throw new NestedFormulaException(e, fieldName);
            }
        }
        if (referencedFieldInfo == null) {
            return new JsValue("null", "false", true);
        } else {
            String value;
            String guardJs = null;
            FormulaSchema.FieldOrColumn foci = referencedFieldInfo.getFieldOrColumnInfo();

            if (BaseCompositeFormulaContext.isGlobalContextFieldReference(fieldName) || referencedFieldInfo.getFormulaContext() instanceof FormulaContext.WithLocalJsContext) {
                // TODO SLT: This works for $Api and $System, but maybe not for $User, which makes a certain amount of sense.
                // FIXME handle $Api.Enterprise_Server_URL_390.  See other binding observers as well.
                value = context.toJavascriptName(fieldName);
                guardJs = makeGuardForGlobalVariableField(value);
            } else {
                // Create a path using FK references.
                StringBuilder guard = new StringBuilder(); // This is the guard to guard against NPEs with trailing &&
                if (formula != null) {
                    if ("null".equals(formula.getJavascriptRaw())) {
                        // We need to get the source here instead of the formula and reparse because javascript may not have been generated.
                        RuntimeFormulaInfo formulaInfo = FormulaInfoFactory.create(context.getGlobalProperties().getFormulaType(), context, ((FormulaProvider)referencedFieldInfo).getSource());
                        formula = formulaInfo.getFormula();
                    }
                    value = formula.getJavascriptRaw();
                    String jsGuard = formula.getJavascriptGuard();
                    if (jsGuard != null) guard.append(jsGuard);
                } else {
                    // TODO SLT 206: FIXME Division is handled like picklists in formula land.  Awesome.
                    if (foci == null) {
                        return new JsValue("null", "false", true);  // ReportBucketFormulaFieldInfo...
                    }
                    value = getClientFieldName(referencedFieldInfo, guard, foci, true);
                }
                if (guard.length()>0) {
                    guardJs = guard.toString();
                }
            }

            if (formula == null) {
                // If this is not a formula field, wrap the data type we get on the client
                return wrapJSFieldForType(context, value, guardJs, referencedFieldInfo.getDataType());
            } else {
                // TODO SLT 206: FIXME Currency conversions might be broken here.
                return new JsValue(value, guardJs, true);
            }
        }
    }
    
    /**
     * Prevent NPE. e.g. $User.Manager.FirstName needs to make sure that
     * $User.Manager is defined.
     * @param value - the js field value, e.g. context.$User.Manager.FirstName
     * @return
     */
    private String makeGuardForGlobalVariableField(String value) {
        StringBuilder guard = new StringBuilder();

        List<String> path = new ArrayList<String>(Splitter.on('.').splitToList(value));

        if (path.size() > 2) {
            StringBuilder accumulatedPath = new StringBuilder();
            if ("context".equals(path.get(0))) {
                // Don't need to verify context variable exists.
                accumulatedPath.append(path.remove(0));
            }

            // Don't go all the way to the end. We are just blocking against NPE for
            // accessing that last field
            for (int i = 0; i < path.size() - 1; i++) {
                if (guard.length() > 0) {
                    guard.append("&&");
                }

                if (accumulatedPath.length() > 0) {
                    accumulatedPath.append(".");
                }

                accumulatedPath.append(path.get(i));
                guard.append(accumulatedPath);
            }

        }

        return guard.length() > 0 ? guard.toString() : null;
    }

    private JsValue wrapJSFieldForType(FormulaContext context, String value, String guardJs, FormulaDataType dataType) {
        boolean datesAreStrings = context.jsDatesAreStrings();
        if (dataType.isPercent()) {
            // Percent needs to be scaled by 100.0
            guardJs = guardJs != null ? guardJs + "&&" + value + "!=null" : value + "!=null";
            if (context.useHighPrecisionJs()) {
                // It'll fail when you call div.
                value = value + ".div(100)";
            } else {
                // It'll convert null to 0...
                value = value + "/100.0";
            }
        } else if (datesAreStrings && dataType.isDateOnly()) {
            value = FormulaCommandInfoImpl.jsNvl2(context, new JsValue(value, null, true),
                    String.format(FunctionDateValue.JS_FORMAT_TEMPLATE, value), "null");
        } else if (datesAreStrings && dataType.isTimeOnly()) {
            value = FormulaCommandInfoImpl.jsNvl2(context, new JsValue(value, null, true),
                    String.format(FunctionTimeValue.JS_FORMAT_TEMPLATE, value), "null");
        } else if (datesAreStrings && dataType.isDateTime()) {
            value = FormulaCommandInfoImpl.jsNvl2(context, new JsValue(value, null, true),
                    context.getJsEngMod() + ".parseDateTime(" + value + ")", "null");
        }

        return new JsValue(value,  guardJs, true);
    }

    private String getClientFieldName(ContextualFormulaFieldInfo referencedFieldInfo, StringBuilder guard, 
            FieldOrColumn foci, boolean includeContextPrefix) {
        List<FormulaFieldReferenceInfo> fieldPath = null;
        StringBuilder path = new StringBuilder();
        try {
            // W-3747109: the generated javascript should not have the "PersonContact." prefix
            // since ADS will return the myField__pc directly on the record.
            fieldPath = FormulaValidationHooks.get().getFieldPath(referencedFieldInfo, false);
        } catch (NullPointerException ex) { // NOPMD
            // TODO SLT 208: Figure out why this is happening with parent references during compile
        }
        if (fieldPath != null) {
            for (FormulaFieldReferenceInfo fk : fieldPath) {
                FormulaSchema.Field info = fk.getFieldOrColumn().getFieldInfo();
                if (guard != null) {
                    if (guard.length() > 0) {
                        guard.append("&&");
                    }
                    guard.append("context.record."+path+info.getForeignKeyRelationshipName());
                }
                path.append(info.getForeignKeyRelationshipName()).append(".");
            }
        }
        
        return FormulaValidationHooks.get().parseHook_generateJsFieldReference(foci, path, includeContextPrefix);

    }
    
    public static List<FormulaFieldReferenceInfo> getFieldPath(ContextualFormulaFieldInfo formulaFieldInfo) {
        return FormulaValidationHooks.get().getFieldPath(formulaFieldInfo, true);
    }
}
