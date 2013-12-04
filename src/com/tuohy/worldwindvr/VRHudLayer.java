package com.tuohy.worldwindvr;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwindx.examples.util.ImageAnnotation;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

public class VRHudLayer extends RenderableLayer {

	Font messageFont = new Font("Helvetica",Font.PLAIN,28);
	OutlinedTextAnnotation messageAnnotation;
	
	private BufferedImage cursorImage;
	private ImageAnnotation cursorAnnotation;
	
	
	private ImageAnnotation makeCursorAnnotation() {
		return new ImageAnnotation(cursorImage,64,64);
	}

	public VRHudLayer(){
		if(WorldWindVRConstants.RenderHorizontalResolution<1920){
			messageFont = new Font("Helvetica",Font.PLAIN,16);
		}
		try {
			cursorImage = ImageIO.read(new File("resources/cursorx.png"));
			cursorAnnotation = new ImageAnnotation(cursorImage, 64, 64);
		} catch (IOException e) {
			System.err.println("Couldn't read cursor file");
			e.printStackTrace();
		}
		
		this.showMessageImmediately("Welcome to the WorldWindVR Demo!", 7);
		this.queueMessage("Use W,A,S,D to Navigate");
		this.queueMessage("Use Shift Key to Change Navigation Speed");
		this.queueMessage("Use Space Bar to Change Locations");
		this.queueMessage("Note: on first visit to any location...");
		this.queueMessage("...it will take time to cache imagery.");
		this.queueMessage("Press Escape to Exit");
//		this.drawCursor(WorldWindVRConstants.RenderHorizontalResolution/4, WorldWindVRConstants.RenderVerticalResolution/2);
		VRMenu menu = new VRMenu((int)WorldWindVRConstants.RenderHorizontalResolution,(int)WorldWindVRConstants.RenderVerticalResolution);
		//menu.setScreenPoint(new Point((int) (WorldWindVRConstants.RenderHorizontalResolution/2), (int) (WorldWindVRConstants.RenderVerticalResolution/2)));
		menu.setScreenPoint(new Point(0,0));
		//this.addRenderable(menu);
	}

	public void prepareForEye(boolean left){
		float anchorPointX = WorldWindVRConstants.RenderHorizontalResolution/4.0f;
		float anchorPointY = WorldWindVRConstants.RenderVerticalResolution/2.0f;
		float messageWidth = messageAnnotation.getGraphicsContext().getFontMetrics().stringWidth(messageAnnotation.getOutlinedText());
		float messageHeight = messageAnnotation.getGraphicsContext().getFontMetrics().getHeight();
		anchorPointX -= (messageWidth/2.0f);
		anchorPointY += messageHeight/2.0f;

		//not sure why we need this
		anchorPointX += 100;
//		anchorPointY += 110 ;

		int pixelSeparation = 40;
		if(left){
			messageAnnotation.setScreenPoint(new Point((int) (anchorPointX+pixelSeparation),(int) anchorPointY));
		}
		else{
			messageAnnotation.setScreenPoint(new Point((int) (anchorPointX),(int) anchorPointY));
		}
	}

	Timer timer;
	List<String> messageQueue = Collections.synchronizedList(new ArrayList<String>());

	public void showMessageImmediately(String messageText, int secondsToDisplay){
		messageQueue.clear();
		showMessage(messageText, secondsToDisplay);
	}


	public void queueMessage(String messageText){
		if(timer==null){
			this.showMessage(messageText, 4);
		}
		else{
			messageQueue.add(messageText);
		}
	}
	
	public void drawCursor(int x, int y) {
		if (cursorAnnotation!=null) {
			this.removeRenderable(cursorAnnotation);
		}
		cursorAnnotation = makeCursorAnnotation();
		cursorAnnotation.setScreenPoint(new Point(x,y));
		this.addRenderable(cursorAnnotation);
	}
	public void drawCursor(float x, float y) {
		drawCursor((int)x,(int)y);
	}

	private void showMessage(String messageText, int secondsToDisplay) {
		if(messageAnnotation!=null){
			this.removeRenderable(messageAnnotation);
		}
		messageAnnotation = new OutlinedTextAnnotation(messageText, messageFont);
		messageAnnotation.setValue(AVKey.VIEW_OPERATION, AVKey.VIEW_PAN);
		messageAnnotation.getAttributes().setSize(new Dimension(500,500));
		this.addRenderable(messageAnnotation);

		//1- Taking an instance of Timer class.
		if(timer!=null){
			timer.cancel();
		}
		timer = new Timer("FadeOut");

		//2- Taking an instance of class contains your repeated method.
		FadeInOutMessageTask t = new FadeInOutMessageTask(secondsToDisplay);
		timer.schedule(t, 0, 100);
	}

	class FadeInOutMessageTask extends TimerTask {
		private int times = 0;
		private int totalTimes = 40;
		private double fadeInIntervals = 10;

		public FadeInOutMessageTask(int seconds){
			totalTimes = seconds*10;
			
			//if seconds is less than 0, we show the message until it is dismissed
			if(seconds<0){
				totalTimes = Integer.MAX_VALUE;
			}
		}

		public void run() {
			times++;
			if (times <= totalTimes) {
				if(times<fadeInIntervals){
					messageAnnotation.getAttributes().setOpacity(times/fadeInIntervals);
				}
				else if(times>(totalTimes-fadeInIntervals)){
					messageAnnotation.getAttributes().setOpacity((totalTimes-times)/fadeInIntervals);
				}
			} else {
				messageAnnotation.getAttributes().setOpacity(0.0);
				//Stop Timer.
				this.cancel();
				if(!messageQueue.isEmpty()){
					showMessage(messageQueue.remove(0),4);
				}
				else{
					timer = null;
				}
			}
		}
	}

}
