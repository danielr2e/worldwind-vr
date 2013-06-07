/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package com.tuohy.worldwindvr;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import gov.nasa.worldwind.BasicSceneController;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.StereoSceneController;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.render.DrawContext;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.jogamp.common.nio.*;
import com.jogamp.newt.Display;
import com.jogamp.opengl.util.gl2.*;
import com.tuohy.worldwindvr.scratch.DistortionCorrection.Eye;

import static javax.media.opengl.GL2.*;

/**
 * TODO: This file needs to be updated to implement "correct" stereo, as described at:
 * http://www.orthostereo.com/geometryopengl.html
 *  - May be the same as this: http://www.roadtovr.com/2013/04/25/vr-expert-to-oculus-rift-devs-make-sure-youre-doing-3d-right-5267
 * 
 * TODO: actually, stereoscopy is a trick proposition in WorldWind because of their weird
 * camera positioning system.
 *
 * TODO: the eyes become very wonky with increasing pitch, we MUST adjust for this
 * 
 * TODO: need to apply barrel distortion
 *  - http://jmonkeyengine.org/forum/topic/oculus-rift-support/
 *  - https://developer.oculusvr.com/forums/viewtopic.php?f=20&t=88&p=1021&hilit=opengl#p1021
 *
 * A SceneController which renders WorldWind for the Oculus Rift (i.e. in Side-By-Side 3D with
 * barrel distortion)
 *
 * Note that shaders and bits of the rendering were adapted from 38LeinaD's LWJGL
 * example posted on the OculurVR.com forums on April 01, 2013: 
 * 
 * https://developer.oculusvr.com/forums/viewtopic.php?f=20&t=88&p=8976&hilit=lwjgl#p977
 *
 * @author dtuohy
 */
public class OculusStereoSceneController extends BasicSceneController implements StereoSceneController
{
	/**
	 * The default focus angle. May be specified in the World Wind configuration file as the
	 * <code>gov.nasa.worldwind.StereoFocusAngle</code> property. The default if not specified in the configuration is
	 * 1.6 degrees.
	 */
	protected static final double DEFAULT_FOCUS_ANGLE = Configuration.getDoubleValue(AVKey.STEREO_FOCUS_ANGLE, 1.6);

	/**
	 * This is the FOV appropriate for the Oculus Rift.
	 */
	protected static final double DEFAULT_FOV = 110.0;

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
	/**
	 * Indicates whether stereo is being applied, either because a stereo device is being used or a stereo mode is in
	 * effect. This field is included because the question is asked every frame, and tracking the answer via a boolean
	 * avoids the overhead of more complicated logic that determines the stereo-drawing implementation to call.
	 */
	protected boolean inStereo = false;


	//shaders for applying barrel distortion (which is inverted by the rift's optics)
	protected int shader=0;
	protected int vertShader=0;
	protected int fragShader=0;

	//used by the shader
	private int LensCenterLocation;
	private int ScreenCenterLocation;
	private int ScaleLocation;
	private int ScaleInLocation;
	private int HmdWarpParamLocation;

	//used for FBO, to which the scene is rendered before distortion
	protected int colorTextureID = -1;
	protected int framebufferID = -1;
	protected int depthRenderBufferID = -1;


