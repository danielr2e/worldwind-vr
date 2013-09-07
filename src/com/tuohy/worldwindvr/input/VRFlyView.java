package com.tuohy.worldwindvr.input;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.ViewUtil;
import gov.nasa.worldwind.view.firstperson.BasicFlyView;
import gov.nasa.worldwind.view.firstperson.FlyViewLimits;

import javax.media.opengl.GL;

/**
 * Custom fly view for WorldWindVR.  This class uses a custom input
 * handler and has a special 'apply' implementation that plays well
 * with stereoscopic rendering.
 * 
 * @author dtuohy
 *
 */
public class VRFlyView extends BasicFlyView {

	//used to force the viewport to the correct dimensions based on the display height/width
	int hardCodedHeight = -1;
	int hardCodedWidth = -1;
	
	public VRFlyView(){
        this.viewInputHandler = new VRFlyViewInputHandler();

        this.viewLimits = new FlyViewLimits();
        this.viewLimits.setPitchLimits(DEFAULT_MIN_PITCH, DEFAULT_MAX_PITCH);
        this.viewLimits.setEyeElevationLimits(DEFAULT_MIN_ELEVATION, DEFAULT_MAX_ELEVATION);

        loadConfigurationValues();
	}
	
    public void setHeading(Angle heading)
    {
        if (heading == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

//        System.out.println("heading now " + heading);
        
        this.heading = ViewUtil.normalizedHeading(heading);
        this.heading = heading;
        this.updateModelViewStateID();
        //resolveCollisionsWithPitch();
    }
    
    /**
     * This method replaces the normal apply() method in order to support
     * seamless (no delay, no jitter) stereoscopic rendering.  It applies
     * an offset to the current eye position, and does not notify 
     * input handlers to continue any existing animations.  Animations
     * interfere with stereoscopic rendering, so they cannot be allowed
     * to execute when the offset camera location views are being rendered.
     * 
     * @param dc
     * @param offsetDir
     * @param anchorPosition 
     * @param offsetAmount
     * @param verticalOffsetMeters 
     */
    public void applyWithOffset(DrawContext dc, Angle offsetDir, Position anchorPosition, Angle offsetAmount, double verticalOffsetMeters)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (dc.getGL() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGLIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoGlobeSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }
        
        //we do not instruct the handlers to apply the current state, because it would cause animators to increment
//        if (this.viewInputHandler != null)
//            ((TestFlyViewInputHandler) this.viewInputHandler).apply();
//
        //here is where we have to make sure the offset is enforced
        this.setEyePosition(new Position(LatLon.greatCircleEndPosition(anchorPosition, offsetDir, offsetAmount),anchorPosition.getAltitude()+verticalOffsetMeters));

        doApply(dc);

        if (this.viewInputHandler != null)
            this.viewInputHandler.viewApplied();
    }
	
    @Override
    protected void doApply(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (dc.getGL() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGLIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (dc.getGlobe() == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        // Update DrawContext and Globe references.
        this.dc = dc;
        this.globe = this.dc.getGlobe();

        //========== modelview matrix state ==========//
        // Compute the current modelview matrix.
        this.modelview = ViewUtil.computeTransformMatrix(this.globe, this.eyePosition, this.heading, this.pitch,
            this.roll);
        if (this.modelview == null)
            this.modelview = Matrix.IDENTITY;

        // Compute the current inverse-modelview matrix.
        this.modelviewInv = this.modelview.getInverse();
        if (this.modelviewInv == null)
            this.modelviewInv = Matrix.IDENTITY;

        //========== projection matrix state ==========//
        // Get the current OpenGL viewport state.
        int[] viewportArray = new int[4];
        this.dc.getGL().glGetIntegerv(GL.GL_VIEWPORT, viewportArray, 0);
        
        //MODIFIED for Oculus by DRT 6/5/2013: here we divide the view port width by half because we are only
        //rendering to half of the screen
        this.viewport = new java.awt.Rectangle(viewportArray[0], viewportArray[1], hardCodedWidth/2, hardCodedHeight);
//        System.out.println("viewport is " + viewport);
        
        // Compute the current clip plane distances.
        this.nearClipDistance = this.computeNearClipDistance();
        this.farClipDistance = this.computeFarClipDistance();

        // Compute the current viewport dimensions.
        double viewportWidth = this.viewport.getWidth() <= 0.0 ? 1.0 : this.viewport.getWidth();
        double viewportHeight = this.viewport.getHeight() <= 0.0 ? 1.0 : this.viewport.getHeight();

        // Compute the current projection matrix.
        this.projection = Matrix.fromPerspective(this.fieldOfView, viewportWidth, viewportHeight, this.nearClipDistance,
            this.farClipDistance);

        // Compute the current frustum.
        this.frustum = Frustum.fromPerspective(this.fieldOfView, (int) viewportWidth, (int) viewportHeight,
            this.nearClipDistance, this.farClipDistance);

        //========== load GL matrix state ==========//
        loadGLViewState(dc, this.modelview, this.projection);

        //========== after apply (GL matrix state) ==========//
        afterDoApply();
        
    }

    /**
     * This method should be called when the primary display dimensions have been determined and
     * will be used to ensure the correct viewport dimensions at all times.
     * 
     * We hard code the display height/width because we were seeing strange behavior when allowing
     * WorldWind to recompute the viewport after offsetting the right eye.
     * 
     * @param width
     * @param height
     */
	public void hardCodeViewPortHeightAndWidth(int width, int height) {
		this.hardCodedWidth = width;
		this.hardCodedHeight = height;
	}
	
}
