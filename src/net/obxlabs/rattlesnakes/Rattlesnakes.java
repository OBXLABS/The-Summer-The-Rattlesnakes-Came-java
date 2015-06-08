/*
 * Copyright (c) 2012 All Right Reserved, Jason E. Lewis [http://obxlabs.net]
 */

package net.obxlabs.rattlesnakes;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import TUIO.TuioCursor;
import TUIO.TuioProcessing;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PVector;
import traer.physics.ParticleSystem;

/**
 * Rattlesnakes' Processing sketch.
 * 
 * $LastChangedRevision: 66 $
 * $LastChangedDate: 2012-12-12 12:43:37 -0500 (Wed, 12 Dec 2012) $
 * $LastChangedBy: bnadeau $
 */
public class Rattlesnakes extends PApplet {

	private static final long serialVersionUID = -6174986636083286207L;

	static Logger logger = Logger.getLogger(Rattlesnakes.class);
	
	//random number generator
	static XSRandom rand = new XSRandom(System.nanoTime());

	//physics system
	static ParticleSystem physics;
	
	//constants
	static final int FPS = 60;
	static final int START_TEXT_INDEX = 5;
	
	//properties editable in the config.properties file
	static boolean FULLSCREEN;				//true to open in fullscreen
	static int FRAME_WIDTH;					//frame width in window mode
	static int FRAME_HEIGHT;				//frame height in window mode
	static int BG_COLOR;					//background color
	static String SNAKE_FONT;				//name of snake font
	static int SNAKE_FONT_SIZE;				//size of snake font
	static String SNAKE_FILE;				//name of snake texts file
	static int SNAKE_COLOR;					//snake color
	static String[] TEXT_FILES;				//text files for main background text
	static float[][] TEXT_LINES_SPACINGS;	//line spacings for each line and text
	static String TEXT_FONT;				//name of main background text font
	static float TEXT_VERTICAL_MARGIN;		//top margin between edge and text
	static float TEXT_HORIZONTAL_MARGIN;	//left and right margins between edges and text
	static int TEXT_COLOR;					//text color
	static float TEXT_FADEIN_SPEED;			//speed at which the words fade in
	static float TEXT_FADEOUT_SPEED;		//speed at which the words fade out
	static int UNBITABLE_MARGIN;			//number of pixels were words can't be bit
	static float SNAKE_BITE_MASS;			//mass of the snake's bitable prey
	static float SNAKE_BITE_STRENGTH_MULT;  //strength of the snake's attraction to its prey
	static float SNAKE_BITE_MIN_DISTANCE;	//minimum distance for the snake's attraction
	static float SNAKE_BITE_OPACITY_TRIGGER;//minimum word opacity for a snake to bite
	static float[] SNAKE_SCALING_FACTORS;	//scaling factors to define the snake's shape
	static float[] SNAKE_SCALING_POSITIONS;	//scaling positions to define the snake's shape
	static int TEXT_CHANGE_INTERVAL;		//interval between background text changes
	static int TEXT_CHANGE_SPEED;			//speed at which the text wave animates
	static int IDLE_INTERVAL;				//time until idle state with background animations
	static int BGSNAKE_INTERVAL;			//interval between background idle animations
	static float BGSNAKE_OPACITY;			//opacity of background animation words
	
	static int FIRST_BITE_DELAY;			//delay between touch and a first bite
	static int NEXT_BITE_MINIMUM_DELAY;		//minimum delay between a touch and a second bite
	static int NEXT_BITE_MAXIMUM_DELAY;		//maximum delay between a touch and a second bite
	
	static int SHIFT_SOUNDS_TIME;			//time in millis to shift sounds between texts
	static float AMBIENT_VOLUME_START;		//volume of first ambient sounds
	static float AMBIENT_VOLUME_END;		//volume of ambient sounds after fade down
	static float THREAT_VOLUME;				//volume of threat sounds
	static float RATTLE_VOLUME;				//volume of rattle sounds
	static float STRIKE_VOLUME;				//volume of strike sounds
	static String FIRST_STRIKE_SND_FILE;	//first strike sound file

	static float PHYSICS_GRAVITY;			//gravity of the particle system
	static float PHYSICS_DRAG;				//drag of the particle system
	static int SMOOTH_LEVEL;				//anti-aliasing level
	
	static boolean TUIO_ENABLE;				//true to enable tuio input
	static int TUIO_PORT;					//port of the TUIO connection
	
	static boolean DEBUG;					//true to show debug layer
	static boolean MENU;					//true to show menu layer
	static boolean BOUNDS;					//true to show word bounds
	static boolean BG_TEXT;					//true to show background text (all the time)
	
	PFont debugFont;						//font used in the debug layer
	
	PFont snakeFont;						//font used for the snakes
	Snake[] snakes;							//the snakes
	String[] snakeLines;					//lines of text for the snakes
	boolean firstBite;						//true until the first bite
	ArrayList<Snake> rippleSnakes;			//array of snakes that caused a ripple
	ArrayList<Ripple> ripples;				//array of active ripples
	
	int textIndex;							//index of the current background text
	String[][] textLines;					//lines of the background texts
	Word[][][] textWords;					//words of the background texts
	HashMap<Integer, PFont> textFonts;		//fonts of the background lines
	int[][] textFontSizes;					//fonts of the background lines
	float[][] textWordSpacing;				//word spacing offset for the background lines
	int totalWordsSeen;						//counter of total word seen for a page
	int totalWords;							//counter of total words in a page
	
	ArrayList<Word> biteableWords;			//array of currently bitable words

	BezierPath bgSnake;						//snaky path
	ArrayList<Word> visibleWords;			//list of hightlighted words
	long lastTouch;							//last time there was a touch
	long lastBgSnake;						//last time an idle animatiom started
	
	boolean changingLock;					//true (locked) until we get a first touch
	boolean changing;						//true when we are changing the text
	int changingText;						//index of the changing text
	long lastChanging;						//last time the text changed
	int[] textChangingDelays;				//array of text changing delays
	long nextTextChange;					//time (in millis) when the text is allowed to change
	float textChangeSpeed;					//offset for text change speed used when touching during change
	
	TuioProcessing tuioClient;				//TUIO client
	Map<Integer, Touch> touches;			//map of current active touches
	String tuioServerAddr;					//address of the TUIO server
	
	static SoundManager soundManager;		//sound manager for the application
	
