package com.force.formula;

import java.util.Date;

/**
 * Interface for currency data used in a formula.  
 * @author stamm
 * @since 0.0.1
 */
public interface FormulaCurrencyData {
	/**
	 * @return the 3-char ISO code for the currency
	 */
    String getIsoCode();

    /**
     * @return the amount of the currency
     */
    Number getAmount();

    /**
     * @return the date of the currency, to use for currency conversion if needed.
     */
    Date getDate();
}
