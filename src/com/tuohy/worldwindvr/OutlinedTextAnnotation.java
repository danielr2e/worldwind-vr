package com.tuohy.worldwindvr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import gov.nasa.worldwindx.examples.util.ImageAnnotation;

/**
 * Specialization of ImageAnnotation that displays a line of text with
 * an outline.  This is useful for displaying text directly on the screen
 * or globe with necessary contrast against any background.
 * 
 * @author dtuohy
 *
 */
public class OutlinedTextAnnotation extends ImageAnnotation {

	private String outlinedText;
	private Color outlineColor = Color.BLACK;
	private Color fillColor = new Color(50,255,100);
	Font font;
	Graphics2D graphicsContext;
	
	public OutlinedTextAnnotation(String text, Font font){
		this.outlinedText = text;
		this.font = font;
		//compute the needed height/width of the image
	    
		Graphics2D tempGraphics = (Graphics2D) new BufferedImage(200,200,BufferedImage.TYPE_INT_ARGB).getGraphics();
		tempGraphics.setFont(font);
		int width = tempGraphics.getFontMetrics().stringWidth(text)+ 2;
		int height = tempGraphics.getFontMetrics().getHeight()+ 2;
		
		
		//render the outlined text into the image
		BufferedImage image = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
		graphicsContext = (Graphics2D)image.getGraphics();

	    graphicsContext.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	        RenderingHints.VALUE_ANTIALIAS_ON);

	    graphicsContext.setRenderingHint(RenderingHints.KEY_RENDERING,
	        RenderingHints.VALUE_RENDER_QUALITY);

	    FontRenderContext frc = graphicsContext.getFontRenderContext();
	    String s = new String(text + " ");
	    TextLayout textTl = new TextLayout(s, font, frc);
	    AffineTransform transform = new AffineTransform();
	    Shape outline = textTl.getOutline(null);
	    Rectangle outlineBounds = outline.getBounds();
	    transform = graphicsContext.getTransform();
	    transform.translate(width / 2 - (outlineBounds.width / 2), height / 2
	        + (outlineBounds.height / 2));
	    graphicsContext.transform(transform);
	    graphicsContext.setStroke(new BasicStroke(4));
	    graphicsContext.setColor(outlineColor);
	    graphicsContext.draw(outline);
	    graphicsContext.setColor(fillColor);
	    graphicsContext.fill(outline);
	    graphicsContext.setClip(outline);
		this.setImageSource(image, imageWidth, imageHeight);
	}
	
	public Graphics2D getGraphicsContext(){
		return this.graphicsContext;
	}
	
	public String getOutlinedText(){
		return this.outlinedText;
	}
	
}
