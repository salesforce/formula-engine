/**
 *
 */
package com.force.formula;

import com.force.formula.util.FormulaGeolocationService;

import jakarta.annotation.Nullable;

/**
 * @author stamm
 *
 */
public class MockLocation implements FormulaGeolocation {

    private final GeolocationDisplayMode displayMode;
    private final Number latitude;
    private final Number longitude;
    private String xyzEncoded;

    public MockLocation(Number latitude, Number longitude) {
        this(GeolocationDisplayMode.DecimalDegrees, latitude, longitude);
    }

    public MockLocation(GeolocationDisplayMode displayMode, Number latitude, Number longitude) {
        this.displayMode = displayMode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public Number getLatitude() {
        return this.latitude;
    }

    @Override
    public Number getLongitude() {
        return this.longitude;
    }

    public GeolocationDisplayMode getDisplayMode() {
        return this.displayMode;
    }

    @Override
    public String toString() {
        return getGeolocationService().getRepresentation(this, getDisplayMode());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FormulaGeolocation))
            return false;
        FormulaGeolocation other = (FormulaGeolocation)o;
        return areEquals(this.latitude, other.getLatitude())
            && areEquals(this.longitude, other.getLongitude());
    }

    private boolean areEquals(Number aNumber, Number otherNumber) {
        return (aNumber == null && otherNumber == null)
            || (aNumber != null && aNumber.equals(otherNumber));
    }

    @Override
    public int hashCode() {
        String s = toString();
        return s == null ? 0 : s.hashCode();
    }

    @Nullable
    public String getEncodedValue() {
        if (this.xyzEncoded == null) {
            this.xyzEncoded = getGeolocationService().computeXyzEncoded(this);
        }

        return this.xyzEncoded;
    }

    private FormulaGeolocationService getGeolocationService() {
        return FormulaEngine.getHooks().getFormulaGeolocationService();
    }
}
