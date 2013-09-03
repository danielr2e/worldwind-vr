package com.tuohy.worldwindvr;

/**
 * Describes an example location for use with WorldWindVR
 * @author dtuohy
 *
 */
public class SampleGeographicLocation {

	String locationName;
	double[] locationParams;
	
	public SampleGeographicLocation(String locationName, double[] locationParams){
		this.locationName = locationName;
		this.locationParams = locationParams;
	}
	
	public String getLocationName() {
		return locationName;
	}
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}
	public double[] getLocationParams() {
		return locationParams;
	}
	public void setLocationParams(double[] locationParams) {
		this.locationParams = locationParams;
	}
	
}
