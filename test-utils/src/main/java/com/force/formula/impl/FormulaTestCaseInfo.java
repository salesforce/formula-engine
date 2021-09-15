/*
 * Created on Mar 2
 * Copyright, 1999-2005,  salesforce.com,
 * All Rights Reserved Company Confidential
 */
package com.force.formula.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.force.formula.impl.BaseFormulaGenericTests.FormulaTestRunnable;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

/**
 * @author syendluri, adimayuga
 * @since 0.1.0 Member Variables: testCaseFieldInfo: referenceFields
 *        listOfFieldsUsedForRunnables
 */

public class FormulaTestCaseInfo {

    // Whether to compare the results of the different evaluation methods.
    public static enum CompareType {Number, Text, Approximate, None, Date, DateTime, Default }
    // TODO: Make this an extensible enum to  be generic
    
    public static interface EvaluationContext {
    	
    }
    public static enum DefaultEvaluationContext implements EvaluationContext {Formula, Template}

    public FormulaTestCaseInfo(FormulaTestUtils utils, String tcName, String testLabels, String accuracyIssue, FieldDefinitionInfo tcFormulaFieldInfo,
                               List<FieldDefinitionInfo> referenceFields, String owner, String compareType, String evalContexts,  String compareTemplate,
                               String whyIgnoreSql, 
                               boolean multipleResultTypes) {
        this.testUtils = utils;
        this.testCaseName = tcName;
        this.testLabels = testLabels.length() > 0 ? Splitter.on(',').splitToList(testLabels) : Collections.emptySet();
        this.accuracyIssue = AccuracyIssue.fromLabel(accuracyIssue);
        this.testCaseFieldInfo = tcFormulaFieldInfo;
        this.owner = owner;
        this.referenceFields = referenceFields != null ? referenceFields : new ArrayList<FieldDefinitionInfo>();
        this.createList = flattenFieldList(this.referenceFields);
        this.compare = "number".equals(compareType) ? CompareType.Number : "approximate".equals(compareType) ?
                CompareType.Approximate : "text".equals(compareType) ? CompareType.Text : "none".equals(compareType)  ?
                CompareType.None :  "date".equals(compareType)  ?
                CompareType.Date :  "datetime".equals(compareType)  ?
                CompareType.DateTime : CompareType.Default;

        if (this.compare == CompareType.Default) {
            // Set based on field info type
            if (testCaseFieldInfo.isNumeric()) {
                this.compare = CompareType.Number;
            } else if (testCaseFieldInfo.getReturnType().isDateOnly()) {
                this.compare = CompareType.Date;
            } else if (testCaseFieldInfo.getReturnType().isDateTime()) {
                this.compare = CompareType.DateTime;
            } else
                this.compare = CompareType.Text;
        }
        this.whyIgnoreSql = whyIgnoreSql.length() > 0 ? whyIgnoreSql : null;
        this.evalContexts = parseContext(evalContexts);
        this.compareContexts = parseContext(compareTemplate);
        this.multipleResultTypes = multipleResultTypes;
    }

    protected Set<EvaluationContext> parseContext(String contexts) {
        Set<EvaluationContext> ctx = new HashSet<EvaluationContext>(3);
        if ("none".equals(contexts)) {
            return ctx;
        }
        if ("formula".equals(contexts)) {
           ctx.add(DefaultEvaluationContext.Formula);
        }
        if ("template".equals(contexts)) {
            ctx.add(DefaultEvaluationContext.Template);
        }
        if (ctx.size() == 0) {
            ctx.add(DefaultEvaluationContext.Formula);
            ctx.add(DefaultEvaluationContext.Template);
        }
        return ctx;
    }

    public String getDataFileName() {
        return "".equals(this.dataFileName) ? null : this.dataFileName;
    }

    public List<FieldDefinitionInfo> getReferenceFields() {
        return this.referenceFields;
    }

    public FieldDefinitionInfo getTestCaseFieldInfo() {
        return this.testCaseFieldInfo;
    }

    public String getName() {
        return this.testCaseName;
    }

    public Collection<String> getTestLabels() { 
        return this.testLabels;
    }
    
    public AccuracyIssue getAccuracyIssue() {
        return this.accuracyIssue;
    }
    
    // Setters
    public void setDataFileName(String dataFileName) {
        this.dataFileName = dataFileName;
    }

    public void setSwapArgs(boolean swapArgs) {
        this.swapArgs = swapArgs;
    }

    public void setSwapTypes(boolean swapTypes) {
        this.swapTypes = swapTypes;
    }

    public boolean isSwapArgs() {
        return this.swapArgs;
    }

