package com.force.formula.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.force.formula.FormulaGeolocation;
import com.force.formula.FormulaGeolocation.GeolocationDisplayMode;

/**
 * Service providing geo-location related features.
 *
 * @author ahersans
 * @since 180
 * @see GeoLocation
 */
public interface FormulaGeolocationService {
    // TODO: find a better place ? 
    static final int DEFAULT_XYZ_LENGTH = 60;
    // Location delimiter
    static final String LOCATION_DELIMITER = "**";

    /**
     * Returns the representation of the given location in the selected display mode.
     */
    @Nonnull
    String getRepresentation(@Nonnull FormulaGeolocation location, @Nonnull GeolocationDisplayMode displayMode);

    /**
     * Returns the internal x-y-z-encoded representation, to be stored in the db for quick distance calculations.
     */
    @Nonnull
    String computeXyzEncoded(@Nonnull FormulaGeolocation location);
    
    /**
     * Returns sql expressions for the x, y, and z components given a sql expression for the internal x-y-z-encoded representation.
     */
    @Nonnull
    String[] getXyzStrings(@Nonnull String xyzEncoded);

    /**
     * Returns sql expressions for the x, y, and z components given a pair of latitude and longitued values.
     */
    @Nonnull
    String[] getXyzStrings(double latitude, double longitude);

    /**
     * Returns sql expressions for the x, y, and z components given a pair of latitude and longitude sql expressions.
     */
    @Nonnull
    String[] getXyzStrings(@Nonnull String latitude, @Nonnull String longitude);

    /**
     * Computes the distance between the two locations in the provided unit.
     */
    @Nullable
    Double computeDistance(@Nonnull FormulaGeolocation location, @Nonnull FormulaGeolocation otherLocation, @Nonnull DistanceUnit unit);

    /**
     * Returns a SQL expression that computes the distance between the given locations provided in xyz-coordinates.
     */
    @Nonnull
    String generateDistanceSqlExpression(@Nonnull String x1Expr, @Nonnull String y1Expr, @Nonnull String z1Expr,
                                         @Nonnull String x2Expr, @Nonnull String y2Expr, @Nonnull String z2Expr,
                                         @Nonnull String diameterExpression);
    /**
     * Returns a SQL expression that computes the filter/sort distance function between the given locations provided
     * in xyz-coordinates.  This does not compute the actual distance.  Instead, it is only used for filtering and sorting,
     * and used in conjunction with generateFilterSortDistanceRhs() below.  When filtering, this method provides the
     * expression for the left hand side of the filter condition (which contains references to db columns), and method
     * generateFilterSortDistanceRhs() provides the absolute value for the right hand side of the filter condition (which
     * is a constant).  When sorting, just sort on this sql expression, which is faster to evaluate than the actual distance.
     */
    @Nonnull
    String generateFilterSortDistanceLhs(@Nonnull String x1Expr, @Nonnull String y1Expr, @Nonnull String z1Expr,
                                         @Nonnull String x2Expr, @Nonnull String y2Expr, @Nonnull String z2Expr);

    /**
     * Computes the filter/sort distance function for the given distance and unit of distance.  This is used in conjunction
     * with generateSingleSidedDistanceSqlExpression() when filtering.
     */
    double generateFilterSortDistanceRhs(double distance, DistanceUnit distanceUnit);
    
    FormulaGeolocation[] getBoundingBox(FormulaGeolocation location, double distance, DistanceUnit distanceUnit);
}
