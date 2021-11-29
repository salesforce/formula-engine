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
 * @see FormulaGeolocation
 */
public interface FormulaGeolocationService {
    // TODO: find a better place ? 
    int DEFAULT_XYZ_LENGTH = 60;
    // Location delimiter
    String LOCATION_DELIMITER = "**";

    /**
     * @return the representation of the given location in the selected display mode.
     * @param location the location to convert to a string
     * @param displayMode how to represent the location
     */
    @Nonnull
    String getRepresentation(@Nonnull FormulaGeolocation location, @Nonnull GeolocationDisplayMode displayMode);

    /**
     * @return the internal x-y-z-encoded representation, to be stored in the db for quick distance calculations.
     * @param location the location to convert to a XYZ
     */
    @Nonnull
    String computeXyzEncoded(@Nonnull FormulaGeolocation location);
    
    /**
     * @return sql expressions for the x, y, and z components given a sql expression for the internal x-y-z-encoded representation.
     * @param xyzEncoded the XYZ coordiante for the location
     */
    @Nonnull
    String[] getXyzStrings(@Nonnull String xyzEncoded);

    /**
     * Returns sql expressions for the x, y, and z components given a pair of latitude and longitued values.
     * @param latitude the latitude 
     * @param longitude the longitude
     * @return the three SQL expressions to get XYZ 
     */
    @Nonnull
    String[] getXyzStrings(double latitude, double longitude);

    /**
     * Returns sql expressions for the x, y, and z components given a pair of latitude and longitude sql expressions.
     * @param latitude the latitude 
     * @param longitude the longitude
     * @return the three SQL expressions to get XYZ 
     */
    @Nonnull
    String[] getXyzStrings(@Nonnull String latitude, @Nonnull String longitude);

    /**
     * Computes the distance between the two locations in the provided unit.
     * @param location the location to test
     * @param otherLocation the other location
     * @param unit the unit of measure between them
     * @return the distance between the units
     */
    @Nullable
    Double computeDistance(@Nonnull FormulaGeolocation location, @Nonnull FormulaGeolocation otherLocation, @Nonnull DistanceUnit unit);

    /**
     * Returns a SQL expression that computes the distance between the given locations provided in xyz-coordinates.
     * @param x1Expr the SQL expression for the x component of the left hand side
     * @param y1Expr the SQL expression for the y component of the left hand side
     * @param z1Expr the SQL expression for the z component of the left hand side
     * @param x2Expr the SQL expression for the x component of the right hand side
     * @param y2Expr the SQL expression for the y component of the right hand side
     * @param z2Expr the SQL expression for the z component of the right hand side
     * @param diameterExpression the expression for the diameter of the earth
     * @return the SQL expression for the distance
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
     * @param x1Expr the SQL expression for the x component of the left hand side
     * @param y1Expr the SQL expression for the y component of the left hand side
     * @param z1Expr the SQL expression for the z component of the left hand side
     * @param x2Expr the SQL expression for the x component of the right hand side
     * @param y2Expr the SQL expression for the y component of the right hand side
     * @param z2Expr the SQL expression for the z component of the right hand side
     * @return the SQL expression for the distance suitable for sorting
     */
    @Nonnull
    String generateFilterSortDistanceLhs(@Nonnull String x1Expr, @Nonnull String y1Expr, @Nonnull String z1Expr,
                                         @Nonnull String x2Expr, @Nonnull String y2Expr, @Nonnull String z2Expr);

    /**
     * Computes the filter/sort distance function for the given distance and unit of distance.  This is used in conjunction
     * with generateSingleSidedDistanceSqlExpression() when filtering.
     * @param distance the distance between points
     * @param distanceUnit the unit of measure
     * @return  the square of the euclidian_distance_in_radius_of_the_earth from the arc_distance and the radius_of_earth.
     */
    double generateFilterSortDistanceRhs(double distance, DistanceUnit distanceUnit);
    
    FormulaGeolocation[] getBoundingBox(FormulaGeolocation location, double distance, DistanceUnit distanceUnit);
}