	/**
	 * Main application setup.
	 */
	public void setup() {
		//if set, open in fullscreen
		if (FULLSCREEN) {
			//fill up the screen
			size(displayWidth, displayHeight, OPENGL);
			if (!DEBUG) noCursor(); //hide the cursor
		}
		//if not, open in standard window mode
		else {
			size(FRAME_WIDTH, FRAME_HEIGHT, OPENGL);
		}

		//disable the depth
		hint(DISABLE_DEPTH_TEST);
		hint(DISABLE_DEPTH_SORT);
		
		if (SMOOTH_LEVEL > 0) smooth(SMOOTH_LEVEL);	//set anti-aliasing
		else noSmooth();
		
		background(BG_COLOR);	//clear background
		frameRate(FPS);			//limit framerate
		
		if (touches == null)
			touches = Collections.synchronizedMap(new HashMap<Integer, Touch>());

		//init TUIO client
		if (TUIO_ENABLE) {
			logger.info("Init TUIO");
			tuioClient  = new TuioProcessing(this);
			try {
			    InetAddress addr = InetAddress.getLocalHost();
	
			    //convert to string
			    tuioServerAddr = addr.getHostAddress() + ":" + TUIO_PORT;
			} catch (UnknownHostException e) {
				logger.warn(e.getMessage());
			}
		}
				
		//init particle system
		if (physics == null)
			physics = new ParticleSystem( PHYSICS_GRAVITY, PHYSICS_DRAG );
		
		//create the debug font
		debugFont = createFont("Arial", 16);

		//read the text lines
		readTexts();
		
		//setup the fonts
		setupFonts();
		
		//setup the background texts
		setupTexts();
		
		//setup audio
		setupAudio();

		//setup the snakes
		setupSnakes();
		
		//setup the background animation
		setupIdleAnimation();
		
		//setup the changing animation
		setupChangingOfTheTexts();
		
		//start audio
		soundManager.ambient(0).setVolume(0);
		soundManager.fadeInAndRepeatAmbient(0, AMBIENT_VOLUME_START, 5000);
	}

	/**
	 * Exit the app.
	 */
	public void exit()
	{
		//stop the sounds manager
		soundManager.stop();

		//call overridden super method
		super.exit();
	}
	
	/**
	 * Read text lines.
	 */
	public void readTexts() {
		textLines = new String[TEXT_FILES.length][];
		
		//read the lines of each text
		for(int i = 0; i < TEXT_FILES.length; i++)
			//read the lines from the text file
			textLines[i] = loadStrings(TEXT_FILES[i]);
	}
	
	/**
	 * Create and setup the bitmap fonts necessary to display the texts.
	 */
	public void setupFonts() {
		textFonts = new HashMap<Integer, PFont>();
		textFontSizes = new int[TEXT_FILES.length][];

		//create map of font size to characters
		HashMap<Integer, String> charsets = new HashMap<Integer, String>();
		
		//get the characters we need for each font size
		for(int i = 0; i < TEXT_FILES.length; i++) {
			//create the tmp font used to calculate font sizes
			//we base our calculations on this font and then create the
			//font closest to the size needed for a line of text to fill
			//the width of the screen
			PFont tmpFont = createFont(TEXT_FONT, 72);

			//make space for fonts sizes
			textFontSizes[i] = new int[textLines[i].length];
			
			//for each line, calculate the needed font size and split in words
			for(int j = 0; j < textLines[i].length; j++) {
				//set the temp font
				textFont(tmpFont);
				
				//find the best size that fits the width of the screen
				//by trying the temp font at different sizes
				int tmpSize = 24;
				do {
					textSize(tmpSize+=1);
				} while(textWidth(textLines[i][j]) < width-TEXT_HORIZONTAL_MARGIN*2);

				//we found the closest size, so create the font for that line
				//and start using it for calculations
				textFontSizes[i][j] = tmpSize-=1;
				
				//get the set of characters for this size
				String charset = charsets.get(tmpSize);
				//if it's the first time we get this size
				//we won't have a charset, so make an empty one
				if (charset == null)
					charset = "";
				
				//add characters from the line that are not in the charset
				for(int k = 0; k < textLines[i][j].length(); k++) {
					char c = textLines[i][j].charAt(k);
					if (charset.indexOf(c) == -1) charset += c;
				}
				
				//save for later
				charsets.put(tmpSize, charset);
			}
		}
		
		//go through the font sizes and create a font for
		//each but only for the characters we saw in the texts
		Iterator<Integer> it = charsets.keySet().iterator();
		while(it.hasNext()) {
			Integer tmpSize = it.next();
			String charset = charsets.get(tmpSize);
			
			PFont pfont = createFont(TEXT_FONT, tmpSize, true, charset.toCharArray());
			textFonts.put(tmpSize, pfont);
		}
	}
	
	/**
	 * Setup the background texts.
	 */
	public void setupTexts() {
		textWords = new Word[TEXT_FILES.length][][];
		textWordSpacing = new float[TEXT_FILES.length][];

		//load each text
		for(int i = 0; i < TEXT_FILES.length; i++)
			setupText(i);
		
		//set to start with first text
		textIndex = START_TEXT_INDEX;
		totalWords = totalWordsForText(textIndex);
		totalWordsSeen = 0;
	}
	
	/**
	 * Get the total number of words in a page of text
	 * @param index index of the page
	 * @return
	 */
	public int totalWordsForText(int index) {
		int count = 0;
		for(Word[] words : textWords[index])
			count += words.length;
		return count;
	}
	
