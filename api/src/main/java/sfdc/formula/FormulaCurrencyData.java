package sfdc.formula;

import java.util.Date;

/**
 * Interface for currency data used in a formula.  
 * @author stamm
 * @since 0.0.1
 */
public interface FormulaCurrencyData {
    public String getIsoCode();

    public Number getAmount();
    
    public Date getDate();
}
