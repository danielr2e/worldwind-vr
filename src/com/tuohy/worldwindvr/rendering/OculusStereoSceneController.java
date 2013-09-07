package com.tuohy.worldwindvr.rendering;

import static javax.media.opengl.GL.GL_COLOR_BUFFER_BIT;
import static javax.media.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static javax.media.opengl.GL.GL_DEPTH_TEST;
import static javax.media.opengl.GL.GL_FRAMEBUFFER;
import static javax.media.opengl.GL.GL_TEXTURE_2D;
import static javax.media.opengl.GL2.GL_ENABLE_BIT;
import static javax.media.opengl.GL2.GL_TRANSFORM_BIT;
import static javax.media.opengl.GL2.GL_VIEWPORT_BIT;
import static javax.media.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static javax.media.opengl.GL2ES2.GL_VERTEX_SHADER;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;
import gov.nasa.worldwind.BasicSceneController;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.StereoSceneController;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilitiesImmutable;

import com.tuohy.worldwindvr.WorldWindVR;
import com.tuohy.worldwindvr.WorldWindVRConstants;
import com.tuohy.worldwindvr.input.VRFlyView;

import de.fruitfly.ovr.HMDInfo;
import de.fruitfly.ovr.OculusRift;

/**
 * TODO: This file needs to be updated to implement "correct" stereo, as described at:
 * http://www.orthostereo.com/geometryopengl.html
 *  - May be the same as this: http://www.roadtovr.com/2013/04/25/vr-expert-to-oculus-rift-devs-make-sure-youre-doing-3d-right-5267
 *
 * A SceneController which renders WorldWind for the Oculus Rift (i.e. in Side-By-Side 3D with
 * barrel distortion).
 *
 *
 * @author dtuohy
 */
public class OculusStereoSceneController extends BasicSceneController implements StereoSceneController
{

	/**
	 * The proportion of the screen to actually render on.  The scene will be rendered in the 
	 * center but only on this fraction of the over all screen real estate (all else will be black).
	 * 
	 * 1.0 = render full screen
	 * 
	 * This interacts with the 'lens center' and 'scale' parameters in OculusRiftDistortionStrategy
	 * 
	 */
	public static final double SCREEN_RENDERABLE_AREA_RATIO = 0.75;

	/** The average radius of the earth, in meters */
	public static final double AVG_EARTH_RADIUS_METERS = 6371000;

	/**
	 * The default focus angle. May be specified in the World Wind configuration file as the
	 * <code>gov.nasa.worldwind.StereoFocusAngle</code> property. The default if not specified in the configuration is
	 * 1.6 degrees.
	 */
	protected static final double DEFAULT_FOCUS_ANGLE = Configuration.getDoubleValue(AVKey.STEREO_FOCUS_ANGLE, 1.6);

	/**
	 * Whether or not to apply barrel distortion, which inverts the Rift's pincushion distortion.
	 */
	private static final boolean USE_BARREL_DISTORTION_SHADER = true;

	/**
	 * This is the FOV appropriate for the Oculus Rift.
	 */
	protected static final double DEFAULT_FOV = 110.0;

	/**
	 *  The ratio of the current altitude to the interpupillary distance
	 * used during stereoscopic rendering.  Defining IPD as a ratio means
	 * that we can ensure a richer depth effect at any altitude (we are calling
	 * this 'dynamic hyperstereoscopy').  Lower = more exaggerated stereo
	 */
	private static final double ALTITUDE_TO_IPD_RATIO = 50.0;

	/**
	 * Indicates whether stereo is being applied, either because a stereo device is being used or a stereo mode is in
	 * effect. This field is included because the question is asked every frame, and tracking the answer via a boolean
	 * avoids the overhead of more complicated logic that determines the stereo-drawing implementation to call.
	 */
	protected boolean inStereo = true;

	/** The current stereo mode. May not be set to null; use {@link AVKey#STEREO_MODE_NONE} instead. */
	protected String stereoMode = AVKey.STEREO_MODE_NONE;
	/** The angle between eyes. Larger angles give increased 3D effect. */
	protected Angle focusAngle = Angle.fromDegrees(DEFAULT_FOCUS_ANGLE);
	/** Indicates whether left and right eye positions are swapped. */
	protected boolean swapEyes = false;
	/** Indicates the GL drawable capabilities. Non-null only after this scene controller draws once. */
	protected GLCapabilitiesImmutable capabilities;
	/** Indicates whether hardware device stereo is available. Valid only after this scene controller draws once. */
	protected boolean hardwareStereo = false;