	/**
	 * Setup a specified text from the list of text files.
	 * @param index index of the text to setup
	 */
	public void setupText(int index) {
		//make sure the index is within bounds
		if (index < 0) return;
		if (index >= TEXT_FILES.length) return;
			
		//words and word spacings
		textWords[index] = new Word[textLines[index].length][];
		textWordSpacing[index] = new float[textLines[index].length];
		
		//create the word arrays to store touched words
		biteableWords = new ArrayList<Word>();
		//skippedWords = new ArrayList<Word>();
		
		//start position words at the set margin
		float x = TEXT_HORIZONTAL_MARGIN;
		float y = TEXT_VERTICAL_MARGIN;
		
		//for each line, calculate the needed font size and split in words
		for(int i = 0; i < textLines[index].length; i++) {
			//set the font and font size
			textSize(textFontSizes[index][i]);		
			textFont(textFonts.get(textFontSizes[index][i]));

			//get the width of the line using that font
			float lineWidth = textWidth(textLines[index][i]);
			
			//the lines are rarely exactly the width of the screen,
			//so space the words 
			String[] wordStrings = textLines[index][i].split(" ");
			textWordSpacing[index][i] = 0;
			if (wordStrings.length > 1)
				textWordSpacing[index][i] = (width-TEXT_HORIZONTAL_MARGIN*2-lineWidth)/(float)(wordStrings.length-1) + textWidth(" ");
			
			//create the word objects
			textWords[index][i] = new Word[wordStrings.length];
			for(int w = 0; w < wordStrings.length; w++) {
				textWords[index][i][w] = new Word(this, wordStrings[w]);
				textWords[index][i][w].opacity = 0;
				textWords[index][i][w].position.set(x, y + textAscent(), 0);								
				textWords[index][i][w].bounds.setBounds((int)(x - textWordSpacing[index][i]/2),
														(int)(y - ((TEXT_LINES_SPACINGS.length > index && TEXT_LINES_SPACINGS[index].length > i) ? TEXT_LINES_SPACINGS[index][i]/2+1 : 0)),
														(int)(textWidth(wordStrings[w]) + textWordSpacing[index][i]),
														(int)((textAscent()+textDescent()) + ((TEXT_LINES_SPACINGS.length > index && TEXT_LINES_SPACINGS[index].length > i) ? TEXT_LINES_SPACINGS[index][i]+2 : 0)));

				//move the cursor to the next word's position
				x += textWidth(wordStrings[w]) + textWordSpacing[index][i];
			}
			
			//move the cursor to the start of the next line
			x = TEXT_HORIZONTAL_MARGIN;
			y += textAscent() + textDescent() +
					((TEXT_LINES_SPACINGS.length > index && TEXT_LINES_SPACINGS[index].length > i) ? TEXT_LINES_SPACINGS[index][i] : 0);
		}
	}
	
	/**
	 * Setup the snakes.
	 */
	public void setupSnakes() {
		//read the snake lines from the text file
		snakeLines = cleanStrings(loadStrings(SNAKE_FILE));
				
		//create the snake font
		snakeFont = createFont(SNAKE_FONT, SNAKE_FONT_SIZE);
				
		//create the snakes
		snakes = new Snake[4];
		
		//top-left snake
		snakes[0] = new Snake(this, 0, snakeLines[0], snakeFont, 24, RIGHT);
		snakes[0].setScales(SNAKE_SCALING_POSITIONS, SNAKE_SCALING_FACTORS);
		snakes[0].setBite(SNAKE_BITE_MASS, SNAKE_BITE_STRENGTH_MULT, SNAKE_BITE_MIN_DISTANCE);
		snakes[0].setRetract(RIGHT, 0, 0, 0.02f);
		snakes[0].setOrigin(50, 400, 4);
		snakes[0].translate(-100, -80, 0);
		snakes[0].retract();
	
		//top-right snake
		snakes[1] = new Snake(this, 1, snakeLines[1], snakeFont, 24, LEFT);
		snakes[1].setScales(SNAKE_SCALING_POSITIONS, SNAKE_SCALING_FACTORS);
		snakes[1].setBite(SNAKE_BITE_MASS, SNAKE_BITE_STRENGTH_MULT, SNAKE_BITE_MIN_DISTANCE);
		snakes[1].setRetract(RIGHT, 250, 25, 0.05f);
		snakes[1].setOrigin(50, 400, 4);
		snakes[1].translate(width-400, -80, 0);
		snakes[1].retract();
		
		//bottom-left snake
		snakes[2] = new Snake(this, 2, snakeLines[2], snakeFont, 24, RIGHT);
		snakes[2].setScales(SNAKE_SCALING_POSITIONS, SNAKE_SCALING_FACTORS);	
		snakes[2].setBite(SNAKE_BITE_MASS, SNAKE_BITE_STRENGTH_MULT, SNAKE_BITE_MIN_DISTANCE);
		snakes[2].setRetract(LEFT, 500, 50, 0.2f);
		snakes[2].setOrigin(50, 400, 4);
		snakes[2].translate(-100, height+90, 0);
		snakes[2].retract();

		//bottom-right snake
		snakes[3] = new Snake(this, 3, snakeLines[3], snakeFont, 24, LEFT);
		snakes[3].setScales(SNAKE_SCALING_POSITIONS, SNAKE_SCALING_FACTORS);		
		snakes[3].setBite(SNAKE_BITE_MASS, SNAKE_BITE_STRENGTH_MULT, SNAKE_BITE_MIN_DISTANCE);
		snakes[3].setRetract(LEFT, 100, 100, 0.4f);
		snakes[3].setOrigin(50, 400, 4);
		snakes[3].translate(width-400, height+90, 0);
		snakes[3].retract();	

		//flag to keep track of the first bite
		firstBite = true;
		
		//init the ripples caused by the snakes
		rippleSnakes = new ArrayList<Snake>(4);
		ripples = new ArrayList<Ripple>(4);
	}
	
	/**
	 * Setup the idle animations.
	 */
	public void setupIdleAnimation() {
		//create array of words highlighted by the animation
		visibleWords = new ArrayList<Word>();
		
		//init the last touch time
		lastTouch = millis();
		
		//reset the background snake (bezier path)
		resetBgSnake();
	}
	
	/**
	 * Reset the background snake animation.
	 */
	public void resetBgSnake() {
		//set the last time the background snake animation started
		lastBgSnake = millis();
		
		//reset the bezier path to a random position above the canvas
		float sx = random(0, width);
		float ex = random(0, width);
		bgSnake = new BezierPath(this,
				sx, -10,
				(sx + ex)/2.0f + random(-300, 300), width/3,
				(sx + ex)/2.0f + random(-300, 300), width*2/3,
				ex, height+100
		);
		
		//set it to randomly move up or down
		if (random(1.0f) < 0.5) bgSnake.reverse();		
	}
	
	/**
	 * Setup the text changing animation.
	 */
	public void setupChangingOfTheTexts() {
		//if the changing delays have never been initialized
		//create an array that holds as many delays as the text page with most lines
		if (textChangingDelays == null) {
			int maxDelays = 0;
			for(String[] lines : textLines)
				if (lines.length > maxDelays)
					maxDelays = lines.length;
			textChangingDelays = new int[maxDelays];
		}
		
		//lock until the first touch
		changingLock = true;
		
		//flag text as not changing
		changing = false;
		
		//set the last time the text changed
		lastChanging = millis();
		
		//set the next time the text is allowed to change
		nextTextChange = lastChanging + TEXT_CHANGE_INTERVAL;
	}
	
