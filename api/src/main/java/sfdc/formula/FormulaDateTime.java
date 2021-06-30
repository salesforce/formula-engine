package sfdc.formula;

import java.util.Date;

/**
 * Describe your class here.
 *
 * @author dchasman
 * @since 140
 */
public class FormulaDateTime implements Comparable<FormulaDateTime> {

    public FormulaDateTime(Date date) {
        this.date = date;
    }

    public long getTime() {
        return (date != null) ? date.getTime() : 0;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof FormulaDateTime)) {
        	return false;
        }
        if (date == null) {
            return (((FormulaDateTime)obj).date == null);
        }

        return date.equals(((FormulaDateTime)obj).date);
    }
    
    @Override
    public int hashCode() {
        return (date == null ? super.hashCode() : date.hashCode());
    }
    

    @Override
    public String toString() {
        return (date != null) ? date.toString() : "null";
    }

    @Override
    //@edu.umd.cs.findbugs.annotations.SuppressWarnings("NP_NULL_ON_SOME_PATH_MIGHT_BE_INFEASIBLE")
    public int compareTo(FormulaDateTime o) {
        if (o == null) {
            return 1;
        }

        Date other = o.getDate();

        if (date == other) {
            return 0;
        } else if (date != null && other == null) {
            return 1;
        } else if (date == null && other != null) {
            return -1;
        } else {
            return date.compareTo(other);
        }
    }

    public static Date unwrap(Object wrapper) {
        if (wrapper instanceof FormulaDateTime) {
            return ((FormulaDateTime)wrapper).getDate();
        } else {
            return (Date)wrapper;
        }
    }

    private final Date date;
}
