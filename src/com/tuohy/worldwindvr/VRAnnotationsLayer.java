package com.tuohy.worldwindvr;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.RenderableLayer;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class VRAnnotationsLayer extends RenderableLayer {

	Font messageFont = new Font("Helvetica",Font.PLAIN,28);
	OutlinedTextAnnotation messageAnnotation;

	public VRAnnotationsLayer(){
		if(WorldWindVRConstants.RenderHorizontalResolution<1920){
			messageFont = new Font("Helvetica",Font.PLAIN,16);
		}
		
		this.showMessageImmediately("Welcome to the WorldWindVR Demo!", 7);
		this.queueMessage("Use W,A,S,D to Navigate");
		this.queueMessage("Use Shift Key to Change Navigation Speed");
		this.queueMessage("Use Space Bar to Change Locations");
		this.queueMessage("Note: on first visit to any location...");
		this.queueMessage("...it will take time to cache imagery.");
		this.queueMessage("Press Escape to Exit");
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

		int pixelSeparation = 20;
		if(left){
			messageAnnotation.setScreenPoint(new Point((int) (anchorPointX+pixelSeparation),(int) anchorPointY));
		}
		else{
			messageAnnotation.setScreenPoint(new Point((int) (anchorPointX-pixelSeparation),(int) anchorPointY));
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
