/*
 * Copyright (c) 2012 All Right Reserved, Jason E. Lewis [http://obxlabs.net]
 */

package net.obxlabs.rattlesnakes;

import java.awt.Rectangle;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PVector;
import traer.physics.Attraction;
import traer.physics.Particle;
import traer.physics.Spring;
import traer.physics.Vector3D;

import pitaru.sonia_v2_9.*;

/**
 * A snake made of text that can bite a target and retract.
 * 
 * $LastChangedRevision: 67 $
 * $LastChangedDate: 2012-12-12 23:26:28 -0500 (Wed, 12 Dec 2012) $
 * $LastChangedBy: bnadeau $
 */
public class Snake {
	
	static Logger logger = Logger.getLogger(Snake.class);
	
	//parent Processing applet
	PApplet p;
	
	int id;						//id of the snake
	int side;					//side the head is one (LEFT or RIGHT)
	
	String text;				//textual value
	PFont pfont;				//Processing font
	float textWidth;			//width of the text
	float snakeWidth;			//width of the snake
	
	PVector position;			//position
	SnakeSection[] sections;	//bits of snake
	Particle head;				//cached head particle
	Spring[] sectionSprings;	//springs controlling the sections
	Spring[] originSprings;		//springs connecting the sections with their origins
	
	Word bitWord;				//word object the snake bit on
	Touch bitTouch;				//touch object the snake is biting on
	Particle prey;				//prey particle to control the bite animation
	Attraction preyAttraction;	//attraction between the head and the prey
	float strengthMult;			//strength multiplier for the attraction to the prey
	boolean bitSoundPlayed; 	//true if the strike sound was played for the current bite
	ArrayList<Sample> strikeSamples; //array of strike sounds
	ArrayList<Sample> rattleSamples; //array of rattle sounds
	boolean bitWordContracted;	//true when the snake contract the word it bit on last
	
	boolean retracting;			//true if the snake is retracting, false if not
	
	String cleanText;			//cleaned (no space) textual value
	PVector[] letterPositions;	//position of each letter
	float[][] letterForces;		//force factor affecting each letter
	float[] letterScales;		//scale factor of each letter
	Rectangle[] letterBounds;	//letter bounding boxes

	/**
	 * Constructor.
	 * @param parent parent Processing applet 
	 * @param id id
	 * @param text textual value
	 * @param font Processing font
	 * @param sectionCount number of sections
	 * @param side side of the head (LEFT or RIGHT)
	 */
	public Snake(PApplet parent, int id, String text, PFont font, int sectionCount, int side) {
		this.p = parent;
		this.id = id;
		this.side = side;
		this.text = text;
		this.pfont = font;
		this.position = new PVector();
		
		//set the font to make some calculations
		p.textFont(pfont);
		
		//create the sections
		sections = new SnakeSection[sectionCount+1];
		for(int i = 0; i < sections.length; i++)
			sections[i] = new SnakeSection(this, i);
		
		//create the springs between sections
		sectionSprings = new Spring[sections.length-1];
		
		//build the letters of the snake
		buildLetters(side);		
		
		//build the sections of the snake
		buildSections(side);		
		
		//build the forces that control the letters
		buildForces();
		
		//create the prey and its controls
		prey = Rattlesnakes.physics.makeParticle(100, 0, 0, 0);
		prey.makeFixed();
		preyAttraction = Rattlesnakes.physics.makeAttraction(head(), prey, 0, 50);
		preyAttraction.turnOff();
		strengthMult = 7.69f;
		bitSoundPlayed = false;
		strikeSamples = new ArrayList<Sample>();
		rattleSamples = new ArrayList<Sample>();
		
		//the snake is not retracting by default
		retracting = false;
		
		//reset word contraction flag until snake bites
		bitWordContracted = false;
	}
	
