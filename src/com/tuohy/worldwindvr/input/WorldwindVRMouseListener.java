package com.tuohy.worldwindvr.input;

import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import com.tuohy.worldwindvr.WorldWindVR;
import com.tuohy.worldwindvr.WorldWindVRConstants;
import com.tuohy.worldwindvr.rendering.OculusStereoSceneController;

/**
 * Implements VR mouse control.  The goal here is to give the user mouselook
 * in a way that integrates well with Oculus Rift free look.
 * 
 * @author dtuohy
 *
 */
public class WorldwindVRMouseListener implements MouseMotionListener {

	WorldWindVR vrFrame;

	//used to manipulate the mouse for first person control
	Robot robot;

	//display dimensions
	int halfWidth;
	int halfHeight;

	long lastMouseMoveTime;


	public WorldwindVRMouseListener(WorldWindVR vrFrame) {
		this.vrFrame = vrFrame;
		try {
			robot = new java.awt.Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		halfWidth = (int) (java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth()/2.0);
		halfHeight = (int) (java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight()/2.0);

		// Transparent 16 x 16 pixel cursor image.
		BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

		// Create a new blank cursor.
		Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
				cursorImg, new Point(0, 0), "blank cursor");

		// Set the blank cursor to the JFrame.
		vrFrame.setCursor(blankCursor);
	}

	public void mouseMoved(MouseEvent e) {

		OculusStereoSceneController c = vrFrame.getOculusSceneController();

		//change the yaw reference angle based on the mouse's x movement
		if(!vrFrame.isRobotModeOn()){
			float xMoveDistDegrees = (halfWidth - e.getX())*WorldWindVRConstants.MOUSE_MOVE_DEGREES_PER_PIXEL;	
			c.setReferenceYawAngleDegrees(normalizeDegrees(c.getReferenceYawAngleDegrees() - xMoveDistDegrees));
			
			//it is unclear if there is a good way to integrate y mouse look, since it permits strange contortions
			//of the view (e.g. flipping the head upside down)
			//		float yMoveDistDegrees = -(halfHeight - e.getY())*WorldWindVRConstants.MOUSE_MOVE_DEGREES_PER_PIXEL;
			//		c.setReferencePitchAngleDegrees(normalizeDegrees(c.getReferencePitchAngleDegrees() - yMoveDistDegrees));

			//restore the cursor to it's original position
			robot.mouseMove(halfWidth, halfHeight);
		
		}
		else{
			e.consume();
		}
	}

	/**
	 * Normalizes value to between 0 and 360;
	 * @param d
	 * @return
	 */
	private double normalizeDegrees(double d) {
		while(d>360){
			d -= 360;
		}
		while(d<0){
			d+=360;
		}
		return d;
	}

	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub

	}

}