	//the Frame that contains the UI
	WorldWindVR vrFrame;

	//contains logic for applying barrel distortion shader and lens offset
	A_DistortionStrategy distorter = new OculusRiftDistortionStrategy();

	//the interface to the rift
	OculusRift oculusRift;

	//shaders for applying barrel distortion (which is inverted by the rift's optics)
	protected int shader=0;
	protected int vertShader=0;
	protected int fragShader=0;

	//used for FBO, to which the scene is rendered before distortion
	protected int colorTextureID = -1;
	protected int framebufferID = -1;
	protected int depthRenderBufferID = -1;
	
	//these are the 'reference' angles, controlled by the mouse, from which the user can look around in the rift
	private double referencePitchAngleDegrees;
	private double referenceYawAngleDegrees;


	/** Constructs an instance and initializes its stereo mode to */
	public OculusStereoSceneController()
	{
		OculusRift.LoadLibrary(new File(System.getProperty("java.io.tmpdir")));
		oculusRift = new OculusRift();
		oculusRift.init();

		HMDInfo hmdInfo = oculusRift.getHMDInfo();
		System.out.println(hmdInfo);

		String stereo = System.getProperty(AVKey.STEREO_MODE);

		if ("redblue".equalsIgnoreCase(stereo))
			this.setStereoMode(AVKey.STEREO_MODE_RED_BLUE);
		else if ("device".equalsIgnoreCase(stereo))
			this.setStereoMode(AVKey.STEREO_MODE_DEVICE);
	}

	public void setStereoMode(String mode)
	{
		this.stereoMode = mode != null ? mode : AVKey.STEREO_MODE_NONE;

		// If device-implemented stereo is used, stereo is considered always in effect no matter what the stereo mode.
		this.inStereo = this.isHardwareStereo() || AVKey.STEREO_MODE_RED_BLUE.equals(this.stereoMode);
	}

	public String getStereoMode()
	{
		return this.stereoMode;
	}

	/**
	 * {@inheritDoc} The default focus angle is 1.6 degrees.
	 *
	 * @param a the left-right eye direction difference. If null, the angle is set to 0.
	 */
	public void setFocusAngle(Angle a)
	{
		this.focusAngle = a != null ? a : Angle.ZERO;
	}

	public Angle getFocusAngle()
	{
		return this.focusAngle;
	}

	public void setSwapEyes(boolean swapEyes)
	{
		this.swapEyes = swapEyes;
	}

	public boolean isSwapEyes()
	{
		return this.swapEyes;
	}