	/**
	 * Build the letters of the snake.
	 * @param side side the head is one (LEFT or RIGHT)
	 */
	public void buildLetters(int side) {
		//clean up the text, remove spaces
		cleanText = text.replace(" ", "");
		
		//create a position for each letter
		int letterCount = cleanText.length();
		letterPositions = new PVector[letterCount];
		
		//create a bounding box for each letter
		letterBounds = new Rectangle[letterCount];
		
		//create the default letter scales
		//this is usually set using the setScales method afterwards
		//letterScales = null;
		//letterScales = new float[1][2];
		//letterScales[0][0] = 0;
		//letterScales[0][1] = 1;
		letterScales = new float[letterCount];
		for(int i = 0; i < letterCount; i++)
			letterScales[i] = 1;
		
		//counter in the clean string
		int iClean = 0;
		
		//loop through all letters in the original string
		for(int i = 0; i < text.length(); i++) {
			//if the character is a space, we can skip it
			if (text.charAt(i) == ' ')
				continue;

			//set the position of the letter
			letterPositions[iClean] = new PVector(p.textWidth(text.substring(0, i)), 0, 0);
			
			//set the bounding box of the letter
			float letterWidth = p.textWidth(text.charAt(i));
			float letterHeight = p.textAscent() + p.textDescent();
			letterBounds[iClean] = new Rectangle((int)-letterWidth/2, (int)-letterHeight/2, (int)letterWidth, (int)letterHeight);

			//increment the clean string counter
			iClean++;
		}		
	}

	/**
	 * Build the sections.
	 * @param side side of the snake's head (LEFT or RIGHT)
	 */
	public void buildSections(int side) {
		if (side == PConstants.RIGHT) buildSectionsRight();
		else if (side == PConstants.LEFT) buildSectionsLeft(); 
	}
	
	/**
	 * Build the sections for a snake with the head on the left.
	 */
	public void buildSectionsLeft() {
		//set the font
		p.textFont(pfont);
		
		//index of the last letter
		int last = letterPositions.length - 1;
		
		//width of the snake based on the first and last position of the letters
		snakeWidth = letterPositions[last].x + p.textWidth(cleanText.charAt(last)) - letterPositions[0].x;
		
		//go through the sections and create a particle in the particle system for each
		for(int i = 0; i < sections.length; i++) {
			//if it's not the section for the head, create a normal particle
			if (i > 0)
				sections[i].particle = Rattlesnakes.physics.makeParticle(1f, i*snakeWidth/(sections.length-1), 0, 0);
			//if it's the section for the head, create a heavier particle
			else 
				sections[i].particle = Rattlesnakes.physics.makeParticle(10f, 0, 0, 0);
			
			//if not the first section, create a spring to connect to the last section
			if (i > 0)
				sectionSprings[i-1] = Rattlesnakes.physics.makeSpring(sections[i-1].particle, sections[i].particle, 1+((i-1)*(1f/sections.length*0.96f)), 1f, sections[i].particle.position().distanceTo(sections[i-1].particle.position()));
		}
		
		//make the tail fix
		sections[sections.length-1].particle.makeFixed();

		//setup section origins, the snakes starts flat
		//create springs between each section and its origin
		originSprings = new Spring[sections.length];
		for(int i = 0; i < sections.length; i++) {
			//create the origin particle for each section and make it fix
			Particle s = sections[i].particle;
			sections[i].origParticle = Rattlesnakes.physics.makeParticle(s.mass(), s.position().x(), s.position().y(), s.position().z());
			sections[i].origParticle.makeFixed();
			
			//create the spring that ties the origin and its matching section
			originSprings[i] = Rattlesnakes.physics.makeSpring(sections[i].origParticle, s, 0.05f, 0.2f, 150f);
		}	
	}
	
	/**
	 * Build the sections for a snake with the head on the right.
	 */
	public void buildSectionsRight() {
		//set the font
		p.textFont(pfont);
		
		//index of the last letter
		int last = letterPositions.length - 1;

		//width of the snake based on the first and last position of the letters
		snakeWidth = letterPositions[last].x + p.textWidth(cleanText.charAt(last)) - letterPositions[0].x;
		
		//go through the sections and create a particle in the particle system for each		
		for(int i = 0; i < sections.length; i++) {
			//if it's not the section for the head, create a normal particle
			if (i < sections.length-1)
				sections[i].particle = Rattlesnakes.physics.makeParticle(1f, i*snakeWidth/(sections.length-1), 0, 0);
			//if it's the section for the head, create a heavier particle
			else 
				sections[i].particle = Rattlesnakes.physics.makeParticle(10f, i*snakeWidth/i, 0, 0);
			
			//if not the first section, create a spring to connect to the last section
			if (i > 0)
				sectionSprings[i-1] = Rattlesnakes.physics.makeSpring(sections[i-1].particle, sections[i].particle, 1+((sections.length-i)*(1f/sections.length*0.96f)), 1f, sections[i].particle.position().distanceTo(sections[i-1].particle.position()));
		}

		//make the tail fix
		sections[0].particle.makeFixed();

		//setup section origins, the snakes starts flat
		//create springs between each section and its origin
		originSprings = new Spring[sections.length];
		for(int i = 0; i < sections.length; i++) {
			//create the origin particle for each section and make it fix
			Particle s = sections[i].particle;
			sections[i].origParticle = Rattlesnakes.physics.makeParticle(s.mass(), s.position().x(), s.position().y(), s.position().z());
			sections[i].origParticle.makeFixed();

			//create the spring that ties the origin and its matching section
			originSprings[i] = Rattlesnakes.physics.makeSpring(sections[i].origParticle, s, 0.05f, 0.2f, 150f);
		}			
	}
	
