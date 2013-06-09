package com.tuohy.worldwindvr;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.StereoOptionSceneController;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.AWTInputHandler;
import gov.nasa.worldwind.awt.ViewInputHandler;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.ScreenImage;
import gov.nasa.worldwind.view.firstperson.BasicFlyView;
import gov.nasa.worldwind.view.firstperson.FlyToFlyViewAnimator;
import gov.nasa.worldwind.view.firstperson.FlyViewInputHandler;

import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.tuohy.worldwindvr.scratch.TestFlyView;


/**
 * Launcher for the Rift-Compatible WorldWind Virtual Reality application.
 * 
 * @author dtuohy
 *
 */
public class WorldWindVR{


	// the first-person view
	public static BasicFlyView view;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Configuration.setValue("gov.nasa.worldwind.avkey.SceneControllerClassName","com.tuohy.worldwindvr.OculusStereoSceneController");
		System.setProperty("gov.nasa.worldwind.stereo.mode", "redblue");
		view = new VRFlyView();
		
		//uncomment below to get non-rift stereo 3D
//		Configuration.setValue("gov.nasa.worldwind.avkey.SceneControllerClassName","com.tuohy.worldwindvr.scratch.StereoOptionSceneControllerTest");
//		System.setProperty("gov.nasa.worldwind.stereo.mode", "device");		
//		view = new TestFlyView();
		
		Frame frame = new Frame("WorldwindFull");
		final WorldWindowGLCanvas worldWindowGLCanvas = new WorldWindowGLCanvas();
		worldWindowGLCanvas.setModel(new BasicModel());
		worldWindowGLCanvas.setView(view);
//		view.setViewInputHandler(new FlyViewInputHandler());

		worldWindowGLCanvas.addKeyListener(new java.awt.event.KeyListener() {
			public void keyTyped(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					System.exit(0);
				}
			}
		});

		frame.add(worldWindowGLCanvas);
		frame.setSize(640, 480);
		frame.setUndecorated(true);
		int size = frame.getExtendedState();
		size |= Frame.MAXIMIZED_BOTH;
		frame.setExtendedState(size);

		frame.setVisible(true);
		worldWindowGLCanvas.requestFocus();
		
		//set up a reasonable initial camera orientation and globe location.

        // Set view heading, pitch and fov
        view.setHeading(Angle.fromDegrees(0));
        view.setPitch(Angle.fromDegrees(89));
        view.setFieldOfView(Angle.fromDegrees(45));
        view.setRoll(Angle.fromDegrees(0));
        //view.setZoom(0);

        Position pos = new Position(new LatLon(Angle.fromDegrees(45), Angle.fromDegrees(-120)), 2000);
        view.setEyePosition(pos);
	}


}
