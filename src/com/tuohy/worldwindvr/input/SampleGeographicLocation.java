package com.tuohy.worldwindvr.input;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import gov.nasa.worldwind.geom.LatLon;
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
	
	public static SampleGeographicLocation fromLine(String sglstring) {
		StringTokenizer tks = new StringTokenizer(sglstring,",");
		if (tks.countTokens()>=4) {
			String name = tks.nextToken();
			double lat = (new Double(tks.nextToken())).doubleValue();
			double lon = (new Double(tks.nextToken())).doubleValue();
			double elevation = (new Double(tks.nextToken())).doubleValue();
			LatLon ll = LatLon.fromDegrees(lat,lon);
			Position p = new Position(ll,elevation);
			if (tks.hasMoreTokens()) {
				System.err.println(tks.countTokens() + " extra fields when parsing location string: " + sglstring);
			}
			return new SampleGeographicLocation(name,p);
		}
		//FIXME: throw an exception instead?
		System.err.println("Too few fields when parsing location string, skipping:\n" + sglstring);
		return null;
	}
	
	public static ArrayList<SampleGeographicLocation> fromFileLocation(String filename) {
		ArrayList<SampleGeographicLocation> al = new ArrayList<SampleGeographicLocation>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			try {
				String l = br.readLine();
				while (l!=null) {
					if (! l.startsWith("#")) { // ignore comments
						SampleGeographicLocation sgl = fromLine(l);
						if (sgl != null) {
							al.add(sgl);
						}						
					}
					l = br.readLine();
				}
				br.close();
			} catch (IOException ioe) {
				System.out.println("IOException while processing locations file: " + filename + "\n" + ioe);
			}
		} catch (FileNotFoundException fnf) {
			System.out.println("File "+filename+" not found");
			return al;
		}
		
		return al;
	}
	
}
