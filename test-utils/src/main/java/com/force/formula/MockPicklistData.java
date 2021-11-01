/*
 * Copyright, 2001, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */

package com.force.formula;

import java.util.Iterator;
import java.util.Map;

import com.force.formula.MockFormulaPicklistInfo.Item;
import com.force.formula.MockFormulaPicklistInfo.PicklistItemWithHint;
import com.force.formula.util.FormulaI18nUtils;
import com.force.i18n.LabelRef;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * Immutable PicklistData. Contains a Value, but also has the metadata for the set of possible values.
 * 
 * Used for testing formulas that use picklist data.
 * @author koliver
 */
public class MockPicklistData extends MockBasePicklistData implements Iterable<MockPicklistData.Option> {
    private final String[] values;
    private final String[] display;
    private final String[] hints;

    public static final MockPicklistData EMPTY = new MockPicklistData(new String[0], new String[0]);

    public class Option {
        private String value;
        private String display;
        private String hint;

        public Option(String value, String display, String hint) {
            this.value = value;
            this.display = display;
            this.hint = hint;
        }

        public String getValue() {
            return value;
        }

        public String getDisplay() {
            return display;
        }

        public String getHint() {
            return hint;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void setDisplay(String display) {
            this.display = display;
        }

        public void setHint(String hint) {
            this.hint = hint;
        }
    }