	/** Constructs an instance and initializes its stereo mode to */
	public OculusStereoSceneController()
	{
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

	public static float K0 = 1.0f;
	public static float K1 = 0.22f;
	public static float K2 = 0.24f;
	public static float K3 = 0.0f;

	/**
	 * Implement stereo using the SBS Oculus format.
	 *
	 * @param dc the current draw context.
	 */
	protected void doDrawStereoOculus(DrawContext dc)
	{
		GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

		View dcView = dc.getView();

		//set the FOV appropriate for the rift
		dcView.setFieldOfView(Angle.fromDegrees(DEFAULT_FOV));

		int w = (int)(dcView.getViewport().getFrame().getWidth());
		int h = (int)(dcView.getViewport().getFrame().getHeight());

		//initialize the frame buffer, into which we will render the view
		if(framebufferID<0){
			this.initShaders(gl, VERTEX_SHADER_SOURCE, FRAGMENT_SHADER_SOURCE);
			//TODO: HARD CODED, but need to grab this from the display.  This FBO will be
			//the render target before the screen, to which we will apply the shaders
			this.initFBO(gl,1920,1080);
			LensCenterLocation = gl.glGetUniformLocation(shader, "LensCenter");
			ScreenCenterLocation = gl.glGetUniformLocation(shader, "ScreenCenter");
			ScaleLocation = gl.glGetUniformLocation(shader, "Scale");
			ScaleInLocation = gl.glGetUniformLocation(shader, "ScaleIn");
			HmdWarpParamLocation = gl.glGetUniformLocation(shader, "HmdWarpParam");
		}

		// Draw the left eye into the frame buffer

		//taken from github example
		gl.glPushAttrib(GL_TRANSFORM_BIT | GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT);
		//bind the framebuffer ...
		gl.glBindTexture(GL_TEXTURE_2D, 0);  //this was taken from DistortionCorrection
		gl.glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glPushAttrib(GL_VIEWPORT_BIT);

		//render into the FBO
		//left eye
		gl.glViewport(0,0,w,h);
		super.draw(dc);

		//right eye
		// Move the view to the right eye
		Angle viewHeading = dcView.getHeading();
		//TODO: applying the new heading appears to screw things up, stretches out the view?
		//need to fix this or no stereoscopy
		//		dcView.setHeading(dcView.getHeading().subtract(this.getFocusAngle()));
		//		System.out.println("applying!");
		//		dcView.apply(dc);
		try{
			gl.glViewport(w, 0, w, h);
			super.draw(dc);
		}
		finally
		{
			// Restore the original view heading
			dcView.setHeading(viewHeading);
			dcView.apply(dc);
		}

		//unbind the FBO
		gl.glPopAttrib();
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glPopAttrib();

		//attempt to render FBO to screen        	
		//left eye
		renderHalfScreenTexturedQuad(gl, this.colorTextureID, 0,0, w, h, true);

		//right eye
		renderHalfScreenTexturedQuad(gl, this.colorTextureID, 0,0, w, h, false);

		
		// Move the view to the right eye
		//		Angle viewHeading = dcView.getHeading();

		//TODO: for right now, we don't move the right-eye, and hence we're not really getting stereoscopy
		//It appears that there are several challenges to address in order to get stereo working correctly
		//in the Rift (or anywhere, for that matter).  My suspicion is that the trouble is caused by the
		//camera being positioned in the spherical coordinate system, and this making it very hard to 
		//offset the camera appropriately.  We either have to have math that accounts for that, or make
		//our own custom rendering logic that positions the OpenGL camera precisely as we want
		//		dcView.setHeading(dcView.getHeading().subtract(this.getFocusAngle()));
		//		dcView.apply(dc);

		//TO DELETE: This is the original code used to render into the window frame

		// Draw the right eye frame
		//		try
		//		{
		//			x=w;
		//			gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
		//			gl.glViewport(w, 0, w, h);
		//			super.draw(dc);
		//		}
		//		finally
		//		{
		//			// Restore the original view heading
		//			dcView.setHeading(viewHeading);
		//			dcView.apply(dc);
		//		}
	}

	/**
	 * Renders the texture with the given textureId to half of the screen, with
	 * a boolean indicating whether that half should be the left or right.
	 * 
	 * @param gl
	 * @param textureId
	 * @param left
	 */
	public void renderHalfScreenTexturedQuad(GL2 gl, int textureId,int x, int y, int w, int h, boolean left)
	{
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode(GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glPushAttrib(GL_ENABLE_BIT);

		try
		{
//			gl.glUseProgram(shader);
			gl.glEnable(GL.GL_TEXTURE_2D);
			gl.glDisable(GL_DEPTH_TEST);
			gl.glActiveTexture(GL.GL_TEXTURE0);
			gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);

			//TODO: adapted from Distortion Correction.  It seems like I have this all wrong, in terms of when the shader should be applied
			//This message on WW forums: http://forum.worldwindcentral.com/showthread.php?t=32175&highlight=shader
			// suggests that I should probably be rendering each eye to a TEXTURE (FBO), and then applying the shader to
			//that whole texture, THEN rendering that texture to the screen.  This actually seems to be exactly what DistortionCorrection
			// is doing so maybe I can just borrow more from that?
			//
			// AND maybe I don't have to even write the shader in JOGL!  Can I just load the OpenGL shader from this Git project?
			//  https://github.com/dghost/glslRiftDistort
			float as = w/h;

			float scaleFactor = 1.0f;

//			this.validate();
//			Util.checkGLError();

			float DistortionXCenterOffset;
			if (left) {
				DistortionXCenterOffset = 0.25f;
			}
			else {
				DistortionXCenterOffset = -0.25f;
			}

			gl.glUniform2f(LensCenterLocation, x + (w + DistortionXCenterOffset * 0.5f)*0.5f, y + h*0.5f);
			gl.glUniform2f(ScreenCenterLocation, x + w*0.5f, y + h*0.5f);
			gl.glUniform2f(ScaleLocation, (w/2.0f) * scaleFactor, (h/2.0f) * scaleFactor * as);;
			gl.glUniform2f(ScaleInLocation, (2.0f/w), (2.0f/h) / as);
			gl.glUniform4f(HmdWarpParamLocation, K0, K1, K2, K3);

//			System.out.println("ScaleLocation: " + ScaleLocation);
			
			//this actually renders the texture from the FBO into the screen
			if(left){
				gl.glBegin(GL_TRIANGLE_STRIP);
				gl.glTexCoord2f(0.0f, 0.0f);   gl.glVertex2f(-1.0f, -1.0f);
				gl.glTexCoord2f(0.5f, 0.0f);   gl.glVertex2f(0.0f, -1.0f);
				gl.glTexCoord2f(0.0f, 1.0f);   gl.glVertex2f(-1.0f, 1.0f);
				gl.glTexCoord2f(0.5f, 1.0f);   gl.glVertex2f(0.0f, 1.0f);
				gl.glEnd();
			}
			else{
				gl.glBegin(GL_TRIANGLE_STRIP);
				gl.glTexCoord2f(0.5f, 0.0f);   gl.glVertex2f(0.0f, -1.0f);
				gl.glTexCoord2f(1.0f, 0.0f);   gl.glVertex2f(1.0f, -1.0f);
				gl.glTexCoord2f(0.5f, 1.0f);   gl.glVertex2f(0.0f, 1.0f);
				gl.glTexCoord2f(1.0f, 1.0f);   gl.glVertex2f(1.0f, 1.0f);
				gl.glEnd();            

			}
		}
		finally
		{
//			gl.glUseProgram(0);
	        gl.glEnable(GL_DEPTH_TEST);
			
			gl.glPopMatrix();
			gl.glMatrixMode(GL_MODELVIEW);
			gl.glPopMatrix();
			gl.glPopAttrib();
			
			
		}
	}

	private void initFBO(GL2 gl, int screenWidth, int screenHeight) {
		//ORIGINAL from the lwjgl example
		//        framebufferID = glGenFramebuffers();                                                                                
		//        colorTextureID = glGenTextures();                                                                                               
		//        depthRenderBufferID = glGenRenderbuffers();       

		//REPLACEMENT from the jogl example (https://github.com/demoscenepassivist/SocialCoding/blob/master/code_demos_jogamp/src/framework/base/BaseFrameBufferObjectRendererExecutor.java)
		int[] result = new int[1];
		gl.glGenFramebuffers(1, result, 0);
		framebufferID = result[0];
		gl.glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
		//allocate the colour texture ...
		gl.glGenTextures(1, result, 0);
		colorTextureID = result[0];
		//allocate render buffer? This code is my guess, was not in original example	
		//		gl.glGenRenderbuffers(1, result, 0);
		//		depthRenderBufferID = result[0];
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

		//set up done, rebind to the normal window buffer?
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);                                                                    
	}

