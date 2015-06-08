/*
 * Copyright (c) 2012 All Right Reserved, Jason E. Lewis [http://obxlabs.net]
 */

package net.obxlabs.rattlesnakes;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;

import processing.core.PApplet;

import pitaru.sonia_v2_9.*;

/**
 * Sound manager for Rattlesnakes sketch.
 * 
 * $LastChangedRevision$
 * $LastChangedDate$
 * $LastChangedBy$
 */
public class SoundManager {
	
	static Logger logger = Logger.getLogger(SoundManager.class);

	//the parent applet
	PApplet p;
	
	//constant for left and right channels
	static final int LEFT = 0;
	static final int RIGHT = 1;
	
	ArrayList<Shift> shiftingSamples; 		//shifting samples
	
	ArrayList<Sample> ambientSamples;		//ambient samples
	
	ArrayList<Sample> threatSamples;		//threat samples
	boolean activeThreats;					//true when threats are active
	Sample currentThreat;					//the current playing threat
	long currentThreatStart;				//when the current threat started
	long nextThreatTime;					//when the next threat should start
	float threatVolume;						//threat sample volume

	ArrayList<Sample> rattleSamples;		//rattle samples
	ArrayList<Sample> usedRattleSamples;	//used rattle samples
	ArrayList<Sample> rattleReleaseQueue;	//rattle samples queued for release
	
	ArrayList<Sample> strikeSamples;		//strike samples
	ArrayList<Sample> usedStrikeSamples;	//used strike samples
	ArrayList<Sample> strikeReleaseQueue;	//strike sample queued for release
	Sample firstStrike;						//sample for the first strike
	
	/**
	 * Constructor.
	 * @param parent parent applet
	 */
	public SoundManager(PApplet parent) {
		p = parent;
		Sonia.start(p);

		firstStrike = null;
		shiftingSamples = new ArrayList<Shift>();
		
		currentThreat = null;
		currentThreatStart = -1;
		nextThreatTime = 0;
		activeThreats = false;
		threatVolume = 1;
		
		logger.info("Initialized sound manager.");
	}
	
	/**
	 * Load ambient samples from a folder.
	 * @param folder
	 */
	public void loadAmbientSamples(String folder) {
		logger.info("Loading ambient samples...");
		ambientSamples = loadSamples(folder);
	}
	
	/**
	 * Load threat samples from a folder.
	 * @param folder
	 * @param volume
	 */
	public void loadThreatSamples(String folder, float volume) {
		logger.info("Loading threat samples...");
		threatSamples = loadSamples(folder);
		threatVolume = volume;
	}
	
	/**
	 * Load rattle samples from a folder.
	 * @param folder
	 */
	public void loadRattleSamples(String folder) {
		logger.info("Loading rattle samples...");
		rattleSamples = loadSamples(folder);		
		usedRattleSamples = new ArrayList<Sample>();
		rattleReleaseQueue = new ArrayList<Sample>();
	}

	/**
	 * Load samples from a folder.
	 * @param folder
	 * @return list of loaded samples
	 */
	private ArrayList<Sample> loadSamples(String folder) {
		//read sound files
		String[] files = aifFilesInDirectory(folder);
		Arrays.sort(files);
		
		logger.info("...found " + files.length + " in " + folder);
		
		if (files.length == 0) return null;
		
		ArrayList<Sample> samples = new ArrayList<Sample>(files.length);
		for(int i = 0; i < files.length; i++) {
			Sample s = new Sample(folder + java.io.File.separator + files[i]);
			logger.info(" - " + files[i]);

			//mute by default
			s.setVolume(0, LEFT);
			s.setVolume(0, RIGHT);
			
			samples.add(s);
		}	
		return samples;
	}
	
