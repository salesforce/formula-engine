package com.force.formula.v2.impl;

import com.force.formula.*;
import com.force.formula.commands.FormulaJsTestUtils;
import com.force.formula.impl.MapFormulaContext;
import com.force.formula.sql.FormulaWithSql;
import com.force.formula.util.FormulaI18nUtils;
import com.force.formula.v2.GoldFileOutputGenerator;
import com.force.formula.v2.IFormulaExecutor;
import com.force.formula.v2.Utils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ExecutionPaths implements IFormulaExecutor<MapFormulaContext.MapEntity, DbTester>, GoldFileOutputGenerator<MapFormulaContext.MapEntity> {

    FORMULA("formula"){
        boolean isNullAsNull = false;

        @Override
        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            return executeForFormula(formulaString, formulaDataType, testInput, testEntity, isNullAsNull);
        }

        @Override
        public String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType, MapFormulaContext.MapEntity testEntity) {
            return "";
        }
    },

    FORMULA_NULL_AS_NULL("formulaNullAsNull"){
        boolean isNullAsNull = true;

        @Override
        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            return executeForFormula(formulaString, formulaDataType, testInput, testEntity, isNullAsNull);
        }

        @Override
        public String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType, MapFormulaContext.MapEntity testEntity) {
            return "";
        }
    },

    SQL("sql"){
        boolean isNullAsNull = false;

        @Override
        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            return executeForSQL(formulaString, formulaDataType, testInput, testEntity, dbTester, isNullAsNull);
        }

        //Generates SQL string
        @Override
        public String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType, MapFormulaContext.MapEntity testEntity) {
            return generateSQLForGoldFile(formulaString, formulaDataType, testEntity, isNullAsNull);
        }
    },

    SQL_NULL_AS_NULL("sqlNullAsNull"){
        boolean isNullAsNull = true;

        @Override
        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            return executeForSQL(formulaString, formulaDataType, testInput, testEntity, dbTester, isNullAsNull);
        }

        @Override
        public String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType, MapFormulaContext.MapEntity testEntity) {
            return generateSQLForGoldFile(formulaString, formulaDataType, testEntity, isNullAsNull);
        }
    },

    JAVASCRIPT("javascript"){
        boolean isHighPrecision = true;
        boolean isNullAsNull = false;

        @Override
        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            return executeForJavascript(formulaString, formulaDataType, testInput, testEntity, isHighPrecision, isNullAsNull);
        }

        @Override
        public String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType,MapFormulaContext.MapEntity testEntity) {
            return generateJavascriptForGoldFile(formulaString, formulaDataType, testEntity, isHighPrecision, isNullAsNull);
        }
    },

    JAVASCRIPT_NULL_AS_NULL("javascriptNullAsNull"){
        boolean isHighPrecision = true;
        boolean isNullAsNull = true;

        @Override
        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            return executeForJavascript(formulaString, formulaDataType, testInput, testEntity, isHighPrecision, isNullAsNull);
        }

        @Override
        public String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType,MapFormulaContext.MapEntity testEntity) {
            return generateJavascriptForGoldFile(formulaString, formulaDataType, testEntity, isHighPrecision, isNullAsNull);
        }
    },

    JAVASCRIPT_LOW_PRECISION("javascriptLp"){
        boolean isHighPrecision = false;
        boolean isNullAsNull = false;

        @Override
        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            return executeForJavascript(formulaString, formulaDataType, testInput, testEntity, isHighPrecision, isNullAsNull);
        }

        @Override
        public String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType,MapFormulaContext.MapEntity testEntity) {
            return generateJavascriptForGoldFile(formulaString, formulaDataType, testEntity, isHighPrecision, isNullAsNull);
        }
    },

    JAVASCRIPT_LOW_PRECISION_NULL_AS_NULL("javascriptLpNullAsNull"){
        boolean isHighPrecision = false;
        boolean isNullAsNull = true;

        @Override
        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            return executeForJavascript(formulaString, formulaDataType, testInput, testEntity, isHighPrecision, isNullAsNull);
        }

        @Override
        public String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType,MapFormulaContext.MapEntity testEntity) {
            return generateJavascriptForGoldFile(formulaString, formulaDataType, testEntity, isHighPrecision, isNullAsNull);
        }
    }

    ;

    public final String name;

    ExecutionPaths(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ExecutionPaths get(String name){
        return EXECUTION_PATHS_MAP.get(name);
    }

    private static final Map<String, ExecutionPaths> EXECUTION_PATHS_MAP;

    static {
        Map<String, ExecutionPaths> map = new HashMap<>();
        for (ExecutionPaths executionPath : ExecutionPaths.values()) {
            map.put(executionPath.getName(), executionPath);
        }
        EXECUTION_PATHS_MAP = Collections.unmodifiableMap(map);
    }


    //Utility methods to remove code duplication

    protected String executeForFormula(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput,
                             MapFormulaContext.MapEntity testEntity, boolean isNullAsNull){
        try{
            MockFormulaType mockFormulaType = (isNullAsNull) ? MockFormulaType.NULLASNULL : MockFormulaType.DEFAULT;

            FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                    new MockFormulaContext(mockFormulaType, formulaDataType),
                    testEntity,
                    mockFormulaType,
                    testInput);

            RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(mockFormulaType,
                    formulaRuntimeContext,
                    formulaString);

            Formula formula = formulaInfo.getFormula();

            Object result = formula.evaluate(formulaRuntimeContext);

            String outputValue = null;
            if (result != null) {
                outputValue = (result instanceof BigDecimal) ? ((BigDecimal)result).toPlainString()
                        : result.toString();
            } else if (formulaDataType.isBoolean()) {
                // Oracle/Formula null values in formulas are resolved to null since they are always guarded with
                // NVLs.  In java BooleanField doesn't support tristate, so it resolves to false.
                outputValue = Boolean.FALSE.toString();
            }
            return outputValue == null ? "null" : outputValue;
        }catch(Throwable e){
            return "Error: " + e.getMessage();
        }
    }

    protected String executeForSQL(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput,
                         MapFormulaContext.MapEntity testEntity, DbTester dbTester, boolean isNullAsNull){
        try {
            MockFormulaType mockFormulaType = (isNullAsNull) ? MockFormulaType.NULLASNULL : MockFormulaType.DEFAULT;

            FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                    new MockFormulaContext(mockFormulaType, formulaDataType),
                    testEntity,
                    mockFormulaType,
                    testInput);

            String result = dbTester.evaluateSql("SQLTest", formulaRuntimeContext, testInput, formulaString, isNullAsNull);
            return result == null ? "null" : result;
        } catch (Throwable e) {
            String message = (dbTester != null ? dbTester.getSqlExceptionMessage(e) : e.getMessage());
            return "Error: " + message;
        }
    }

    protected String executeForJavascript(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput,
                                          MapFormulaContext.MapEntity testEntity, boolean isHighPrecision, boolean isNullAsNull){
        try {
            MockFormulaType mockFormulaType = (isNullAsNull) ? MockFormulaType.JAVASCRIPT_NULLASNULL : MockFormulaType.JAVASCRIPT;

            FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                    new MockFormulaContext(mockFormulaType, formulaDataType),
                    testEntity,
                    mockFormulaType,
                    testInput);

            RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(mockFormulaType,
                    formulaRuntimeContext,
                    formulaString);

            Formula formula = formulaInfo.getFormula();

            formulaRuntimeContext.setProperty(FormulaContext.HIGHPRECISION_JS, isHighPrecision);

            Object result = FormulaJsTestUtils.get().evaluateFormula(formula,
                    formulaRuntimeContext.getFormulaReturnType().getDataType(),
                    formulaRuntimeContext,
                    Utils.createJSMapFromTestInput(testInput));

            result = FormulaI18nUtils.formatResult(formulaRuntimeContext, formulaRuntimeContext.getFormulaReturnType(), result);

            return result == null ? "null" : String.valueOf(result);
        } catch (Throwable e) {
            return "Error: " + e.getMessage();
        }
    }

    protected String generateSQLForGoldFile(String formulaString, FormulaDataType formulaDataType,
                                  MapFormulaContext.MapEntity testEntity, boolean isNullAsNull){
        try{
            MockFormulaType mockFormulaType = (isNullAsNull) ? MockFormulaType.NULLASNULL : MockFormulaType.DEFAULT;
            FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                    new MockFormulaContext(mockFormulaType, formulaDataType),
                    testEntity,
                    mockFormulaType,
                    null);

            RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(mockFormulaType,
                    formulaRuntimeContext,
                    formulaString);

            FormulaWithSql formula = (FormulaWithSql) formulaInfo.getFormula();
            return Utils.getSQLOutput(formula.getSQLRaw(), formula.getGuard(), isNullAsNull);
        }catch(Throwable e){
            return "Error: " + e.getMessage();
        }
    }

    protected String generateJavascriptForGoldFile(String formulaString, FormulaDataType formulaDataType,
                                         MapFormulaContext.MapEntity testEntity, boolean isHighPrecision, boolean isNullAsNull){
        try{
            MockFormulaType mockFormulaType = (isNullAsNull) ? MockFormulaType.JAVASCRIPT_NULLASNULL : MockFormulaType.JAVASCRIPT;

            FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                    new MockFormulaContext(mockFormulaType, formulaDataType),
                    testEntity,
                    mockFormulaType,
                    null);

            formulaRuntimeContext.setProperty(FormulaContext.HIGHPRECISION_JS, isHighPrecision);

            RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(mockFormulaType,
                    formulaRuntimeContext,
                    formulaString);

            Formula formula = formulaInfo.getFormula();

            return Utils.getJavascriptOutput(formula.toJavascript(), isHighPrecision, isNullAsNull);
        } catch (Throwable e) {
            return "Error: " + e.getMessage();
        }
    }

}