    public boolean isSwapTypes() {
        return this.swapTypes;
    }
    
    public boolean ignoreSql() {
    	return this.whyIgnoreSql != null;
    }
    
    public String getWhyIgnoreSql() {
    	return this.whyIgnoreSql;
    }
    
    public String getOwner() {
        return this.owner;
    }

    private List<FieldDefinitionInfo> flattenFieldList(List<FieldDefinitionInfo> nestedFields) {
        List<FieldDefinitionInfo> flattenedList = new LinkedList<FieldDefinitionInfo>();
        for (FieldDefinitionInfo field : nestedFields) {
            if (field.hasChildren())
                flattenedList.addAll(flattenFieldList(field.getRefFields()));
            flattenedList.add(field);
        }
        return flattenedList;
    }

    /*
     * This returns only non-formula reference fields
     */
    public List<FieldDefinitionInfo> getFieldsToCreate() throws Exception {
        if (this.testRunnables == null)
            generateRunnables();
        return createList;
    }

    public List<FieldDefinitionInfo> getNonFormulaFieldRefList() {
        List<FieldDefinitionInfo> result = new LinkedList<FieldDefinitionInfo>();
        for (FieldDefinitionInfo fdi : flattenFieldList(referenceFields)) {
            if (!fdi.isFormula())
                result.add(fdi);
        }
        return result;
    }

    /**
     * Returns a new runnable instance by swapping the args in the formula. ie.
     * if formula = (customnumber1__c + customnumber2__c) It will return a
     * runnable with following formula. (customnumber2__c +
     * customnumber1__c)
     */
    private FormulaTestRunnable generateArgSwappedTC() {
        FormulaTestRunnable runnable = new FormulaTestRunnable(this.testCaseFieldInfo.clone(), this.testCaseName);

        String formula = this.testCaseFieldInfo.getFormula();
        FieldDefinitionInfo firstField = this.referenceFields.get(0);
        FieldDefinitionInfo secondField = this.referenceFields.get(1);

        formula = formula.replaceFirst(secondField.getDevName(), firstField.getDevName());
        formula = formula.replaceFirst(firstField.getDevName(), secondField.getDevName());
        runnable.addReferenceField(secondField);
        runnable.addReferenceField(firstField);
        runnable.getTcFieldInfo().setFormula(formula);
        runnable.setIsSwapped(true);

        return runnable;
    }

    /**
     * This method will create instances of FormulaTestRunnable with swapping
     * the data types of FormulaField, and reference fields. So, if TestCase is
     * number=number+number, then following testcases will be generated to run:
     * number = number + number currency = number + number percent = number +
     * number currency = number + currency number = number + currency percent =
     * number + currency number = number + percent percent = number + percent
     * currency = number + percent currency = percent + currency percent =
     * percent + currency number = percent + currency number = percent + percent
     * currency = percent + percent percent = percent + percent number =
     * currency + currency percent = currency + currency currency = currency +
     * currency
     *
     * Each combination will result in instance of FormulaTestRunnable.
     */
    private List<FormulaTestRunnable> generateTypeSwappedTCs(int swappableFieldCount) throws CloneNotSupportedException {

        List<FormulaTestRunnable> resultList = new LinkedList<FormulaTestRunnable>();
        Map<String, FieldDefinitionInfo> listOfFieldsUsedForRunnables = new HashMap<String, FieldDefinitionInfo>();

        for (List<String> perm : FormulaTestUtils.getSwapSets(swappableFieldCount)) {

            // If we've been told not to generate multiple result types (visualforce usage) then
            // filter out everything that isn't for the correct result type.
            if (!multipleResultTypes) {
                String resultType = perm.get(perm.size()-1);
                if (!resultType.equals(testCaseFieldInfo.getReturnType().getName())) {
                    continue;
                }
            }

            // Clone the Original TestCase Field Definition to modify the data
            // type and formula to store in a new runnable.
            FieldDefinitionInfo newTcField = this.testCaseFieldInfo.clone();

            FormulaTestRunnable runnable = new FormulaTestRunnable(newTcField, this.testCaseName);
            // Now, let's set runnable with changed TC datatype, formula and
            // also changed referencedField Definitions,
            // to match the current swap set in loop.
            for (int i = perm.size() - 1; i >= 0; i--) {
                if (i == perm.size() - 1) {
                    newTcField.setReturnType(testUtils.getDataType(perm.get(i)));
                    continue;
                }
                String newType = perm.get(i);
                FieldDefinitionInfo fieldInfo = this.referenceFields.get(i).clone();
                String fieldName = fieldInfo.getDevName();
                fieldInfo.setDevName(fieldName.replaceAll(fieldInfo.getReturnType().getName(), newType));
                fieldInfo.setLabelName(fieldInfo.getDevName());
                fieldInfo.setReturnType(testUtils.getDataType(newType));
                newTcField.setFormula(newTcField.getFormula().replaceAll(fieldName, fieldInfo.getDevName()));
                runnable.addReferenceField(fieldInfo);
                listOfFieldsUsedForRunnables.put(fieldInfo.getDevName(), fieldInfo);

            }

            runnable.setTcFieldInfo(newTcField);
            Collections.reverse(runnable.getFieldNames());
            resultList.add(runnable);

        }

        this.createList.addAll(0, listOfFieldsUsedForRunnables.values());

        return resultList;
    }