	/**
	 * Load strike samples from a folder, and find first strike.
	 * @param folder folder to search in
	 * @param first name of the first strike
	 */
	public void loadStrikeSamples(String folder, String first) {
		//read sound files
		String[] files = aifFilesInDirectory(folder);
		Arrays.sort(files);
		
		logger.info("...found " + files.length + " in " + folder);
		
		if (files.length == 0) return;
		
		strikeSamples = new ArrayList<Sample>(files.length);
		for(int i = 0; i < files.length; i++) {
			Sample s = new Sample(folder + java.io.File.separator + files[i]);
			logger.info(" - " + files[i]);
			
			//mute by default
			s.setVolume(0, LEFT);
			s.setVolume(0, RIGHT);
			strikeSamples.add(s);
			
			//look out for first strike
			if (files[i].compareTo(first) == 0)
				firstStrike = s;
		}
		
		usedStrikeSamples = new ArrayList<Sample>();
		strikeReleaseQueue = new ArrayList<Sample>();
	}	

	/**
	 * Get the path to an ambient sample file.
	 * @param file
	 * @return path
	 */
	static String ambientPath(String file) {
		return "sounds"+java.io.File.separator+"ambient"+java.io.File.separator+file;
	}

	/**
	 * Get the path to a threat sample file.
	 * @param file
	 * @return path
	 */
	static String threatPath(String file) {
		return "sounds"+java.io.File.separator+"threat"+java.io.File.separator+file;
	}

	/**
	 * Get the path to a rattle sample file.
	 * @param file
	 * @return path
	 */
	static String rattlePath(String file) {
		return "sounds"+java.io.File.separator+"rattle"+java.io.File.separator+file;
	}
	
	/**
	 * Get the path to a strike sample file.
	 * @param file
	 * @return path
	 */
	static String strikePath(String file) {
		return "sounds"+java.io.File.separator+"strike"+java.io.File.separator+file;
	}

	/**
	 * Get a list of exclusive rattles, which won't be assigned again until released.
	 * @param n number of rattles
	 * @return list of rattles
	 */
	public ArrayList<Sample> exclusiveRattles(int n) {
		//make space for the players
		ArrayList<Sample> samples = new ArrayList<Sample>(n);
		
		//grab n players from the off list
		for(int i = 0; i < n && !rattleSamples.isEmpty(); i++) {
			Sample s = rattleSamples.remove(Rattlesnakes.rand.nextInt(rattleSamples.size()));
			samples.add(s);
		}
		
		//add the players to the used players
		usedRattleSamples.addAll(samples);
		
		return samples;
	}
	
	/**
	 * Release a list of rattle samples
	 * @param samples samples to release
	 */
	public void releaseRattles(ArrayList<Sample> samples) {
		for(Sample s : samples)
			if (usedRattleSamples.remove(s))
				rattleSamples.add(s);
	}
	
	/**
	 * Fade out and release a list of rattle samples.
	 * @param samples samples to fade and release
	 * @param duration fade duration
	 * @param delay delay before fade
	 */
	public void fadeOutAndReleaseRattles(ArrayList<Sample> samples, int duration, int delay) {
		//add samples to shift queue
		for(Sample s : samples)
			shiftingSamples.add(new Fade(s, 0, duration, delay, true));
		
		//add samples to release rattle queue
		rattleReleaseQueue.addAll(samples);
	}
	
	/**
	 * Get a random threat sample.
	 * @return
	 */
	Sample randomThreat() {
		return threatSamples.get(Rattlesnakes.rand.nextInt(threatSamples.size()));
	}
	
	/**
	 * Get a list of exclusive strike samples, which won't be assigned again until released.
	 * @param n number of samples
	 * @return list of samples
	 */
	ArrayList<Sample> exclusiveStrikes(int n) {
		//make space for the players
		ArrayList<Sample> samples = new ArrayList<Sample>(n);
		
		//grab n players from the off list
		for(int i = 0; i < n && !strikeSamples.isEmpty(); i++) {
			Sample s = strikeSamples.remove(Rattlesnakes.rand.nextInt(strikeSamples.size()));
			samples.add(s);
		}
		
		//add the players to the used players
		usedStrikeSamples.addAll(samples);
		
		//remove
		return samples;
	}
	
