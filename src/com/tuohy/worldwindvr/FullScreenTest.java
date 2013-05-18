package com.tuohy.worldwindvr;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.ScreenImage;
import gov.nasa.worldwind.view.firstperson.BasicFlyView;
import gov.nasa.worldwind.view.firstperson.FlyToFlyViewAnimator;

import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A test class used during early experiments with implementing Oculus Rift VR
 * support in WorldWind.
 * 
 * 
 * @author dtuohy
 *
 */
public class FullScreenTest{


	// the first-person view
    public static BasicFlyView view;

    /**
     * @param args
     */
    public static void main(String[] args) {
            view = new BasicFlyView();
            Frame frame = new Frame("WorldwindFull");
            final WorldWindowGLCanvas worldWindowGLCanvas = new WorldWindowGLCanvas();
            worldWindowGLCanvas.setModel(new BasicModel());
            worldWindowGLCanvas.setView(view);

            worldWindowGLCanvas.addKeyListener(new java.awt.event.KeyListener() {
                    public void keyTyped(KeyEvent e) {
                    }

                    public void keyReleased(KeyEvent e) {
                    }

                    public void keyPressed(KeyEvent e) {
                            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                                    System.exit(0);
                            }
                    }
            });
            
            frame.add(worldWindowGLCanvas);
            frame.setSize(640, 480);
            frame.setUndecorated(true);
            int size = frame.getExtendedState();
            size |= Frame.MAXIMIZED_BOTH;
            frame.setExtendedState(size);

            frame.setVisible(true);
            worldWindowGLCanvas.requestFocus();
    }


}