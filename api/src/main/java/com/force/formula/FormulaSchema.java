package com.force.formula;

/**
 * Interface for accessing schema associated with a context.  
 * note: Salesforce sObject = Entity.  field = collection of columns considered the same
 * ApiElement can refer to things within a column (for things like packed bits)
 * @author stamm
 * @since 0.0.1
 */
public final class FormulaSchema  {
	/**
	 * Represents a row type that addressable with a formula.
	 * Equivalent to an "SobjectType" in force.com (or EntityInfo in core) 
 	*/
	public interface Entity {
		/**
		 * @return an internal name for the entity info.  Should never be displayed to a customer,
		 * but it often useful in logs
		 */
		String getName();
		/**
		 * @return a user-displayable name
		 */
		String getLabel();
	}

	/**
	 * Represents an element on the entity that's addressable through the API.
	 * Could correspond to a field, a column, or a column part (boolean value for a bitvector)
	 */
	public interface ApiElement {
		String getName();
		Field getFieldInfo();
		boolean isColumnInfo();
	}
	
	/**
	 * Represents an element on the entity that's a column or a collection of columns
	 */
	public interface FieldOrColumn extends ApiElement {
	    /**
	     * @return the data type of this element.  Note, if this is a field consisting
	     * of multiple columns, a compound data type will be returned.
	     */
		FormulaDataType getDataType();

		@Override
		Field getFieldInfo();
		
		Entity getEntityInfo();
	}

	/**
	 * Represents an element on the entity that's a field (an entity that is one or more
	 * columns, but is treated as a single value for administration)
	 */

	public interface Field extends FieldOrColumn {
		/**
		 * Return the foreign key domains that could be used in formulas
		 * @return the FormulaForeignKeyDomains (UddMode.ORG_ACCESSIBLE)
		 */
		FormulaSchema.Entity[] getFormulaForeignKeyDomains();
		
		/**
		 * @return Whether this field is calculated or not.
		 * Used to deal with the weird DB null semantics of booleans
		 */
		default boolean isCalculated() {
			return false;
		}
		
		/**
		 * @return the field type of the formula
		 */
		FormulaFieldInfo.FormulaFieldType getFieldType();
		
		/**
		 * If this is a foreign key, return the relationship name that should be used when
		 * resolving javascript references
		 * @return the foreign key relationship api name, or null if this isn't a foreign key
		 */
	    default String getForeignKeyRelationshipName() {
	    	return null;
	    }

	}
	
}
