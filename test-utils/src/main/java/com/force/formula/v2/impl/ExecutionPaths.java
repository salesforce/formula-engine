package com.force.formula.v2.impl;

import com.force.formula.*;
import com.force.formula.commands.FormulaJsTestUtils;
import com.force.formula.impl.MapFormulaContext;
import com.force.formula.util.FormulaI18nUtils;
import com.force.formula.v2.IFormulaExecutor;
import com.force.formula.v2.Utils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum ExecutionPaths implements IFormulaExecutor<MapFormulaContext.MapEntity, DbTester> {

    FORMULA("formula"){

        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            try{
                FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                        new MockFormulaContext(MockFormulaType.DEFAULT, formulaDataType),
                        testEntity,
                        MockFormulaType.DEFAULT,
                        testInput);

                RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(MockFormulaType.DEFAULT,
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

    /*    @Override
        public String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType, MapFormulaContext.MapEntity testEntity) {
            try{
                FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                        new MockFormulaContext(MockFormulaType.DEFAULT, formulaDataType),
                        testEntity,
                        MockFormulaType.DEFAULT,
                        null);

                RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(MockFormulaType.DEFAULT,
                        formulaRuntimeContext,
                        formulaString);

                FormulaWithSql formula = (FormulaWithSql) formulaInfo.getFormula();
                return Utils.getSQLOutput(formula, false);
            }catch(Throwable e){
                return "Error: " + e.getMessage();
            }
        } */
    },

    JAVASCRIPT("javascript"){

        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            try {
                FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                        new MockFormulaContext(MockFormulaType.JAVASCRIPT, formulaDataType),
                        testEntity,
                        MockFormulaType.JAVASCRIPT,
                        testInput);

                formulaRuntimeContext.setProperty(FormulaContext.HIGHPRECISION_JS, true);

                RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(MockFormulaType.JAVASCRIPT,
                        formulaRuntimeContext,
                        formulaString);

                Formula formula = formulaInfo.getFormula();

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

     /*   @Override
        public String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType,MapFormulaContext.MapEntity testEntity) {
            try{
                FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                        new MockFormulaContext(MockFormulaType.JAVASCRIPT, formulaDataType),
                        testEntity,
                        MockFormulaType.JAVASCRIPT,
                        null);

                formulaRuntimeContext.setProperty(FormulaContext.HIGHPRECISION_JS, true);

                RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(MockFormulaType.JAVASCRIPT,
                        formulaRuntimeContext,
                        formulaString);

                Formula formula = formulaInfo.getFormula();

                return Utils.getJavascriptOutput(formula, true, false);
            } catch (Throwable e) {
                return "Error: " + e.getMessage();
            }
        } */
    },

    SQL("sql"){

        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            try {
                FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                        new MockFormulaContext(MockFormulaType.DEFAULT, formulaDataType),
                        testEntity,
                        MockFormulaType.DEFAULT,
                        testInput);

                String result = dbTester.evaluateSql("SQLTest", formulaRuntimeContext, testInput, formulaString, false);
                return result == null ? "null" : result;
            } catch (Throwable e) {
                String message = (dbTester != null ? dbTester.getSqlExceptionMessage(e) : e.getMessage());
                return "Error: " + message;
            }
        }

        //SQL Execution Path executes the given formula on a particular database
       /* @Override
        public String generateGoldFileOutput(String formulaString, FormulaDataType formulaDataType, MapFormulaContext.MapEntity testEntity) {
            return null;
        } */
    },

    FORMULA_NULL_AS_NULL("formulaNullAsNull"){

        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            try{
                FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                        new MockFormulaContext(MockFormulaType.JAVASCRIPT_NULLASNULL, formulaDataType),
                        testEntity,
                        MockFormulaType.JAVASCRIPT_NULLASNULL,
                        testInput);

                RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(MockFormulaType.JAVASCRIPT_NULLASNULL,
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
    },

    JAVASCRIPT_NULL_AS_NULL("javascriptNullAsNull"){

        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            try {
                FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                        new MockFormulaContext(MockFormulaType.JAVASCRIPT_NULLASNULL, formulaDataType),
                        testEntity,
                        MockFormulaType.JAVASCRIPT_NULLASNULL,
                        testInput);

                RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(MockFormulaType.JAVASCRIPT_NULLASNULL,
                        formulaRuntimeContext,
                        formulaString);

                Formula formula = formulaInfo.getFormula();

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
    },

    SQL_NULL_AS_NULL("sqlNullAsNull"){

        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            try {
                FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                        new MockFormulaContext(MockFormulaType.JAVASCRIPT_NULLASNULL, formulaDataType),
                        testEntity,
                        MockFormulaType.JAVASCRIPT_NULLASNULL,
                        testInput);

                String result = dbTester.evaluateSql("SQLTest", formulaRuntimeContext, testInput, formulaString, true);
                return result == null ? "null" : result;
            } catch (Throwable e) {
                String message = (dbTester != null ? dbTester.getSqlExceptionMessage(e) : e.getMessage());
                return "Error: " + message;
            }
        }
    },

    JAVASCRIPT_LOW_PRECISION("javascriptLp"){
        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            try {

                FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                        new MockFormulaContext(MockFormulaType.JAVASCRIPT, formulaDataType),
                        testEntity,
                        MockFormulaType.JAVASCRIPT,
                        testInput);

                RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(MockFormulaType.JAVASCRIPT,
                        formulaRuntimeContext,
                        formulaString);

                Formula formula = formulaInfo.getFormula();

                formulaRuntimeContext.setProperty(FormulaContext.HIGHPRECISION_JS, false);

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
    },

    JAVASCRIPT_LOW_PRECISION_NULL_AS_NULL("javascriptLpNullAsNull"){

        public String execute(String formulaString, FormulaDataType formulaDataType, Map<String, Object> testInput, MapFormulaContext.MapEntity testEntity, DbTester dbTester) {
            try {
                FormulaRuntimeContext formulaRuntimeContext = new MapFormulaContext(
                        new MockFormulaContext(MockFormulaType.JAVASCRIPT_NULLASNULL, formulaDataType),
                        testEntity,
                        MockFormulaType.JAVASCRIPT_NULLASNULL,
                        testInput);

                RuntimeFormulaInfo formulaInfo = FormulaEngine.getFactory().create(MockFormulaType.JAVASCRIPT_NULLASNULL,
                        formulaRuntimeContext,
                        formulaString);

                Formula formula = formulaInfo.getFormula();

                formulaRuntimeContext.setProperty(FormulaContext.HIGHPRECISION_JS, false);

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

}
