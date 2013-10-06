package com.tuohy.worldwindvr;

import java.util.Timer;
import java.util.TimerTask;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;

import com.tuohy.worldwindvr.input.SampleGeographicLocation;
import com.tuohy.worldwindvr.input.WorldWindVRKeyboardListener;
import com.tuohy.worldwindvr.rendering.OculusStereoSceneController;

public class PrecacheRobot {

	//final double dtheta = 3.142/50; // pi/50 -> 100 updates per revolution
	// rem: 6378.137 km/earth radian. We want ~1km per revolution, so 1/100th of a km per update
	final double height = 1000; // meters above ground
	final double radiansPer100m = 1.0 / 63781.37;
	final double stepSize = 10; // meters
	WorldWindVR vrFrame;
	WorldWindVRKeyboardListener vrKey;
	OculusStereoSceneController sceneController;
	/**
	 * @param args
	 */
	public PrecacheRobot(WorldWindVRKeyboardListener vrKey, WorldWindVR vrFrame) {
		this.vrFrame = vrFrame;
		this.vrKey = vrKey;
	}
	
	class CameraLocation {
		Angle latitude;
		Angle longitude;
		double elevation;
		
		public Position toPosition() {
			return new Position(this.latitude, this.longitude, this.elevation);
		}
		
		public LatLon toLatLon() {
			return new LatLon(this.latitude, this.longitude);
		}
		
		public CameraLocation(LatLon latLon) {
			this.latitude = latLon.getLatitude();
			this.longitude = latLon.getLongitude();
			this.elevation = vrFrame.wwd.getView().getGlobe().getElevation(this.latitude, this.longitude) + height;
			vrFrame.view.setEyePosition(this.toPosition());
		}
		
		public void move(Angle bearing, Angle distance) {
			LatLon newLatLon = LatLon.greatCircleEndPosition(this.toLatLon(),bearing,distance);
			this.update(newLatLon);
			vrFrame.view.setEyePosition(this.toPosition());
		}
		
		public void update(LatLon location) {
			this.latitude = location.getLatitude();
			this.longitude = location.getLongitude();
			vrFrame.view.setEyePosition(this.toPosition());
		}
		
	}

	class PrecacheTravelTask extends TimerTask {
		LatLon focus;
		CameraLocation cam;
		double curRadius; //meters
		Angle theta;
		Angle dtheta;
		// reminder: dtheta = step size / radius
		
		PrecacheTravelTask(LatLon focus) {
			this.focus = focus;
			curRadius = 1;
			theta = Angle.ZERO;
			dtheta = Angle.fromRadians(stepSize / 100*curRadius);
			cam = new CameraLocation(LatLon.greatCircleEndPosition(focus, Angle.ZERO, Angle.fromRadians(radiansPer100m)));			
		}
		
		public void run() {
			theta = theta.add(dtheta);
			if (theta.radians > (Math.PI * 2)) {
				theta = Angle.ZERO;
				curRadius += 1;
				dtheta = Angle.fromRadians(stepSize / 100*curRadius);
			}			
			cam = new CameraLocation(LatLon.greatCircleEndPosition(focus,theta,Angle.fromRadians(curRadius*radiansPer100m)));
		}
	}
	public static void test() {
		SampleGeographicLocation spot = new SampleGeographicLocation("The Grand Canyon",new double[]{110.12,60.11,36.19529915228048,-111.7481440380943,1530});
	}
	public static void main(String[] args) {
		SampleGeographicLocation spot = new SampleGeographicLocation("The Grand Canyon",new double[]{110.12,60.11,36.19529915228048,-111.7481440380943,1530});
		
		
	}
	public void start() {
		Timer pulse = new Timer();
		pulse.schedule(new PrecacheTravelTask(LatLon.fromDegrees(36.19529915228048,-111.7481440380943)), 0, 1000);
		// TODO Auto-generated method stub
		
	}
}
