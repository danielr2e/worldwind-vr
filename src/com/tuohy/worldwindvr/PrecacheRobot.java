package com.tuohy.worldwindvr;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.SurfacePolyline;
import gov.nasa.worldwindx.examples.ApplicationTemplate;

import com.tuohy.worldwindvr.input.WorldWindVRKeyboardListener;
import com.tuohy.worldwindvr.rendering.OculusStereoSceneController;

/**
 * Directs the camera through a path that causes WorldWind to load the imagery
 * data needed for the set of sample locations.
 * 
 * @author gtuohy
 *
 */
public class PrecacheRobot {

	private final boolean DEBUG_MODE_ON = false;
	RenderableLayer debugLayer = new RenderableLayer();
	SurfacePolyline debugLine = new SurfacePolyline();
	List<LatLon> debugLocations = Collections.synchronizedList(new ArrayList<LatLon>());

	//final double dtheta = 3.142/50; // pi/50 -> 100 updates per revolution
	// rem: 6378.137 km/earth radian. We want ~1km per revolution, so 1/100th of a km per update
	final double height = 1200; // meters above ground
	final double radiansPer100m = 1.0 / 63781.37;
	final double radiansPerMeter = 1.0/6378137;
	final double stepSize = 20; // meters
	WorldWindVR vrFrame;
	WorldWindVRKeyboardListener vrKey;
	OculusStereoSceneController sceneController;

	//parameters and state for the robot
	long startTime;
	LatLon focus;
	double radius = 100; // 100 + (dradius/2pi)theta
	double dradius = 500; // distance(m) between each loop of the spiral
	float millisPerStep = 20; //how often to take another step along the spiral
	Angle theta;
	Angle phi;
	Angle dtheta= Angle.fromRadians(stepSize / radius);
	Angle dphi = Angle.fromDegrees(1);
	float millisToSpendAtEachLoc = 80000;  //the amount of time to spend at each location before switching
	double focusElevation;
	//simply keeps track of how many times the robot has been consulted
	int intervalsCounter = 0;
	// reminder: dtheta = step size / radius

	//these parameters allow us to 'pick up where we left off' each time we return to a previously visited location
	int timesThroughAllLocations = 0;	//how many times we have cycled through all sample locations
	float stepsPerTimeAtLoc = millisToSpendAtEachLoc/millisPerStep;
	

	/**
	 * @param args
	 */
	public PrecacheRobot(WorldWindVRKeyboardListener vrKey, WorldWindVR vrFrame) {
		this.vrFrame = vrFrame;
		this.vrKey = vrKey;

		if(DEBUG_MODE_ON){
			ApplicationTemplate.insertBeforePlacenames(vrFrame.getWwd(), debugLayer);
			BasicShapeAttributes attr = new BasicShapeAttributes();
			attr.setOutlineWidth(3);
			attr.setOutlineMaterial(new Material(Color.CYAN));
			attr.setDrawOutline(true);
			debugLine.setAttributes(attr);
			debugLine.setLocations(debugLocations);
			debugLayer.addRenderable(debugLine);
		}
	}

//	class CameraLocation {
//		Angle latitude;
//		Angle longitude;
//		double elevation;
//
//		public Position toPosition() {
//			return new Position(this.latitude, this.longitude, this.elevation);
//		}
//
//		public LatLon toLatLon() {
//			return new LatLon(this.latitude, this.longitude);
//		}
//
//		public CameraLocation(LatLon latLon, double fixedHeight) {
//			this.latitude = latLon.getLatitude();
//			this.longitude = latLon.getLongitude();
//			//this.elevation = vrFrame.wwd.getView().getGlobe().getElevation(this.latitude, this.longitude) + height;
//			this.elevation = fixedHeight;
//			vrFrame.view.setEyePosition(this.toPosition());
//		}
//
//	}

	public Angle getCurrentHeading(){
		// Rotation
		// Basic method: rotate 2 degrees per update
		float numSteps = (System.currentTimeMillis() - startTime)/this.millisPerStep; 
		return Angle.fromDegrees(dphi.degrees*numSteps);
	}
	
	public Angle getCurrentPitch(){
		//default: just point 40 degrees from normal
		return Angle.fromDegrees(40);
	}

	public Position getCurrentPosition() {

		//switch to next location, if appropriate
		if((System.currentTimeMillis() - startTime)>this.millisToSpendAtEachLoc){
			this.resetRobotToNextLocation();
		}
		
		//the first term here adds the steps that were executed on previous visits to the current location
		float numSteps = (timesThroughAllLocations*stepsPerTimeAtLoc) + (System.currentTimeMillis() - startTime)/this.millisPerStep; 

		dtheta = Angle.fromRadians(stepSize / radius);
		theta = Angle.fromDegrees(dtheta.degrees*numSteps);
		radius = dradius + (dradius/(Math.PI * 2))*theta.radians;
		intervalsCounter++;
		//			if (theta.radians > (Math.PI * 2)) {
		//				if (DEBUG_MODE_ON) { System.out.println(theta.radians + ", " + dtheta.radians); }
		//				theta = Angle.ZERO;
		//				circleIndex += 1;
		//				dtheta = Angle.fromRadians(stepSize / (dradius*circleIndex));
		//				if (DEBUG_MODE_ON) { 
		//					System.out.println("Next Circle: " + circleIndex + ", " + dtheta.degrees); 
		//				}
		//			}			
		LatLon latlon = LatLon.greatCircleEndPosition(focus,theta,Angle.fromRadians(radius * radiansPerMeter));

		//updates a surface line with the new position, this line will show the entire camera path
		if(DEBUG_MODE_ON){

			//only add every 5th point so that the line doesn't get too many vertices
			if(intervalsCounter%5==0){
				debugLocations.add(latlon);
			}

			//only update the line every 10 iterations or so to save CPU
			if(debugLocations.size()%2==0){
				debugLine.setLocations(debugLocations);
			}
		}
		return Position.fromDegrees(latlon.latitude.degrees, latlon.longitude.degrees, focusElevation+height);
	}

	public static void test() {
		//		SampleGeographicLocation spot = new SampleGeographicLocation("The Grand Canyon",new double[]{110.12,60.11,36.19529915228048,-111.7481440380943,1530});
	}
	public static void main(String[] args) {
		//		SampleGeographicLocation spot = new SampleGeographicLocation("The Grand Canyon",new double[]{110.12,60.11,36.19529915228048,-111.7481440380943,1530});


	}
	public void start() {
		vrFrame.setRobotModeOn(true);
		vrFrame.getSampleLocationsProvider().reset();
		
		//Atlanta - easier for debugging
		resetRobotToNextLocation();

	}

	/**
	 * Take the robot to the next sample location.
	 */
	private void resetRobotToNextLocation() {
		startTime = System.currentTimeMillis();
		focus = new LatLon(vrFrame.getSampleLocationsProvider().getNextLocation().getPosition());//LatLon.fromDegrees(33.755,-84.39);
		theta = Angle.ZERO;
		phi = Angle.ZERO;
		focusElevation = vrFrame.wwd.getView().getGlobe().getElevation(focus.getLatitude(), focus.getLongitude());
		
		//when we get back to the original sample location, note that we have cycled through all locations one more time
		if(vrFrame.getSampleLocationsProvider().getCurLocIndex()==0){
			this.timesThroughAllLocations++;
		}
		if(DEBUG_MODE_ON){
			debugLocations.clear();
		}
	}

	public void end() {
		vrFrame.setRobotModeOn(false);
		vrFrame.getSampleLocationsProvider().reset();
	}

}
