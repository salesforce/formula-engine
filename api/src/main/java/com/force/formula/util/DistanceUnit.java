package com.force.formula.util;

import com.force.formula.FormulaGeolocation;

/**
 * Enum for the different types of unit that are support for distance computation.
 *
 * @author ahersans
 * @see FormulaGeolocation
 * @see FormulaGeolocationService
 */
public enum DistanceUnit {
    Mile("mi", 3958.761),
    Kilometer("km", 6371.009),
    ;

    private final double diameter;
    private final String abbrev;

    private DistanceUnit(String abbrev, double radius) {
        this.abbrev = abbrev;
        this.diameter = 2 * radius;
    }

    public String getAbbreviation() {
        return this.abbrev;
    }

    public double getEarthMeanDiameter() {
        return this.diameter;
    }

    public double getMaxDistanceOnEarth() {
        return Math.PI * getEarthMeanDiameter() / 2.0d;
    }
    
    public static DistanceUnit getUnitByName(String name) {
        if ("mi".equalsIgnoreCase(name))
            return Mile;
        else if ("km".equalsIgnoreCase(name))
            return Kilometer;
        else
            return null;
    }
}