	/**
	 * Setup the audio using Minim. 
	 */
	public void setupAudio() {	
		//init sound manager
		soundManager = new SoundManager(this);

		//load sounds
		soundManager.loadAmbientSamples(dataPath(SoundManager.ambientPath("")));
		soundManager.loadThreatSamples(dataPath(SoundManager.threatPath("")), THREAT_VOLUME);
		soundManager.loadRattleSamples(dataPath(SoundManager.rattlePath("")));
		soundManager.loadStrikeSamples(dataPath(SoundManager.strikePath("")), FIRST_STRIKE_SND_FILE);
	}
	
	/**
	 * Clean an array of strings by removing empty strings and comment strings.
	 * @param strings array of strings usually loaded from a property file
	 * @return new array of clean strings
	 */
	public String[] cleanStrings(String[] strings) {
		//count how many strings we will have at the end
		int count = 0;
		for(String s : strings)
			if (!s.isEmpty() && s.charAt(0) != '#') count++;
		
		//create the new array
		String[] cleanStrings = new String[count];
		
		//fill the new array of clean strings
		count = 0;
		for(int i = 0; i < strings.length; i++) {
			if (!strings[i].isEmpty() && strings[i].charAt(0) != '#')
				cleanStrings[count++] = strings[i];
		}
		
		return cleanStrings;
	}
	
	/**
	 * Main draw.
	 */
	public void draw() {
		//clear background
		background(BG_COLOR);
		
		//update particle system
		physics.tick();
		
		//update the sound manager
		soundManager.update();

		//update ripples
		updateRipples();
		
		//if it's not time, then handle touches on words
		handleBitableWords();
		handleVisibleWords();		
		
		//check if it's time to change the text (and change if it is)
		long now = millis();
		if (changing || (!changingLock && nextTextChange < now && touches.isEmpty())) {
			changingOfTheTexts();
		}
		else {
			//check if it's time for the idle animation
			moveBgSnake();

			//make the snakes bite
			snakeBite();
		}
		
		//draw the current background text
		drawText(textIndex);
		if (changing) drawText(changingText);
		
		//draw the snakes
		drawSnakes();
		
		//draw debug and menu layer
		if (DEBUG) drawDebug();
		if (MENU) drawMenu();
	}
	
	/**
	 * Update the ripples.
	 */
	public void updateRipples() {
		//go through ripples and update them
		//remove them when they are too big
		Iterator<Ripple> it = ripples.iterator();
		while(it.hasNext()) {
			Ripple r = it.next();
			r.update();
			if (r.radius*r.radius > width*width+height*height)
				it.remove();
		}
		
		//check if biting snakes have bit and need to cause their ripple
		for(Snake s : snakes) {
			if (s.isBiting() && s.bitSoundPlayed && s.distanceFromPrey() < 40 && !rippleSnakes.contains(s)) {
				addRipple(s.prey.position().x(), s.prey.position().y());
				rippleSnakes.add(s);
			}
		}
	}
	
	/**
	 * Check if it's time to change the text, and change it if it is.
	 * @return true if we are changing the text, false if it's not time
	 */
	public boolean changingOfTheTexts() {
		//if we weren't changing on the last frame, then init changing values
		if (!changing) {
			changing = true;			//start changing
			changingText = textIndex;	//keep track of the changing text index
			lastChanging = millis();	//track when we started changing
			bgSnake.end();				//end the idle animation in case we're in the middle of it
			textChangeSpeed = TEXT_CHANGE_SPEED;	//reset the text change speed
			
			//we wan't each changing animation to be slightly different
			//so we generate random delays for each line to start animating
			updateChangingDelays();		
			
			//adjust the sounds for the next text
			updateChangingSounds();
		}
		
		//if there is a touch during the change
		//speed it up to move quickly to the next screen
		if (!touches.isEmpty() || textChangeSpeed != TEXT_CHANGE_SPEED) {
			textChangeSpeed *= 0.97f;
			if (textChangeSpeed <= 0) textChangeSpeed = 1;
		}
				
		//fade the words in and out in a wave based on when the changing animation started
		long diff = millis() - lastChanging;
		boolean done = true;
		for(int l = 0; l < textWords[changingText].length; l++) {
			//if we have reached the set delay for a line, start animation for the line
			if (diff > textChangingDelays[l]) {
				//calculate the word to fade in based on the time since the animation started
				//TODO move the TEXT_CHANGE_SPEED into the config file
				int i = (int)((diff-textChangingDelays[l])/textChangeSpeed*textWords[changingText][l].length);
				for(int wIndex = 0; wIndex < textWords[changingText][l].length; wIndex++) {
					Word word = textWords[changingText][l][wIndex];
					//fade in the word reached by the animation at this point
					//if it's already fading in, no need to ask twice
					if (wIndex == i) {
						if (!word.isFadingIn())
							if (textChangeSpeed==TEXT_CHANGE_SPEED)
								word.fadeIn(0.8f, 0.03f, 0.01f);
							else
								word.fadeIn(0.8f, 0.3f, 0.1f);
						done = false;
					}
					//fade out all the other words of the line
					else {
						//if its opacity is above zero, then we need to fade it out
						if (word.opacity > 0) {
							//don't fade out until done fading in
							//and no need to fade out if it's already fading out
							if (!word.isFading())
								if (textChangeSpeed==TEXT_CHANGE_SPEED)								
									word.fadeOut();
								else
									word.fadeOut(0.1f);
							else done = false;
						}
						else
							word.setSeen(false);
					}
				}
			}
			else {
				done = false;
			}
		}
		
		//if all the words were faded in then out completely
		if (done) {
			//flag text as not changing
			changing = false;
			
			//set the last time the text changed
			lastChanging = millis();
			
			//set the next time the text is allowed to change
			nextTextChange = millis() + TEXT_CHANGE_INTERVAL;

			//move to the next (and check if we reached the end)
			if (++textIndex >= TEXT_FILES.length) {
				firstBite = true;
				changingLock = true;
				textIndex = 0;
			}
			
			//reset word seen counter
			totalWords = totalWordsForText(textIndex);
			totalWordsSeen = 0;
		}

		//return true when we are changing
		return true;
	}
	
	/**
	 * Generate random delay for each line of the changing text animation.
	 */
	public void updateChangingDelays() {
		//generate the random delays
		int minDelay = Integer.MAX_VALUE;
			
		for(int i = 0; i < textWords[textIndex].length; i++) {
			//changingDelays.set(i, delay);
			textChangingDelays[i] = rand.nextInt()%1000;
			if (textChangingDelays[i] < minDelay) minDelay = textChangingDelays[i];
		}
		
		//if some of the random delays are negative, then offset them
		//so that the minimum is zero so that the animation starts smoothly
		for(int i = 0; i < textChangingDelays.length; i++)
			textChangingDelays[i] -= minDelay;
	}
	
