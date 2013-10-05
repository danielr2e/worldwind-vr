package com.tuohy.worldwindvr.input;


import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import com.tuohy.worldwindvr.WorldWindVR;

public class WorldWindVRKeyboardListener implements KeyListener {

	WorldWindVR vrFrame;
	int curLoc = 0;
	int curSpeed = 0;
	List<SampleGeographicLocation> cameraLocations = new ArrayList<SampleGeographicLocation>();
	List<CameraSpeed> cameraSpeeds = new ArrayList<CameraSpeed>();
	
	public WorldWindVRKeyboardListener(WorldWindVR vrFrame){
		this.vrFrame = vrFrame;
		
		//camera speeds that the user can switch through with the shift key
		cameraSpeeds.add(new CameraSpeed(2.0, "Slow"));
		cameraSpeeds.add(new CameraSpeed(7.0, "Medium"));
		cameraSpeeds.add(new CameraSpeed(12.0, "Fast"));
		
		//sample locations which the user can switch between with the space bar
		cameraLocations.add(new SampleGeographicLocation("Half Dome - Yosemite National Park",new double[]{-85.93,79.12,37.71666068247625,-119.5583673004536,1956.18}));
		cameraLocations.add(new SampleGeographicLocation("Cascades Volcano Range",new double[]{-13.29,90,46.4546216,-121.495883,1938}));
		cameraLocations.add(new SampleGeographicLocation("Chamonix - Aiguille du Midi",new double[]{110.12,60.11,45.8786,6.8872,4250}));
		cameraLocations.add(new SampleGeographicLocation("The Grand Canyon",new double[]{110.12,60.11,36.19529915228048,-111.7481440380943,1530}));
		cameraLocations.add(new SampleGeographicLocation("Alpstein, Switzerland",new double[]{110.12,60.11,47.2500,9.3333,1530}));
		cameraLocations.add(new SampleGeographicLocation("Glencoe, Scotland",new double[]{110.12,60.11,56.6828, 5.1060,1530}));
//		cameraLocations.add(new SampleGeographicLocation("Saint Mary's - Glacier National Park",new double[]{110.12,60.11,39.8358,-105.6469,1530}));
//		cameraLocations.add(new SampleGeographicLocation("Bora Bora",new double[]{110.12,60.11,-16.4944,151.7364,1530}));
//		locations.add(new SampleGeographicLocation("Niagara Falls",new double[]{110.12,60.11,43.08,-79.07,1530}));
	}
	
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			goToNextLocation();
		}
		if(e.getKeyCode() == KeyEvent.VK_F1){
			vrFrame.addTest3dModelsLayer();
		}
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			((VRFlyViewInputHandler)vrFrame.getView().getViewInputHandler()).setCameraTranslationSpeed(cameraSpeeds.get(curSpeed).speed);
			vrFrame.getAnnotationsLayer().showMessageImmediately("Camera Speed: " + cameraSpeeds.get(curSpeed).name, 4);
			curSpeed++;
			if(curSpeed>=cameraSpeeds.size()){
				curSpeed = 0;
			}
		}
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			System.exit(0);
		}
	}
	
	private void goToNextLocation() {
		vrFrame.viewToLocation(cameraLocations.get(curLoc).getLocationParams());
		vrFrame.getAnnotationsLayer().showMessageImmediately(cameraLocations.get(curLoc).getLocationName(), 4);
		curLoc++;
		if(curLoc>=cameraLocations.size()){
			curLoc = 0;
		}
	}

	public void keyTyped(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}
	
	private class CameraSpeed{
		double speed;
		String name;
		
		public CameraSpeed(double speed, String name){
			this.speed = speed;
			this.name = name;
		}
	}
}