	/**
	 * Get the exclusive first strike sample.
	 * @return sample
	 */
	Sample exclusiveFirstStrike() {
		if (strikeSamples.remove(firstStrike));
			usedStrikeSamples.add(firstStrike);
		
		return firstStrike;
	}
	
	/**
	 * Release a list of strike samples.
	 * @param samples
	 */
	public void releaseStrikes(ArrayList<Sample> samples) {
		for(Sample s : samples)
			if (usedStrikeSamples.remove(s))
				strikeSamples.add(s);
	}	
	
	/**
	 * Fade out and release a list of strike samples.
	 * @param samples samples to fade and release
	 * @param duration fade duration
	 */
	public void fadeOutAndReleaseStrikes(ArrayList<Sample> samples, int duration) {
		//add samples to shift queue
		for(Sample s : samples)
			shiftingSamples.add(new Fade(s, 0, duration, 0, true));
		
		//add samples to release strike queue
		strikeReleaseQueue.addAll(samples);
	}	
	
	/**
	 * Get the ambient sample at the specified index.
	 * @param index
	 * @return sample
	 */
	public Sample ambient(int index) {
		if (index < 0) return null;
		if (index >= ambientSamples.size()) return null;
		
		return ambientSamples.get(index);
	}
	
	/**
	 * Repeat ambient sample at the specified index.
	 * @param index
	 * @param volume
	 */
	public void repeatAmbient(int index, float volume) {
		repeatAmbient(index, volume, -1);
	}
	
	/**
	 * Repeat ambient sample at the specified index for a number of times.
	 * @param index
	 * @param volume
	 * @param repeats number of times to repeat
	 */
	public void repeatAmbient(int index, float volume, int repeats) {
		if (index < 0) return;
		if (index >= ambientSamples.size()) return;
		
		Sample s = ambientSamples.get(index); 
		s.setVolume(volume, LEFT);
		s.setVolume(volume, RIGHT);
		if (repeats == -1) s.repeat();
		else s.repeatNum(repeats);
	}

	/**
	 * Fade ambient sample at specified index.
	 * @param index
	 * @param to fade to volume
	 * @param duration fade duration
	 * @param stopWhenDone true to stop the sample when done fading
	 */
	public void fadeAmbient(int index, float to, int duration, boolean stopWhenDone) {
		//the ambient
		Sample s = ambientSamples.get(index);
	
		//add samples to shift queue
		shiftingSamples.add(new Fade(s, to, duration, 0, stopWhenDone));
	}
	
	/**
	 * Fade and repeat ambient at specified index.
	 * @param index
	 * @param to fade to volume
	 * @param duration fade duration
	 */
	public void fadeInAndRepeatAmbient(int index, float to, int duration) {
		//the ambient
		Sample s = ambientSamples.get(index);
		
		//add samples to shift queue
		shiftingSamples.add(new Fade(s, to, duration, 0, false));
		
		//start repeat
		s.repeat();
	}
	
