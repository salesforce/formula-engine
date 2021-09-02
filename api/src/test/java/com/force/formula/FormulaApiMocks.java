/**
 * 
 */
package com.force.formula;

import com.force.formula.FormulaFieldInfo.FormulaFieldType;
import com.force.formula.FormulaSchema.Entity;
import com.force.formula.FormulaSchema.Field;

/**
 * Mocks for testing the api classes.  Use the ones in test-utils as they are more
 * extensible.
 * @author stamm
 */
public class FormulaApiMocks {


	public static class MockField implements FormulaSchema.Field {
		private final String name;
		private final FormulaDataType type;
		public MockField(String name, FormulaDataType type) {
			this.name = name;
			this.type = type;
		}
		@Override
		public FormulaDataType getDataType() {
			return this.type;
		}
		@Override
		public Field getFieldInfo() {
			return this;
		}
		@Override
		public Entity getEntityInfo() {
			return null;
		}
		@Override
		public String getName() {
			return this.name;
		}
		@Override
		public boolean isColumnInfo() {
			return true;
		}
		@Override
		public Entity[] getFormulaForeignKeyDomains() {
			return null;
		}
		@Override
		public boolean isCalculated() {
			return false;
		}
		@Override
		public FormulaFieldType getFieldType() {
			return null;
		}
		@Override
		public String getForeignKeyRelationshipName() {
			return null;
		}
		@Override
		public String toString() {
			return name;
		}
	}
	
	public enum MockType implements FormulaDataType {
		TEXT("Text"),
	    BOOLEAN("Boolean"),
	    DATETIME("DateTime");
		
	    private final String name;
	    MockType(String name) {
	        this.name = name;
	    }
		@Override
		public String getName() {
			return this.name;
		}
		@Override
		public String getJavaName() {
			return this.name;
		}
		@Override
		public String getLabel() {
			return this.name;
		}
		@Override
		public boolean isSimpleText() {
			return this == TEXT;
		}
		@Override
		public boolean isBoolean() {
			return this == BOOLEAN;
		}
		@Override
		public boolean isInteger() {
			return false;
		}
		@Override
		public boolean isDecimal() {
			return false;
		}
		@Override
		public boolean isPercent() {
			return false;
		}
		@Override
		public boolean isDateTime() {
			return this == DATETIME;
		}
		@Override
		public boolean isDateOnly() {
			return false;
		}

	}
}