    /***************************************************************************
     * Generates a list of runnable FormualTestRunnables based on swap, etc.
     *
     * @return List<FormulaTestRunnable>
     */
    public List<FormulaTestRunnable> getRunnables(FormulaTestUtils testUtils) throws Exception {
        if (this.testRunnables == null)
            generateRunnables();
        return testRunnables;
    }

    private void generateRunnables() throws Exception {
        this.testRunnables = new LinkedList<FormulaTestRunnable>();
        FormulaTestRunnable runnable = new FormulaTestRunnable(testCaseFieldInfo, testCaseName,
                                                               getNonFormulaFieldRefList());
        runnable.setIsSwapped(false);

        this.testRunnables.add(runnable);

        if (isSwappable()) {
            if (swapArgs)
                this.testRunnables.add(generateArgSwappedTC());
            if (swapTypes)
                this.testRunnables.addAll(generateTypeSwappedTCs(this.referenceFields.size()));
        }
    }

    /**
     * Check to see if this testcase can be tested with arg swapping or type
     * swapping.
     */

    private boolean isSwappable() {
        if (referenceFields.size() > 2)
            return false;
        else {
            for (FieldDefinitionInfo fdi : referenceFields)
                if (fdi.isFormula())
                    return false;
        }

        return true;
    }

    public boolean isNegative() {
        return isNegative;
    }

    public void setNegative(boolean isNegative, String errorCode, String errorMsg) {
        this.isNegative = isNegative;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public CompareType getCompareType() {
        return compare;
    }

    public boolean compareWithContext(EvaluationContext c) {
        return compareContexts.contains(c);
    }

    public boolean evalForContext(EvaluationContext c) {
        return evalContexts.contains(c);
    }

    public String getEncoding() {
        return this.encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public static enum AccuracyIssue {
        Normal("normal"), 
        BadDecimal("badDecimal"),        // Bug in Decimal
        NeedHighPrecision("needHp"),  // Need high precision
        FloorCeiling("floorCeiling")  // Small precision errors effect floor & ceiling.  ignore them.
        ;
        private final String label;
        AccuracyIssue(String label) {
            this.label = label;
        }
        static final Map<String,AccuracyIssue> BY_LABEL = ImmutableMap.copyOf(Arrays.asList(values()).stream().collect(Collectors.toMap(a->a.label, b->b)));
        public static AccuracyIssue fromLabel(String label) {
            if (label == null || label.length() == 0) return Normal;
            AccuracyIssue issue = BY_LABEL.get(label);
            assert issue != null : "Illegal accuracy issue: " + label;
            return issue;
        }
        public boolean ignoreHighPrecision() {
            return this == BadDecimal || this == FloorCeiling;
        }
        public boolean ignoreLowPrecision() {
            return this == NeedHighPrecision || this == FloorCeiling;
        }
    }

    
    FormulaTestUtils testUtils;
    private final Collection<String> testLabels;
    private final AccuracyIssue accuracyIssue;
    
    private final String owner;
    private final String testCaseName;
    private String dataFileName;
    private boolean swapTypes = false;
    private boolean swapArgs = false;
    private boolean isNegative = false;
    private String whyIgnoreSql;
    private String errorCode = null;
    private String errorMsg = null;
    private String encoding = null;
    // this field holds the formula field info which needs to be tested.
    private final FieldDefinitionInfo testCaseFieldInfo;
    // list of references fields used by the formula in the testecase.
    private List<FieldDefinitionInfo> referenceFields;
    //
    private List<FieldDefinitionInfo> createList;
    private List<FormulaTestRunnable> testRunnables = null;

    private CompareType compare;
    private Set<EvaluationContext> compareContexts;
    private Set<EvaluationContext> evalContexts;
    private boolean multipleResultTypes;
}
