package com.force.formula.v2;

import com.force.formula.MockFormulaDataType;
import com.force.formula.v2.data.FormulaFieldDefinition;
import com.google.common.collect.ImmutableSet;

import java.util.*;

public class Utils {

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

    public static List<FormulaFieldDefinition> flattenFieldList(List<FormulaFieldDefinition> nestedFields) {
        List<FormulaFieldDefinition> flattenedList = new LinkedList<>();
        for (FormulaFieldDefinition field : nestedFields) {
            if (field.getReferenceFields()!=null && !field.getReferenceFields().isEmpty())
                flattenedList.addAll(flattenFieldList(field.getReferenceFields()));
            flattenedList.add(field);
        }
        return flattenedList;
    }

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

}