    @Override
    public Iterator<Option> iterator() {
        return new Iterator<Option>() {
            private int idx = 0;

            @Override
            public boolean hasNext() {
                return idx < values.length;
            }

            @Override
            public Option next() {
                Option rval = new Option(values[idx], display[idx], hints != null ? hints[idx] : null);
                idx++;
                return rval;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** placeholder PicklistItem that can be used with SelectElement to introduce option groups */
    public static class OptionGroup implements Item {
        private final LabelRef labelRef;

        public OptionGroup(String labelRefSection, String labelRefParam) {
            this.labelRef = new LabelRef(labelRefSection, labelRefParam);
        }

        @Override
        public String getDisplay() {
            return FormulaI18nUtils.getLocalizer().getLabel(labelRef);
        }
        
        @Override
        public String getDbValue() {
            return null;
        }
        
        @Override
        public String getApiValue() {
            return null;
        }
    }

    public MockPicklistData(String[] values, String[] display) {
        this(values, display, null);
    }

    public MockPicklistData(String[] values, String[] display, String defaultVal) {
        this(values, display, null, defaultVal);
    }

    public MockPicklistData(String[] values, String[] display, String[] hints, String defaultVal) {
        super(defaultVal);
        this.values = values;
        this.display = display;
        this.hints = hints;
    }


    /**
     * @param valueMap a map from value to display
     */
    public MockPicklistData(Map<String, String> valueMap) {
        this(valueMap, null);
    }

    public MockPicklistData(Map<String, String> valueMap, String defaultVal) {
        super(defaultVal);
        this.values = valueMap.keySet().toArray(new String[valueMap.size()]);
        this.display = valueMap.values().toArray(new String[valueMap.size()]);
        this.hints = null;
    }

    /**
     * constructor for picklist with hints.
     * To avoid ambiguity, this is only constructor supporting hints.  For non-multiple, pass
     * in false for multiple flag and if required, array containing single selected value.
     * @param values values array
     * @param display display value array
     * @param hints hints array
     * @param selectedValues initial selection. accept <code>null</code> value
     * @param multiple whether multiple selection used
     */
    public MockPicklistData(String[] values, String[] display, String[] hints, String[] selectedValues, boolean multiple) {
        super(selectedValues, multiple);
        this.values = values;
        this.display = display;
        this.hints = hints;
    }

    /**
     * constructor to make this instance as multi-select picklist.
     * @param values values array
     * @param display display value array
     * @param selectedValues initial selection. accept <code>null</code> value.
     * @param multiple if this is multi-select picklist
     */
    public MockPicklistData(String[] values, String[] display, String[] selectedValues, boolean multiple) {
        this(values, display, null, selectedValues, multiple);
    }

    /**
     * Construct an instance from the specified enum info.  Equivalent to
     * <code>new PicklistData(enumInfo.getAllEnumItems())</code>.
     * @param enumInfo the enum info to use.
     */
    public MockPicklistData(MockFormulaPicklistInfo enumInfo) {
        this(enumInfo.getAllEnumItems());
    }

    
    /**
     * Construct an instance from the specified enum info.  Equivalent to
     * <code>new PicklistData(enumInfo.getAllEnumItems())</code>.
     * @param enumInfo the enum info to use.
     * @param value the value of the enum
     */
    public MockPicklistData(MockFormulaPicklistInfo enumInfo, String value) {
        this(enumInfo.getAllEnumItems(), enumInfo.getEnumItemByDbValue(value));
    }
    
    /**
     * Construct an instance from the specified enum items.
     * @param enumItems the array of enum items to use.
     */
    public MockPicklistData(Item[] enumItems) {
        this(enumItems, null);
    }

    public MockPicklistData(Item[] enumItems, Item defaultValue) {
        super(defaultValue != null ? getPicklistValue(defaultValue) : null);
        this.values = new String[enumItems.length];
        this.display = new String[enumItems.length];
        this.hints = null;

        int i = 0;

        for (Item item : enumItems) {
            values[i] = getPicklistValue(item);
            display[i] = item.getDisplay();
            i++;
        }
    }

    /**
     * Construct an instance from the specified enum items.
     * @param enumItems the array of enum items to use.
     */
    public MockPicklistData(Iterable<? extends Item> enumItems) {
        this(enumItems, (String)null);
    }

    public MockPicklistData(Iterable<? extends Item> enumItems, Item defaultValue) {
        this(enumItems, defaultValue != null ? getPicklistValue(defaultValue) : null);
    }

    /**
     * Construct an instance from the specified enum items.
     * @param enumItems the array of enum items to use.
     * @param defaultValue the default value to use
     */
    public MockPicklistData(Iterable<? extends Item> enumItems, String defaultValue) {
        super(defaultValue);
        // This check is ugly, but it's what we need to do to determine if we are dealing with an Iterable<? extends PickListItemWithHint>.
        // The instanceof operator cannot be applied to a generic Iterable to determine what type of objects it contains.
        if (enumItems.iterator().hasNext() && enumItems.iterator().next() instanceof PicklistItemWithHint) {
            // Add hints if the PicklistItem type includes them
            this.hints = Iterables.toArray(Iterables.transform(enumItems, new Function<Item, String>() {
                @Override
                public String apply(Item item) { return ((PicklistItemWithHint)item).getHint(); } }), String.class);
        } else {
            this.hints = null;
        }
        this.values = Iterables.toArray(Iterables.transform(enumItems, new Function<Item, String>() {
            @Override
            public String apply(Item item) { return getPicklistValue(item); } }), String.class);
        this.display = Iterables.toArray(Iterables.transform(enumItems, new Function<Item, String>() {
            @Override
            public String apply(Item item) { return item.getDisplay(); } }), String.class);
    }

    /**
	 * Sometimes you might want the HtmlValue, not the db value.
     */
    private static String getPicklistValue(Item item) {
        return item.getDbValue();
    }

    /**
     *  Sorts the Picklist using the display String based on the current user's language.
     *  Returns itself for convenience.
     */
    @Override
    public MockPicklistData sort() {
        sortPicklist(display, values);
        return this;
    }

    @Override
    public String[] getValues() {
        return this.values;
    }

    @Override
    public String[] getDisplay() {
        return this.display;
    }

    @Override
    public String[] getHints() {
        return this.hints;
    }

    

    /**
     * returns current selected value.
     * don't call this if this is multi-select picklist
     */
    @Override
    public String getDisplayedValue() {
        String value = null;
        String defaultVal = getDefaultValue();

        if (defaultVal != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null && values[i].equals(defaultVal)) {
                    value = display[i];
                    break;
                }
            }
        }

        return value;
    }

    /** Returns the size of the picklist, as measured by the number of values */
    @Override
    public int size() {
        if (this.values == null)
            return 0;
        return this.values.length;
    }

    @Override
    public void setDisplay(int i, String s) {
        this.display[i] = s;
    }


}