	/**
	 * Build the forces that control the letter positions.
	 */
	public void buildForces() {
		//make space for forces between each letter of the clean text and each section
		int letterCount = cleanText.length();
		letterForces = new float[letterCount][sections.length];
		
		//clean string counter
		int iClean = 0;
		
		//loop through the original string and create the forces for each letter
		for(int i = 0; i < text.length(); i++) {
			//skip spaces
			if (text.charAt(i) == ' ')
				continue;
			
			//calculate the force of each section control point on the letter
			float totalForce = 0;
			float distance = 0;
			float maxDistance = snakeWidth/(sections.length-1);
			
			//go through sections and set force based on the distance between it and the letter
			for(int j = 0; j < sections.length; j++) {						
				distance = sections[j].particle.position().distanceTo(letterPositions[iClean].x, letterPositions[iClean].y, letterPositions[iClean].z);
				distance -= maxDistance;
				letterForces[iClean][j] = distance > 0 ? 0 : -distance / maxDistance;
				totalForce += letterForces[iClean][j];
			}
			
			//normalize the forces
			if (totalForce > 0)
				for(int j = 0; j < sections.length; j++)
					letterForces[iClean][j] /= totalForce;		

			//increment clean string counter
			iClean++;
		}		
	}
	
	/**
	 * Get the number of sections in the snake.
	 * @return number of sections
	 */
	public int sectionCount() { return sections.length; } 
	
	/**
	 * Set the scaling factor (the shape).
	 * @param positions positions (from 0 to 1) on the snake
	 * @param scales scaling factor for each position
	 */
	public void setScales(float[] positions, float[] scales) {
		//make space for the array copies
		float[] positionsCopy = null;
		float[] scalesCopy = null;
		
		//if the head is on the left side, then reverse values
		if (side == PConstants.LEFT) {
			positionsCopy = new float[positions.length];
			System.arraycopy( positions, 0, positionsCopy, 0, positions.length );
			for(int i = 0; i < positions.length; i++)
				positionsCopy[i] = 1-positions[positions.length - 1 - i];

			scalesCopy = new float[scales.length];
			System.arraycopy( scales, 0, scalesCopy, 0, scales.length );
			for(int i = 0; i < scales.length; i++)
				scalesCopy[i] = scales[scales.length - 1 - i];
		}
		//if the head is on the right side,
		//then we assume the values are in the right order
		else {
			positionsCopy = positions;
			scalesCopy = scales;
		}
		
		//array lengths must match and it must contain at least 2 values (start and end)
		if (positionsCopy.length != scalesCopy.length) {
			logger.warn("Setting snake scale factors failed. Number of positions and number of scale factors don't match.");
			return;
		}
		if (positionsCopy.length < 2) {
			logger.warn("Setting snake scale factors failed. At least 2 scale factors are required.");
			return;
		}
		
		//make space for scaling factors
		float[][] scalePoints = new float[positionsCopy.length][2];

		//set scales
		//convert percentage positions to letter indexes, and store scaling values
		for(int i = 0; i < positionsCopy.length; i++) {
			scalePoints[i][0] = (int)(positionsCopy[i]*cleanText.length());
			scalePoints[i][1] = scalesCopy[i];
		}	
		
		//calculate the first and the next letter scale factors
		int letterScaleIndex = (int)scalePoints[0][0];
		float letterScale = scalePoints[0][1];
		int nextLetterScaleIndex = (int)scalePoints[1][0];
		float nextLetterScale = scalePoints[1][1];
		
		//index of the next scaling value to use after reaching nextLetterScaleIndex
		int scaleIndex = 2;
		
		//current scale factor
		float currentScale = 1;
		
		//loop through all letter positions
		for(int i = 0; i < letterPositions.length; i++) {
			//if we reached the nextLetterScaleIndex, then we need
			//to get values for the next two scale factors
			//TODO compute all scales for each letter on init
			if (i > nextLetterScaleIndex) {
				//next indexes before the first ones
				letterScaleIndex = nextLetterScaleIndex;
				letterScale = nextLetterScale;

				//if the next scale index is within the bounds of the scale factor array
				//then we grad the next one
				//TODO extend letterscales on init so that we don't have to check here
				if (scaleIndex < scalePoints.length) {				
					nextLetterScaleIndex = (int)scalePoints[scaleIndex][0];
					nextLetterScale = scalePoints[scaleIndex][1];				

					//increment counter for the next one
					scaleIndex++;
				}
				else {
					//if we reached the end of the scale factor array
					//then we set the index to the last letter
					nextLetterScaleIndex = letterPositions.length-1;
				}
			}
			
			//calculate the current scale (interpolate between the two indexes)
			letterScales[i] = (i-letterScaleIndex)/(float)(nextLetterScaleIndex-letterScaleIndex)*(nextLetterScale-letterScale)+letterScale;
		}		
	}
	
