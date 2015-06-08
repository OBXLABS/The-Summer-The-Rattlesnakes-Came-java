/*
 * Copyright (c) 2012 All Right Reserved, Jason E. Lewis [http://obxlabs.net]
 */

package net.obxlabs.rattlesnakes;

import processing.core.PApplet;

/**
 * A bezier curve with a controllable time element.
 * 
 * $LastChangedRevision: 64 $
 * $LastChangedDate: 2012-12-11 16:34:26 -0500 (Tue, 11 Dec 2012) $
 * $LastChangedBy: bnadeau $
 */
public class BezierPath {
	
	//direction the point moves on the path
	static enum PathDirection {
		FORWARD, BACKWARD;
	}
	
	//parent Processing applet
	PApplet p;
	
	float[] s;			//start point
	float [] c1;		//first control point
	float [] c2;		//second control point
	float [] e;			//end point

	float t;			//time or position on the path (0 to 1)
	PathDirection d;	//direction of the path
	float spd;			//speed at which the point moves on the path
	
	/**
	 * Constructor.
	 * @param parent the parent Processing applet
	 * @param sx	start x position
	 * @param sy	start y position
	 * @param c1x	control point 1 x position
	 * @param c1y	control point 1 y position
	 * @param c2x	control point 2 x position
	 * @param c2y	control point 2 y position
	 * @param ex	end x position
	 * @param ey	end y position
	 */
	public BezierPath(PApplet parent, float sx, float sy, float c1x, float c1y,
					  float c2x, float c2y, float ex, float ey) {
		p = parent;
		s = new float[]{sx, sy};
		c1 = new float[]{c1x, c1y};
		c2 = new float[]{c2x, c2y};
		e = new float[]{ex, ey};
		t = 0;
		d = PathDirection.FORWARD;
		spd = 0.004f;
	}
	
	/**
	 * Set the path direction to move backwards.
	 */
	public void reverse() { d = PathDirection.BACKWARD; }
	
	/**
	 * Update the current position on the path.
	 */
	public void update() {
		//if we reached the end, nothing to do
		if (done()) return;		
		
		//increment position
		t += spd;
		if (t > 1.0) t = 1.0f;
	}
	
	/**
	 * Check if the position reached the end of the path.
	 * @return true of the position reached the end
	 */
	public boolean done() {
		return t == 1.0f;
	}
	
	/**
	 * Set the position to the end of the path.
	 */
	public void end() { t = 1.0f; }
	
	/**
	 * Get the current x position on the path.
	 * @return x position
	 */
	public float x() { return p.bezierPoint(s[0], c1[0], c2[0], e[0], d==PathDirection.BACKWARD?1-t:t); }
	
	/**
	 * Get the current y position on the path.
	 * @return y position
	 */
	public float y() { return p.bezierPoint(s[1], c1[1], c2[1], e[1], d==PathDirection.BACKWARD?1-t:t); }
	
	
	/**
	 * Draw the bezier path.
	 */
	public void draw() { p.bezier(s[0], s[1], c1[0], c1[1], c2[0], c2[1], e[0], e[1]); }
}
