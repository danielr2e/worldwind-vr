package com.tuohy.worldwindvr;

/**
 * The shaders used to effect barrel distortion.  These were taken from the LWJGL implementation
 * posted to the oculusvr.com forums by 38LeinaD.
 * 
 * @author dtuohy
 *
 */
public class ShaderSource {

	//Original from 38leinaD
	public final static String VERTEX_SHADER_SOURCE_BARREL = 
			"void main() {\n" +
					"   gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
					"   gl_Position = gl_Vertex;\n" +
					"}";


//	public final static String VERTEX_SHADER_SOURCE_BARREL = 
//			"uniform mat4 g_WorldViewProjectionMatrix;" +
//			"attribute vec4 inPosition;" +
//			"attribute vec2 inTexCoord;" +
//			"varying vec2 texCoord;" +
//			"void main() {" +
//			"  vec2 pos = (g_WorldViewProjectionMatrix * inPosition).xy;" +
//			"gl_Position = vec4(pos, 0.0, 1.0);    " +
//			"texCoord = inTexCoord;}";
	

	
//Original from 38leinaD
//	protected final static String FRAGMENT_SHADER_SOURCE_BARREL = 
//			"uniform sampler2D tex;\n" +
//					"uniform vec2 LensCenter;\n" +
//					"uniform vec2 ScreenCenter;\n" +
//					"uniform vec2 Scale;\n" +
//					"uniform vec2 ScaleIn;\n" +
//					"uniform vec4 HmdWarpParam;\n" +
//					"\n" + 
//					"vec2 HmdWarp(vec2 texIn)\n" + 
//					"{\n" + 
//					"   vec2 theta = (texIn - LensCenter) * ScaleIn;\n" +
//					"   float  rSq= theta.x * theta.x + theta.y * theta.y;\n" +
//					"   vec2 theta1 = theta * (HmdWarpParam.x + HmdWarpParam.y * rSq + " +
//					"           HmdWarpParam.z * rSq * rSq + HmdWarpParam.w * rSq * rSq * rSq);\n" +
//					"   return LensCenter + Scale * theta1;\n" +
//					"}\n" +
//					"\n" +
//					"\n" +
//					"\n" + 
//					"void main()\n" +
//					"{\n" +
//					"   vec2 tc = HmdWarp(gl_TexCoord[0]);\n" +
//					"   if (any(notEqual(clamp(tc, ScreenCenter-vec2(0.25,0.5), ScreenCenter+vec2(0.25, 0.5)) - tc, vec2(0.0, 0.0))))\n" +
//					"       gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);\n" +
//					"   else\n" +
//					"       gl_FragColor = texture2D(tex, tc);\n" +
//					"}";
	
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
					"   if (any(notEqual(clamp(tc, ScreenCenter-vec2(0.5,0.5), ScreenCenter+vec2(0.5, 0.5)) - tc, vec2(0.0, 0.0))))\n" +
					"       gl_FragColor = vec4(0.5, 0.5, 0.5, 1.0);\n" +
					"   else\n" +
					"       gl_FragColor = texture2D(tex, tc);\n" +
					"}";
	
}
