package com.tuohy.worldwindvr;

import javax.media.opengl.GL2;

/**
 * Abstract base class for implementations of the VR distortion.  Implementations
 * should provide shader source as well as a method for rendering a half-screen
 * (per-eye) texture quad.
 * 
 * @author dtuohy
 *
 */
public abstract class A_DistortionStrategy {

	public abstract String getBarrelVertexShaderSource();
	
	public abstract String getBarrelFragmentShaderSource();

	public abstract void renderHalfScreenTexturedQuad(GL2 gl, float f, float g, float h, float i, boolean b);

	public abstract void initialize(GL2 gl, int shader);

}
