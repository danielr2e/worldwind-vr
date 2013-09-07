package com.tuohy.worldwindvr;

/**
 * Holds various constants used by WorldWindVR.
 * 
 * @author dtuohy
 *
 */
public class WorldWindVRConstants {

	public static final float MOUSE_MOVE_DEGREES_PER_PIXEL = 0.08f;
	
	public static final float ScreenHorizontalResolution = 1280;
	public static final float ScreenVerticalResolution = 800;

	//sets the resolution at which the scene is actually rendered - can be used as for nice (though expensive) anti-aliasing
//	public static final float RenderHorizontalResolution = 1920;
//	public static final float RenderVerticalResolution = 1080;
	
	public static float RenderHorizontalResolution = 3840;
	public static float RenderVerticalResolution = 2160;
	
	public static float RenderSurfaceScale = 1.25f;

	public static float HScreenSize = 0.14976f;
	public static float VScreenSize = 0.0936f;

	public static float InterpupillaryDistance = 0.058f;
	public static float LensSeperationDistance = InterpupillaryDistance;

	public static float AspectRatio = ScreenHorizontalResolution*0.5f / ScreenVerticalResolution;
	public static float EyeToScreenDistance = 0.041f;
	public static float FieldOfViewY = 2.0f * (float)Math.atan2(VScreenSize * 0.5f, EyeToScreenDistance); // in radians; not degrees!

	public static float LensCenter =  1 - 2 * LensSeperationDistance / HScreenSize;

    public static float K0 = 1.0f;
    public static float K1 = 0.22f;
    public static float K2 = 0.24f;
    public static float K3 = 0.0f;
    
    public static float h_meters = HScreenSize/4.0f - InterpupillaryDistance/2.0f;
    public static float h = 4.0f * h_meters / HScreenSize;

	
}
