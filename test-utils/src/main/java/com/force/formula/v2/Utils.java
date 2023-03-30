package com.force.formula.v2;

import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

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

}
