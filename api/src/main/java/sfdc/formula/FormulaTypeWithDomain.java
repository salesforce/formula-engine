/**
 * 
 */
package sfdc.formula;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A type associated with a set of possible associated domains of the value.  Usually Id,
 * but could be an id list.
 * @author stamm
 */
public interface FormulaTypeWithDomain extends Type, ParameterizedType{

    /**
     * Is a value of this type applicable to the givenset of domains
     * @return if the set of domains is applicable is applicable
     */
    boolean isApplicable(FormulaSchema.Entity[] targetDomains);
    
    /**
     * Is a value of this type applicable to the given target entity's foreign key
     * @param foci
     * @return if the entity is applicable
     */
    default boolean isApplicable(FormulaSchema.FieldOrColumn foci) {
    	if (foci instanceof FormulaSchema.Field) {
    		return isApplicable(((FormulaSchema.Field)foci).getFormulaForeignKeyDomains());
    	}
    	return false;
    }
    
    /**
     * A that represents the type that must conform to a set of domains (i.e. referential integrity)
     * Usually a salesforce Id.
     * Allows additional annotation to be an specialized sobjectrow self reference.
     * @author stamm
     */
    public interface IdType extends FormulaTypeWithDomain {
    	
    	/**
    	 * @return the set of entities that represent the domain
    	 */
        FormulaSchema.Entity[] getDomains();

    	
        /**
         * ID types can be output as String but only if they are not originally referred to as SObjectRow "this"
         * @return true if other code will support generating text from this object reference
         */
        default boolean isTypeText() {
            return true;
        }
        
        /**
         * @return a new IdType that includes the additional domains.
         */
        IdType addToDomain(FormulaSchema.Entity[] additionalDomains);
    }
}
