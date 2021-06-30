package sfdc.formula.impl;

public class TableSet {
    public String mainAlias;
    public String cfAlias;
    public String currencyAlias;

    public TableSet(String main, String cf, String currency) {
        this.mainAlias = main;
        this.cfAlias = cf;
        this.currencyAlias = currency;
    }
}
