package com.tuohy.worldwindvr;

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
	
}
