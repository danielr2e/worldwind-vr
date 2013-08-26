package com.tuohy.worldwindvr;

import static javax.media.opengl.GL.GL_TRIANGLE_STRIP;

import javax.media.opengl.GL2;

/**
 * Most of this code, including the distortion shaders and the procedure for offsetting the
 * rendering of the bound FBOs on to the screen, is taken from example code posted by 38LeinaD
 * on the oculusvr.com forums.
 * 
 * @author dtuohy
 *
 */
public class OculusRiftDistortionStrategy extends A_DistortionStrategy implements WorldWindVRConstants{

	public final static String VERTEX_SHADER_SOURCE_BARREL = 
			"void main() {\n" +
					"   gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
					"   gl_Position = gl_Vertex;\n" +
					"}";

	protected final static String FRAGMENT_SHADER_SOURCE_BARREL = 
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

	@Override
	public String getBarrelVertexShaderSource() {
		return VERTEX_SHADER_SOURCE_BARREL;
	}

	@Override
	public String getBarrelFragmentShaderSource() {
		return FRAGMENT_SHADER_SOURCE_BARREL;
	}

	//used by the shader
	private int LensCenterLocation;
	private int ScreenCenterLocation;
	private int ScaleLocation;
	private int ScaleInLocation;
	private int HmdWarpParamLocation;
	public static float K0 = 1.0f;
	public static float K1 = 0.22f;
	public static float K2 = 0.24f;
	public static float K3 = 0.0f;

	/**
	 * Renders the currently bound texture to half of the screen, with
	 * a boolean indicating whether that half should be the left or right.
	 * 
	 * @param gl
	 * @param textureId
	 * @param left
	 */
	public void renderHalfScreenTexturedQuad(GL2 gl, float x, float y, float w, float h, boolean left)
	{

		//compute the parameters for barrel distortion shader
		if (left) {
			gl.glUniform2f(LensCenterLocation, x + (w + LensCenter * 0.5f) * 0.5f, y + h*0.5f);
		}
		else {
			gl.glUniform2f(LensCenterLocation, x + (w - LensCenter * 0.5f) * 0.5f, y + h*0.5f);
		}
		float r = 1 + LensCenter;
		
		//NOTE: Original scale calculation in 38LeinaD's code commented out
//		float scale = distfunc(r);
		float scale = 1.1f;

		float scaleFactor = 1f/ scale;

		gl.glUniform2f(ScreenCenterLocation, x + w*0.5f, y + h*0.5f);
		gl.glUniform2f(ScaleLocation, (w/2.0f) * scaleFactor, (h/2.0f) * scaleFactor * AspectRatio);;
		gl.glUniform2f(ScaleInLocation, (2.0f/w), (2.0f/h) / AspectRatio);
		gl.glUniform4f(HmdWarpParamLocation, K0, K1, K2, K3);

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

	public static float distfunc(float r) {
		float r2 = r * r; // r^2
		float r4 = r2 * r2; // r^4
		float r6 = r4 * r2; // r^6
		return K0 + K1 * r2 + K2 * r4 + K3 * r6;
	}

	public void initialize(GL2 gl, int shader) {
		LensCenterLocation = gl.glGetUniformLocation(shader, "LensCenter");
		ScreenCenterLocation = gl.glGetUniformLocation(shader, "ScreenCenter");
		ScaleLocation = gl.glGetUniformLocation(shader, "Scale");
		ScaleInLocation = gl.glGetUniformLocation(shader, "ScaleIn");
		HmdWarpParamLocation = gl.glGetUniformLocation(shader, "HmdWarpParam");
	}


}
