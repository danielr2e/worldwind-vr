package com.tuohy.worldwindvr.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import com.tuohy.worldwindvr.WorldWindVR;

public class WorldWindVRKeyboardListener implements KeyListener {

	WorldWindVR vrFrame;
	int curSpeed = 0;

	List<CameraSpeed> cameraSpeeds = new ArrayList<CameraSpeed>();

	public WorldWindVRKeyboardListener(WorldWindVR vrFrame){
		this.vrFrame = vrFrame;

		//camera speeds that the user can switch through with the shift key
		cameraSpeeds.add(new CameraSpeed(2.0, "Slow"));
		cameraSpeeds.add(new CameraSpeed(7.0, "Medium"));
		cameraSpeeds.add(new CameraSpeed(12.0, "Fast"));

	}

	public void keyPressed(KeyEvent e) {
		
		//any key press disabled imagery caching mode
		if(vrFrame.isRobotModeOn()){
			vrFrame.getRobot().end();
			vrFrame.getAnnotationsLayer().showMessageImmediately("WorldWindVR",3);
		}
		else{
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
			if (e.getKeyCode() == KeyEvent.VK_R){
				vrFrame.getRobot().start();
				vrFrame.getAnnotationsLayer().showMessageImmediately("In Imagery Caching Mode - press any key to exit", -1);
			}
		}
	}

	private void goToNextLocation() {
		SampleGeographicLocation loc = vrFrame.getSampleLocationsProvider().getNextLocation();
		vrFrame.viewToLocation(loc.getPosition());
		vrFrame.getAnnotationsLayer().showMessageImmediately(loc.getLocationName(), 4);
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
