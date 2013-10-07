package com.tuohy.worldwindvr.input;

import gov.nasa.worldwind.geom.Position;

/**
 * Describes an example location for use with WorldWindVR
 * @author dtuohy
 *
 */
public class SampleGeographicLocation {

	String locationName;
	Position position;

	public SampleGeographicLocation(String locationName, Position pos){
		this.locationName = locationName;
		this.position = pos;
	}
	
	public String getLocationName() {
		return locationName;
	}
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}
	public Position getPosition() {
		return position;
	}
	public void setPosition(Position position) {
		this.position = position;
	}
	
}
