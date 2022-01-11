/**
 * 
 */
package com.force.formula.impl;

import com.force.formula.FormulaGeolocation;
import com.force.formula.FormulaGeolocation.GeolocationDisplayMode;
import com.force.formula.util.DistanceUnit;
import com.force.formula.util.FormulaGeolocationService;

/**
 * Implementation of a {@link FormulaGeolocationService}.
 *
 * A Geolocation (or just location) is a point on the surface of the earth.  It is defined by a latitude and a longitude,
 * which is also what the user sees in the UI or API.  Internally, a geolocation is stored in three columns in the database,
 * the latitude, the longitude, plus the internal x-y-z-encoded representation, which is used for quick distance calculations
 * in the database.
 *
 * A location is converted into 3-dimensional coordinate system, normalized to the range [-1,1].  For example, the north pole
 * is at the coordinate (0,0,1), while the point on the equator below the Royal Observatory in Greenwich, London is at the
 * coordinate (0,1,0).  (See inner class LocationXYZ for actual computation details.)
 * Then, the internal x-y-z-encoded representation is the concatenation of these three numbers in decimal in fixed lengths.
 *
 * To calculate the great-circle distance between two locations, which is the shortest distance between two points on a sphere,
 * we first calculate the Euclidian distance between those two points in the globe by:
 *     euclidian_distance_in_radius_of_the_earth = sqrt((x1 - x2) ^ 2 + (y1 - y2) ^ 2 + (z1 - z2) ^ 2)
 * Then we can get the great-circle distance by:
 *     arc_distance = radius_of_earth * 2 * arcSin(euclidian_distance_in_radius_of_the_earth / 2)
 *
 * When filtering or sorting location values stored in the db, we only need to filter or sort by the square of the euclidian
 * distance, which is much cheaper to compute.  (See generateFilterSortDistanceLhs() and generateFilterSortDistanceRhs()).
 *
 * @author ahersans
 * @see FormulaGeolocationService
 */
public class GeolocationServiceImpl implements FormulaGeolocationService  {
    private static final String SPACE   = " ";
    private static final String DEGREES = "\u00b0";
    private static final String MINUTES = "'";
    private static final String SECONDS = "''";
    
    public static double MIN_LAT = Math.toRadians(-90d);  // -PI/2
    public static double MAX_LAT = Math.toRadians(90d);   //  PI/2
    public static double MIN_LON = Math.toRadians(-180d); // -PI
    public static double MAX_LON = Math.toRadians(180d);
    public static double EPSILON = .0000001;
    
    // Provide an instance for testing.  This can be instantiated with Spring if desired
    private static final GeolocationServiceImpl INSTANCE = new GeolocationServiceImpl();
    public static FormulaGeolocationService getInstance() {
    	return INSTANCE;
    }

    @Override
    public String getRepresentation(final FormulaGeolocation location, final GeolocationDisplayMode displayMode) {
        final Number latitude = location.getLatitude();
        final Number longitude = location.getLongitude();

        switch (displayMode) {
            case DecimalDegrees:
                return (latitude == null ? "" : latitude + SPACE) + (longitude == null ? "" : longitude);
            case DegreesMinutesSeconds:
                final String latitudeSuffix = latitude == null ? "" : latitude.doubleValue() >= 0 ? CardinalPoint.North.getInitial() : CardinalPoint.South.getInitial();
                final String longitudeSuffix = longitude == null ? "" : longitude.doubleValue() >= 0 ? CardinalPoint.East.getInitial() : CardinalPoint.West.getInitial();
                return (latitude == null ? "" : getDegreesMinutesSeconds(Math.abs(latitude.doubleValue()))) + latitudeSuffix + SPACE
                     + (longitude == null ? "" : getDegreesMinutesSeconds(Math.abs(longitude.doubleValue())) + longitudeSuffix);
            default:
                throw new IllegalStateException();
        }
    }

    private String getDegreesMinutesSeconds(final Number coordinate) {
        int degrees = (int)Math.floor(coordinate.doubleValue());
        final double remainingMinutes = (coordinate.doubleValue() - degrees) * 60.0d;
        int minutes = (int)Math.floor(remainingMinutes);
        // TODO ahersans: revisit the rounding : see RoundingMode
        int seconds = (int)Math.floor(Math.round((remainingMinutes - minutes) * 60.0d));

        // Fix potential rounding errors
        if (seconds >= 60) {
            minutes++;
            seconds -= 60;
        }
        if (minutes >= 60) {
            degrees++;
            minutes -= 60;
        }

        return Math.abs(degrees) + DEGREES + minutes + MINUTES + seconds + SECONDS;
    }

    private enum CardinalPoint {
        North("N"),
        South("S"),
        East("E"),
        West("W"),
        ;

        private final String initial;

        CardinalPoint(final String initial) { this.initial = initial; }
        
        private String getInitial() { return this.initial; }
    }

