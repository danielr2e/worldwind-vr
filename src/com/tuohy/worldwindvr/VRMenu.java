package com.tuohy.worldwindvr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwindx.examples.util.ImageAnnotation;

public class VRMenu extends ImageAnnotation {
	
//	private RenderableLayer drawLayer;
//	private int width, height;
	private static final Color orange = new Color(182,170,80);
	//private BasicWWTexture black;
		
	public VRMenu() {
		this(400,400);
	}
	
	public VRMenu (int horizontal, int vertical) {
		int width=400;
		int height=400;
	    BufferedImage bg = new BufferedImage(width,height, IndexColorModel.TRANSLUCENT);
		Graphics2D g = bg.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		        RenderingHints.VALUE_ANTIALIAS_ON);
		Rectangle rect = new Rectangle(width,height);
		AffineTransform transform = g.getTransform();
//		transform.translate(horizontal/2, (vertical/2));
//		transform.translate(horizontal/2, vertical/2);
//		g.transform(transform);
		g.setColor(Color.BLACK);
		g.fill(rect);
		g.setColor(orange);
	    g.setStroke(new BasicStroke(4));
		g.draw(rect);
		g.clip(rect);
		
		this.setImageSource(bg,width,height);
//		drawLayer = rl;
//		this.width = width;
//		this.height = height;
//		try {
//			black = new BasicWWTexture(ImageIO.read(new File("resources/black.png")));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

//	public void render(DrawContext dc) {
//		// TODO Auto-generated method stub
////		dc.drawUnitQuadOutline();
////		SurfaceQuad sq = new SurfaceQuad();
////		BufferedImage bg = new BufferedImage(400,400, IndexColorModel.TRANSLUCENT);
////		Graphics2D g = bg.createGraphics();
////		g.setColor(Color.BLACK);
////		g.fillRect(0, 0, 400, 400);
////		g.setColor(orange);
//		
////		(new ImageAnnotation(g)).render(dc);
//	}

}
