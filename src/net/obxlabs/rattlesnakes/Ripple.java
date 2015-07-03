/*
 * Copyright (C) <2015>  <Jason Lewis>
  
    This program is free software: you can redistribute it and/or modify
    it under the terms of the BSD 3 clause with added Attribution clause license.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   BSD 3 clause with added Attribution clause License for more details.
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
