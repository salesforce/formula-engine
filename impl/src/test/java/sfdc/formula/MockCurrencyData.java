/*
 * Copyright, 1999-2018, salesforce.com
 * All Rights Reserved
 * Company Confidential
 */
package sfdc.formula;

import java.util.Date;

/**
 * Describe your class here.
 *
 * @author stamm
 * @since 212
 */
public class MockCurrencyData implements FormulaCurrencyData, Comparable<MockCurrencyData> {

    public static final String EMPTY_ISO_CODE = "000";
    
    private final String isoCode;
    private Number amount;
    private Date date;

    @Override
    public String getIsoCode() {
        return this.isoCode;
    }

    @Override
    public Number getAmount() {
        return this.amount;
    }
    
    @Override
    public Date getDate() { 
        return this.date; 
    }

    public MockCurrencyData(String isoCode, Number amount) {
        this(isoCode, amount, null);
    }

    public MockCurrencyData(String isoCode, Number amount, Date date) {
        if (isoCode == null) {
            throw new IllegalArgumentException("isoCode of currency data should never be null. '000' for single currency org");
        }
        this.isoCode =  isoCode;
        this.amount = amount;
        this.date = date;
    }    

    /**
     * Gets locale-independent (database) representation of this currency data for filter
     * This is the reverse of parse(RequestInfo ri, String data).
     */
    @Override
    public String toString() {
        return getValueForDb();
    }

    public String getValueForDb() {
        return getValueForDbOrApi(false);
    }

    public String getValueForApi() {
        return getValueForDbOrApi(true);
    }

    private String getValueForDbOrApi(boolean isApi) {
        StringBuilder s = new StringBuilder();
        if (this.isoCode != null && !EMPTY_ISO_CODE.equals(this.isoCode)) {
            s.append(this.isoCode);
            if (this.amount != null && !isApi)
                s.append(" ");
        }
        if (this.amount != null)
            s.append(this.amount.toString());
        return s.toString();
    }



    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MockCurrencyData))
            return false;

        MockCurrencyData other = (MockCurrencyData)o;
        assert this.isoCode != null && other.isoCode != null : "isoCode is null";
        return ((this.amount == null && other.amount == null) || (this.amount != null && this.amount
            .equals(other.amount)))
            && this.isoCode.equals(other.isoCode);
    }

    /**
     * Compares two CurrencyDatas for ordering. If the argument CurrencyData has 
     * a different currency, it is first converted.
     *
     * @param  that the <code>CurrencyData</code> to be compared.
     * @return the value <code>0</code> if the argument CurrencyData is equal to
     *          this CurrencyData; a value less than <code>0</code> if this CurrencyData
     *          is before the CurrencyData argument; and a value greater than
     *          <code>0</code> if this CurrencyData is after the CurrencyData argument.
     * @exception NullPointerException if <code>that</code> is null.
     */
    @Override
    public int compareTo(MockCurrencyData that) {
        //if (!this.isoCode.equals(that.isoCode))
        //    that = UddUtil.getOrgInfo().getCurrencyInfo().convert(that, this.isoCode);
        double thisAmount = this.amount.doubleValue();
        double thatAmount = that.amount.doubleValue();
        if (thisAmount < thatAmount)
            return -1;
        else if (thisAmount == thatAmount)
            return 0;
        else
            return +1;
    }

    @Override
    public int hashCode() {
        int hash = this.isoCode.hashCode();
        if (this.amount != null)
            hash += this.amount.hashCode();
        return hash;
    }
}
