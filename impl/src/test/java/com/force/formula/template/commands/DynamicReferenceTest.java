/**
 * 
 */
package com.force.formula.template.commands;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

import com.force.formula.*;
import com.force.formula.FormulaSchema.*;
import com.force.formula.FormulaTypeWithDomain.IdType;
import com.force.formula.commands.*;
import com.force.formula.impl.*;
import com.force.i18n.BaseLocalizer;

/**
 * Test dynamic references in templates
 * @see https://developer.salesforce.com/docs/atlas.en-us.pages.meta/pages/pages_dynamic_vf_sample_standard.htm
 * @author stamm
 */
public class DynamicReferenceTest extends BaseCustomizableParserTest {

	public DynamicReferenceTest(String name) {
		super(name);
	}

    
    @Override
    protected MockFormulaType getFormulaType() {
        return MockFormulaType.DYNAMIC;
    }

    private FormulaEngineHooks oldHooks = null;
    private FormulaFactory oldFactory = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        oldFactory = FormulaEngine.getFactory();
        FormulaEngine.setFactory(TEST_FACTORY);
        oldHooks = FormulaEngine.getHooks();
        FormulaEngine.setHooks(new FormulaValidationHooks() {
			@Override
			public BaseLocalizer getLocalizer() {
				return new MockLocalizerContext.MockLocalizer();
			}
			@Override
			public Type parseHook_getFieldReturnTypeOverride(FormulaDataType columnType, FormulaAST node,
					FormulaContext context, Entity[] domain) {
				if (columnType == MockFormulaDataType.LIST) {
					return List.class;
				} else if (columnType == MockFormulaDataType.MAP) {
					return Map.class;
				} else {
					return FormulaValidationHooks.super.parseHook_getFieldReturnTypeOverride(columnType, node, context, domain);
				}
			}
			@Override
			public Object getFieldReferenceValue(ContextualFormulaFieldInfo fieldInfo, FormulaDataType dataType,
					FormulaRuntimeContext context, FormulaFieldReference fieldReference, boolean useUnderlyingType)
					throws FormulaException {
				if (dataType == MockFormulaDataType.MAP) {
					return context.getObject(fieldReference);
				} else if (dataType == MockFormulaDataType.LIST) {
					return context.getObject(fieldReference);
				}
				return FormulaValidationHooks.super.getFieldReferenceValue(fieldInfo, dataType, context, fieldReference,
						useUnderlyingType);
			}
		});
    }

    @Override
    protected void tearDown() throws Exception {
        FormulaEngine.setHooks(oldHooks);
        FormulaEngine.setFactory(oldFactory);
    }
    
    static final FormulaFactoryImpl TEST_FACTORY;
    static {
        List<FormulaCommandInfo> types = new ArrayList<>(FormulaCommandTypeRegistryImpl.getDefaultCommands());
        // Test format and template parsing
        types.add(new FunctionFormat());
        types.add(new FunctionTemplate());
        types.add(new DynamicReference());
        types.add(new DynamicFieldSelector());
        types.add(new FieldReferenceCommandInfo());
        TEST_FACTORY = new FormulaFactoryImpl(new FormulaCommandTypeRegistryImpl(types));
    }


    /**
     * Verify various formats for Text formula type ( valid and invalid)
     * @priority Medium
     * @hierarchy Declarative App Builder.Formula Fields.Format
     * @userStory Annotation Debt
     *
     */
    public void testListReference() throws Exception {
        assertEquals("First", evaluateString("List[0]"));
        assertEquals("Second", evaluateString("List[1]"));
        try {
        	evaluateString("List[2]");
        } catch (FormulaEvaluationException x) {
        	assertEquals("Subscript value 2 not valid.  Must be between 0 and 1", x.getMessage());
        }
    }
   
    /**
     * Verify various formats for Text formula type ( valid and invalid)
     * @priority Medium
     * @hierarchy Declarative App Builder.Formula Fields.Format
     * @userStory Annotation Debt
     *
     */
    public void testMapReference() throws Exception {
        assertEquals(new BigDecimal("1"), evaluateBigDecimal("Map['Foo']"));
        try {
        	evaluateString("Map['Baz']");
        } catch (FormulaEvaluationException x) {
        	assertEquals("Map key Baz not found in map.", x.getMessage());
        }
    }
   
    
	static class MockIdFormulaType implements IdType {
		private final Entity[] domain;
		public MockIdFormulaType(Entity[] domain) {
			this.domain = domain;
		}

		public MockIdFormulaType(FieldOrColumn field, boolean isSobjectRow) {
			this(((Field)field).getFormulaForeignKeyDomains());
		}

		@Override
		public boolean isApplicable(Entity[] targetDomains) {
			return false;
		}

		@Override
		public Type[] getActualTypeArguments() {
			return null;
		}

		@Override
		public Type getRawType() {
			return MockIdFormulaType.class;
		}

		@Override
		public Type getOwnerType() {
			return null;
		}

		@Override
		public Entity[] getDomains() {
			return domain;
		}

		@Override
		public IdType addToDomain(Entity[] additionalDomains) {
			return null;
		}
	}
}