	/**
	 * Set the original body position.
	 * @param amplitude amplitude of the sin wave of the body
	 * @param length length of the body
	 * @param cycles number of cycles in the wave
	 */
	public void setOrigin(float amplitude, float length, float cycles) {
		//rads for the number of cycles
		float rads = cycles*PConstants.TWO_PI;
		
		//go through each section and find its correct position to create the wave
		for(int i = 0; i < sections.length; i++) {
			//find the x position of this section
			float xrad = i*rads/(sections.length-1);
			
			//adjust the section's position and its original position
			sections[i].origParticle.position().set(xrad * length/sections.length, -amplitude * (float)Math.sin(xrad), 0);
			sections[i].particle.position().set(sections[i].origParticle.position());
			
			//adjust the spring with the last section
			if (i > 0)
				sectionSprings[i-1].setRestLength(sections[i].origParticle.position().distanceTo(sections[i-1].origParticle.position()));
		}
	}
	
	/**
	 * Get the head's particle.
	 * @return head particle
	 */
	public Particle head() {
		if (head == null) {
			if (sections[0].particle.isFixed()) head = sections[sections.length-1].particle;
			else head = sections[0].particle;
		}
		return head;
	}
	
	/**
	 * Get the original head's particle.
	 * @return original head particle
	 */
	public Particle originalHead() {
		if (sections[0].origParticle.isFixed()) return sections[sections.length-1].origParticle;
		else return sections[0].origParticle;		
	}
	
	/**
	 * Translate the snake.
	 * @param x x offset
	 * @param y y offset
	 * @param z z offset
	 */
	public void translate(float x, float y, float z) {
		//translate
		position.add(x, y, z);

		//translate sections, which are absolutes
		for(SnakeSection section : sections) {
			section.particle.position().add(x, y, z);
			section.origParticle.position().add(x, y, z);
		}
	}
	
	/**
	 * Check if the snake is biting.
	 * @return true if biting, false if not
	 */
	public boolean isBiting() { return preyAttraction.isOn(); }

	/**
	 * Set the properties that control the bite behavior.
	 * @param mass			mass of the bitten object (used by particle system)
	 * @param strength		strength of the bite's attraction
	 * @param minDistance	minimum distance for the bite's attraction to affect the position
	 */
	public void setBite(float mass, float strength, float minDistance) {
		prey.setMass(mass);
		preyAttraction.setMinimumDistance(minDistance);
		strengthMult = strength;
	}
	
	/**
	 * Set the sound samples for strikes and rattles.
	 * @param strikes list of strike sound samples
	 * @param svolume strike sound volume
	 * @param rattles list of rattle sound samples
	 * @param rvolume rattle sound volume
	 */
	public void setSamples(ArrayList<Sample> strikes, float svolume, ArrayList<Sample> rattles, float rvolume) {
		strikeSamples.clear();
		
		if (strikes != null) {
			for(Sample s : strikes) {
				s.setVolume(svolume, SoundManager.LEFT);
				s.setVolume(svolume, SoundManager.RIGHT);
				strikeSamples.add(s);
			}
		}
		
		setRattleSamples(rattles, rvolume);
	}
	
