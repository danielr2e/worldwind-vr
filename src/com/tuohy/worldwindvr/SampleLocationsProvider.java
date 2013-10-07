package com.tuohy.worldwindvr;

import gov.nasa.worldwind.geom.Position;

import java.util.ArrayList;
import java.util.List;

import com.tuohy.worldwindvr.input.SampleGeographicLocation;

public class SampleLocationsProvider {

	List<SampleGeographicLocation> cameraLocations = new ArrayList<SampleGeographicLocation>();
	int curLoc = 0;
	
	public SampleLocationsProvider(){
		
		//TODO: These should be read from a file
		
		//sample locations which the user can switch between with the space bar
		cameraLocations.add(new SampleGeographicLocation("The Grand Canyon",Position.fromDegrees(36.19529915228048,-111.7481440380943,1530.0)));
		cameraLocations.add(new SampleGeographicLocation("Half Dome - Yosemite National Park",Position.fromDegrees(37.71666068247625,-119.5583673004536,1956.18)));
		cameraLocations.add(new SampleGeographicLocation("Cascades Volcano Range",Position.fromDegrees(46.4546216,-121.495883,1938.0)));
		cameraLocations.add(new SampleGeographicLocation("Chamonix - Aiguille du Midi",Position.fromDegrees(45.8786,6.8872,4250.0)));
		cameraLocations.add(new SampleGeographicLocation("Alpstein, Switzerland",Position.fromDegrees(47.2500,9.3333,2030.0)));
//		cameraLocations.add(new SampleGeographicLocation("Glencoe, Scotland",Position.fromDegrees(56.6828, 5.1060,1530.0)));
//		cameraLocations.add(new SampleGeographicLocation("Saint Mary's - Glacier National Park",new double[]{110.12,60.11,39.8358,-105.6469,1530}));
//		cameraLocations.add(new SampleGeographicLocation("Bora Bora",new double[]{110.12,60.11,-16.4944,151.7364,1530}));
//		locations.add(new SampleGeographicLocation("Niagara Falls",new double[]{110.12,60.11,43.08,-79.07,1530}));
	}

	public SampleGeographicLocation getNextLocation() {
		SampleGeographicLocation loc = cameraLocations.get(curLoc);
		curLoc++;
		if(curLoc>=cameraLocations.size()){
			curLoc = 0;
		}
		return loc;
	}

	/**
	 * Resets the provider to the first location.
	 */
	public void reset() {
		curLoc = 0;
	}

	public int getCurLocIndex() {
		return curLoc;
	}

}
