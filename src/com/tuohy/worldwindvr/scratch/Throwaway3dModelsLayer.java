package com.tuohy.worldwindvr.scratch;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.ogc.collada.ColladaRoot;
import gov.nasa.worldwind.ogc.collada.impl.ColladaController;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import com.tuohy.worldwindvr.WorldWindVR;

public class Throwaway3dModelsLayer {

	WorldWindVR vrFrame;

	public Throwaway3dModelsLayer(WorldWindVR vrFrame){
		this.vrFrame = vrFrame;

//		new WorkerThread(new File("testmodels/dinosaur.DAE"), Position.fromDegrees(36.19529915228048,-111.7481440380943,0), this).start();
		new WorkerThread(new File("testmodels/trex/models/t-rex11.dae"), Position.fromDegrees(36.30318582,-111.954568865,100), this).start();
//		new WorkerThread(new File("testmodels/gates/models/Jurassic Park Gates.dae"), Position.fromDegrees(36.19529915228048,-111.7481440380943,1000), this).start();
	}
	
	/**
     * Adds the specified <code>colladaRoot</code> to this app frame's <code>WorldWindow</code> as a new
     * <code>Layer</code>.
     *
     * @param colladaRoot the ColladaRoot to add a new layer for.
     */
    protected void addColladaLayer(ColladaRoot colladaRoot)
    {
        // Create a ColladaController to adapt the ColladaRoot to the World Wind renderable interface.
        ColladaController colladaController = new ColladaController(colladaRoot);

        // Adds a new layer containing the ColladaRoot to the end of the WorldWindow's layer list.
        RenderableLayer layer = new RenderableLayer();
        layer.addRenderable(colladaController);
        vrFrame.getWwd().getModel().getLayers().add(layer);
        System.out.println("added dinosaur layer!");
    }
	
    
    /** A <code>Thread</code> that loads a COLLADA file and displays it in an <code>AppFrame</code>. */
    public static class WorkerThread extends Thread
    {
        /** Indicates the source of the COLLADA file loaded by this thread. Initialized during construction. */
        protected Object colladaSource;
        /** Geographic position of the COLLADA model. */
        protected Position position;
        Throwaway3dModelsLayer layer;
        ColladaRoot colladaRoot;
        
        
        /**
         * Creates a new worker thread from a specified <code>colladaSource</code> and <code>appFrame</code>.
         *
         * @param colladaSource the source of the COLLADA file to load. May be a {@link java.io.File}, a {@link
         *                      java.net.URL}, or an {@link java.io.InputStream}, or a {@link String} identifying a file
         *                      path or URL.
         * @param position      the geographic position of the COLLADA model.
         * @param appFrame      the <code>AppFrame</code> in which to display the COLLADA source.
         */
        public WorkerThread(Object colladaSource, Position position, Throwaway3dModelsLayer layer)
        {
            this.colladaSource = colladaSource;
            this.position = position;
            this.layer = layer;
        }

        /**
         * Loads this worker thread's COLLADA source into a new <code>{@link gov.nasa.worldwind.ogc.collada.ColladaRoot}</code>,
         * then adds the new <code>ColladaRoot</code> to this worker thread's <code>AppFrame</code>.
         */
        public void run()
        {
            try
            {
                colladaRoot = ColladaRoot.createAndParse(this.colladaSource);
                colladaRoot.setPosition(this.position);
                colladaRoot.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
                colladaRoot.setModelScale(new Vec4(30.0, 30.0, 30.0));

                // Schedule a task on the EDT to add the parsed document to a layer
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        layer.addColladaLayer(colladaRoot);
                        
                		Timer timer = new Timer("MoveRex");

                		//2- Taking an instance of class contains your repeated method.
                		MoveRexTask t = new MoveRexTask(60);
                		timer.schedule(t, 0, 100);
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
   

	class MoveRexTask extends TimerTask {
		//times member represent calling times.
		private double times = 0;
		private double totalTimes;
		LatLon start = LatLon.fromDegrees(36.30318582,-111.954568865);
		LatLon end = LatLon.fromDegrees(36.173121213137755,-111.69061780538789);

		public MoveRexTask(int seconds){
			totalTimes = seconds*20;
		}

		public void run() {
			times++;
			if (times <= totalTimes) {
				double amount = times/totalTimes;
				LatLon curPos = LatLon.interpolate(amount, start, end);
				colladaRoot.setPosition(Position.fromDegrees(curPos.getLatitude().getDegrees(), curPos.getLongitude().getDegrees(),100));
				
			} else {
				//Stop Timer.
				this.cancel();
			}
		}
	}
        
    }
}
