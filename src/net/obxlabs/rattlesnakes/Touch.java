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

/**
 * A identifiable touch location with attributes specific to Rattlesnakes.
 * 
 * $LastChangedRevision: 64 $
 * $LastChangedDate: 2012-12-11 16:34:26 -0500 (Tue, 11 Dec 2012) $
 * $LastChangedBy: bnadeau $
 */
public class Touch {
	
	int id;			//id of the touch
	float x, y; 	//x and y positions
	long start;		//start time in millis
	int delay;		//random delay for bite 
	int bites;		//number of active bites on that touch

	/**
	 * Constructor.
	 * @param id id
	 * @param x x position
	 * @param y y position
	 */
	public Touch(int id, float x, float y, long start, int delay) 
	{
		this.id = id;
		this.x = x;
		this.y = y;
		this.start = start;
		this.delay = delay;
		this.bites = 0;
	}
	
	/**
	 * Set the position.
	 * @param x x position
	 * @param y y position
	 */
	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}
}