    /**
     * Returns the internal x-y-z-encoded representation, to be stored in the db for quick distance calculations.
     */
    @Override
    public String computeXyzEncoded(final FormulaGeolocation location) {
        final Number latitude = location.getLatitude();
        final Number longitude = location.getLongitude();
        
        if (latitude == null || longitude == null)
            return null;
        
        return new LocationXYZ(latitude.doubleValue(), longitude.doubleValue()).getEncoded();
    }

    /**
     * Utility to convert latitude and longitude into 3-dimensional coordinates.
     */
    private static class LocationXYZ {
        final double x, y, z;

        LocationXYZ(final double latitude, final double longitude) {
            final double latitudeRadian = Math.toRadians(latitude);
            final double longitudeRadian = Math.toRadians(longitude);
            final double latitudeLineRadius = Math.cos(latitudeRadian);
            this.x = Math.sin(longitudeRadian) * latitudeLineRadius;
            this.y = Math.cos(longitudeRadian) * latitudeLineRadius;
            this.z = Math.sin(latitudeRadian);
        }

        private double getX() { return this.x; }
        private double getY() { return this.y; }
        private double getZ() { return this.z; }

        /**
         * Format the coordinates into the internal x-y-z-encoded representation
         */
        private String getEncoded() {
            final String encoded = String.format("%+20.17f%+20.17f%+20.17f", this.x, this.y, this.z);
            assert encoded.length() == FormulaGeolocationService.DEFAULT_XYZ_LENGTH;
            return encoded;

        }
    }

    /**
     * Returns sql expressions for the x, y, and z components given a sql expression for the internal x-y-z-encoded representation.
     * Because the internal x-y-z-encoded representation uses fixed-widths for each of the 3 components, we only need to use SUBSTR() to extract each portion.
     */
    @Override
    public String[] getXyzStrings(final String xyzEncoded) {
        return new String[] { "TO_NUMBER(SUBSTR(" + xyzEncoded + ",1,20))", "TO_NUMBER(SUBSTR(" + xyzEncoded + ",21,20))", "TO_NUMBER(SUBSTR(" + xyzEncoded + ",41))" };
    }

    /**
     * Returns sql expressions for the x, y, and z components given a pair of latitude and longitued values.
     */
    @Override
    public String[] getXyzStrings(final double latitude, final double longitude) {
        final String encoded = new LocationXYZ(latitude, longitude).getEncoded();
        return new String[] { encoded.substring(0, 20), encoded.substring(20, 40), encoded.substring(40) };
    }

    /**
     * Returns sql expressions for the x, y, and z components given a pair of latitude and longitude sql expressions.
     */
    @Override
    public String[] getXyzStrings(final String latitude, final String longitude) {
        // Sql expressions to convert latitude and longitude into 3-dimensional coordinates.
        // They are the same as LocationXYZ, except that they work for sql expressions, not numbers.
        final String xExpr = "sin((" + longitude + ")*" + RADIAN_FACTOR + ")*cos((" + latitude + ")*" + RADIAN_FACTOR + ")";
        final String yExpr = "cos((" + longitude + ")*" + RADIAN_FACTOR + ")*cos((" + latitude + ")*" + RADIAN_FACTOR + ")";
        final String zExpr = "sin((" + latitude + ")*" + RADIAN_FACTOR + ")";
        return new String[] { xExpr, yExpr, zExpr };
    }

    private static final double RADIAN_FACTOR = Math.PI / 180.0;

    /**
     * Computes the distance between the two locations in the provided unit.  If either of the provided locations
     * proves to be incomplete (null x,y), then <code>null</code> will be returned.
     */
    @Override
    public Double computeDistance(final FormulaGeolocation location, final FormulaGeolocation otherLocation, final DistanceUnit unit) {
        if (validateLocation(location) && validateLocation(otherLocation)) {
            final LocationXYZ loc1 = new LocationXYZ(location.getLatitude().doubleValue(), location.getLongitude().doubleValue());
            final LocationXYZ loc2 = new LocationXYZ(otherLocation.getLatitude().doubleValue(), otherLocation.getLongitude().doubleValue());
            final double xDelta = loc1.getX() - loc2.getX();
            final double yDelta = loc1.getY() - loc2.getY();
            final double zDelta = loc1.getZ() - loc2.getZ();
            final double straightLineDistance = Math.hypot(Math.hypot(xDelta, yDelta), zDelta);
            
            return unit.getEarthMeanDiameter() * Math.asin(straightLineDistance / 2.0d);
        } else { 
            return null;
        }
    }

    /**
     * Returns a SQL expression that computes the distance between the given locations provided in xyz-coordinates.
     */
    @Override
    public String generateDistanceSqlExpression(final String x1Expr, final String y1Expr, final String z1Expr, final String x2Expr, final String y2Expr, final String z2Expr, final String diameterExpression) {
        final String squareEuclidianDistance = generateFilterSortDistanceLhs(x1Expr, y1Expr, z1Expr, x2Expr, y2Expr, z2Expr);
        
        return "(" + diameterExpression + "*ASIN(SQRT(" + squareEuclidianDistance + ")/2))";
    }

