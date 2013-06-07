package com.tuohy.worldwindvr;

import javax.media.opengl.GL;

import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.ViewUtil;
import gov.nasa.worldwind.view.firstperson.BasicFlyView;
import gov.nasa.worldwind.view.firstperson.FlyViewLimits;

/**
 * Custom fly view for WorldWindVR.
 * 
 * @author dtuohy
 *
 */
public class VRFlyView extends BasicFlyView {

	public VRFlyView(){
        this.viewInputHandler = new VRFlyViewInputHandler();

        this.viewLimits = new FlyViewLimits();
        this.viewLimits.setPitchLimits(DEFAULT_MIN_PITCH, DEFAULT_MAX_PITCH);
        this.viewLimits.setEyeElevationLimits(DEFAULT_MIN_ELEVATION, DEFAULT_MAX_ELEVATION);

        loadConfigurationValues();
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
        this.viewport = new java.awt.Rectangle(viewportArray[0], viewportArray[1], viewportArray[2]/2, viewportArray[3]);
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
	
}
