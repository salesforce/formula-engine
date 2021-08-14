package com.force.formula.impl;

import java.util.Set;

import com.force.formula.*;

/**
 * Default implementation of FormulaFactory.  You may probably to override all these methods to 
 * customize the functionality.
 *
 * @author stamm
 */
public final class FormulaFactoryImpl implements FormulaFactory {

	private final FormulaCommandTypeRegistry typeRegistry;
	
	public FormulaFactoryImpl() {
	    this(new FormulaCommandTypeRegistryImpl());
	}
	
	public FormulaFactoryImpl(FormulaCommandTypeRegistry registry) {
	    this.typeRegistry = registry;
	}

	@Override
	public FormulaCommandTypeRegistry getTypeRegistry() {
		return typeRegistry;
	}

	@Override
	public RuntimeFormulaInfo create(FormulaTypeSpec type, FormulaContext context, String source)
			throws FormulaException {
        return new DefaultFormulaInfoImpl(context, source, type.getDefaultProperties());
	}

	@Override
	public RuntimeFormulaInfo create(FormulaTypeSpec type, FormulaContext context, String source,
			boolean existingFormula, boolean forceDisabled) throws FormulaException {
        return new DefaultFormulaInfoImpl(context, source, type.getDefaultProperties(), existingFormula, forceDisabled,false);
	}

	@Override
	public RuntimeFormulaInfo create(FormulaTypeSpec type, FormulaContext context, String source,
			boolean existingFormula, boolean forceDisabled, boolean isCreateOrEdit) throws FormulaException {
        return new DefaultFormulaInfoImpl(context, source, type.getDefaultProperties(), existingFormula, forceDisabled,
                isCreateOrEdit);
	}

	@Override
	public RuntimeFormulaInfo create(FormulaContext context, String source, FormulaProperties properties)
			throws FormulaException {
        return new DefaultFormulaInfoImpl(context, source, properties);
	}

	@Override
	public String[] getReferencesFromEncodedSource(String source) {
        return FormulaUtils.getReferencesFromEncodedSource(source);
	}

	@Override
	public String decode(NameDetokenizer nameTokenizer, String source, FormulaProperties properties)
			throws FormulaException {
        return FormulaUtils.decode(nameTokenizer, source, properties);
	}

	@Override
	public Set<String> getReferences(String source, FormulaProperties properties) throws FormulaException {
        FormulaAST ast = FormulaUtils.parse(source, properties);
        return BaseFormulaInfoImpl.getReferencedNames(ast, properties);
	}
}