	/**
	 * Set the rattle sounds samples.
	 * @param rattles list of rattle sound samples
	 * @param rvolume rattle sound volume
	 */
	public void setRattleSamples(ArrayList<Sample> rattles, float rvolume) {
		rattleSamples.clear();

		if (rattles != null) {
			for(Sample s : rattles) {
				s.setVolume(rvolume, SoundManager.LEFT);
				s.setVolume(rvolume, SoundManager.RIGHT);
				rattleSamples.add(s);
			}	
		}		
	}
	
	/**
	 * Set one strike sound sample.
	 * @param strike strike sample
	 * @param svolume strike sound volume
	 */
	public void setStrikeSample(Sample strike, float volume) {
		strikeSamples.clear();

		strike.setVolume(volume, SoundManager.LEFT);
		strike.setVolume(volume, SoundManager.RIGHT);
		strikeSamples.add(strike);
	}
	
	/**
	 * Bite on a touch object.
	 * @param t the touch object to bite
	 */
	public void bite(Word w, Touch t) {
		//keep track of the word object
		bitWord = w;
		
		//keep track of the touch object
		bitTouch = t;
		bitTouch.bites++;
		
		//reset the strike sound
		bitSoundPlayed = false;
		
		//bite on the touch's location
		bite(t.x, t.y, 0);
	}
	
	/**
	 * Bite at a specific location.
	 * @param x x position
	 * @param y y position
	 * @param z z position
	 */
	private void bite(float x, float y, float z) {
		//disable retracting
		retracting = false;
		
		//stop sections from retracting
		for(SnakeSection section : sections)
			section.stopRetract();

		//enable springs between sections and their origins
		for(Spring s : originSprings)
			s.turnOn();

		//set and enable the attraction to the bitten object
		prey.position().set(x, y, z);
		preyAttraction.setStrength(strengthMult * originalHead().position().distanceTo(x, y, z));
		preyAttraction.turnOn();
	}
	
	/**
	 * Set the retract behavior values.
	 * @param direction direction of the retraction
	 * @param delay delay before retracting
	 * @param wave delay between each snake section
	 * @param speed speed of retraction
	 */
	public void setRetract(int direction, int delay, int wave, float speed) {
		for(SnakeSection section : sections)
			section.setRetract(direction, delay, wave, speed);
	}
	
	/**
	 * Retract the snake.
	 */
	public void retract() {
		//clear the bitten objects
		if (bitWord != null) {
			bitWord.decontract();
			bitWordContracted = false;
			bitWord = null;
		}
		bitTouch = null;
		
		//turn off the attraction to the bitten object
		preyAttraction.turnOff();

		//turn off the spring between the sections and their origins
		for(Spring s : originSprings)
			s.turnOff();
		
		//start retracting the sections
		for(SnakeSection section : sections)
			section.startRetract();
		
		//fade out rattle sounds
		//for(Sample s : rattleSamples) {
		//	ap.shiftGain(ap.getGain(), -80, 2000);
			//ap.setGain(-80);
		//}
		
		//flag snake as retracting
		retracting = true;
	}
	
	/**
	 * Update.
	 */
	public void update() {
		//if the snake is retracting,
		//then update the sections
		if (retracting) {
			//make the sections retract to the origin
			for(SnakeSection section : sections)
				section.update();
			
			//if all the sections are out of the screen,
			//reset to origin (this solves the twitching snake syndrome)
			if (outside()) {
				for(SnakeSection section : sections)
					section.reset();
				retracting = false;
			}
		}
		//if we're not retracting, and we're biting on something
		else if (bitTouch != null) {
			//then adjust the position of the bitten object to match the touch			
			prey.position().set(bitTouch.x, bitTouch.y, 0);
			
			//if the head is very close to the prey, then play the strike sound
			if (!bitSoundPlayed && distanceFromPrey() < 600) {
			//if (!bitSoundPlayed && head().velocity().length() > 5) {
				for(Sample s : strikeSamples)
					s.play();
				for(Sample s : rattleSamples)
					s.repeat();
				bitSoundPlayed = true;
			}

			//if (headPos.distanceTo((float)bitWord.bounds.getCenterX(), (float)bitWord.bounds.getCenterY(), bitWord.position.z) < 100) {
			if (!bitWordContracted) {
				Vector3D pos = prey.position();
				if (bitWord.bounds.contains(head().position().x(), head().position().y())) {
					bitWord.contract(pos.x(), pos.y());
					bitWordContracted = true;
				}
			}
		}
		
		//update the letter positions, which are controlled by the sections
		//TODO: update this only if the positions moved
		PVector newPosition = new PVector();
		for(int i = 0; i < letterPositions.length; i++) {
			newPosition.set(0, 0, 0);
			for(int j = 0; j < sections.length; j++) {
				newPosition.add(sections[j].particle.position().x() * letterForces[i][j],
								sections[j].particle.position().y() * letterForces[i][j],
								sections[j].particle.position().z() * letterForces[i][j]);
			}
			letterPositions[i].set(newPosition);
		}
	}
	
