package com.force.formula.v2;

import com.force.formula.MockFormulaDataType;
import com.force.formula.commands.FormulaJsTestUtils;
import com.force.formula.util.FormulaTextUtil;
import com.force.formula.v2.data.FormulaFieldDefinition;
import com.google.common.collect.ImmutableSet;

import java.util.*;

/**
 * A utility class contains a set of static utility methods used across the test framework
 */
public class Utils {

    /**
     * Creates a date object from its string value
     *
     * @param dateString a string representation of a DateTime or TimeOnly or DateOnly
     * @param isDateOnly if true, then the time part of the Date object will be set to 0
     * @return a Date object created from the string input value
     */
    public static Date getDateObject(String dateString, Boolean isDateOnly) {
        Calendar myCal = Calendar.getInstance();
        myCal.clear();
        if (dateString == null || dateString.length() == 0) return myCal.getTime();

        StringTokenizer stDate = new StringTokenizer(dateString, ":");
        int year = stDate.hasMoreTokens() ? Integer.parseInt(stDate.nextToken()) : 2004;
        int month = stDate.hasMoreTokens() ? Integer.parseInt(stDate.nextToken()) - 1 : 0;
        int dayOfMonth = stDate.hasMoreTokens() ? Integer.parseInt(stDate.nextToken()) : 1;
        int hourOfDay = stDate.hasMoreTokens() ? Integer.parseInt(stDate.nextToken()) : 0;
        int minutes = stDate.hasMoreTokens() ? Integer.parseInt(stDate.nextToken()) : 0;
        int seconds = stDate.hasMoreTokens() ? Integer.parseInt(stDate.nextToken()) : 0;
        TimeZone timeZone = stDate.hasMoreTokens() ? TimeZone.getTimeZone(stDate.nextToken()) : null;
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        myCal.setTimeZone(timeZone);
        if (isDateOnly) {
            // remove the time part:
            myCal.set(year, month, dayOfMonth, 0, 0, 0);
        } else {
            myCal.set(year, month, dayOfMonth, hourOfDay, minutes, seconds);
        }
        return myCal.getTime();
    }

    /**
     * Returns the flattened ordered list of reference fields by removing all levels of nesting
     *
     * @param nestedFields a list of nested reference fields
     * @return a flattened ordered list of reference fields by removing all levels of nesting
     */
    public static List<FormulaFieldDefinition> flattenFieldList(List<FormulaFieldDefinition> nestedFields) {
        List<FormulaFieldDefinition> flattenedList = new LinkedList<>();
        if(nestedFields!=null && !nestedFields.isEmpty()){
            for (FormulaFieldDefinition field : nestedFields) {
                if (field.getReferenceFields()!=null && !field.getReferenceFields().isEmpty())
                    flattenedList.addAll(flattenFieldList(field.getReferenceFields()));
                flattenedList.add(field);
            }
        }
        return flattenedList;
    }

    /**
     * Gets the MockFormulaDataType from a given string dataType value
     *
     * @param dataType a string representing a dataType
     * @return a MockFormulaDataType for a given string dataType value
     */
    public static MockFormulaDataType getDataType(String dataType){
        if (ImmutableSet.of("email", "url", "phone", "textarea").contains(dataType)) return MockFormulaDataType.TEXT;
        if ("number".equals(dataType)) return MockFormulaDataType.DOUBLE;
        if ("currency".equals(dataType)) return MockFormulaDataType.CURRENCY;
        if ("percent".equals(dataType)) return MockFormulaDataType.PERCENT;
        MockFormulaDataType formulaDataType = MockFormulaDataType.fromCamelCaseName(dataType);
        if (formulaDataType == null) {
                throw new IllegalArgumentException("Given dataType is not supported: " + dataType);
            }
        return formulaDataType;
    }

    /**
     * Creates a javascript map from the given test input data
     *
     * @param testInput test input for the test case
     * @return a js map created from the test input to be used for javascript execution
     */
    public static Map<String,Object> createJSMapFromTestInput(Map<String, Object> testInput){
        Map<String,Object> record = testInput != null ? new HashMap<String,Object>(testInput) : new HashMap<String,Object>();
        Map<String,Object> jsMap = new HashMap<>();
        jsMap.put("record", FormulaJsTestUtils.get().makeJSMap(record));
        return jsMap;
    }

    /**
     * Generates an xml formatted output for the given formula sql
     *
     * @param rawSql raw sql part of the formula sql
     * @param sqlGuard sql guard part of the formula sql
     * @param nullAsNull value of the formula engine property nullAsNull
     * @return an xml formatted output for the given formula sql
     */
    public static String getSQLOutput(String rawSql, String sqlGuard, boolean nullAsNull){
        StringBuffer output = new StringBuffer();
        output.append("    <SqlOutput nullAsNull=\""+nullAsNull+"\">\n");
        output.append("       <Sql>");
        output.append(FormulaTextUtil.escapeToXml(rawSql));
        output.append("</Sql>\n");
        output.append("       <Guard>");
        output.append(FormulaTextUtil.escapeToXml(sqlGuard));
        output.append("</Guard>\n");
        output.append("    </SqlOutput>\n");
        return output.toString();
    }

    /**
     * Generates an xml formatted output for the given formula javascript
     *
     * @param jsCode javascript code
     * @param highPrec value for the javascript high precision property
     * @param nullAsNull value for the javascript nullAsNull property
     * @return an xml formatted output for the given formula javascript
     */
    public static String getJavascriptOutput(String jsCode, boolean highPrec, boolean nullAsNull){
        StringBuffer output = new StringBuffer();
        output.append("    <JsOutput highPrec=\""+highPrec+"\" nullAsNull=\""+nullAsNull+"\">");
        output.append(FormulaTextUtil.escapeToXml(jsCode));
        output.append("</JsOutput>\n");
        return output.toString();
    }

}
