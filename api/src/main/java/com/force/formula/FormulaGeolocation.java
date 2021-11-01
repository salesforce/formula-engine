package com.force.formula;

import javax.annotation.Nullable;

/**
 * Represents a location on earth, defined by a latitude and a longitude.
 *
 * @author ahersans
 * @since 180
 * @see com.force.formula.util.FormulaGeolocationService
 */
public interface FormulaGeolocation {

    @Nullable
    Number getLatitude();

    @Nullable
    Number getLongitude();

    // Optional enum for helping with display.  Not required, but used by the GeolocationService
    public enum GeolocationDisplayMode {
        DecimalDegrees,
        DegreesMinutesSeconds,
        ;
    }
}