	/**
	 * Change the sounds from the current to the next text.
	 */
	public void updateChangingSounds() {
		//the current text index is textIndex
		//and we are changing left or right based on chagingDirection
		//if moving from screen 1 to screen 2 fade to next sounds
		if (textIndex == 0) {
			//fade out first ambient sound
			soundManager.fadeAmbient(0, 0, 30000, true);
			//fade in second ambient sound
			soundManager.fadeInAndRepeatAmbient(1, AMBIENT_VOLUME_START, 30000);
			//start playing threat sounds
			soundManager.playThreats();
		}
		//when moving from screen 3 to 4
		else if (textIndex == 2) {
			//bring down the ambient sound
			soundManager.fadeAmbient(1, AMBIENT_VOLUME_END, TEXT_CHANGE_INTERVAL, false);
		}
		//when moving from the last to the first screen
		else if (textIndex == TEXT_FILES.length-1) {
			//fade in the first ambient sound
			soundManager.fadeInAndRepeatAmbient(0, AMBIENT_VOLUME_START, 30000);
			//fade out the second ambient sound
			soundManager.fadeAmbient(1, 0, 30000, true);
			//stop threat sounds
			soundManager.stopThreats();
		}
	}
	
	/**
	 * Manage the words that have been touches and set as bitable.
	 */
	public void handleBitableWords() {
		//fade out and bite off all bitable words that aren't under touches
		Iterator<Word> it = biteableWords.iterator();
		while(it.hasNext()) {
			Word w = it.next();
			
			//check if any of the active touches is within the word bounds
			boolean keep = false;
			synchronized(touches) {
				for(Integer id : touches.keySet()) {
					Touch t = touches.get(id);
					if (w.bounds.contains(t.x, t.y)) {
						keep = true;
						break;
					}
				}
			}
			
			//if no touch was within the word, then fade it out
			if (!keep) {
				//if the word passed a certain opacity, flag as seen
				if (!w.wasSeen()) {
					totalWordsSeen++;
					w.setSeen(true);
					
					//if we reach a high enough ratio of word seen
					//flag for changing of the text
					if (totalWordsSeen/(float)totalWords > 0.8) {
						int n = millis() + 3000;
						if (n < nextTextChange) nextTextChange = n;
					}
				}
				
				//fade out and remove it from the list
				w.fadeOut();
				it.remove();
			}
		}
		
		//highlight all words under touches
		synchronized(touches) {
			for(Integer id : touches.keySet()) {
				Touch t = touches.get(id);
				highlightWordAt((int)t.x, (int)t.y);
			}
		}
	}
	
	/**
	 * Manage the words that have been highlighted but that are not bitable.
	 * <p>Those words can include words highlighted by the idle animation,
	 * or words that can't be bitten.</p>
	 */
	public void handleVisibleWords() {
		//fade out visible words that aren't under the background snake
		//or under any touches
		Iterator<Word> it = visibleWords.iterator();
		while(it.hasNext()) {
			Word w = it.next();
			
			//if it's not fading in
			if (!w.isFading()) {
				//fade out and remove it from the list
				w.fadeOut();
				it.remove();
			}
		}
	}

	/**
	 * Check if it's time for the idle animation, and animated if it is.
	 * <p>This animation is controlled by a snaky bezier curve. A point
	 * moves and follows the curve, and highlights the words it passed over.</p>
	 */
	public void moveBgSnake() {
		//check if it's time for the idle animation
		long now = millis();
		if (!touches.isEmpty() || now - lastTouch < IDLE_INTERVAL || now - lastBgSnake < BGSNAKE_INTERVAL) return;
		
		//update the animation
		bgSnake.update();
		
		//if the animation cycle is done, reset it for next time
		if (bgSnake.done())
			resetBgSnake();

		//check if we are within the window
		if (bgSnake.x() < 0 || bgSnake.x() > width) return;
		if (bgSnake.y() < 0 || bgSnake.y() > height) return;
		
		//highlight the word under the idle animation location
		highlightWordAt((int)bgSnake.x(), (int)bgSnake.y(), BGSNAKE_OPACITY, false);
	}
	
	/**
	 * Make the snakes bite.
	 */
	public void snakeBite() {	
		//don't bite for the first 3 texts
		if (textIndex < 3) return;
		
		//increment one bite each text after that
		if (countSnakesBiting() > textIndex-3) return;
		
		//get time to trigger bites
		long now = millis();
		
		//for each bitable that is visible enough
		//check if its matching snake is available to bite
		for(Word w : biteableWords) {
			//make sure the word is visible enough
			if (w.opacity < SNAKE_BITE_OPACITY_TRIGGER) continue;
			
			//get the touch over that word to know where to bite
			Touch wtouch = null;
			synchronized(touches) {
				for(Integer id : touches.keySet()) {
					Touch t = touches.get(id);
					
					//if the word contains the touch, we have a match
					if (w.bounds.contains(t.x, t.y)) {

						//if the touch is already bitten
						//only bite the new touch after the assigned delay and if
						//this touch is bit less than the best found touch for the bite
						if (t.bites > 0) {
							if (now >= t.start+t.delay && (wtouch == null || t.bites < wtouch.bites))
								wtouch = t;
						}
						//if the touch has no bite yet, just wait for the first bite delay
						else if (now >= t.start+FIRST_BITE_DELAY) {
							wtouch = t;
							break;
						}
					}
				}
			}
			
			//if there's no touch over it, it might have moved away
			if (wtouch == null) continue;
			
			//get the snake for the given word
			Snake s = snakeForWord(w);
			
			//if not already biting, bite!
			if (!s.isBiting()) {
				//if this is the first bite
				if (firstBite) {
					//turn off first rattle players
					soundManager.stopThreats();
					
					//s.setStrikeSound(strikeSamples[firstStrikeSample]);
					s.setRattleSamples(soundManager.exclusiveRattles(1), RATTLE_VOLUME);
					s.setStrikeSample(soundManager.exclusiveFirstStrike(), STRIKE_VOLUME);
					
					firstBite = false;
				}
				else {
					int numSnds = numSoundsForText(textIndex);
					s.setSamples(soundManager.exclusiveStrikes(numSnds), STRIKE_VOLUME,
								 soundManager.exclusiveRattles(numSnds), RATTLE_VOLUME);
				}
				
				//bite that word/touch pair
				s.bite(w, wtouch);
				
				//increase the delay till the next snake bite
				wtouch.delay += (int)random(NEXT_BITE_MINIMUM_DELAY, NEXT_BITE_MAXIMUM_DELAY);
			}
		}
	}
	
