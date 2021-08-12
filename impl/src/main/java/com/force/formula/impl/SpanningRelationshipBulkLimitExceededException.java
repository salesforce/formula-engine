package com.force.formula.impl;

import java.util.Map;

import com.force.formula.FormulaException;
import com.force.formula.FormulaI18nUtils;
/*******
 * 
 * Exception thrown when the limit of spanning relationships was exceeded in reports
 *
 * @author mlopezamaro
 * @since 190
 */
public class SpanningRelationshipBulkLimitExceededException extends FormulaException{
    
    private static final long serialVersionUID = 1L;

	public enum SpanningBulkQuerySource{
        SOQL,
        REPORT;   
    }

    private int numberOfSpansUsed;
    private int maximumSpanningLimit;
    private Map<String, Integer> fieldsWithObjectReferences;
    private SpanningBulkQuerySource source;
    
    public SpanningRelationshipBulkLimitExceededException(int numberOfSpansUsed, int maximumSpanningLimit, Map<String, Integer> fieldsWithObjectReferences, SpanningBulkQuerySource source) {
        super("");
        this.numberOfSpansUsed = numberOfSpansUsed;
        this.fieldsWithObjectReferences = fieldsWithObjectReferences;
        this.maximumSpanningLimit = maximumSpanningLimit;
        this.source = source;
    }
    
    @Override
    public String getMessage() {
       return getMessageToDisplay(false);
    }
    
    public String getMessageForHtml(){
       return getMessageToDisplay(true);
    }
    
    private String getMessageToDisplay(boolean forHtml){
       // The message will display the fields that have spans in their formulas and the # of spans on each field.
        StringBuilder fieldsWithSpans = new StringBuilder();
        for(Map.Entry<String, Integer> fieldEntry : fieldsWithObjectReferences.entrySet()){
            fieldsWithSpans.append(String.format("%s%s : %d", forHtml? "<br>" : " " ,fieldEntry.getKey(), fieldEntry.getValue()));
        }
        if (source == SpanningBulkQuerySource.SOQL){
            return FormulaI18nUtils.getLocalizer().getLabel("Exception", "SpanningRelationshipLimitExceptionForSoql", maximumSpanningLimit, numberOfSpansUsed, fieldsWithSpans.toString());
        } else {
            return FormulaI18nUtils.getLocalizer().getLabel("ReportBuilder", "spanningLimitExceeded", maximumSpanningLimit, numberOfSpansUsed, fieldsWithSpans.toString());   
        }            
    }

}
