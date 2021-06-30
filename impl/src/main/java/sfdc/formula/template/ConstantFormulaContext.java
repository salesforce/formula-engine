package sfdc.formula.template;

import sfdc.formula.*;
import sfdc.formula.impl.NullFormulaContext;

public class ConstantFormulaContext extends NullFormulaContext {
    private final FormulaContext outerContext;
    
    public ConstantFormulaContext(FormulaContext context) {
        super(context);
        this.outerContext = context;
    }
    
    @Override
    public String getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FormulaReturnType getFormulaReturnType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toDurableName(String name) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String fromDurableName(String reference) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean isFunctionSupported(FormulaCommandType function) {
        return outerContext.isFunctionSupported(function);
    }

    @Override
    public boolean isNew() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isClone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FormulaRuntimeContext getOriginalValuesContext() throws FormulaException {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name) {
        return (T)outerContext.getProperty(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        outerContext.setProperty(name, value);
    }

    @Override
    public boolean isSqlPostgresStyle() {
        return outerContext.isSqlPostgresStyle();
    } 

}

