/*
 * Copyright (c) 2012 All Right Reserved, Jason E. Lewis [http://obxlabs.net]
 */

package net.obxlabs.rattlesnakes;

import processing.core.PVector;

/**
 * A circular ripple that expends with time.
 * 
 * $LastChangedRevision$
 * $LastChangedDate$
 * $LastChangedBy$
 */
public class Ripple {
	PVector center;	//center location
	float radius;	//radius
	float speed;	//growth speed
	
	/**
	 * Constructor.
	 * @param x x coordinate of the center
	 * @param y y coordinate of the center
	 * @param s speed
	 */
	public Ripple(float x, float y, float s) {
		center = new PVector(x, y, 0);
		speed = s;
		radius = 100;
	}
	
	/**
	 * Update the ripple, make it grow.
	 */
	public void update() {
		radius += speed;
	}
}
