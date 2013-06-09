package com.tuohy.worldwindvr.scratch;

import javax.media.opengl.GL;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.ViewUtil;
import gov.nasa.worldwind.view.firstperson.BasicFlyView;
import gov.nasa.worldwind.view.firstperson.FlyViewInputHandler;
import gov.nasa.worldwind.view.firstperson.FlyViewLimits;

/**
 * Custom fly view for WorldWindVR.
 * 
 * @author dtuohy
 *
 */
public class TestFlyView extends BasicFlyView {

	//used to force the viewport to the correct dimensions based on the display height/width
	int hardCodedHeight = -1;
	int hardCodedWidth = -1;
	
	public TestFlyView(){
        this.viewInputHandler = new TestFlyViewInputHandler();

        this.viewLimits = new FlyViewLimits();
        this.viewLimits.setPitchLimits(DEFAULT_MIN_PITCH, DEFAULT_MAX_PITCH);
        this.viewLimits.setEyeElevationLimits(DEFAULT_MIN_ELEVATION, DEFAULT_MAX_ELEVATION);

        loadConfigurationValues();
	}
	
	@Override
    public void apply(DrawContext dc)
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

        if (this.viewInputHandler != null)
            this.viewInputHandler.apply();

        doApply(dc);

        if (this.viewInputHandler != null)
            this.viewInputHandler.viewApplied();
    }
	
    public void applyWithOffset(DrawContext dc, Angle offsetDir, Angle offsetAmount)
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

//        if (this.viewInputHandler != null)
//            ((TestFlyViewInputHandler) this.viewInputHandler).apply();
//
        //here is where we have to make sure the offset is preserved
        Position curPosition = this.getEyePosition();
        this.setEyePosition(new Position(LatLon.greatCircleEndPosition(curPosition, offsetDir, offsetAmount),curPosition.getAltitude()));

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
        this.viewport = new java.awt.Rectangle(viewportArray[0], viewportArray[1], viewportArray[2], viewportArray[3]);

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

    protected void afterDoApply()
    {
        // Establish frame-specific values.
        this.lastEyePosition = this.computeEyePositionFromModelview();
        this.horizonDistance = this.computeHorizonDistance();

        // Clear cached computations.
        this.lastEyePoint = null;
        this.lastUpVector = null;
        this.lastForwardVector = null;
        this.lastFrustumInModelCoords = null;
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