	/**
	 * Get the distance between the head and the prey (bite).
	 * @return distance in pixels
	 */
	public float distanceFromPrey() {
		return head().position().distanceTo(prey.position());
	}
	
	/**
	 * Check if the snakes is outside the window.
	 * @return true if completely outside
	 */
	public boolean outside() {
		final Rectangle windowBnds = new Rectangle(0, 0, p.width, p.height);
		for(int i = 0; i < letterPositions.length; i++) {
			if (windowBnds.intersects(letterBounds[i].x + letterPositions[i].x,
									  letterBounds[i].y + letterPositions[i].y,
									  letterBounds[i].width, letterBounds[i].height)) return false;
		}
		return true;
	}
	
	/**
	 * Draw.
	 */
	public void draw() {
		//if there is no section, nothing to draw
		if (sections.length == 0) return;

		//set the font and align
		p.textFont(pfont);
		p.textAlign(PConstants.CENTER, PConstants.CENTER);
		
		//loop through all letter positions
		for(int i = 0; i < letterPositions.length; i++) {
			//move to the right position and draw the letter
			p.translate(letterPositions[i].x, letterPositions[i].y, letterPositions[i].z);
			p.textSize(pfont.getSize()*letterScales[i]);
			p.text(cleanText.charAt(i), 0, 0);
			p.translate(-letterPositions[i].x, -letterPositions[i].y, -letterPositions[i].z);
		}		
	}
	
	/**
	 * Draw the bounds of the letters.
	 */
	public void drawBounds() {
		for(int i = 0; i < letterPositions.length; i++) {
			p.rect(letterBounds[i].x + letterPositions[i].x,
					letterBounds[i].y + letterPositions[i].y, letterBounds[i].width, letterBounds[i].height);
		}
	}
	
	/**
	 * Draw the skeleton.
	 */
	public void drawSkeleton() {
		//if there is no section, then nothing to draw
		if (sections.length == 0) return;
		
		//save the fill color
		int savedFill = p.g.fillColor;

		//loop through sections and draw a line between each, and a dot for each
		Particle lastPt = null;
		for(SnakeSection section : sections) {
			p.noFill();

			if (lastPt == null) lastPt = section.particle;
			else {
				p.line(lastPt.position().x(), lastPt.position().y(), section.particle.position().x(), section.particle.position().y());
				lastPt = section.particle;
			}
			
			p.fill(savedFill);
			p.ellipse(section.particle.position().x(), section.particle.position().y(), 8, 8);
		}		
	}
	
	/**
	 * Draw the curved spine.
	 */
	public void drawSpine() {
		//if there is less than two sections, we can't draw a curve
		if (sections.length < 2) return;

		//draw the fill color
		int savedFill = p.g.fillColor;
		
		//set to not fill
		p.noFill();
		
		//draw the curved spine calculate from the section positions
		p.beginShape();
		p.curveVertex(sections[0].particle.position().x(), sections[0].particle.position().y(), sections[0].particle.position().z());
		for(SnakeSection section : sections) {
			p.curveVertex(section.particle.position().x(), section.particle.position().y(), section.particle.position().z());
		}
		p.curveVertex(sections[sections.length-1].particle.position().x(), sections[sections.length-1].particle.position().y(), sections[sections.length-1].particle.position().z());
		p.endShape();
		
		p.fill(savedFill);
	}
	
	/**
	 * Get string value.
	 */
	public String toString() {
		return "Snake-" + id;
	}
}
