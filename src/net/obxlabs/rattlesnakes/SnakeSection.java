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

import org.apache.log4j.Logger;

import processing.core.PConstants;
import traer.physics.Particle;
import traer.physics.Vector3D;

/**
 * A section of {@link Snake}.
 * <p>This controls a flexible point in a snake's body.</p>
 * 
 * @see Snake
 * 
 * $LastChangedRevision: 64 $
 * $LastChangedDate: 2012-12-11 16:34:26 -0500 (Tue, 11 Dec 2012) $
 * $LastChangedBy: bnadeau $
 */
public class SnakeSection {

	static Logger logger = Logger.getLogger(SnakeSection.class);
	
	//parent snake
	Snake snake;
	int id = -1; //id of the section (usually its index in the snake)
	
	//particle that control the position using the particle system
	Particle particle;
	
	//fixed particle of the original position of the section
	//used to retract to the original position
	Particle origParticle;
	
	//retract variables
	boolean retract;			//true if the section is retracting
	Vector3D retractDistance;	//used during the retract behavior
	int retractDirection;		//direction of the snake, used to get the position from the head
	long retractStart;			//time we started retracting
	float retractSpeed;			//speed at which the snake retracts
	int retractDelay;			//delay until the snake retracts
	int retractWave;			//delay between letters to retract, wave effect
	
	/**
	 * Constructor.
	 * @param snake parent snake
	 * @param id id of the section in the snake
	 */
	public SnakeSection(Snake snake, int id) {
		this.snake = snake;
		this.id = id;
		this.retractDistance = new Vector3D();
		this.retract = false;
	}
	
	/**
	 * Set retract behavior properties.
	 */
	public void setRetract(int direction, int delay, int wave, float speed) {
		this.retractDirection = direction;
		this.retractDelay = delay;
		this.retractWave = wave;
		this.retractSpeed = speed;
	}
	
	/**
	 * Stop retracting.
	 */
	public void stopRetract() {
		this.retract = false;
	}
	
	/**
	 * Start retracting.
	 * @param direction direction of the snake, head position
	 */
	public void startRetract() { 
		this.retract = true;
		this.retractStart = snake.p.millis();
	}

	/**
	 * Update.
	 */
	public void update() {
		//get the index of the section in the snake, position from the head
		int index = retractDirection == PConstants.LEFT ? id : snake.sectionCount()-1-id;

		//each section of a snake starts retracting at a different time
		//make sure that we reached the good time for this section
		long now = snake.p.millis();
		if (retractStart + retractDelay + index*retractWave  > now) return;

		//move linearly towards the origin
		retractDistance.set(origParticle.position());
		retractDistance.subtract(particle.position());
		float length = retractDistance.length();
		
		//we're close enough, done.
		if (length < 1) {
			particle.velocity().set(0, 0, 0);
			stopRetract();
			return;
		}
		
		//move the particle that control the position of the section
		retractDistance.multiplyBy(retractSpeed/length * (now-retractStart-retractDelay-index*retractWave)/100);
		particle.velocity().add(retractDistance);
	}
	
	/**
	 * Reset the section.
	 */
	public void reset() {
		particle.velocity().set(0, 0, 0);
		particle.position().set(origParticle.position());
		stopRetract();
	}
	
	/**
	 * Get string.
	 */
	public String toString() { return snake + "_Section-" + id; }
}
