/**
 *
 */
package com.force.formula.impl.sql;

import java.lang.reflect.Type;
import java.util.Date;

import com.force.formula.FormulaDateTime;
import com.force.formula.sql.SQLPair;

/**
 * Implementation of FormulaSqlHooks for Salesforce DataCloud Hyper DB.
 * Extends FormulaPostgreSQLHooks and overrides methods that use PostgreSQL-specific
 * functions or syntax not supported by Hyper DB.
 *
 * @author adatta
 * @since 0.9.11
 */
public interface FormulaDataCloudHooks extends FormulaPostgreSQLHooks {

    // -------------------------------------------------------
    // #1: Replace pg_catalog.make_interval() and ::timestamp(0) with
    //     INTERVAL arithmetic and DATE_TRUNC('second', ...) to match
    //     PostgreSQL's ::timestamp(0) truncation behavior.
    //     Hyper DB does not support timestamp with precision specifier.
    // -------------------------------------------------------

    @Override
    default String sqlAddDaysToDate(Object lhsValue, Type lhsDataType, Object rhsValue, Type rhsDataType, boolean isAddition) {
        // Round the total seconds before creating the interval to match PostgreSQL's
        // ::timestamp(0) rounding behavior. DATE_TRUNC truncates (loses 0.9s),
        // while ::timestamp(0) rounds (0.5s+ rounds up). ROUND(...) before the
        // interval creation gives identical results.
        if (lhsDataType == Date.class || lhsDataType == FormulaDateTime.class) {
            return String.format("(%s%s(INTERVAL '1 second'*ROUND(%s*86400.0)))::timestamp",
                    lhsValue, isAddition ? "+" : "-", rhsValue);
        } else {
            return String.format("((INTERVAL '1 second'*ROUND(%s*86400.0))%s%s)::timestamp",
                    lhsValue, isAddition ? "+" : "-", rhsValue);
        }
    }

    @Override
    default String sqlSubtractTwoTimestamps(boolean inSeconds, Type dateType) {
        // Round EPOCH values to whole seconds before subtraction to match PostgreSQL's
        // ::timestamp(0) rounding behavior (::timestamp(0) rounds, not truncates).
        return inSeconds
                ? "(ROUND(EXTRACT(EPOCH FROM %s))-ROUND(EXTRACT(EPOCH FROM %s)))::numeric"
                : "((ROUND(EXTRACT(EPOCH FROM %s))-ROUND(EXTRACT(EPOCH FROM %s)))::numeric/86400)";
    }

    // -------------------------------------------------------
    // #2: Math function precision alignment with PostgreSQL.
    //     Hyper DB limits NUMERIC precision to 38, and its math functions
    //     return slightly different last digits than PostgreSQL.
    // -------------------------------------------------------

    @Override
    default String sqlExponent(String argument) {
        return "EXP(" + argument + "::numeric(38,18))";
    }

    @Override
    default String sqlTrigConvert(String argument) {
        return argument + "::numeric(38,18)";
    }

    // -------------------------------------------------------
    // #3: CAST(text AS NUMERIC) truncates decimals in Hyper.
    //     Use explicit precision to preserve decimal places.
    // -------------------------------------------------------

    @Override
    default String sqlToNumber() {
        return "CAST(%s AS DECIMAL(38,18))";
    }

    // -------------------------------------------------------
    // #3b: Hyper's NUMERIC arithmetic can produce values like 1.0000000000000001
    //      or 0.9999999999999999 where PostgreSQL produces exact 1.0.
    //      Use explicit ROUND instead of type cast to guarantee rounding
    //      (Hyper's ::numeric(p,s) may truncate instead of round).
    // -------------------------------------------------------

    @Override
    default String sqlCeilFloorArg(String argument) {
        return argument + "::numeric(38,18)";
    }

    @Override
    default int getExternalPrecision() {
        // Hyper DB's NUMERIC precision limit is 38 total digits.
        // PostgreSQL's default of 33 may exceed Hyper's capacity when combined
        // with large integer parts. Use 18 to match the scale of numeric(38,18).
        return 18;
    }

    // -------------------------------------------------------
    // #4: Fix ::timestamp(0) in sqlToCharTimestamp, sqlAddMonths, sqlLastDayOfMonth
    //     Hyper DB does not support timestamp with precision specifier.
    //     Use DATE_TRUNC('second', ...) to truncate to whole seconds.
    // -------------------------------------------------------

    @Override
    default String sqlToCharTimestamp() {
        // Use DATE_TRUNC here since TO_CHAR only formats, no rounding needed
        return "TO_CHAR(DATE_TRUNC('second', (%s)::timestamp), 'YYYY-MM-DD HH24:MI:SS')";
    }

