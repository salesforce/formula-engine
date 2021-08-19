package com.force.formula.util;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaSchema.ApiElement;
import com.google.common.base.Ascii;

/**
 * Composite adapter to provide access to field values where the data source is an EntityObject.
 * 
 * Use this to create the "$Foo" style of global properties by extending this class and calling addContextProvider
 * in the constructor
 *
 * @author dchasman
 * @since 144
 */
public class BaseCompositeFormulaContext implements FormulaRuntimeContext {
    @FunctionalInterface
    public interface FormulaRuntimeContextProvider {
        FormulaRuntimeContext get(FormulaContext outerContext) throws FormulaException, SQLException;
    }
    
    // Use this to prevent any funny business with formula properties
    public static class ConstantGlobalFormulaProperties extends GlobalFormulaProperties {
        public ConstantGlobalFormulaProperties(FormulaTypeSpec topLevelType) {
            super(topLevelType);
        }

        @Override
        public void pushFormulaType(FormulaTypeSpec type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FormulaTypeSpec popFormulaType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setShouldIgnoreFls(boolean ignore) {
            throw new UnsupportedOperationException();
        }
    }
    

    public BaseCompositeFormulaContext(FormulaRuntimeContext defaultContext, FormulaTypeSpec topLevelFormulaType) {
        this.defaultContext = defaultContext;
        this.globalFormulaProperties = new GlobalFormulaProperties(topLevelFormulaType);
        this.additionalContextProviders = new HashMap<String, FormulaRuntimeContextProvider>();
        this.additionalContexts = new HashMap<String, FormulaRuntimeContext>();
        this.allowSelfReference = true;
    }

    /**
     * Provide a way for a child formula context to filter out formulas simply without breaking inheritance
     * @param name the uppercase name of the formula.
     * @return <tt>false</tt> if the context shouldn't be available.
     */
    protected boolean filterFormulaContext(String contextName) {
        return true;
    }


    @Override
    public BigDecimal getNumber(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaRuntimeContext context = getContextThrowRuntimeOnly(fieldName);
        return context.getNumber(getFieldName(fieldName));
    }

    @Override
    public String getString(String fieldName, boolean useNative) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaRuntimeContext context = getContextThrowRuntimeOnly(fieldName);
        return context.getString(getFieldName(fieldName), useNative);
    }

    @Override
    public String getMaskedString(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaRuntimeContext context = getContextThrowRuntimeOnly(fieldName);
        return context.getMaskedString(getFieldName(fieldName));
    }

    @Override
    public FormulaDateTime getDateTime(String fieldname) throws InvalidFieldReferenceException,
            UnsupportedTypeException {
        FormulaRuntimeContext context = getContextThrowRuntimeOnly(fieldname);
        return context.getDateTime(getFieldName(fieldname));
    }

    @Override
    public Date getDate(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaRuntimeContext context = getContextThrowRuntimeOnly(fieldName);
        return context.getDate(getFieldName(fieldName));
    }