	protected void initShaders(GL2 gl, String vertexShader, String fragmentShader) {
		shader=gl.glCreateProgram();

		vertShader=createVertShader(gl, vertexShader);
		fragShader=createFragShader(gl, fragmentShader);
		//        Util.checkGLError();

		if (vertShader != 0 && fragShader != 0) {
			gl.glAttachShader(shader, vertShader);
			gl.glAttachShader(shader, fragShader);

			gl.glLinkProgram(shader);
			//            if (glGetProgram(shader, GL_LINK_STATUS) == GL_FALSE) {
			//                System.out.println("Linkage error");
			//                printLogInfo(shader);
			//                System.exit(0);
			//            }

			gl.glValidateProgram(shader);
			//            if (gl.glGetProgram(shader, GL_VALIDATE_STATUS) == GL_FALSE) {
			//                printLogInfo(shader);
			//                System.exit(0);
			//            }

		} else {
			System.out.println("No shaders");
			System.exit(0);
		}
		//        Util.checkGLError();
	}

	private int createVertShader(GL2 gl, String vertexCode){
		vertShader=gl.glCreateShader(GL_VERTEX_SHADER);

		if (vertShader==0) {
			return 0;
		}


		//        gl.glShaderSource(vertShader, vertexCode);
		gl.glShaderSource(vertShader, 1, new String[] { vertexCode }, (int[]) null, 0);
		gl.glCompileShader(vertShader);

		//        if (glGetShader(vertShader, GL_COMPILE_STATUS) == GL_FALSE) {
		//            printLogInfo(vertShader);
		//            vertShader=0;
		//        }
		return vertShader;
	}

