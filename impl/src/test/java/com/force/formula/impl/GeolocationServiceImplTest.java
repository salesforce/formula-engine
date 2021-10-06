/**
 * 
 */
package com.force.formula.impl;

import org.junit.Assert;

import com.force.formula.FormulaGeolocation;
import com.force.formula.FormulaGeolocation.GeolocationDisplayMode;
import com.force.formula.impl.GeolocationServiceImpl.GeoLocationImpl;
import com.force.formula.util.DistanceUnit;

import junit.framework.TestCase;

/**
 * @author ahersans
 */
public class GeolocationServiceImplTest extends TestCase {

	/**
	 * @param name
	 */
	public GeolocationServiceImplTest(String name) {
		super(name);
	}


    /**
     * test latitude/longitude bounding box of distances from Sfo
     */
    public void testBoundingBox() throws Exception {
        FormulaGeolocation sfo = new GeoLocationImpl(37.7749295, -122.4194155);
        FormulaGeolocation[] bbox =  getBoundingBox(sfo, 0, DistanceUnit.Mile);
        Assert.assertTrue(Math.abs(bbox[0].getLatitude().doubleValue() - 37.7749295) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[1].getLatitude().doubleValue() - 37.7749295) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[0].getLongitude().doubleValue() - -122.41942122957795) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[1].getLongitude().doubleValue() - -122.41940977042208) < GeolocationServiceImpl.EPSILON);
        
        
        bbox =  getBoundingBox(sfo, 10, DistanceUnit.Mile);
        Assert.assertTrue(Math.abs(bbox[0].getLatitude().doubleValue() - 37.63019790465216) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[1].getLatitude().doubleValue() - 37.91966109534784) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[0].getLongitude().doubleValue() - -122.60252782623061) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[1].getLongitude().doubleValue() - -122.23630317376941) < GeolocationServiceImpl.EPSILON);
        
        bbox =  getBoundingBox(sfo, 15, DistanceUnit.Mile);
        Assert.assertTrue(Math.abs(bbox[0].getLatitude().doubleValue() - 37.55783210697823) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[1].getLatitude().doubleValue() - 37.992026893021766) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[0].getLongitude().doubleValue() - -122.69408134384803) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[1].getLongitude().doubleValue() - -122.144749656152) < GeolocationServiceImpl.EPSILON);
        
        bbox =  getBoundingBox(sfo, 500, DistanceUnit.Mile);
        Assert.assertTrue(Math.abs(bbox[0].getLatitude().doubleValue() - 30.53834973260784) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[1].getLatitude().doubleValue() - 45.01150926739216) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[0].getLongitude().doubleValue() - -131.58952267467964) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[1].getLongitude().doubleValue() - -113.24930832532037) < GeolocationServiceImpl.EPSILON);
        
        // the distance crosses 180 meridian
        bbox =  getBoundingBox(sfo, 3000, DistanceUnit.Mile);
        Assert.assertTrue(Math.abs(bbox[0].getLatitude().doubleValue() - -5.644549104352972) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[1].getLatitude().doubleValue() - 81.19440810435297) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[0].getLongitude().doubleValue() - -180) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[1].getLongitude().doubleValue() - 180) < GeolocationServiceImpl.EPSILON);
        
        // distance crosses poles 
        // Alert,Nunavut northern most inhabited area
        FormulaGeolocation alertNunavut = new GeoLocationImpl(82.5014, -62.3389);
        bbox =  getBoundingBox(alertNunavut, 900, DistanceUnit.Kilometer);
        Assert.assertTrue(Math.abs(bbox[0].getLatitude().doubleValue() - 74.40751698056398) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[1].getLatitude().doubleValue() - 90) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[0].getLongitude().doubleValue() - -180) < GeolocationServiceImpl.EPSILON);
        Assert.assertTrue(Math.abs(bbox[1].getLongitude().doubleValue() - 180) < GeolocationServiceImpl.EPSILON);
        
    }
    
    private FormulaGeolocation[] getBoundingBox(FormulaGeolocation location, double distance, DistanceUnit distanceUnit) throws Exception  {
    	FormulaGeolocation[] bbox =  new GeolocationServiceImpl().getBoundingBox(location, distance, distanceUnit);
    	Assert.assertTrue("bounding box not of size 2", bbox.length == 2);
        Assert.assertTrue("Min Latitude is > Max Latitude", bbox[0].getLatitude().doubleValue() <= bbox[1].getLatitude().doubleValue());
        Assert.assertTrue("Min Longitude is > Max Longitude", bbox[0].getLongitude().doubleValue() <= bbox[1].getLongitude().doubleValue());
        
        return bbox;
    }
    
    public void testGetRepresentation() throws Exception {
        FormulaGeolocation sfo = new GeoLocationImpl(37.7749295, -122.4194155);
        Assert.assertEquals("37.7749295 -122.4194155", GeolocationServiceImpl.getInstance().getRepresentation(sfo, GeolocationDisplayMode.DecimalDegrees));
        Assert.assertEquals("37°46'30''N 122°25'10''W", GeolocationServiceImpl.getInstance().getRepresentation(sfo, GeolocationDisplayMode.DegreesMinutesSeconds));
        Assert.assertEquals("-0.66723275737906640-0.42375601391045736+0.61256125257401660", GeolocationServiceImpl.getInstance().computeXyzEncoded(sfo));
    }

}
