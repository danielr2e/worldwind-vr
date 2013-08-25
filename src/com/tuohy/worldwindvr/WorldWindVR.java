package com.tuohy.worldwindvr;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.view.firstperson.BasicFlyView;
import gov.nasa.worldwindx.examples.util.LayerManagerLayer;

import java.awt.Frame;
import java.awt.event.KeyEvent;

import javax.media.opengl.GLAutoDrawable;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;
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
		System.setProperty("gov.nasa.worldwind.stereo.mode", "redblue");	//TODO: can we fix this so we don't have to specify a bogus redblue parameter?
		view = new VRFlyView();
		
		//uncomment below to get non-rift stereo 3D
//		Configuration.setValue("gov.nasa.worldwind.avkey.SceneControllerClassName","com.tuohy.worldwindvr.scratch.DynamicHyperstereoscopySceneController");
//		System.setProperty("gov.nasa.worldwind.stereo.mode", "device");		
//		view = new TestFlyView();
		
		Frame frame = new Frame("WorldwindFull");
		final WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
		wwd.setModel(new BasicModel());
		wwd.setView(view);
//		view.setViewInputHandler(new FlyViewInputHandler());

		wwd.addKeyListener(new java.awt.event.KeyListener() {
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
		
		//TODO: For some reason Bing Imagerly layer MURDERS frame rate, but 
		//the virtual earth aerial imagery does not (although much of the imagery
		//appears to be the same?  Do not know why.
		//From neilson: http://forum.worldwindcentral.com/showpost.php?p=103021&postcount=19
//		For the Bing and other layers with more than a few levels:
//			Code:
//			    <ForceLevelZeroLoads>false</ForceLevelZeroLoads>
//			    <RetainLevelZeroTiles>false</RetainLevelZeroTiles>
//		 wwd.getModel().getLayers().add(new LayerManagerLayer(wwd));
//		wwd.getModel().getLayers().getLayerByName("Bing Imagery").setEnabled(true);
		wwd.getModel().getLayers().getLayerByName("MS Virtual Earth Aerial").setEnabled(true);
		
		//TODO: this appears not to work when we render on the middle part of the screen with 
		//offsets (I guess it uses screen coordinates?, can we get it working again?
		wwd.getModel().getLayers().getLayerByName("Place Names").setEnabled(false);
		for(Layer l : wwd.getModel().getLayers()){
			System.out.println(l.getName() + " " + l.getClass());
		}

		frame.add(wwd);
		frame.setUndecorated(true);
		int size = frame.getExtendedState();
		size |= Frame.MAXIMIZED_BOTH;
		frame.setExtendedState(size);

		frame.setVisible(true);
		wwd.requestFocus();

		//this causes worldwind to render at a given framerate (like a normal gaming application)
		//without it, worldwind will only repaint when something changes
		FPSAnimator animator = new FPSAnimator(wwd, 60);
		animator.add(wwd);
		animator.start();
		
		//set up a reasonable initial camera orientation and globe location.
//        cameraToCascades();
        cameraToGrandCanyon();
//        cameraToHalfDome();
	}

	private static void cameraToGrandCanyon() {
		view.setHeading(Angle.fromDegrees(59.10165621513766));
        view.setPitch(Angle.fromDegrees(78.11));
        view.setRoll(Angle.fromDegrees(0));
        view.setEyePosition(new Position(new LatLon(Angle.fromDegrees(35.97785295310992), Angle.fromDegrees(-111.98296612831203)), 2400));
	}

	private static void cameraToCascades() {
		view.setHeading(Angle.fromDegrees(-13.29));
        view.setPitch(Angle.fromDegrees(90));
        view.setRoll(Angle.fromDegrees(0));
        view.setEyePosition(new Position(new LatLon(Angle.fromDegrees(46.4546216), Angle.fromDegrees(-121.495883)), 1938));
	}
	
	private static void cameraToHalfDome() {
		view.setHeading(Angle.fromDegrees(-85.93005803264815));
        view.setPitch(Angle.fromDegrees(79.11571000079975));
        view.setRoll(Angle.fromDegrees(0));
        view.setEyePosition(new Position(new LatLon(Angle.fromDegrees(37.73131411140283), Angle.fromDegrees(-119.48923402888437)), 2600));
 	}

}