	private int createFragShader(GL2 gl, String fragCode){

		fragShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
		if (fragShader==0) {
			return 0;
		}
		//        gl.glShaderSource(fragShader, fragCode);
		gl.glShaderSource(fragShader, 1, new String[] { fragCode }, (int[]) null, 0);
		gl.glCompileShader(fragShader);
		//        if (glGetShader(fragShader, GL_COMPILE_STATUS) == GL_FALSE) {
		//            printLogInfo(fragShader);
		//            fragShader=0;
		//        }
		return fragShader;
	}

	private final static String VERTEX_SHADER_SOURCE = 
			"void main() {\n" +
					"   gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
					"   gl_Position = gl_Vertex;\n" +
					"}";

	private final static String FRAGMENT_SHADER_SOURCE = 
			"uniform sampler2D tex;\n" +
					"uniform vec2 LensCenter;\n" +
					"uniform vec2 ScreenCenter;\n" +
					"uniform vec2 Scale;\n" +
					"uniform vec2 ScaleIn;\n" +
					"uniform vec4 HmdWarpParam;\n" +
					"\n" + 
					"vec2 HmdWarp(vec2 texIn)\n" + 
					"{\n" + 
					"   vec2 theta = (texIn - LensCenter) * ScaleIn;\n" +
					"   float  rSq= theta.x * theta.x + theta.y * theta.y;\n" +
					"   vec2 theta1 = theta * (HmdWarpParam.x + HmdWarpParam.y * rSq + " +
					"           HmdWarpParam.z * rSq * rSq + HmdWarpParam.w * rSq * rSq * rSq);\n" +
					"   return LensCenter + Scale * theta1;\n" +
					"}\n" +
					"\n" +
					"\n" +
					"\n" + 
					"void main()\n" +
					"{\n" +
					"   vec2 tc = HmdWarp(gl_TexCoord[0]);\n" +
					"   if (any(notEqual(clamp(tc, ScreenCenter-vec2(0.25,0.5), ScreenCenter+vec2(0.25, 0.5)) - tc, vec2(0.0, 0.0))))\n" +
					"       gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);\n" +
					"   else\n" +
					"       gl_FragColor = texture2D(tex, tc);\n" +
					"}";

}