    @Override
    default String sqlAddMonths(String dateArg, Type dateArgType, String numMonths) {
        StringBuffer sb = new StringBuffer();
        sb.append(" (CASE");
        sb.append(" WHEN extract(day FROM (date_trunc('month', %s) + interval '1 month -1 day')::timestamp)::numeric = ");
        sb.append("      extract(day FROM (date_trunc('day', %s)))::numeric ");
        sb.append(" THEN '1 day'");
        sb.append(" ELSE '0 day'");
        sb.append(" END )::interval ");

        String dayAddition = String.format(sb.toString(), dateArg, dateArg);
        // Wrap result with DATE_TRUNC to match PostgreSQL's ::timestamp(0) precision
        return String.format("DATE_TRUNC('second', (%s + " + dayAddition
                + " + ('1 month'::interval*TRUNC(%s))) - " + dayAddition + ")::timestamp", dateArg, numMonths);
    }

    @Override
    default String sqlLastDayOfMonth() {
        return "EXTRACT(DAY FROM (date_trunc('month',%s)+ interval '1 month -1 day')::timestamp)::numeric";
    }

    // -------------------------------------------------------
    // #4b: DATE function - Hyper DB throws errors on invalid date inputs
    //      (month=0, day=0, month=13, etc.) even inside CASE WHEN guards
    //      (eager evaluation). Clamp both min AND max to ensure TO_DATE
    //      always receives a valid date. The guard expression still catches
    //      out-of-range values and returns NULL; this clamping just prevents
    //      TO_DATE from erroring during guard evaluation.
    // -------------------------------------------------------

    @Override
    default String sqlConstructDate(String yearSql, String monthSql, String daySql) {
        return "TO_DATE((" + yearSql + ") || '-' || LEAST(GREATEST((" + monthSql + ")::int, 1), 12)"
                + " || '-' || LEAST(GREATEST((" + daySql + ")::int, 1), 31), 'YYYY-MM-DD')";
    }

    @Override
    default String sqlDateFromYearAndMonth(String yearValue, String monthValue) {
        return "TO_DATE((" + yearValue + ") || '-' || LEAST(GREATEST((" + monthValue + ")::int, 1), 12), 'YYYY-MM')";
    }

    // -------------------------------------------------------
    // #5: Time formatting - Hyper doesn't support TO_CHAR with interval or
    //     nested TO_CHAR(TO_TIMESTAMP(...)) for time formatting.
    //     Use EXTRACT-based arithmetic and LPAD instead.
    //     Also, EXTRACT(EPOCH FROM timestamp) returns seconds since Unix epoch,
    //     not seconds since midnight. Subtract DATE_TRUNC('day', ...) to get
    //     seconds since midnight, matching PostgreSQL's TO_CHAR(ts, 'SSSS.MS').
    // -------------------------------------------------------

    @Override
    default String sqlToCharTime() {
        // Hyper doesn't support nested TO_CHAR(TO_TIMESTAMP(...)) for time formatting.
        // Build HH:MM:SS.mmm manually from milliseconds-since-midnight using arithmetic.
        // Parenthesize (%1$s)::int so the cast applies to the full expression, not just
        // the last literal in the substituted SQL (::int binds tighter than arithmetic).
        return "LPAD(TRUNC(%1$s/3600000)::int::text,2,'0') || ':' || "
                + "LPAD((TRUNC(%1$s/60000)::int %% 60)::text,2,'0') || ':' || "
                + "LPAD((TRUNC(%1$s/1000)::int %% 60)::text,2,'0') || '.' || "
                + "LPAD(((%1$s)::int %% 1000)::text,3,'0')";
    }

    @Override
    default String sqlIntervalToDurationString(String intervalArg, boolean includeDays, String daysIsParam) {
        // Hyper doesn't support TO_CHAR(interval, 'HH24:MI:SS').
        // Use EXTRACT to get total seconds from the interval, then format manually.
        // NOTE: Hyper's LPAD truncates strings longer than the pad width, so use
        // GREATEST(2, LENGTH(...)) for hours which can exceed 2 digits.
        String totalSecs = "EXTRACT(EPOCH FROM " + intervalArg + ")::bigint";
        String hhRaw = "(" + totalSecs + "/3600)::text";
        String hh = "LPAD(" + hhRaw + ",GREATEST(2,LENGTH(" + hhRaw + ")),'0')";
        String mm = "LPAD(((" + totalSecs + "%3600)/60)::text,2,'0')";
        String ss = "LPAD((" + totalSecs + "%60)::text,2,'0')";
        String hhmmss = hh + "||':'||" + mm + "||':'||" + ss;
        if (daysIsParam != null) {
            String days = "(" + totalSecs + "/86400)";
            String hhInDay = "LPAD(((" + totalSecs + "%86400)/3600)::text,2,'0')";
            String withDays = days + "||':'||" + hhInDay + "||':'||" + mm + "||':'||" + ss;
            return "CASE WHEN " + daysIsParam + " THEN " + withDays + " ELSE " + hhmmss + " END";
        } else if (includeDays) {
            String days = "(" + totalSecs + "/86400)";
            String hhInDay = "LPAD(((" + totalSecs + "%86400)/3600)::text,2,'0')";
            return days + "||':'||" + hhInDay + "||':'||" + mm + "||':'||" + ss;
        } else {
            return hhmmss;
        }
    }

