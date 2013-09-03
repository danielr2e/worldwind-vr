package com.tuohy.worldwindvr;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Version;
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
public class WorldWindVR extends Frame{

	// the first-person view
	public static BasicFlyView view;

	//displays messages to the user
	private VRAnnotationsLayer annotationsLayer;

	public WorldWindVR(){
		super("WorldWindVR");
		System.out.println("Starting WorldWindVR with WorldWind " + Version.getVersion());
		
		Configuration.setValue("gov.nasa.worldwind.avkey.SceneControllerClassName","com.tuohy.worldwindvr.OculusStereoSceneController");
		System.setProperty("gov.nasa.worldwind.stereo.mode", "redblue");	//TODO: can we fix this so we don't have to specify a bogus redblue parameter?
		view = new VRFlyView();
		
		//uncomment below to get non-rift stereo 3D
//		Configuration.setValue("gov.nasa.worldwind.avkey.SceneControllerClassName","com.tuohy.worldwindvr.scratch.DynamicHyperstereoscopySceneController");
//		System.setProperty("gov.nasa.worldwind.stereo.mode", "device");		
//		view = new TestFlyView();
		final WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
		wwd.setModel(new BasicModel());
		wwd.setView(view);
		((OculusStereoSceneController)wwd.getSceneController()).setVrFrame(this);
//		view.setViewInputHandler(new FlyViewInputHandler());

		wwd.addKeyListener(new WorldWindVRKeyboardListener(this));
		
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
		
		//NOTE: this reduces framerate by about 25%, but that is WAY better than Bing Imagery, which reduces by 60% or more
		wwd.getModel().getLayers().getLayerByName("MS Virtual Earth Aerial").setEnabled(true);
		//This layer appears to be necessary to get the coarse-grained (Zoomed out) view
		wwd.getModel().getLayers().getLayerByName("Blue Marble May 2004").setEnabled(true);
		//disabling these two gets us about a 25% boost in framerate
		wwd.getModel().getLayers().getLayerByName("NASA Blue Marble Image").setEnabled(false);
		wwd.getModel().getLayers().getLayerByName("i-cubed Landsat").setEnabled(false);
		
		//TODO: this appears not to work when we render on the middle part of the screen with 
		//offsets (I guess it uses screen coordinates?, can we get it working again?
		wwd.getModel().getLayers().getLayerByName("Place Names").setEnabled(false);
		for(Layer l : wwd.getModel().getLayers()){
			System.out.println(l.getName() + " " + l.getClass() + " " + l.isEnabled());
		}
		
		//prepare annotations layer
		annotationsLayer = new VRAnnotationsLayer();
		wwd.getModel().getLayers().add(annotationsLayer);

		add(wwd);
		setUndecorated(true);
		int size = getExtendedState();
		size |= MAXIMIZED_BOTH;
		setExtendedState(size);

		//this causes worldwind to render at a given framerate (like a normal gaming application)
		//without it, worldwind will only repaint when something changes
		FPSAnimator animator = new FPSAnimator(wwd, 60);
		animator.add(wwd);
		animator.start();
		
		//set up a reasonable initial camera orientation and globe location.
//        cameraToCascades();
        cameraToGrandCanyon();
//        cameraToHalfDome();
        
        //show the application and request mouse/keyboard focus
		setVisible(true);
		wwd.requestFocus();
	}
	
	public VRAnnotationsLayer getAnnotationsLayer() {
		return annotationsLayer;
	}

	public static void main(String[] args) {
		new WorldWindVR();
	}
	
	private static void cameraToGrandCanyon() {
		double[] loc = new double[]{110.12,60.11,36.19529915228048,-111.7481440380943,1530};
		viewToLocation(loc);
	}

	protected static void viewToLocation(double[] loc) {
		view.setHeading(Angle.fromDegrees(loc[0]));
        view.setPitch(Angle.fromDegrees(loc[1]));
        view.setRoll(Angle.fromDegrees(0));
        view.setEyePosition(new Position(new LatLon(Angle.fromDegrees(loc[2]), Angle.fromDegrees(loc[3])), loc[4]));
	}
}
