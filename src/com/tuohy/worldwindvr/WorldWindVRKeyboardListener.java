package com.tuohy.worldwindvr;


import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

public class WorldWindVRKeyboardListener implements KeyListener {

	WorldWindVR vrFrame;
	int curLoc = 0;
	List<SampleGeographicLocation> locations = new ArrayList<SampleGeographicLocation>();
	
	public WorldWindVRKeyboardListener(WorldWindVR vrFrame){
		this.vrFrame = vrFrame;
		locations.add(new SampleGeographicLocation("Half Dome - Yosemite National Park",new double[]{-85.93,79.12,37.71666068247625,-119.5583673004536,1956.18}));
		locations.add(new SampleGeographicLocation("Cascades Volcano Range",new double[]{-13.29,90,46.4546216,-121.495883,1938}));
		locations.add(new SampleGeographicLocation("The Grand Canyon",new double[]{110.12,60.11,36.19529915228048,-111.7481440380943,1530}));
	}
	
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			goToNextLocation();
		}
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			System.exit(0);
		}
	}
	
	private void goToNextLocation() {
		WorldWindVR.viewToLocation(locations.get(curLoc).getLocationParams());
		vrFrame.getAnnotationsLayer().showMessageImmediately(locations.get(curLoc).getLocationName(), 4);
		curLoc++;
		if(curLoc>=locations.size()){
			curLoc = 0;
		}
	}

	public void keyTyped(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}
}
