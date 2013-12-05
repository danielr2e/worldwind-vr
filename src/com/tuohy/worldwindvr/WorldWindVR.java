package com.tuohy.worldwindvr;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Version;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;

import java.awt.Frame;

import javax.swing.UIManager;

import com.jogamp.opengl.util.FPSAnimator;
import com.tuohy.worldwindvr.input.VRFlyView;
import com.tuohy.worldwindvr.input.WorldWindVRKeyboardListener;
import com.tuohy.worldwindvr.input.WorldwindVRMouseListener;
import com.tuohy.worldwindvr.rendering.OculusStereoSceneController;
import com.tuohy.worldwindvr.rendering.VRMSVirtualEarthLayer;
import com.tuohy.worldwindvr.scratch.Throwaway3dModelsLayer;


/**
 * Launcher for the Rift-Compatible WorldWind Virtual Reality application.
 * 
 * @author dtuohy
 *
 */
public class WorldWindVR extends Frame{

	public enum InteractionMode{VR,ROBOT,MENU};

	//the worldwind OpenGL canvas
	WorldWindowGLCanvas wwd;

	// the first-person view
	public VRFlyView view;

	//displays messages to the user
	private VRAnnotationsLayer annotationsLayer;

	//whether to use the rift and interpret mouse movements
	InteractionMode currentMode = InteractionMode.VR;
	//robot that is used to precache imagery
	private PrecacheRobot robot;

	// displays messages, menus, cursor, etc.
	private VRHudLayer menuLayer;

	//custom hi-resolution imagery layer
	VRMSVirtualEarthLayer vrVirtualEarthLayer = new VRMSVirtualEarthLayer();

	//contains the locations that the user can rotate through
	SampleLocationsProvider sampleLocationsProvider = new SampleLocationsProvider();

	public WorldWindVR(boolean useHighResImagery, boolean startInPrecacheMode){
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
		WorldWindVRKeyboardListener vrkbl = new WorldWindVRKeyboardListener(this);
		wwd.addKeyListener(vrkbl);

		wwd.addMouseMotionListener(new WorldwindVRMouseListener(this));

		//add the VR Virtual Earth imagery layer
		if(useHighResImagery){
			wwd.getModel().getLayers().add(this.vrVirtualEarthLayer);
		}

		//TODO: For some reason Bing Imagery layer MURDERS frame rate, but 
		//the virtual earth aerial imagery does not (although much of the imagery
		//appears to be the same?  Do not know why.
		//From neilson: http://forum.worldwindcentral.com/showpost.php?p=103021&postcount=19
		//		For the Bing and other layers with more than a few levels:
		//			Code:
		//			    <ForceLevelZeroLoads>false</ForceLevelZeroLoads>
		//			    <RetainLevelZeroTiles>false</RetainLevelZeroTiles>
		//		 wwd.getModel().getLayers().add(new LayerManagerLayer(wwd));
		//		wwd.getModel().getLayers().getLayerByName("Bing Imagery").setEnabled(true);

		try{
			//NOTE: this reduces framerate by about 25%, but that is WAY better than Bing Imagery, which reduces by 60% or more
			wwd.getModel().getLayers().getLayerByName("MS Virtual Earth Aerial").setEnabled(false);
			//This layer appears to be necessary to get the coarse-grained (Zoomed out) view
			wwd.getModel().getLayers().getLayerByName("Blue Marble May 2004").setEnabled(true);
			//disabling these two gets us about a 25% boost in framerate
			wwd.getModel().getLayers().getLayerByName("NASA Blue Marble Image").setEnabled(false);
			wwd.getModel().getLayers().getLayerByName("i-cubed Landsat").setEnabled(false);

			//HUD elements that are not currently adapted to VR
			wwd.getModel().getLayers().getLayerByName("World Map").setEnabled(false);
			wwd.getModel().getLayers().getLayerByName("Scale bar").setEnabled(false);
			wwd.getModel().getLayers().getLayerByName("Compass").setEnabled(false);
			//		wwd.getModel().getLayers().getLayerByName("Political Boundaries").setEnabled(true);

			//TODO: this appears not to work when we render on the middle part of the screen with 
			//offsets (I guess it uses screen coordinates?, can we get it working again?
			wwd.getModel().getLayers().getLayerByName("Place Names").setEnabled(false);
			for(Layer l : wwd.getModel().getLayers()){
				System.out.println(l.getName() + " " + l.getClass() + " " + l.isEnabled());
			}
		}catch(Exception e){
			e.printStackTrace();
		}

		//prepare annotations layer
		//		annotationsLayer = new VRAnnotationsLayer();
		//		wwd.getModel().getLayers().add(annotationsLayer);
		menuLayer = new VRHudLayer();
		wwd.getModel().getLayers().add(menuLayer);
		robot = new PrecacheRobot(vrkbl, this);

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
		viewToLocation(this.sampleLocationsProvider.getNextLocation().getPosition());

		//show the application and request mouse/keyboard focus
		setVisible(true);
		wwd.requestFocus();

		//if requested, begin in precache mode
		if(startInPrecacheMode){
			Runnable r = new Runnable() {
				public void run() {
					//give the system a few seconds to start up
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					robot.start();
				}
			};

			new Thread(r).start();
		}
	}

	public void addTest3dModelsLayer() {
		//prepare 3d Models layer
		new Throwaway3dModelsLayer(this);
	}

	public VRAnnotationsLayer getAnnotationsLayer() {
		return annotationsLayer;
	}

	public VRHudLayer getMenuLayer() {
		return menuLayer;
	}

	public static void main(String[] args) {

		try {
			// Set System L&F
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Exception e){}

		LaunchDialog dialog = new LaunchDialog();
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}


	public void viewToLocation(Position pos) {
		view.setEyePosition(pos);
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

	public SampleLocationsProvider getSampleLocationsProvider() {
		return sampleLocationsProvider;
	}

	public PrecacheRobot getRobot() {
		return robot;
	}

	public InteractionMode getCurrentInteractionMode() {
		return this.currentMode;
	}

	public void setCurrentInteractionMode(InteractionMode mode) {
		this.currentMode = mode;
	}
}
