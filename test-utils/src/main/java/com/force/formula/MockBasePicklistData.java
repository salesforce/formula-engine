/*
 * Copyright, 2005, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package com.force.formula;

import java.util.*;

import com.force.formula.util.FormulaI18nUtils;
import com.force.formula.util.FormulaTextUtil;

/**
 * Stores selected value(s) in a picklist.  Does not store list of possible values.  Concrete subclasses
 * implement that machinery.
 *
 * @author tkim
 * @since 140
 */
public abstract class MockBasePicklistData {
    public static final char VALUE_SEP = ';';

    /** holds current defualt value. null if this is multi-select */
    private String defaultVal = null;

    /** holds current selected values. null if this is single select */
    private String[] selectedValues = null;

    /** whether or not this is a multiple select */
    private boolean multiple = false;

    public MockBasePicklistData(String defaultVal) {
        this.defaultVal = defaultVal;
    }

    /**
     * constructor to make this instance as multi-select picklist.
     * @param values values array
     * @param display display value array
     * @param selectedValues initial selection. accept <code>null</code> value.
     */
    public MockBasePicklistData(String[] selectedValues, boolean multiple) {
        this.multiple = multiple;
        if (multiple) {
            this.selectedValues = selectedValues;
        } else if (selectedValues != null && selectedValues.length > 0) {
            this.defaultVal = selectedValues[0];
        }
    }

    public abstract String[] getValues();

    public abstract String[] getDisplay();

    public abstract String[] getHints();

    /**
     *  Sorts the Picklist using the display String based on the current user's language.
     *  Returns itself for convenience.
     */
    public abstract MockBasePicklistData sort();

    public final String getDefaultValue() {
        if (this.multiple)
            throw new UnsupportedOperationException();
        return this.defaultVal;
    }

    public final String[] getSelected() {
        return (!this.multiple) ?
            this.defaultVal == null ?
                null : new String[] { this.defaultVal }
            : this.selectedValues;
    }

    public final boolean isMultiple() {
        return multiple;
    }

    public final MockBasePicklistData setMultiple(boolean multiple) {
        if (this.multiple != multiple) {
            if (this.multiple) {
                // multi to single. pickup the first value
                if (this.selectedValues != null && this.selectedValues.length > 0)
                    this.defaultVal = this.selectedValues[0];
                this.selectedValues = null;
            } else {
                // single to multi
                if (this.defaultVal != null)
                    this.selectedValues = new String[] { this.defaultVal };
                this.defaultVal = null;
            }
        }
        this.multiple = multiple;
        return this;
    }

    public final void setDefaultValue(String defaultVal) {
        if (this.multiple)
            throw new UnsupportedOperationException();

        this.defaultVal = defaultVal;
    }

    public final String getStringValue() {
        if (!this.multiple) {
            return getDefaultValue();
        } else if (this.selectedValues != null && this.selectedValues.length > 0) {
            StringBuilder sb = new StringBuilder(this.selectedValues.length*16);
            for (int i = 0; i < this.selectedValues.length; i++) {
                if (i > 0)
                    sb.append(VALUE_SEP);
                sb.append(selectedValues[i]);
            }
            return sb.toString();
        }
        return null;
    }

    @Override
    public final String toString() {
        return "PicklistData: " + this.getStringValue();
    }

    // abstract or use internalGetValues()?
    /**
     * returns current selected value.
     * don't call this if this is multi-select picklist
     */
    public abstract String getDisplayedValue();

    /**
     * Returns true if the default values (which is the picklist's selection) are equal.
     * This might need re-thinking, since it means that two picklists with different values
     * that happen to have the same value selected will say they're equal. Hmm. That makes
     * sense in most cases, but it's conceivable there's ones where it doesn't make sense.
     * I suppose in those cases, they should just be forewarned.
     */
    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof MockBasePicklistData))
            return false;
        MockBasePicklistData other = (MockBasePicklistData)o;
        if (this.multiple)
            return FormulaTextUtil.stringArraysAreEqualNullIsEmpty(this.selectedValues, other.selectedValues);
        else
            return FormulaTextUtil.stringsAreEqualNullIsEmpty(this.defaultVal, other.defaultVal);

    }

    /**
     * Hashes only on the default value (which is the picklist's selection).
     * This might need re-thinking, since it means that two picklists with different values
     * that happen to have the same value selected will say they're equal. Hmm. That makes
     * sense in most cases, but it's conceivable there's ones where it doesn't make sense.
     * I suppose in those cases, they should just be forewarned.
     */
    @Override
    public final int hashCode() {
        String val = getStringValue();
        if (val != null)
            return val.hashCode();
        return 0;
    }

    /**
     * The picklist is empty is its selected value is empty, since that means they haven't picked a value yet.
     */
    public final boolean isEmpty() {
        if (multiple)
            return this.selectedValues == null || this.selectedValues.length == 0;
        else
            return this.defaultVal == null || this.defaultVal.length() == 0;

    }

    /**
     * Sort a set of parallel arrays, where the keys are strings.
     * @param keys the set of keys to be sorted in linguistic order
     * @param values the set of values to be sorted in the same manner.
     */
    // Use of temp array for swapping
    public static <V> void sortPicklist(final String[] keys, V[] values) {
        if (keys == null || values == null)
            return;
        assert keys.length == values.length : "Trying to sort parallel arrays with different values";

        Integer[] finalOrder = new Integer[keys.length];
        for (int i = 0; i < keys.length; i++)
            finalOrder[i] = i;

        final Comparator<String> comp = FormulaI18nUtils.getLocalizer().getComparator(keys.length);
        Arrays.sort(finalOrder, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                return comp.compare(keys[i1], keys[i2]);
            }
        });

        // Now we have a list of final orderings of values.
        Object[] tmp = new Object[keys.length];
        for (int i = 0; i < keys.length; i++)
            tmp[i] = keys[finalOrder[i]];
        System.arraycopy(tmp, 0, keys, 0, keys.length);

        if (keys != values) {
            for (int i = 0; i < keys.length; i++)
                tmp[i] = values[finalOrder[i]];
            System.arraycopy(tmp, 0, values, 0, values.length);
        }
    }

    /**
     * Sort a set of parallel arrays, where the keys are strings.
     * @param keys the set of keys to be sorted in linguistic order
     * @param values the set of values to be sorted in the same manner.
     */
    @SuppressWarnings("unchecked")
    // Use of temp array for swapping
    public static <V> void sortPicklist(final List<String> keys, List<V> values) {
        if (keys == null || values == null)
            return;
        assert keys.size() == values.size() : "Trying to sort parallel arrays with different values";

        Integer[] finalOrder = new Integer[keys.size()];
        for (int i = 0; i < keys.size(); i++)
            finalOrder[i] = i;

        final Comparator<String> comp = FormulaI18nUtils.getLocalizer().getComparator(keys.size());
        Arrays.sort(finalOrder, (i1,i2) -> comp.compare(keys.get(i1), keys.get(i2)));

        // Now we have a list of final orderings of values.
        String[] tmpKeys = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++)
            tmpKeys[i] = keys.get(finalOrder[i]);
        keys.clear();
        keys.addAll(Arrays.asList(tmpKeys));

        V[] tmpValues = (V[])new Object[keys.size()];
        for (int i = 0; i < keys.size(); i++)
            tmpValues[i] = values.get(finalOrder[i]);
        values.clear();
        values.addAll(Arrays.asList(tmpValues));
    }

    /** Returns the size of the picklist, as measured by the number of values */
    public abstract int size();

    /**
     * For setup screens we post-process component lists to disambiguate labels across packages
     */
    public abstract void setDisplay(int i, String s);
}
