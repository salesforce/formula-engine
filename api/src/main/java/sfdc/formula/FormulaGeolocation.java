package sfdc.formula;

import javax.annotation.Nullable;

/**
 * Represents a location on earth, defined by a latitude and a longitude.
 *
 * @author ahersans
 * @since 180
 * @see GeoLocationService
 */
public interface FormulaGeolocation {

    @Nullable
    Number getLatitude();

    @Nullable
    Number getLongitude();

}