    @Override
    public FormulaTime getTime(String fieldname) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaRuntimeContext context = getContextThrowRuntimeOnly(fieldname);
        return context.getTime(getFieldName(fieldname));
    }

    @Override
    public Boolean getBoolean(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaRuntimeContext context = getContextThrowRuntimeOnly(fieldName);
        return context.getBoolean(getFieldName(fieldName));
    }

    @Override
    public FormulaCurrencyData getCurrency(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaRuntimeContext context = getContextThrowRuntimeOnly(fieldName);
        return context.getCurrency(getFieldName(fieldName));
    }

    @Override
    public FormulaGeolocation getLocation(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaRuntimeContext context = getContextThrowRuntimeOnly(fieldName);
        return context.getLocation(getFieldName(fieldName));
    }

    @Override
    public String getCurrencyIsoCode() throws FormulaException
    {
        return getDefaultContext().getCurrencyIsoCode();
    }

    @Override
    public String getCurrencyIsoCode(String fieldName) throws FormulaException
    {
        return getContextThrowRuntimeOnly(fieldName).getCurrencyIsoCode();
    }

    @Override
    public Object getObject(String fieldName) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaRuntimeContext context = getContextThrowRuntimeOnly(fieldName);
        return context.getObject(getFieldName(fieldName));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name) {
        return (T)getDefaultContext().getProperty(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        FormulaContext context = getDefaultContext();
        if (context != null) context.setProperty(name, value);
    }

    @Override
    public FormulaRuntimeContext getOriginalValuesContext() throws FormulaException {
        return getDefaultContext().getOriginalValuesContext();
    }

    @Override
    public boolean convertIdto18Digits() {
        return false;
    }

    @Override
    public boolean isNew() {
        return getDefaultContext().isNew();
    }

    @Override
    public boolean isClone() {
        return getDefaultContext().isClone();
    }

    @Override
    public FormulaReturnType getFormulaReturnType() {
        return getDefaultContext().getFormulaReturnType();
    }

    @Override
    public ContextualFormulaFieldInfo lookup(String devName, boolean isDynamicRefBase) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaRuntimeContext context = getContextFromReference(devName);
        //context.setProperty(FormulaContext.FILTER_ENCRYPTED_FIELDS, new Boolean(true));  // Unused in SFDC since 204
        String fieldName = getFieldName(devName);
        return context.lookup(fieldName, isDynamicRefBase);
    }

    @Override
    public String toDurableName(String name) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaRuntimeContext context = getContextFromReference(name);

        if (context == null) { throw new InvalidFieldReferenceException(name, "Unable to get the reference's context"); }

        String fieldName = getFieldName(name);

        if (context == getDefaultContext()) {
            return context.toDurableName(fieldName);
        } else {
            return String.format("%s.%s", getContextName(name), context.toDurableName(fieldName));
        }
    }
    
    /**
     * @return the name to use when referencing the field in javascript.
     * @throws InvalidFieldReferenceException 
     */
    @Override
    public String toJavascriptName(String name) throws InvalidFieldReferenceException {
        FormulaRuntimeContext context = getContextFromReference(name);

        if (context == null) { throw new InvalidFieldReferenceException(name, "Unable to get the reference's context"); }

        String fieldName = getFieldName(name);

        if (context == getDefaultContext()) {
            return context.toJavascriptName(fieldName);
        } else {
            return String.format("%s.%s", context.getJavascriptReference(), context.toJavascriptName(fieldName));
        }
    }

    @Override
    public String fromDurableName(Connection conn, String reference) throws InvalidFieldReferenceException, UnsupportedTypeException {
        FormulaRuntimeContext context = getContextFromReference(reference);

        int firstDot = reference.indexOf(".");
        String name = reference.substring(firstDot + 1);
        String decodedReference = context.fromDurableName(conn, name);

        return (context == getDefaultContext()) ? decodedReference : String.format("%s.%s", context.getName(),
                decodedReference);
    }

    // Provided a default implementation that fills in null for conn.
    @Override
    public String fromDurableName(String reference) throws InvalidFieldReferenceException, UnsupportedTypeException {
        return fromDurableName(null, reference);
    }

    @Override
    public boolean isFunctionSupported(FormulaCommandType function) {
        return getDefaultContext().isFunctionSupported(function);
    }

    @Override
    public boolean isGlobalVariableSupportedOffline(String variableType) {
        return getGlobalVariablesSupportedOffline().contains(Ascii.toUpperCase(variableType));
    }

    protected Collection<String> getGlobalVariablesSupportedOffline() {
        return Collections.emptySet();
    }

    @Override
    public FormulaContext[] getAdditionalContexts() throws InvalidFieldReferenceException {
        // Insure that the lazy loaded additional context cache is fully loaded
        for (String providerName : additionalContextProviders.keySet()) {
            if (filterFormulaContext(providerName)) {
                getAdditionalContext(providerName);
            }
        }

        return ((additionalContexts.size() > 0)) ? additionalContexts.values().toArray(new FormulaContext[additionalContexts.size()]) : null;
    }

    @Override
    public FormulaContext getParentContext() {
        return null;
    }

    @Override
    public DisplayField[] getDisplayFields(FormulaSchema.Entity entityInfo) {
        return allowSelfReference ? getDefaultContext().getDisplayFields(entityInfo) : null;
    }

    @Override
    public String getName() {
        return getDefaultContext().getName();
    }

    @Override
    public String getFullName(boolean useDurableName, FormulaContext relativeToContext) {
        return getDefaultContext().getFullName(useDurableName, relativeToContext);
    }

    @Override
    public GlobalFormulaProperties getGlobalProperties(){
        return globalFormulaProperties;
    }

    protected String getFieldName(String devName) {
        int firstDot = devName.indexOf(".");
        return (firstDot >= 0) ? devName.substring(firstDot + 1) : devName;
    }

    protected void addContextProvider(String name, FormulaRuntimeContextProvider provider) {
        if (!(name.charAt(0) == '$'
                && globalFormulaProperties.getFormulaType().getDefaultProperties().getGenerateJavascript()
                && !isGlobalVariableSupportedOffline(name))) {
            additionalContextProviders.put(Ascii.toUpperCase(name), provider);
        }
    }

    protected void allowSelfReference(boolean allowSelfReference) {
        this.allowSelfReference = allowSelfReference;
    }

    public FormulaRuntimeContext getDefaultContext() {
        return this.defaultContext;
    }

    public void setDefaultContext(FormulaRuntimeContext context) {
        this.defaultContext = context;
    }

    private String getContextName(String devName) throws InvalidFieldReferenceException {
        if (devName == null) {
            throw new InvalidFieldReferenceException(null, "Invalid reference");
        }
        int i = devName.indexOf('.');
        return i > -1 ? devName.substring(0, i) : null;
    }

    private FormulaRuntimeContext getContextThrowRuntimeOnly(String devName) {
        try {
            return getContextFromReference(devName);
        } catch (InvalidFieldReferenceException x) {
            throw new FormulaEvaluationException(x);
        }
    }

    protected FormulaRuntimeContext getContextFromReference(String fieldName) throws InvalidFieldReferenceException {
        String contextName = getContextName(fieldName);

        if (contextName == null && !allowSelfReference) {
            throw new InvalidFieldReferenceException(fieldName, String.format("Self references are not permitted in this context"));
        }

        FormulaRuntimeContext context = getAdditionalContext(contextName);
        if (context == null) {
            throw new InvalidFieldReferenceException(fieldName, String.format("Unable to find matching FormulaContext for '%s'",
                    (contextName != null) ? contextName : "Default"));
        }

        return context;
    }
    
    /**
     * Returns the final context that is referenced by the field.
     * @param fieldName
     * @throws InvalidFieldReferenceException 
     */
    public FormulaRuntimeContext getFinalContext(String fieldName) throws InvalidFieldReferenceException {
        FormulaRuntimeContext context = this;
        while (context instanceof BaseCompositeFormulaContext) {
            if (isGlobalContextFieldReference(fieldName) && fieldName.indexOf('.') < 0) {
                // need to handle if the context is global
                context = ((BaseCompositeFormulaContext)context).getAdditionalContext(fieldName);
            } else {
                context = ((BaseCompositeFormulaContext)context).getContextFromReference(fieldName);
            }
            fieldName = getFieldName(fieldName);
        }
        return context;
    }

    public FormulaRuntimeContext getAdditionalContext(String contextName) throws InvalidFieldReferenceException {
        if (contextName == null) { return getDefaultContext(); }

        FormulaRuntimeContext context;
        String contextKey = Ascii.toUpperCase(contextName);
        if (!additionalContexts.containsKey(contextKey)) {
            if (!filterFormulaContext(contextKey)) {
                return null; // You're not allowed to see it.
            }
            // Attempt to load the additional context
            FormulaRuntimeContextProvider provider = additionalContextProviders.get(contextKey);
            try {
                context = (provider != null) ? provider.get(this) : null;
            } catch (Exception x) {
                throw new FormulaEvaluationException(x);
            }

            additionalContexts.put(contextKey, context);
        } else {
            context = additionalContexts.get(contextKey);
        }

        return context;
    }

    private boolean allowSelfReference;

    //Sometimes have to defer construction of the default context
    protected FormulaRuntimeContext defaultContext;
    private final GlobalFormulaProperties globalFormulaProperties;
    private final Map<String, FormulaRuntimeContextProvider> additionalContextProviders;
    private final Map<String, FormulaRuntimeContext> additionalContexts;

    /**
     * Returns whether this field reference is for a constant formula context like $System or $User.
     * @param fieldReference
     * @return
     */
    public static boolean isGlobalContextFieldReference(String fieldReference) {
        assert fieldReference != null : "Do not pass in a null field reference";
        return !fieldReference.isEmpty() && fieldReference.charAt(0) == '$';
    }
    
    @Override
    public Map<String, String> getMetaInformation() {
        Map<String, String> info = FormulaRuntimeContext.super.getMetaInformation();
        
        info.put("CompositeFormulaContext_TopLevelFormulaType", 
                getGlobalProperties().getTopLevelFormulaType().getDisplay());
        info.put("CompositeFormulaContext_DefaultContext", 
                "{" + FormulaTextUtil.prettyPrintMap(getDefaultContext().getMetaInformation()) + "}");
        
        return info;
    }

    @Override
    public DisplayField getDisplayField(ApiElement aei) {
        throw new UnsupportedOperationException();
    }
}