    @Override
    default String sqlParseTime(String stringExpr) {
        // Compute seconds since midnight by subtracting the date portion.
        // EXTRACT(EPOCH FROM (ts - DATE_TRUNC('day', ts))) gives the time-of-day
        // in seconds, matching PostgreSQL's TO_CHAR(ts, 'SSSS.MS') semantics.
        // Use ROUND(..., 3) to preserve millisecond precision that Hyper's
        // floating-point EPOCH extraction may lose (e.g., 55.666 → 55.665999...).
        String ts = "TO_TIMESTAMP(" + stringExpr + ", '" + sqlHMSAndMsecs() + "')";
        String secsSinceMidnight = "ROUND(EXTRACT(EPOCH FROM (" + ts + " - DATE_TRUNC('day', " + ts + "))), 3)";
        return String.format(sqlToNumber(), secsSinceMidnight) + " * 1000";
    }

    @Override
    default String sqlExtractTimeFromDateTime(String dateTimeExpr) {
        // Hyper may not support TO_CHAR(timestamp, 'SSSS') for seconds-in-day.
        // Use EXTRACT(EPOCH FROM (ts - DATE_TRUNC('day', ts))) instead.
        // ROUND preserves millisecond precision lost in floating-point EPOCH extraction.
        String secsSinceMidnight = "ROUND(EXTRACT(EPOCH FROM (" + dateTimeExpr
                + " - DATE_TRUNC('day', " + dateTimeExpr + "))), 3)";
        return String.format(sqlToNumber(), secsSinceMidnight) + " * 1000";
    }

    // -------------------------------------------------------
    // #6: JSON functions are not supported in Hyper DB.
    //     These would need test exclusions rather than hooks.
    //     No hook override needed - tests that use json_extract_path_text
    //     or #>> operator will fail and should be filtered out.
    // -------------------------------------------------------

    // -------------------------------------------------------
    // #7: POWER function — Hyper's LOG function may error on LOG(0) in the
    //     guard expression even inside CASE WHEN (eager evaluation).
    //     Use LN(ABS(x))/LN(10) instead of LOG(10,ABS(x)) for overflow
    //     detection, and wrap the guard with a CASE to short-circuit on 0.
    // -------------------------------------------------------

    @Override
    default SQLPair getPowerSql(String[] args, String[] guards) {
        String sql = "POWER(" + args[0] + ", " + args[1] + ")";
        // Hyper evaluates LOG(10,ABS(x)) eagerly even in guard expressions,
        // which errors on x=0. Use a CASE to short-circuit the overflow check.
        String guard = SQLPair.generateGuard(guards, "TRUNC(" + args[1] + ")<>" + args[1]
                + " OR(" + args[0] + "=0 AND " + args[1] + "<0)"
                + " OR(" + args[0] + "<>0 AND (CASE WHEN ABS(" + args[0] + ")=0 THEN 0 ELSE LN(ABS(" + args[0] + "))/LN(10) END)*" + args[1] + ">38)");
        return new SQLPair(sql, guard);
    }

    // -------------------------------------------------------
    // #8: Currency formatting — Hyper DB does not support G (grouping)
    //     and D (decimal) format specifiers in TO_CHAR. Replace with
    //     comma-based format masks (9,999,990.00) instead.
    // -------------------------------------------------------

    @Override
    default StringBuilder getCurrencyMask(int scale) {
        // Hyper doesn't support G/D locale-aware format specifiers.
        // Use comma for grouping and period for decimal point.
        StringBuilder mask = new StringBuilder(40).append("'FM9,999,999,999,999,999,990");
        if (scale > 0) {
            mask.append('.');
            for (int i = 0; i < scale; i++) mask.append('0');
        }
        mask.append('\'');
        return mask;
    }

    // -------------------------------------------------------
    // #9: INITCAP — Hyper may not support COLLATE "en_US" with INITCAP.
    //     Use plain INITCAP without collation specifier.
    // -------------------------------------------------------

    @Override
    default String sqlInitCap(boolean hasLocaleOverride) {
        return "INITCAP(%s)";
    }
}