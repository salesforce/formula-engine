package sfdc.formula;

/**
 * Interface for a "fieldSetMember" so that the formula engine can
 * resolve it at runtime to an actual field path.
 * @author stamm
 * @since 200
 */
public interface FieldSetMemberInfo {

    /**
     * @return the reference to the field path suitable for a dynamic SOQL query
     */
    String getFieldPath();

}