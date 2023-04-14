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

/**
 * An enum class that contains different execution paths that executes for each testcase and also generates
 * corresponding gold file output.
 */
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

    /**
     * A method that executes a formula using provided input and returns the results
     *
     * @param formulaString formula code that needs to be executed
     * @param formulaDataType data type of the formula result
     * @param testInput values for each of the reference fields on which the formula depends
     * @param testEntity entity for which the formula needs to be evaluated
     * @param isNullAsNull formula engine property used for evaluation that tells how null inputs needs to be treated
     * @return a string representation of the output value or error generated
     */
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
            return "Error: " + e.getClass().getName();
        }
    }

    /**
     * A method that executes a sql for the provided formula and returns the results
     *
     * @param formulaString formula code that needs to be executed
     * @param formulaDataType data type of the formula result
     * @param testInput values for each of the reference fields on which the formula depends
     * @param testEntity entity for which the formula needs to be evaluated
     * @param dbTester the sql generated by formula engine will be executed using this database tester
     * @param isNullAsNull formula engine property used for evaluation that tells how null inputs needs to be treated
     * @return a string representation of the output value or error generated
     */
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

    /**
     * A method that executes the javascript for the provided formula and returns the results
     *
     * @param formulaString formula code that needs to be executed
     * @param formulaDataType data type of the formula result
     * @param testInput values for each of the reference fields on which the formula depends
     * @param testEntity entity for which the formula needs to be evaluated
     * @param isHighPrecision if set to true, this returns high precision output value from javascript execution
     * @param isNullAsNull formula engine property used for evaluation that tells how null inputs needs to be treated
     * @return a string representation of the output value or error generated
     */
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

    /**
     * A method that generates sql code for the given formula string
     *
     * @param formulaString formula code that needs to be executed
     * @param formulaDataType data type of the formula result
     * @param testEntity entity for which the formula needs to be evaluated
     * @param isNullAsNull formula engine property used for evaluation that tells how null inputs needs to be treated
     * @return a string representation of the sql code or error generated
     */
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

    /**
     * A method that generates javascript code for the given formula string
     *
     * @param formulaString formula code that needs to be executed
     * @param formulaDataType data type of the formula result
     * @param testEntity entity for which the formula needs to be evaluated
     * @param isHighPrecision if set to true, this returns high precision output value from javascript execution
     * @param isNullAsNull formula engine property used for evaluation that tells how null inputs needs to be treated
     * @return a string representation of the javascript code or error generated
     */
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
