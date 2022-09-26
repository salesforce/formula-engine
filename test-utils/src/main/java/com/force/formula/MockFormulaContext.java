package com.force.formula;

import com.force.formula.util.NullFormulaContext;

/**
 * 
 * @author stamm
 *
 */
public class MockFormulaContext extends NullFormulaContext {
    private final GlobalFormulaProperties globalFormulaProperties;
    private final FormulaDataType returnType;

    public MockFormulaContext(FormulaTypeSpec formulaType, FormulaDataType returnType){
        super((FormulaContext) null);
        this.returnType = returnType;
        globalFormulaProperties = new GlobalFormulaProperties(formulaType);
    }

    @Override
    public boolean isNew() {
        return getProperty("test.ISNEW") != null;
    }

    @Override
    public boolean isClone() {
        return getProperty("test.ISCLONE") != null;
    }

    @Override
    public GlobalFormulaProperties getGlobalProperties() {
        return globalFormulaProperties;
    }

	@Override
	public boolean isFunctionSupported(FormulaCommandType function) {
		return true;
	}

	@Override
    public String fromDurableName(String reference) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public ContextualFormulaFieldInfo lookup(String fieldName, boolean isDynamicRefBase)
            throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new InvalidFieldReferenceException(fieldName, "UNSUPPORTED");
    }

    @Override
    public FormulaReturnType getFormulaReturnType() {
        return new FormulaReturnType() {
            @Override
            public FormulaDataType getDataType() {
                // for now we only return formulas that evaluate to numbers
                return returnType;
            }
        };
    }

    @Override
    public boolean isFunctionSupportedOffline(FormulaCommandType command, int numberOfArguments) {
        return true;
    }

    @Override
    public String toDurableName(String name) throws InvalidFieldReferenceException, UnsupportedTypeException {
        throw new UnsupportedOperationException();
    }
}