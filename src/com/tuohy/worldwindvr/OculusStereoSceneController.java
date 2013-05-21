/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package com.tuohy.worldwindvr;

import java.awt.geom.Rectangle2D;

import gov.nasa.worldwind.BasicSceneController;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.StereoSceneController;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.render.DrawContext;

import javax.media.opengl.*;

/**
 * TODO: This file needs to be updated to implement "correct" stereo, as described at:
 * http://www.orthostereo.com/geometryopengl.html
 *  - May be the same as this: http://www.roadtovr.com/2013/04/25/vr-expert-to-oculus-rift-devs-make-sure-youre-doing-3d-right-5267
 * 
 * TODO: actually, stereoscopy is a trick proposition in WorldWind because of their weird
 * camera positioning system.  We need to get it working, period.
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

		int x = 0;
		int y = 0;
		int w = (int)(dcView.getViewport().getFrame().getWidth()/2.0);
		int h = (int)(dcView.getViewport().getFrame().getHeight());

		// Draw the left eye
		gl.glViewport(x, y, w, h);
		super.draw(dc);

		//TODO: adapted from Distortion Correction.  It seems like I have this all wrong, in terms of when the shader should be applied
		//This message on WW forums: http://forum.worldwindcentral.com/showthread.php?t=32175&highlight=shader
		// suggests that I should probably be rendering each eye to a TEXTURE (FBO), and then applying the shader to
		//that whole texture, THEN rendering that texture to the screen.  This actually seems to be exactly what DistortionCorrection
		// is doing so maybe I can just borrow more from that?
		//
		// AND maybe I don't have to even write the shader in JOGL!  Can I just load the OpenGL shader from this Git project?
		//  https://github.com/dghost/glslRiftDistort

		//		float scaleFactor = 1.0f;
		//		float DistortionXCenterOffset = 0f;
		//		float as = (float)w/(float)h;
		//		int LensCenterLocation;
		//		int ScreenCenterLocation;
		//		int ScaleLocation;
		//		int ScaleInLocation;
		//		int HmdWarpParamLocation;
		//
		//		int shader = 0;
		//		LensCenterLocation = gl.glGetUniformLocation(shader, "LensCenter");
		//		ScreenCenterLocation = gl.glGetUniformLocation(shader, "ScreenCenter");
		//		ScaleLocation = gl.glGetUniformLocation(shader, "Scale");
		//		ScaleInLocation = gl.glGetUniformLocation(shader, "ScaleIn");
		//		HmdWarpParamLocation = gl.glGetUniformLocation(shader, "HmdWarpParam");
		//		gl.glUniform2f(LensCenterLocation, x + (w + DistortionXCenterOffset * 0.5f)*0.5f, y + h*0.5f);
		//		gl.glUniform2f(ScreenCenterLocation, x + w*0.5f, y + h*0.5f);
		//		gl.glUniform2f(ScaleLocation, (w/2.0f) * scaleFactor, (h/2.0f) * scaleFactor * as);;
		//		gl.glUniform2f(ScaleInLocation, (2.0f/w), (2.0f/h) / as);
		//		gl.glUniform4f(HmdWarpParamLocation, K0, K1, K2, K3);
		//
		//
		//		gl.glBegin(GL2.GL_TRIANGLE_STRIP);
		//		gl.glTexCoord2f(0.0f, 0.0f);   
		//		gl.glVertex2f(-1.0f, -1.0f);
		//		gl.glTexCoord2f(0.5f, 0.0f);   
		//		gl.glVertex2f(0.0f, -1.0f);
		//		gl.glTexCoord2f(0.0f, 1.0f);   
		//		gl.glVertex2f(-1.0f, 1.0f);
		//		gl.glTexCoord2f(0.5f, 1.0f);   
		//		gl.glVertex2f(0.0f, 1.0f);
		//		gl.glEnd();


		// Move the view to the right eye
		Angle viewHeading = dcView.getHeading();

		//TODO: for right now, we don't move the right-eye, and hence we're not really getting stereoscopy
		//It appears that there are several challenges to address in order to get stereo working correctly
		//in the Rift (or anywhere, for that matter).  My suspicion is that the trouble is caused by the
		//camera being positioned in the spherical coordinate system, and this making it very hard to 
		//offset the camera appropriately.  We either have to have math that accounts for that, or make
		//our own custom rendering logic that positions the OpenGL camera precisely as we want
//		dcView.setHeading(dcView.getHeading().subtract(this.getFocusAngle()));
//		dcView.apply(dc);

		// Draw the right eye frame
		try
		{
			x=w;
			gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
			gl.glViewport(w, 0, w, h);
			super.draw(dc);
		}
		finally
		{
			// Restore the original view heading
			dcView.setHeading(viewHeading);
			dcView.apply(dc);
		}
	}

}