    /**
     * Returns a SQL expression that computes the filter/sort distance function between the given locations provided
     * in xyz-coordinates.  This does not compute the actual distance.  Instead, it is only used for filtering and sorting,
     * and used in conjunction with generateFilterSortDistanceRhs() below.  When filtering, this method provides the
     * expression for the left hand side of the filter condition (which contains references to db columns), and method
     * generateFilterSortDistanceRhs() provides the absolute value for the right hand side of the filter condition (which
     * is a constant).  When sorting, just sort on this sql expression, which is faster to evaluate than the actual distance.
     *
     * The SQL expression computes the square of the euclidian_distance_in_radius_of_the_earth defined above.
     */
    @Override
    public String generateFilterSortDistanceLhs(final String x1Expr, final String y1Expr, final String z1Expr, final String x2Expr, final String y2Expr, final String z2Expr) {
        return "(POWER(" + x1Expr + "- " + x2Expr + ",2)+POWER(" + y1Expr + "- " + y2Expr + ",2)+POWER(" + z1Expr + "- " + z2Expr + ",2))";
    }

    /**
     * Computes the filter/sort distance function for the given distance and unit of distance.  This is used in conjunction
     * with generateSingleSidedDistanceSqlExpression() when filtering.
     *
     * This computes the square of the euclidian_distance_in_radius_of_the_earth from the arc_distance and the radius_of_earth.
     */
    @Override
    public double generateFilterSortDistanceRhs(final double distance, final DistanceUnit distanceUnit) {
        if (distance <= 0) {
            // treat negative distances as zero
            return 0.0d;
        } else if (distance >= Math.PI * distanceUnit.getEarthMeanDiameter() / 2.0d) {
            // check for overflow, ie when given arc distance > 1/2 earth's circumference
            return 4.0d; // substitute 1 for sin(...) below
        } else {
            final double euclidianDistance = 2.0d * Math.sin(distance / distanceUnit.getEarthMeanDiameter());
            return euclidianDistance * euclidianDistance;
        }
    }
    
    /**
     * Returns whether the provided {@link GeoLocation} is valid.  Valid is defined as the location
     * having a non-null longitude and latitude.
     *  
     * @param location the location to validate.
     * @return whether the location is valid.
     */
    private boolean validateLocation(FormulaGeolocation location) {
        boolean isValid = true;
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            isValid = false;
        }
        
        return isValid;
    }
    
    
    /*
     * extracted from http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates
     */
    @Override
    public FormulaGeolocation[] getBoundingBox(FormulaGeolocation location, double distance, DistanceUnit distanceUnit)  {
        double radLat = Math.toRadians(location.getLatitude().doubleValue());
        double radLon = Math.toRadians(location.getLongitude().doubleValue());

        // angular distance in radians on a great circle
        double radDist = 2.0d * distance / distanceUnit.getEarthMeanDiameter();

        double minLat = radLat - radDist;
        double maxLat = radLat + radDist;
        double minLon, maxLon;
        if (minLat > MIN_LAT && maxLat < MAX_LAT) {
            double deltaLon = Math.asin(Math.sin(radDist) /
                    Math.cos(radLat)) + EPSILON;
            minLon = radLon - deltaLon;
            maxLon = radLon + deltaLon;
        } else {
            // a pole is within the distance
            minLat = Math.max(minLat, MIN_LAT);
            maxLat = Math.min(maxLat, MAX_LAT);
            minLon = MIN_LON;
            maxLon = MAX_LON;
        }
        
        if (minLon < MIN_LON || maxLon > MAX_LON)  {
            // longitudes cross the 180 meridian
            minLon = MIN_LON;
            maxLon = MAX_LON;
        }
        
        // if the distance is 0, we don't need to recompute the latitude value
        // as openJDK11 will have different values returned after Math.* functions
        if (radDist == 0) {
            return new FormulaGeolocation[] {new GeoLocationImpl(location.getLatitude().doubleValue(), Math.toDegrees(minLon)), new GeoLocationImpl(location.getLatitude().doubleValue(), Math.toDegrees(maxLon))};
        }

        return new FormulaGeolocation[] {new GeoLocationImpl(Math.toDegrees(minLat), Math.toDegrees(minLon)), new GeoLocationImpl(Math.toDegrees(maxLat), Math.toDegrees(maxLon))};
    }
    
    /**
     * Sample FormulaGeolocation used for returning the bounding box.
     * @author stamm
     */
    static final class GeoLocationImpl implements FormulaGeolocation {
        private final Number latitude;
        private final Number longitude;

        GeoLocationImpl(Number latitude, Number longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override public Number getLatitude() { return this.latitude; }
        @Override public Number getLongitude() { return this.longitude; }
    }
}