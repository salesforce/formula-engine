/**
 * Copyright (C) 2019-2022 salesforce.com, inc.
 * Formula Engine Client Side Library
 * This is a non "module" version to provide standard implementations.
 */

const FormulaEngine = {};
/*
 * Utility functions for the Client Formula Engine
 * Equivalent to Oracle's NVL function.
 * @param {Object} value - the value to check
 * @param {Object} ifNull - returned if value is null or undefined
 * @returns {Object} value if value is not null or undefined, otherwise ifNull
 */
FormulaEngine.nvl = function (value, ifNull) {
    return value !== null && value !== undefined ? value : ifNull;
};
/*
 * Utility functions for the Client Formula Engine - Equivalent of AuraUtil.js
 * Null Or Empty - Return value if value is not null and not an empty string, ifNull otherwise.
 * @param {Object} value - the value to check
 * @param {Object} ifNull - returned if value is null or undefined or empty
 * @returns {Object} value if value is not null or undefined, otherwise ifNull
 */
FormulaEngine.noe = function (value, ifNull) {
    if (value === undefined || value === null || value === '') {
        return ifNull;
    }
    return value;
};
/**
 * Checks all tests and returns true if any are null or undefined.
 * @param {List} tests - the list of values to check
 * @returns {boolean} true if any value in tests is null or undefined
 */
FormulaEngine.anl = function (tests) {
    return Array.isArray(tests) && tests.some(val => val === null || val === undefined);
};

/**
 * Return the value as a string, while leaving null and undefined alone.
 * @param {Object} value - the object to stringify
 * @returns {String} a stringified version of value
 */
FormulaEngine.tostr = function (value) {
    if (value === undefined || value === null || value === '') {
        return value;
    }
    return String(value);
};

/**
 * Chrome, Node & GraalJS have lenient date parsing for 'YYYY-MM-DD HH:MM:SS'.  Others require ecmascript ISO formatting only (with T & Z).
 * try both
 * @param {String} value - a string value that is similar to an ISO date format.
 * @returns {Date} - a date object that may be invalid
 */
FormulaEngine.parseDateTime = function (value) {
    if (value === undefined || value === null || value === '') {
        return null;
    }
    const d = new Date(value.trim().replace(' ', 'T') + 'Z');
    return isNaN(d) ? new Date(value.trim() + ' GMT') : d;
};

/**
 * ADDMONTHS()
 * Note, Javascript setMonths has strange behavior since it'll make January 30th + 1 month may be March.
 * And last day needs to remain last day to match oracle.
 * So if it's the last day of the month, we add one to the day and add the month, then remove the day.
 * Otherwise we use the "set Date to 0 to be last day of previous month" javascript behavior
 * so Jan 30 + 1 month will be Feb 28 in a non-leap year
 * @param {Date} d - a date to add months to
 * @param {Number} months - the number of months to add to the date
 * @returns {Date} - the date with the numver of months added
 */
FormulaEngine.addmonths = function (d, months) {
    if (d == null || d == null) {
        return null;
    }
    if (!months) {
        return d;
    }
    const lastDay = d.getUTCDay() === (new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + 1, 0))).getUTCDay();
    const adj = new Date(d.getTime() + (lastDay ? 86400000 : 0));
    adj.setUTCMonth(adj.getUTCMonth() + Math.trunc(months));
    if (lastDay) {
        return new Date(adj.getTime() - 86400000);
    }
    if (d.getUTCDate() !== adj.getUTCDate()) {
        adj.setUTCDate(0);
    }
    return adj;
};

/**
 * ISOWEEK(Date)
 * @param {Date} d - the date
 * @param {NUmber} - the ISO Week of the date
 */
FormulaEngine.isoweek = function (date) {
    if (!date) {
        return date;
    }
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNum = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return Math.ceil((((d - yearStart) / 86400000) + 1) / 7);
};

/**
 * ISOYEAR(Date)
 * @param {Date} date - the date
 * @param {NUmber} - the ISO YEAR of the date
 */
FormulaEngine.isoyear = function (date) {
    if (!date) {
        return date;
    }
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNum = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return yearStart.getUTCFullYear();
};

/**
 * DAYOFYEAR(Date)
 * @param {Date} d - the date
 * @param {Number} - the day of the year
 */
FormulaEngine.dayofyear = function (date) {
    if (!date) {
        return date;
    }
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return Math.ceil((1 + (d - yearStart)) / 86400000);
};


/**
 * INITCAP(String) - Capitalize all the "first" characters in words
 * @param {String} str - the string
 * @returns {String} - the string with the first characters of words converted to uppercase
 */
FormulaEngine.initcap = function (str) {
    if (!str) {
        return str;
    }
    // Init cap... Normalize and use unicode to match postgres/oracle behavior
    return str.toLowerCase().replace(/(?:^|[^\p{Ll}\p{Lm}\p{Lu}\p{N}])[\p{Ll}]/gu, m => m.toUpperCase());
};

/**
 * LPAD(String,Number[,String]) - Pad the left side of the string with the pad repeated.
 * @param {String} str - the string to pad on the left
 * @param {Number} len - length to pad
 * @param {String} pad - the optional string to pad.
 */
FormulaEngine.lpad = function (str, len, pad) {
    return !str || !len || len < 1 ? null : (len <= str.length ? str.substring(0, len) : ((Array(256).join(pad) + '').substring(0, len - str.length)) + str);
};

/**
 * FORMATDURATION() - Format the duration as defined by the unix epoch as hh:mm:ss or ddd:hh:mm:ss.
 * @param {Date} s - the date to format
 * @param {boolean} includeDays - whether to include days in the display
 */
FormulaEngine.formatduration = function (s, includeDays) {
    if (isNaN(s)) { // invalid date
        return null;
    }
    if (includeDays) {
        return Math.trunc(s / 86400) + ':' + ('' + (Math.trunc(s / 3600) % 24)).padStart(2, '0') + ':' + ('' + (Math.trunc(s / 60) % 60)).padStart(2, '0') + ':' + ('' + Math.trunc(s % 60)).padStart(2, '0');
    }
    return ('' + Math.trunc(s / 3600)).padStart(2, '0') + ':' + ('' + (Math.trunc(s / 60) % 60)).padStart(2, '0') + ':' + ('' + Math.trunc(s % 60)).padStart(2, '0');
};

if (typeof module != 'undefined' && module.exports) {
   module.exports = FormulaEngine;
}
/** version: 0.3.0 */