	/**
	 * Add a ripple at x,y.
	 * @param x
	 * @param y
	 */
	public void addRipple(float x, float y) {
		ripples.add(new Ripple(x, y, 10 + rand.next(4)));
	}
	
	/**
	 * Check if any snake is biting.
	 * @return true if one or more snake is biting
	 */
	public boolean isAnySnakeBiting() {
		for(Snake s : snakes)
			if (s.isBiting()) return true;
		return false;
	}
	
	/**
	 * Get the number of biting snakes.
	 * @return the number of biting snakes
	 */
	public int countSnakesBiting() {
		int count = 0;
		for(Snake s : snakes)
			if (s.isBiting()) count++;
		return count;
	}
	
	/**
	 * Get the snake for a given word on the screen.
	 * @param w the word to find the snake for
	 * @return the snake
	 */
	public Snake snakeForWord(Word w) {
		//get the center of the word
		PVector center = w.center();
		
		//get the snake that matches the quadrant the word is in
		int index = 0;
		if (center.x > width/2) index += 1;
		if (center.y > height/2) index += 2;
	
		//return the snake
		return snakes[index];
	}
	
	/**
	 * Highlight a word completely (opaque) at x,y and make it bitable.
	 * @param x x position
	 * @param y y position
	 */
	public void highlightWordAt(int x, int y) {
		highlightWordAt(x, y, 1.0f, true);
	}
	
	/**
	 * Highlight a word at x,y.
	 * @param x x position
	 * @param y y position
	 * @param opacity opacity to fade to
	 * @param bite true if the word is bitable by snakes
	 */
	public void highlightWordAt(int x, int y, float opacity, boolean bite) {
		//find the line the word might be on
		//go through the lines and when the bounding box of the first word
		//of a line is passed the y position, the previous line was the one
		int lineIndex = 0;
		for(lineIndex = 0; lineIndex < textWords[textIndex].length; lineIndex++) {
			//if (textWords[textIndex][lineIndex].length == 0) continue;
			if (textWords[textIndex][lineIndex][0].bounds.y > y)
				break;
		}	
		--lineIndex;
		
		if (lineIndex == -1) return;
		
		//check if we are within the bitable margin
		if (x < UNBITABLE_MARGIN || x > width-UNBITABLE_MARGIN) bite = false;
		if (y < UNBITABLE_MARGIN || y > height-UNBITABLE_MARGIN) bite = false;

		//make sure that we're not between lines by checking that
		//the word actually contain the x,y position
		for(Word w : textWords[textIndex][lineIndex]) {
			if (w.bounds.contains(x, y)) {
				//if we want the word to be bitable
				if (bite) {
					//add it to the bitable list if it's not already there
					//and fade it in
					if (!biteableWords.contains(w)) {
						biteableWords.add(w);
						w.fadeIn(opacity, TEXT_FADEIN_SPEED, TEXT_FADEOUT_SPEED);
					}
				}
				//if we don't want the word to be bitable
				else {
					//add it to the list of visible words if not already there
					//and fade it in
					if (!visibleWords.contains(w)) {
						visibleWords.add(w);
						w.fadeIn(opacity, 0.02f, 0.01f);
					}
				}
			}
		}
	}
	
	/**
	 * Draw a background text.
	 * @param index index of the text to draw
	 */
	public void drawText(int index) {
		//set text color and alignment
		fill(TEXT_COLOR);
		noStroke();
		textAlign(LEFT);

		//loop through lines and their words and draw each
		for(int l = 0; l < textWords[index].length; l++) {
			//update and draw all words
			for(Word w : textWords[index][l]) {
				w.update();
				w.draw(ripples);
			}
		}
	}
	
	/**
	 * Draw the snakes.
	 */
	public void drawSnakes() {
		//loop through snakes
		for(Snake s : snakes) {
			//update the snake
			s.update();
		
			//draw the snake
			fill(SNAKE_COLOR);
			noStroke();
			s.draw();
		}		
	}
	
