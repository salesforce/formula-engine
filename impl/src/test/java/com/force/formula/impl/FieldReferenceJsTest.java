package com.force.formula.impl;

import java.util.HashMap;
import java.util.Map;

import com.force.formula.Formula;
import com.force.formula.FormulaDataType;
import com.force.formula.FormulaException;
import com.force.formula.InvalidFieldReferenceException;
import com.force.formula.MockFormulaType;
import com.force.formula.RuntimeFormulaInfo;
import com.force.formula.UnsupportedTypeException;
import com.force.formula.commands.FormulaJsTestUtils;
import com.google.common.collect.ImmutableMap;

/**
 * Test field references in javascript
 * @author stamm
 * @since 0.2
 */
public class FieldReferenceJsTest extends BaseFieldReferenceTest {

    public FieldReferenceJsTest(String name) {
        super(name);
    }
    
    Map<String, Object> getJsMap( BeanFormulaContext context) throws UnsupportedTypeException, InvalidFieldReferenceException {
        Map<String,Object> record = getBeanValues(context);
        return new HashMap<>(ImmutableMap.of("record", record));
    }
    
    @Override
    protected MockFormulaType getFormulaType() {
        return nullAsNull ? MockFormulaType.JAVASCRIPT_NULLASNULL : MockFormulaType.JAVASCRIPT;
    }

    @Override
    protected Object evaluate(String formulaSource, FormulaDataType columnType) throws FormulaException {
        BeanFormulaContext context = setupMockContext(columnType);
        RuntimeFormulaInfo formulaInfo = FormulaInfoFactory.create(getFormulaType(), context, formulaSource);
        Formula formula = formulaInfo.getFormula();
        return FormulaJsTestUtils.get().evaluateFormula(formula, columnType, context, getJsMap(context));
    }

}
