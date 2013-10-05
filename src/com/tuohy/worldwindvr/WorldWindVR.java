package com.tuohy.worldwindvr;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Version;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;

import java.awt.Frame;

import javax.swing.UIManager;

import com.jogamp.opengl.util.FPSAnimator;
import com.tuohy.worldwindvr.input.VRFlyView;
import com.tuohy.worldwindvr.input.WorldWindVRKeyboardListener;
import com.tuohy.worldwindvr.input.WorldwindVRMouseListener;
import com.tuohy.worldwindvr.rendering.OculusStereoSceneController;
import com.tuohy.worldwindvr.scratch.Throwaway3dModelsLayer;


/**
 * Launcher for the Rift-Compatible WorldWind Virtual Reality application.
 * 
 * @author dtuohy
 *
 */
public class WorldWindVR extends Frame{

	//the worldwind OpenGL canvas
	WorldWindowGLCanvas wwd;
	
	// the first-person view
	public VRFlyView view;

	//displays messages to the user
	private VRAnnotationsLayer annotationsLayer;

	public WorldWindVR(){
		super("WorldWindVR");
		System.out.println("Starting WorldWindVR with WorldWind " + Version.getVersion());
		
		Configuration.setValue("gov.nasa.worldwind.avkey.SceneControllerClassName","com.tuohy.worldwindvr.rendering.OculusStereoSceneController");
		System.setProperty("gov.nasa.worldwind.stereo.mode", "redblue");	//TODO: can we fix this so we don't have to specify a bogus redblue parameter?
		view = new VRFlyView();
		
		//uncomment below to get non-rift stereo 3D
//		Configuration.setValue("gov.nasa.worldwind.avkey.SceneControllerClassName","com.tuohy.worldwindvr.scratch.DynamicHyperstereoscopySceneController");
//		System.setProperty("gov.nasa.worldwind.stereo.mode", "device");		
//		view = new TestFlyView();
		wwd = new WorldWindowGLCanvas();
		wwd.setModel(new BasicModel());
		wwd.setView(view);
		((OculusStereoSceneController)wwd.getSceneController()).setVrFrame(this);
//		view.setViewInputHandler(new FlyViewInputHandler());

		wwd.addKeyListener(new WorldWindVRKeyboardListener(this));
		wwd.addMouseMotionListener(new WorldwindVRMouseListener(this));
		
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
//		wwd.getModel().getLayers().getLayerByName("MS Virtual Earth Aerial").setExpiryTime(System.currentTimeMillis() );
		//This layer appears to be necessary to get the coarse-grained (Zoomed out) view
		wwd.getModel().getLayers().getLayerByName("Blue Marble May 2004").setEnabled(true);
		//disabling these two gets us about a 25% boost in framerate
		wwd.getModel().getLayers().getLayerByName("NASA Blue Marble Image").setEnabled(false);
		wwd.getModel().getLayers().getLayerByName("i-cubed Landsat").setEnabled(false);
//		wwd.getModel().getLayers().getLayerByName("Political Boundaries").setEnabled(true);
		
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

	public void addTest3dModelsLayer() {
		//prepare 3d Models layer
		new Throwaway3dModelsLayer(this);
	}
	
	public VRAnnotationsLayer getAnnotationsLayer() {
		return annotationsLayer;
	}

	public static void main(String[] args) {
		
		try {
	        // Set System L&F
	        UIManager.setLookAndFeel(
	            UIManager.getSystemLookAndFeelClassName());
	    } 
	    catch (Exception e){}
		
		LaunchDialog dialog = new LaunchDialog();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		
//		new WorldWindVR();
	}
	
	private void cameraToGrandCanyon() {
		double[] loc = new double[]{110.12,60.11,36.19529915228048,-111.7481440380943,1530};
		viewToLocation(loc);
	}

	public void viewToLocation(double[] loc) {
		view.setHeading(Angle.fromDegrees(loc[0]));
        view.setPitch(Angle.fromDegrees(loc[1]));
        view.setRoll(Angle.fromDegrees(0));
        view.setEyePosition(new Position(new LatLon(Angle.fromDegrees(loc[2]), Angle.fromDegrees(loc[3])), loc[4]));
	}

	public VRFlyView getView() {
		return view;
	}

	public WorldWindowGLCanvas getWwd() {
		return wwd;
	}

	public OculusStereoSceneController getOculusSceneController() {
		return (OculusStereoSceneController) wwd.getSceneController();
	}
}
