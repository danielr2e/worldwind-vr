package com.tuohy.worldwindvr.rendering;

import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.util.WWXML;

import org.w3c.dom.Document;

/**
 * Adaptation of the MS Virtual Earth Layer to better function in WorldWindVR.  This includes
 * eliminating a certain portion of calls to 'assembleTiles', because they are expensive
 * and it is not useful to do them for both eyes.
 * 
 * @author dtuohy
 */
public class VRMSVirtualEarthLayer extends A_VRFriendlyTiledImageLayer
{

	public VRMSVirtualEarthLayer()
	{
		super(getConfigurationDocument());
	}

	protected static Document getConfigurationDocument()
	{
		String filePath = "resources/VRMSVirtualEarthAerialLayer.xml";
		return WWXML.openDocumentFile(filePath, null);
	}

}