	/**
	 * Update the samples.
	 */
	public void update() {		
		//update the shifting samples
		Iterator<Shift> shiftIt = shiftingSamples.iterator();
		while(shiftIt.hasNext()) {
			Shift s = shiftIt.next();
			s.update();
			if(s.isDone())
				shiftIt.remove();
		}
		
		Iterator<Sample> sampleIt;
		
		//check if any of the rattles are ready to be released
		sampleIt = rattleReleaseQueue.iterator();
		while(sampleIt.hasNext()) {
			Sample s = sampleIt.next();
			if (!s.isPlaying()) {
				rattleSamples.add(s);
				sampleIt.remove();
			}
		}

		//check if any of the rattles are ready to be released
		sampleIt = strikeReleaseQueue.iterator();
		while(sampleIt.hasNext()) {
			Sample s = sampleIt.next();
			if (!s.isPlaying()) {
				strikeSamples.add(s);
				sampleIt.remove();
			}
		}		
	
		//update threats
		if (activeThreats) {
			if (currentThreat == null && p.millis() > nextThreatTime) {
				if (currentThreatStart == -1) currentThreatStart = p.millis();
				float duration = (p.millis()-currentThreatStart)/(float)45000;
				if (duration > 1) duration = 1;
				float volume = duration * threatVolume;
				
				currentThreat = randomThreat();
				currentThreat.setVolume(volume);
				currentThreat.play();
			}
			else if (currentThreat != null && !currentThreat.isPlaying()) {
				currentThreat = null;
				nextThreatTime = p.millis()  + (int)Rattlesnakes.rand.nextInt(4000) + 4000; 
			}
		}
	}

	/**
	 * Start playing threat samples.
	 */
	public void playThreats() { activeThreats = true; }
	
	/**
	 * Stop playing theat samples.
	 */
	public void stopThreats() {
		if (currentThreat != null && currentThreat.isPlaying())
			currentThreat.stop();
		
		currentThreat = null;
		currentThreatStart = -1;
		nextThreatTime = 0;
		
		activeThreats = false;
	}
	
	/**
	 * Stop the sound manager.
	 */
	public void stop() { Sonia.stop(); }
	
	/**
	 * Shift interface.
	 * @author Bruno
	 *
	 */
	interface Shift {
		/**
		 * Update the shifting.
		 */
		public void update();
		
		/**
		 * Check if the shift is done.
		 * @return true when done
		 */
		public boolean isDone();
	}
	
	/**
	 * Fade transition for a sound sample.
	 */
	class Fade implements Shift {
		Sample sample;			//the sample
		float from, to;			//from and to volumes
		int in;					//duration
		long start;				//when to start in millis
		boolean stopWhenDone;	//flag to stop the sample when done fading
		boolean done;			//true when the fade is done
		
		/**
		 * Constructor.
		 * @param sample		the sample
		 * @param to			volume to fade to
		 * @param in			fade duration
		 * @param delay			delay before fade
		 * @param stopWhenDone	true to stop the sample when done fading
		 */
		public Fade(Sample sample, float to, int in, int delay, boolean stopWhenDone) {
			this.sample = sample;
			this.from = sample.getVolume(LEFT); //assume left = right
			this.to = to;
			this.in = in;
			this.stopWhenDone = stopWhenDone;
			this.start = p.millis() + delay;
			this.done = false;
		}
		
		/**
		 * Update
		 */
		public void update() {
			//already done? nothing to do
			if (done) return; 
			
			//check if we reached the start time for the fade
			long duration = p.millis()-start;
			if (duration < 0) return;

			//if we're done, set the volume to final target volume
			if (duration >= in) {
				sample.setVolume(to, LEFT);
				sample.setVolume(to, RIGHT);
				if(stopWhenDone)
					sample.stop();
				done = true;
				return;
			}
			//if we're still in the fade duration, adjust the volume
			else {
				float volume = from + (duration/(float)in)*(to-from);
				sample.setVolume(volume, LEFT);
				sample.setVolume(volume, RIGHT);
			}
		}
		
		/**
		 * Check if the fade is done.
		 */
		public boolean isDone() { return done; }
	}
	
	/**
	 * Get the list of files in a passed relative director.
	 */
	private String[] aifFilesInDirectory(String dir) {
		java.io.File folder = new java.io.File(dir);
		 
		// let's set a filter (which returns true if file's extension is .aif)
		java.io.FilenameFilter filter = new java.io.FilenameFilter() {
		  public boolean accept(File dir, String name) {
		    return name.toLowerCase().endsWith(".aif");
		  }
		};
		
		// list the files in the data folder, passing the filter as parameter
		return folder.list(filter);
	}	
}
