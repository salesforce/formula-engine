/**
 * 
 */
package com.force.formula;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock implementation of FormulaPicklistInfo.
 * @author stamm
 * @since 0.1.0
 */
public class MockFormulaPicklistInfo implements FormulaPicklistInfo {

    protected Item[] allEnumItems;
    private Map<String,Item> dbValueToEnumItem = null;

	public MockFormulaPicklistInfo(Item[] items) {
		this.allEnumItems = items;
	}

	public MockFormulaPicklistInfo(List<Item> items) {
		this(items.toArray(new Item[items.size()]));
	}

	// Use a map, of db/api value to display.  Use display value as the api value
	public MockFormulaPicklistInfo(Map<String,String> items) {
		this(items.entrySet().stream().map((e)->new MockPicklistItem(e.getKey(), e.getValue(), e.getValue())).collect(Collectors.toList()));
	}
	
	public Item[] getAllEnumItems() {
		return allEnumItems;
	}

	public Item getEnumItemByDbValue(String dbValue) {
		ensureDbValueToEnumItemBuilt();
		return dbValueToEnumItem.get(dbValue);
	}
	

    /**
     * @param apiValue the api value of the picklist to lookup
     * @return the enum item for the given api value, or null
     * if the given value does not match any valid enum items
     * Case insensitive lookup
     */
    public Item getEnumItemByApiValue(String apiValue) {
        for (Item item : getAllEnumItems()) {
            String itemApiValue = item.getApiValue();
            if (itemApiValue != null && itemApiValue.equalsIgnoreCase(apiValue)) {
                return item;
            }
        }
        return null;
    }

	
    private void ensureDbValueToEnumItemBuilt(){
        if (dbValueToEnumItem != null)
            return;
        Item[] items = getAllEnumItems();
        Map<String,Item> map = new HashMap<>(items.length<<1);
        for (Item item : items) {
            map.put(item.getDbValue(), item);
        }
        dbValueToEnumItem = map;
    }
	
    /**
     * @author stamm
     * Represents a picklist item.
     */
    public interface Item {
        /**
         * @return the value to use when stored in the db
         */
        String getDbValue();

        /**
         * @return the value to use in the API (and formulas)
         */
        String getApiValue();

        /**
         * @return the display value of the enum item according to the current user's language setting.
         */
        String getDisplay();
    }
    
    public static class MockPicklistItem implements Item {
    	private final String dbValue;
    	private final String apiValue;
    	private final String display;
		public MockPicklistItem(String dbValue, String apiValue, String display) {
			this.dbValue = dbValue;
			this.apiValue = apiValue;
			this.display = display;
		}
		@Override
		public String getDbValue() {
			return this.dbValue;
		}
		@Override
		public String getApiValue() {
			return this.apiValue;
		}
		@Override
		public String getDisplay() {
			return this.display;
		}
    }
    
    /**
     * 
     * Interface intended to signal to PicklistData that
     * this item should include hint text
     */
    public interface PicklistItemWithHint extends Item {

        String getHint();
        
    }
	
}
