package com.tuohy.worldwindvr.rendering;

import java.util.Arrays;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.w3c.dom.Document;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.PerformanceStatistic;

/**
 * Modifications to BasicTiledImageLayer that make it more efficient
 * in a VR context.  VR depends on high frame rate rendering, so this class
 * can be used to introduce optimizations.
 * 
 * @author dtuohy
 *
 */
public class A_VRFriendlyTiledImageLayer extends BasicTiledImageLayer {
	
	//the number of renders (which, since this is SBS, is half the frames) that should
	//elapse between visible tile set being recomputed - we don't recompute the tiles every
	//render because it is expensive and redundant to do so for both eyes
	protected static final int RENDERS_BETWEEN_TILE_RECOMPUTE = 4;
	
	public A_VRFriendlyTiledImageLayer(Document configDoc)
	{
		super(configDoc, null);
	}

	int rendersSinceLastTileAssembly = 0;
	protected void draw(DrawContext dc)
	{
        //DRT OPTIMIZATION for WorldWindVR 
		if(rendersSinceLastTileAssembly==0){
			this.assembleTiles(dc); // Determine the tiles to draw.
		}
		rendersSinceLastTileAssembly++;
		if(rendersSinceLastTileAssembly==RENDERS_BETWEEN_TILE_RECOMPUTE){
			rendersSinceLastTileAssembly = 0;
		}

		if (this.currentTiles.size() >= 1)
		{
			// Indicate that this layer rendered something this frame.
			this.setValue(AVKey.FRAME_TIMESTAMP, dc.getFrameTimeStamp());

			if (this.getScreenCredit() != null)
			{
				dc.addScreenCredit(this.getScreenCredit());
			}

			TextureTile[] sortedTiles = new TextureTile[this.currentTiles.size()];
			sortedTiles = this.currentTiles.toArray(sortedTiles);
			Arrays.sort(sortedTiles, levelComparer);

			GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

			if (this.isUseTransparentTextures() || this.getOpacity() < 1)
			{
				gl.glPushAttrib(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_POLYGON_BIT | GL2.GL_CURRENT_BIT);
				this.setBlendingFunction(dc);
			}
			else
			{
				gl.glPushAttrib(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_POLYGON_BIT);
			}

			gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
			gl.glEnable(GL.GL_CULL_FACE);
			gl.glCullFace(GL.GL_BACK);

			dc.setPerFrameStatistic(PerformanceStatistic.IMAGE_TILE_COUNT, this.tileCountName,
					this.currentTiles.size());
			dc.getGeographicSurfaceTileRenderer().renderTiles(dc, this.currentTiles);

			gl.glPopAttrib();

			//            if (this.drawTileIDs)
				//                this.drawTileIDs(dc, this.currentTiles);

			if (this.drawBoundingVolumes)
				this.drawBoundingVolumes(dc, this.currentTiles);

			// Check texture expiration. Memory-cached textures are checked for expiration only when an explicit,
			// non-zero expiry time has been set for the layer. If none has been set, the expiry times of the layer's
			// individual levels are used, but only for images in the local file cache, not textures in memory. This is
			// to avoid incurring the overhead of checking expiration of in-memory textures, a very rarely used feature.
			if (this.getExpiryTime() > 0 && this.getExpiryTime() <= System.currentTimeMillis())
				this.checkTextureExpiration(dc, this.currentTiles);

			//DRT OPTIMIZATION for WorldWindVR - we don't clear the tiles so that we can reuse them between
			//eyes and potentially across frames
			//            this.currentTiles.clear();
		}

		this.sendRequests();
		this.requestQ.clear();
	}
	
}