	/**
	 * Draw debug layer.
	 */
	public void drawDebug() {
		//draw debug info
		fill(0);
		noStroke();
		textAlign(LEFT, BASELINE);
		textFont(debugFont);
		text("fps: " + frameRate, 10, 24);		
		text("heap: " + ((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576) +
				" / " + (Runtime.getRuntime().totalMemory()/1048576) + "mb", 10, 24*2);
		text("TUIO server: " + tuioServerAddr, 10, 24*3);
		text("text: " + (textIndex+1), 10, 24*4);

		//draw debug text to make it easier to adjust the text
		if (BG_TEXT) {
			fill(0);
			noStroke();
			textAlign(LEFT);

			//loop through lines and draw all words as semi-transparent
			for(int l = 0; l < textWords[textIndex].length; l++) {
				textFont(textFonts.get(textFontSizes[textIndex][l]));
				
				for(Word w : textWords[textIndex][l]) {
					w.update();
					float savedOpacity = w.opacity;
					w.opacity = 0.5f;
					w.draw(ripples);
					w.opacity = savedOpacity;
				}
			}
		}
		
		//draw word bounds
		if (BOUNDS) {
			noFill();
			stroke(0, 50);
			for(int lineIndex = 0; lineIndex < textWords[textIndex].length; lineIndex++) {
				Word[] line = textWords[textIndex][lineIndex]; 
				for(int wordIndex = 0; wordIndex < line.length; wordIndex++) {
					Word w = line[wordIndex];
					rect(w.bounds.x, w.bounds.y, w.bounds.width, w.bounds.height);
				}
			}
		}
		
		//draw snake skeletons and spines
		for(Snake s : snakes) {
			fill(255, 0, 0);
			stroke(0, 100);
			s.drawSkeleton();
			stroke(0);
			s.drawSpine();
			
			if (BOUNDS) {
				stroke(0);
				noFill();
				s.drawBounds();
			}

			//draw prey (bite)
			fill(255, 255, 0);
			ellipse(s.prey.position().x(), s.prey.position().y(), 10, 10);
		}
		
		//draw touches
		synchronized (touches) {
			Iterator<Integer> it = touches.keySet().iterator();
		    while (it.hasNext()) {
		    	Touch touch = touches.get(it.next());

		    	// draw the Touch
   				fill(0, 128);
       			stroke(255, 0, 0);
       			ellipse(touch.x, touch.y, 50, 50);
		    }
		}
		
		noFill();
		stroke(0);
		
		//draw ripples
		for(Ripple r : ripples)
			ellipse(r.center.x, r.center.y, r.radius*2, r.radius*2);
		
		//draw background snake
		bgSnake.draw();
		
		//draw bitable margin
		noFill();
		stroke(0, 100);
		rect(UNBITABLE_MARGIN, UNBITABLE_MARGIN, width-UNBITABLE_MARGIN*2, height-UNBITABLE_MARGIN*2);
	}
	
	/**
	 * Draw the menu options.
	 */
	public void drawMenu() {
		//draw debug info
		fill(0);
		noStroke();
		textAlign(LEFT, BASELINE);
		textFont(debugFont);
		text("m: toggle this menu", 10, height-12);	
		text("d: toggle debug layer", 10, height-12-24*1);			
		text("b: toggle word bounds", 10, height-12-24*2);	
		text("t: toggle background text", 10, height-12-24*3);
		text("right arrow: move to next text", 10, height-12-24*4);		
	}
	
	/**
	 * Handle mouse press events.
	 */
	public void mousePressed() { 
		if (TUIO_ENABLE) return;
		if (mouseButton == RIGHT) return;
		addTouch(0, mouseX, mouseY);
	}
	
	/**
	 * Handle mouse release events.
	 */
	public void mouseReleased() {
		if (TUIO_ENABLE) return;
		if (mouseButton == RIGHT) return;
		removeTouch(0);
	}
	
	/**
	 * Handle mouse drag events.
	 */
	public void mouseDragged() {
		if (TUIO_ENABLE) return;
		if (mouseButton == RIGHT) return;
		updateTouch(0, mouseX, mouseY);
	}
	
	/**
	 * Handle key press events.
	 */
	public void keyPressed() {
		switch(key) {
		//show/hide debug layer
		case 'd':
		case 'D':
			DEBUG = !DEBUG;
			break;
		//show/hide menu
		case 'm':
		case 'M':
			MENU = !MENU;
			break;
		//show/hide word bounds
		case 'b':
		case 'B':
			BOUNDS = !BOUNDS;
			break;
		//show/hide background text
		case 't':
		case 'T':
			BG_TEXT = !BG_TEXT;
			break;
		}
		
		switch(keyCode) {
		//change text to next text
		case RIGHT:
			lastTouch = lastChanging = millis() - TEXT_CHANGE_INTERVAL - 1;
			nextTextChange = millis();
			break;
		}
	}
	
	/**
	 * Handle TUIO add cursor event.
	 * @param c the cursor
	 */
	public void addTuioCursor(TuioCursor c) 
	{
		addTouch(c.getCursorID(), c.getScreenX(width), c.getScreenY(height));
	}

	/**
	 * Handle TUIO update cursor event.
	 * @param c the cursor
	 */
	public void updateTuioCursor(TuioCursor c) 
	{
		updateTouch(c.getCursorID(), c.getScreenX(width), c.getScreenY(height));
	}

	/**
	 * Handle TUIO remove cursor event.
	 * @param c the cursor
	 */
	public void removeTuioCursor(TuioCursor c) 
	{
		removeTouch(c.getCursorID());
	}	
	
	/**
	 * Add a touch object to the active list.
	 * @param id id
	 * @param x x position
	 * @param y y position
	 */
	public void addTouch(int id, int x, int y) {
		//stop the idle animation
		bgSnake.end();
		
		//keep track of when we received the last touch
		lastTouch = millis();
				
		//add to touches
		touches.put(new Integer(id), new Touch(id, x, y, millis(), (int)random(NEXT_BITE_MINIMUM_DELAY, NEXT_BITE_MAXIMUM_DELAY)));
	}
	
	/**
	 * Update a touch objct in the active list.
	 * @param id id 
	 * @param x x position
	 * @param y y position
	 */
	public void updateTouch(int id, int x, int y) {
		//update the touch
		synchronized (touches) {
			Touch touch = touches.get(new Integer(id));
			if (touch != null) {
				touch.set(x, y);
			}
		}
	}
	
	/**
	 * Remove a touch object from the active list.
	 * @param id id
	 */
	public void removeTouch(int id) {
		//increase the text change timeout so that it doesn't
		//change right after release
		int n = millis() + (changingLock?TEXT_CHANGE_INTERVAL:2000);
		if (n > nextTextChange) nextTextChange = n;

		//if changing is locked, unlock it
		if (changingLock) changingLock = false;		
		
		//remove the touch
		Touch t = touches.remove(new Integer(id));
		
		//if any snake is biting that touch, then retract it
		//int numSnds = numSoundsForText(textIndex);
		int fadeDelay = 0;
		for(Snake s : snakes) {
			if (s.isBiting() && s.bitTouch == t) {			
				//retract the snake
				s.retract();
				
				//fade out rattle samples
				soundManager.fadeOutAndReleaseRattles(s.rattleSamples, 1000, fadeDelay);
				soundManager.fadeOutAndReleaseStrikes(s.strikeSamples, 1000);
				fadeDelay += 1000;
						
				//get a new set of sounds
				s.setSamples(null, 0, null, 0);
				
				rippleSnakes.remove(s);
			}
		}
	}
	
	public int numSoundsForText(int index) {
		//logger.debug("index " + index + " count " + countSnakesBiting());
		if (index < 5) return 1;
		else if (index < 6) return countSnakesBiting() < 2 ? 1 : 2;
		else return 2;
	}
	
	public static void main(String _args[]) {
		//configure logger
		PropertyConfigurator.configure("logging.properties");
		
		//load properties
		Properties props = new Properties();
		try {
	        //load a properties file
			props.load(new FileInputStream("config.properties"));
	 
	    	//get the standard properties
			FULLSCREEN = (Boolean.valueOf(props.getProperty("fullscreen", "true")));
			FRAME_WIDTH = (Integer.valueOf(props.getProperty("frame.width", "1280")));
			FRAME_HEIGHT = (Integer.valueOf(props.getProperty("frame.height", "720")));
			BG_COLOR = unhex(props.getProperty("background.color", "FF000000"));
			SNAKE_FONT = "fonts"+java.io.File.separator+props.getProperty("snake.font", "Arial")+".ttf";
			SNAKE_FONT_SIZE = (Integer.valueOf(props.getProperty("snake.font.size", "40")));
			SNAKE_FILE = props.getProperty("snake.file", "snakes.txt");
			TEXT_FILES = props.getProperty("text.files", "body.txt").split(",");
			
			//get the line spacing
			//each line spacing is separate by a comma,
			//and each set of line spacing of a text is separated by a semi-colon
			String[] textLineSpacings = props.getProperty("text.line.spacings", "0").split(";");
			TEXT_LINES_SPACINGS = new float[textLineSpacings.length][];
			for(int i = 0; i < TEXT_LINES_SPACINGS.length; i++) {
				String[] lineSpacings = textLineSpacings[i].split(",");
				TEXT_LINES_SPACINGS[i] = new float[lineSpacings.length];
				for(int j = 0; j < TEXT_LINES_SPACINGS[i].length; j++)
					TEXT_LINES_SPACINGS[i][j] = Float.valueOf(lineSpacings[j]);
			}
			
			TEXT_FONT = "fonts"+java.io.File.separator+props.getProperty("text.font", "Arial")+".ttf";
			TEXT_VERTICAL_MARGIN = (Float.valueOf(props.getProperty("text.vertical.margin", "20")));
			TEXT_HORIZONTAL_MARGIN = (Float.valueOf(props.getProperty("text.horizontal.margin", "20")));
			TEXT_FADEIN_SPEED = (Float.valueOf(props.getProperty("text.fadein.speed", "0.05")));
			TEXT_FADEOUT_SPEED = (Float.valueOf(props.getProperty("text.fadeout.speed", "0.01")));
			TEXT_COLOR = unhex(props.getProperty("text.color", "B4000000"));
			UNBITABLE_MARGIN = (Integer.valueOf(props.getProperty("unbitable.margin", "0")));
			//WORDS_BEFORE_BITE = (Integer.valueOf(props.getProperty("words.before.bite", "0")));
			SNAKE_BITE_STRENGTH_MULT = (Float.valueOf(props.getProperty("snake.bite.strength.multiplier", "7.69")));
			SNAKE_BITE_MIN_DISTANCE = (Float.valueOf(props.getProperty("snake.bite.minimum.distance", "50")));
			SNAKE_BITE_MASS = (Float.valueOf(props.getProperty("snake.bite.mass", "100")));
			SNAKE_COLOR = unhex(props.getProperty("snake.color", "C8000000"));
			SNAKE_BITE_OPACITY_TRIGGER = (Float.valueOf(props.getProperty("snake.bite.opacity.trigger", "1")));
			
			FIRST_BITE_DELAY = (Integer.valueOf(props.getProperty("first.bite.delay", "3000")));
			NEXT_BITE_MINIMUM_DELAY = (Integer.valueOf(props.getProperty("next.bite.minimum.delay", "5000")));
			NEXT_BITE_MAXIMUM_DELAY = (Integer.valueOf(props.getProperty("next.bite.maximum.delay", "15000")));
			
			//get the snake scaling to control the shape of the snake
			//it's made of two set of float separated by a semi-colon
			//on the left we have a set of float that indicated the position in the snake from 0 (tail) to 1 (head)
			//and on the right we have the scale factor to the letter at the matching position
			String[] snakeScaling = props.getProperty("snake.shape", "0,1;0,1").split(";");
			String[] snakeScalingPositions = snakeScaling[0].split(",");
			String[] snakeScalingFactors = snakeScaling[1].split(",");
			SNAKE_SCALING_FACTORS = new float[snakeScalingFactors.length];
			for(int i = 0; i < snakeScalingFactors.length; i++)
				SNAKE_SCALING_FACTORS[i] = Float.valueOf(snakeScalingFactors[i]);
			SNAKE_SCALING_POSITIONS = new float[snakeScalingPositions.length];
			for(int i = 0; i < snakeScalingPositions.length; i++)
				SNAKE_SCALING_POSITIONS[i] = Float.valueOf(snakeScalingPositions[i]);
			
			TEXT_CHANGE_INTERVAL = 1000*(Integer.valueOf(props.getProperty("seconds.between.text.change", "30")));
			TEXT_CHANGE_SPEED = 1000*(Integer.valueOf(props.getProperty("text.change.speed", "4")));
			IDLE_INTERVAL = 1000*(Integer.valueOf(props.getProperty("seconds.until.idle", "10")));
			BGSNAKE_INTERVAL = 1000*(Integer.valueOf(props.getProperty("seconds.between.idle.animations", "5")));
			BGSNAKE_OPACITY = (Float.valueOf(props.getProperty("idle.animation.opacity", "0.1f")));
			PHYSICS_GRAVITY = (Float.valueOf(props.getProperty("physics.gravity", "0")));
			PHYSICS_DRAG = (Float.valueOf(props.getProperty("physics.drag", "0.15")));
			SMOOTH_LEVEL = (Integer.valueOf(props.getProperty("smooth.level", "4")));
			
			SHIFT_SOUNDS_TIME = (Integer.valueOf(props.getProperty("shift.sounds.time", "3000")));

			AMBIENT_VOLUME_START = (Float.valueOf(props.getProperty("ambient.volume.start", "1.0")));
			AMBIENT_VOLUME_END = (Float.valueOf(props.getProperty("ambient.volume.end", "1.0")));
			THREAT_VOLUME = (Float.valueOf(props.getProperty("threat.volume", "1.0")));
			RATTLE_VOLUME = (Float.valueOf(props.getProperty("rattle.volume", "1.0")));
			STRIKE_VOLUME = (Float.valueOf(props.getProperty("strike.volume", "1.0")));
			FIRST_STRIKE_SND_FILE = props.getProperty("first.strike.sounds.file", "hi hat.aif");
			
			TUIO_ENABLE = (Boolean.valueOf(props.getProperty("tuio.enable", "true")));
			TUIO_PORT = (Integer.valueOf(props.getProperty("tuio.port", "3333")));
						
			DEBUG = (Boolean.valueOf(props.getProperty("debug", "false")));
			MENU = (Boolean.valueOf(props.getProperty("menu", "false")));
			BOUNDS = (Boolean.valueOf(props.getProperty("bounds", "false")));
			BG_TEXT = (Boolean.valueOf(props.getProperty("bg.text", "false")));
			
	        logger.info("Configuration properties loaded.");
		} catch (IOException ex) {
			logger.error("Exception occurred when trying to load config file.");
			ex.printStackTrace();
	    }
			
		//launch
		if (FULLSCREEN)
			//use present mode if fullscreen
			PApplet.main(new String[] { "--present", net.obxlabs.rattlesnakes.Rattlesnakes.class.getName() });
		else
			//standard mode for window
			PApplet.main(new String[] { net.obxlabs.rattlesnakes.Rattlesnakes.class.getName() });
	}
}