	public boolean isHardwareStereo()
	{
		return this.hardwareStereo;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * If the display device is providing stereo -- {@link #isHardwareStereo()} is <code>true</code> -- this method
	 * returns true even if the stereo mode is {@link AVKey#STEREO_MODE_NONE}. In this case, individual stereo images
	 * are drawn for left and right eyes in order to prevent a blurred scene.
	 */
	public boolean isInStereo()
	{
		return this.inStereo;
	}

	@Override
	protected void draw(DrawContext dc)
	{
		// Capture the capabilities actually in use.
		if (this.capabilities == null)
		{
			this.capabilities = dc.getGLContext().getGLDrawable().getChosenGLCapabilities();
			this.hardwareStereo = this.capabilities.getStereo();
			this.inStereo = this.isHardwareStereo() ? true : this.isInStereo();
		}

		// If stereo isn't to be applied, just draw and return.
		if (!isInStereo())
		{
			super.draw(dc);
			return;
		}
		this.doDrawStereoOculus(dc);        
	}

	/**
	 * Implement no stereo ("Mono") while using a stereo device.
	 * <p/>
	 * Note that this method draws the image twice, once to each of the left and right eye buffers, even when stereo is
	 * not in effect. This is to prevent the stereo device from drawing blurred scenes.
	 *
	 * @param dc the current draw context.
	 */
	protected void doDrawStereoNone(DrawContext dc)
	{
		// If running on a stereo device but want to draw a normal image, both buffers must be filled or the
		// display will be blurry.

		GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		gl.glDrawBuffer(GL2.GL_BACK_LEFT);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		super.draw(dc);

		gl.glDrawBuffer(GL2.GL_BACK_RIGHT);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		super.draw(dc);
	}

	/**
	 * Implement stereo using the SBS Oculus format.
	 *
	 * @param dc the current draw context.
	 */
	int frameCount = 0;
	long firstFrameTime = System.currentTimeMillis();
	protected void doDrawStereoOculus(DrawContext dc)
	{

		//print out the frame rate
		frameCount++;
		if(frameCount==20){
			frameCount = 0;
			double elapsed = System.currentTimeMillis() - firstFrameTime;
			System.out.println("FPS: " + 1.0/(elapsed/20000.0));
			firstFrameTime = System.currentTimeMillis();
		}

		GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		View dcView = dc.getView();

		//consult the oculus for the current camera orientation
		if(oculusRift.isInitialized()){
			oculusRift.poll();
			dcView.setHeading(Angle.fromDegrees(this.referenceYawAngleDegrees + oculusRift.getYawDegrees_LH()));
			dcView.setRoll(Angle.fromDegrees(oculusRift.getRollDegrees_LH()));
			dcView.setPitch((Angle.fromDegrees((this.referencePitchAngleDegrees-oculusRift.getPitchDegrees_LH())+90)));
			//		System.out.println(oculusRift.getYawDegrees_LH() + " " + oculusRift.getRollDegrees_LH() + " " + oculusRift.getPitchDegrees_LH()+90);
		}

//		printCameraPosAndOrientation(dcView);

		//set the FOV appropriate for the rift
		dcView.setFieldOfView(Angle.fromDegrees(DEFAULT_FOV));

		int w = (int)(dcView.getViewport().getFrame().getWidth());
		int h = (int)(dcView.getViewport().getFrame().getHeight());

		//initialize the frame buffer, into which we will render the view, and the barrel distortion shaders
		if(framebufferID<0){

			//creates the Frame Buffer Object into which the initial undistorted SBS scene is rendered off screen
			GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			int width = gd.getDisplayMode().getWidth();
			int height = gd.getDisplayMode().getHeight();

			//It appears I can essentially set the render resolution to whatever I want here, but it doesn't seem to affect
			//framerate - why is this?
			width = (int) WorldWindVRConstants.RenderHorizontalResolution;
			height = (int) WorldWindVRConstants.RenderVerticalResolution;

			this.initFBO(gl,width,height);

			//creates the shaders used for barrel distortion
			initShaders(gl, distorter.getBarrelVertexShaderSource(), distorter.getBarrelFragmentShaderSource());			distorter.initialize(gl, shader);;

			//hard code the viewport height and width (see method comment for reason why)
			((VRFlyView) dcView).hardCodeViewPortHeightAndWidth(width,height);
		}

		// Draw the scene in to the frame buffer
		gl.glPushAttrib(GL_TRANSFORM_BIT | GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT);
		//bind the framebuffer ...
		gl.glBindTexture(GL_TEXTURE_2D, 0);  //this was taken from DistortionCorrection
		gl.glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glPushAttrib(GL_VIEWPORT_BIT);

		//compute offset parameters for dynamic hyperstereoscopy (interpupillary gets higher with altitude)
		Position centerEyePos = new Position(dc.getView().getCurrentEyePosition(),dc.getView().getCurrentEyePosition().getElevation());
		double radiusAtAltitude = AVG_EARTH_RADIUS_METERS + centerEyePos.getAltitude();
		Double elevation = dc.getGlobe().getElevation(centerEyePos.getLatitude(), centerEyePos.getLongitude());
		double circumferenceAtAltitude = 2*Math.PI*radiusAtAltitude;
		double hyperStereoOffsetMeters = (centerEyePos.getAltitude() - elevation)/(ALTITUDE_TO_IPD_RATIO/2);

		//we have to account for roll by offsetting the camera vertically as well (e.g. if the head rolls to the 
		//right, the left eye should be 'high' and the right eye is 'low'.  So we actually have to compute both
		//lateral and vertical offsets from the 'total' offset
		double hyperStereoVerticalOffsetMeters = Math.sin(view.getRoll().radians)*hyperStereoOffsetMeters;
		double hyperStereoLateralOffsetMeters = Math.cos(view.getRoll().radians)*hyperStereoOffsetMeters;
		double hyperStereoLateralOffsetDegrees = (hyperStereoLateralOffsetMeters/circumferenceAtAltitude)*360;
		//		System.out.println("offsetting vertically " + hyperStereoVerticalOffsetMeters + " and laterally " + hyperStereoLateralOffsetMeters + " from " + hyperStereoOffsetMeters);

		//these are the values that will be used to enforce the interpupillary camera offsets
		Angle hyperStereoLateralOffset = Angle.fromDegrees(hyperStereoLateralOffsetDegrees);
		Angle rightEyeOffsetDir = dc.getView().getHeading().add(Angle.POS90);
		Angle leftEyeOffsetDir = dc.getView().getHeading().subtract(Angle.POS90);

		//validate interocular distance
		//		Vec4 p1 = dc.getGlobe().computePointFromPosition(rightEyePos);
		//		Vec4 p2 = dc.getGlobe().computePointFromPosition(leftEyePos);
		//		double dist = p1.distanceTo3(p2);
		//		System.out.println("dist is " + dist + " altitude is " + centerEyePos.getAltitude() + " hyper offset is " +hyperStereoOffset.getDegrees());

		//render into the FBO
		//left eye
		((VRFlyView) dcView).applyWithOffset(dc,leftEyeOffsetDir,centerEyePos,hyperStereoLateralOffset,hyperStereoVerticalOffsetMeters);
		vrFrame.getAnnotationsLayer().prepareForEye(true);
		//		System.out.println("Rendered left eye at " + dcView.getEyePosition());

		//determine the width/height of the actual 'renderable' canvas
		int renderableWidth = (int) (Math.sqrt(SCREEN_RENDERABLE_AREA_RATIO)*w);
		int renderableHeight = (int) (Math.sqrt(SCREEN_RENDERABLE_AREA_RATIO)*h);
		int xOffset = (w - renderableWidth);
		int yOffset = (h - renderableHeight)/2;
		gl.glViewport(xOffset,yOffset,renderableWidth,renderableHeight);
		super.draw(dc);

		//right eye
		// Move the view to the right eye, if we are doing true stereoscopy
		if(this.inStereo){
			((VRFlyView) dcView).applyWithOffset(dc,rightEyeOffsetDir,centerEyePos,hyperStereoLateralOffset,-hyperStereoVerticalOffsetMeters);
			vrFrame.getAnnotationsLayer().prepareForEye(false);
			//			System.out.println("Rendered right eye at " + dcView.getEyePosition());
		}
		try{
			gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
			gl.glViewport(xOffset+renderableWidth, yOffset, renderableWidth, renderableHeight);
			super.draw(dc);
		}
		finally
		{
			// Restore the original eye position
			//			dcView.setEyePosition(centerEyePos);
			//			dcView.apply(dc);
			((VRFlyView) dcView).applyWithOffset(dc,rightEyeOffsetDir,centerEyePos,Angle.fromDegrees(0),0);
		}

		//unbind the FBO
		gl.glPopAttrib();
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glPopAttrib();

		/** render FBO texture to screen with barrel distortion */

		gl.glMatrixMode(GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode(GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glPushAttrib(GL_ENABLE_BIT);

		if(USE_BARREL_DISTORTION_SHADER){
			gl.glUseProgram(shader);
		}
		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL_DEPTH_TEST);
		gl.glActiveTexture(GL.GL_TEXTURE0);

		//	        gl.glClearColor (1.0f, 0.0f, 0.0f, 0.5f);
		//	        gl.glClear (GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		gl.glBindTexture(GL.GL_TEXTURE_2D, colorTextureID);

		try
		{

			//left eye to screen
			distorter.renderHalfScreenTexturedQuad(gl, 0.0f, 0.0f, 0.5f, 1.0f, true);

			//right eye to screen
			distorter.renderHalfScreenTexturedQuad(gl, 0.5f, 0.0f, 0.5f, 1.0f, false);

		}
		finally
		{
			if(USE_BARREL_DISTORTION_SHADER){
				gl.glUseProgram(0);
			}
			gl.glEnable(GL_DEPTH_TEST);
			gl.glPopMatrix();
			gl.glMatrixMode(GL_MODELVIEW);
			gl.glPopMatrix();
			gl.glPopAttrib();
		}
	}

	private void printCameraPosAndOrientation(View dcView) {
		System.out.println("Current camera position and orientation:");
		System.out.println(" Heading: " + dcView.getHeading().getDegrees());
		System.out.println(" Pitch: " + dcView.getPitch().getDegrees());
		System.out.println(" Roll: " + dcView.getRoll().getDegrees());
		System.out.println(" Position: " + dcView.getEyePosition());
	}

	/**
	 * Initializes the FrameBufferObject into which the SBS scene will initially
	 * be rendered before being applied to the screen with distortion.
	 * 
	 * @param gl
	 * @param screenWidth
	 * @param screenHeight
	 */
	private void initFBO(GL2 gl, int screenWidth, int screenHeight) {

		int[] result = new int[1];
		gl.glGenFramebuffers(1, result, 0);
		framebufferID = result[0];
		gl.glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
		//allocate the colour texture ...
		gl.glGenTextures(1, result, 0);
		colorTextureID = result[0];
		//allocate render buffer
		gl.glGenRenderbuffers(1, result,0);
		depthRenderBufferID = result[0];

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, framebufferID);                                               

		// initialize color texture
		gl.glBindTexture(GL.GL_TEXTURE_2D, colorTextureID);                                                                  
		gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);                               
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8, screenWidth, screenHeight, 0,GL.GL_RGBA, GL.GL_UNSIGNED_INT, (java.nio.ByteBuffer) null); 

		gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,GL.GL_TEXTURE_2D, colorTextureID, 0);

		// initialize depth renderbuffer
		gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, depthRenderBufferID);                               
		gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL.GL_DEPTH_COMPONENT24, screenWidth, screenHeight);
		gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER,GL.GL_DEPTH_ATTACHMENT,GL.GL_RENDERBUFFER, depthRenderBufferID); 

		//set up done, rebind to the normal window buffer
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);                                                                    
	}

	/**
	 * Compiles and links the vertex and fragment shaders used to apply barrel distortion.
	 * @param gl
	 * @param vertexShader
	 * @param fragmentShader
	 */
	protected void initShaders(GL2 gl, String vertexShader, String fragmentShader) {
		shader=gl.glCreateProgram();

		vertShader=createVertShader(gl, vertexShader);
		fragShader=createFragShader(gl, fragmentShader);
		//        Util.checkGLError();

		if (vertShader != 0 && fragShader != 0) {
			gl.glAttachShader(shader, vertShader);
			gl.glAttachShader(shader, fragShader);

			gl.glLinkProgram(shader);
			int[] status = new int[1];
			gl.glGetObjectParameterivARB(shader, GL2.GL_LINK_STATUS, status, 0);
			if (GL.GL_FALSE == status[0]) {
				System.err.println("Error during link!");
			} else {
				gl.glValidateProgram(shader);
				gl.glGetObjectParameterivARB(shader, GL2.GL_VALIDATE_STATUS, status, 0);
				if (GL.GL_FALSE == status[0]) {
					System.err.println("error during validate!");
				}
				else{
					System.out.println("Shaders loaded");
				}
			}

		} else {
			System.out.println("No shaders");
			System.exit(0);
		}
	}

	private int createVertShader(GL2 gl, String vertexCode){
		vertShader=gl.glCreateShader(GL_VERTEX_SHADER);

		if (vertShader==0) {
			return 0;
		}

		gl.glShaderSource(vertShader, 1, new String[] { vertexCode }, new int[]{vertexCode.length()}, 0);
		gl.glCompileShader(vertShader);

		int[] status = new int[1];
		gl.glGetObjectParameterivARB(vertShader, GL2.GL_COMPILE_STATUS, status, 0);
		if (GL.GL_FALSE == status[0]) {
			System.err.println("Error during compile of vertShader!");
		}
		return vertShader;
	}

	private int createFragShader(GL2 gl, String fragCode){

		fragShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
		if (fragShader==0) {
			return 0;
		}

		gl.glShaderSource(fragShader, 1, new String[] { fragCode }, new int[]{fragCode.length()}, 0);
		gl.glCompileShader(fragShader);

		int[] status = new int[1];
		gl.glGetObjectParameterivARB(fragShader, GL2.GL_COMPILE_STATUS, status, 0);
		if (GL.GL_FALSE == status[0]) {
			System.err.println("Error during compile of fragShader!");
		}
		return fragShader;
	}

	public void setVrFrame(WorldWindVR worldWindVR) {
		this.vrFrame = worldWindVR;
	}
	
	public double getReferencePitchAngleDegrees() {
		return referencePitchAngleDegrees;
	}

	public void setReferencePitchAngleDegrees(double referencePitchAngleDegrees) {
		this.referencePitchAngleDegrees = referencePitchAngleDegrees;
	}

	public double getReferenceYawAngleDegrees() {
		return referenceYawAngleDegrees;
	}

	public void setReferenceYawAngleDegrees(double referenceYawAngleDegrees) {
		this.referenceYawAngleDegrees = referenceYawAngleDegrees;
	}
}
