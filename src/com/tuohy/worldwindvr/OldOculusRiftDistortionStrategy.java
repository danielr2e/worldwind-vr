package com.tuohy.worldwindvr;

import static javax.media.opengl.GL.GL_TRIANGLE_STRIP;

import javax.media.opengl.GL2;

public class OldOculusRiftDistortionStrategy extends A_DistortionStrategy {
	
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
		//NOTE: in 38LeinaD's original algorithm, this was simply w/h but was the result was not tall enough		
		float as = w/h;
		//smaller scaleFactor = bigger 
		float scaleFactor = 1.0f;
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
	
	public void initialize(GL2 gl, int shader) {
		LensCenterLocation = gl.glGetUniformLocation(shader, "LensCenter");
		ScreenCenterLocation = gl.glGetUniformLocation(shader, "ScreenCenter");
		ScaleLocation = gl.glGetUniformLocation(shader, "Scale");
		ScaleInLocation = gl.glGetUniformLocation(shader, "ScaleIn");
		HmdWarpParamLocation = gl.glGetUniformLocation(shader, "HmdWarpParam");
	}

	
}
