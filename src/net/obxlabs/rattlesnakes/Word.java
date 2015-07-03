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

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.media.opengl.glu.GLUtessellatorCallbackAdapter;

import org.apache.log4j.Logger;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

/**
 * A word with some graphical properties that can fade in and out.
 * 
 * $LastChangedRevision: 66 $
 * $LastChangedDate: 2012-12-12 12:43:37 -0500 (Wed, 12 Dec 2012) $
 * $LastChangedBy: bnadeau $
 */
public class Word {
	
	static Logger logger = Logger.getLogger(Word.class);
	
	//ripple deformation constants
	static float RIPPLE_LENGTH = 400;	//length of ripple wave
	static int RIPPLE_CYCLES = 1;		//number of wave cycles
	static float RIPPLE_AMPLITUDE = 50;	//amplitude of deformation
	
	//fading state
	static enum FadeState { FADE_IN, FADE_OUT, STABLE; }
	
	//parent Processing applet
	PApplet p;
	
	float opacity;						//opacity
	String value;						//textual value
	float fadeInSpeed, fadeOutSpeed;	//fading speeds
	FadeState fadeState;				//fading state (in, out, stable)
	float fadeInTo;						//opacity to fade to
	PVector position;					//position
	Rectangle bounds;					//bounding rectangle
	boolean seen;						//flag to track if a word has been seen
	
	float contractFac;					//contract factor from 0 to 1, where 1 is most contracted
	float contractAcc;					//contract acceleration
	float contractVel;					//velocity affecting the contract factor
	PVector contractFrom;				//point to contract away from
	boolean contracting;				//true when contracting
	float contractPeriod;				//period of contraction for animation
	long contractStart;					//when did the contract start
	
	static ArrayList<Ripple> ripples = null;	//static ripples used by tessellator
	
	protected static FontRenderContext frc = new FontRenderContext(null, false, false);
	
	ArrayList<PVector> vertices;
	ArrayList<int[]> contours;
	GeneralPath outline;
	
    static protected GLU glu = new GLU();
    
    static protected GLUtessellator tessellator = GLU.gluNewTess();
    static float TESSELLATOR_DETAIL = 3.0f;

    static protected GLUtessellatorCallbackAdapter tessCallback;
    int tessCount;
    ArrayList<Float> tessInit;
    float[] tessOrig;
    float[] tess;
    //FloatBuffer tessBuffer;
    //int tessNumTriangles;
	//protected int tcVertexCount = 0;
	//protected boolean tcInitMode = false;
    
    //ArrayList<Float> initVertices;		//list of vertices used for initialization
    //float[] vbVertices;					//vertex buffer vertices
    //FloatBuffer vb;						//vertex buffer			
    //PShape shape;
    
	/**
	 * Constructor.
	 * @param parent parent Processing applet
	 * @param value textual value
	 */
	public Word(PApplet parent, String value) {
		this.p = parent;
		this.opacity = 1;
		this.value = value;
		this.fadeInSpeed = 0.05f;
		this.fadeOutSpeed = 0.01f;
		this.position = new PVector();
		this.fadeState = FadeState.STABLE;
		this.fadeInTo = 1.0f;
		this.bounds = new Rectangle();
		this.seen = false;
		
		this.contractFac = 0;
		this.contractAcc = 0;
		this.contractVel = 0;
		this.contractFrom = new PVector();
		this.contracting = false;
		this.contractPeriod = 100;
		this.contractStart = 0;
		
		initControlPoints(value);
		initOutline();
		initTessellator();
	}
	
	/**
	 * Update.
	 */
	public void update() {
		//fade in or out based on the current state
		if (fadeState != FadeState.STABLE) {
			switch(fadeState) {
			case FADE_IN:
				opacity += fadeInSpeed;
				if (opacity > fadeInTo) { opacity = fadeInTo; fadeState = FadeState.STABLE; }
				break;
			case FADE_OUT:
				opacity -= fadeOutSpeed;
				if (opacity < 0) { opacity = 0; fadeState = FadeState.STABLE; }
				break;
			default:
				break;
			}
		}
			
		//update the contract behavior
		updateContract();
	}
	
	/**
	 * Update contraction.
	 */
	public void updateContract() {
		//apply contract acceleration
		contractVel += contractAcc;
		contractAcc = 0;
		contractFac += contractVel;
		
		//apply friction
		contractVel *= 0.8;
		if (contractVel < 0.1) {
			//decontract
			contractFac *= 0.99;
			if (!contracting) {
				contractVel = 0;
				if (contractFac < 0.001) contractFac = 0;
			}
		}
	}
	
	/**
	 * Check if the word is contracted.
	 * @return true if contracted
	 */
	public boolean isContracted() { return contractFac != 0; }
	
	/**
	 * Contract the word.
	 * @param x x position to contract away from
	 * @param y y position to contract away from
	 */
	public void contract(float x, float y) {
		//if not contraction, set the contract for the first time
		if (!contracting) {
			contractFrom.set(x, y, 0);	
			contractPeriod = 100;
			contracting = true;
			contractStart = p.millis();
		}
		//if we're contracting, adjust it
		else {
			contractFrom.set(x, y, 0);
			contractPeriod = 100 + PApplet.sin((p.millis()-contractStart)/700.0f)*8;
		}
		
		//apply acceleration to contraction
		if (contractFac < 1) contractAcc += (1-contractFac)/5;
	}

	/**
	 * Check if the word is contracting.
	 * @return true if contracting.
	 */
	public boolean isContracting() { return contracting; }
	
	/**
	 * Starts decontracting.
	 */
	public void decontract() { contracting = false; }
	
	/**
	 * Flag the word as seen, it was made visible once.
	 * @param s true to set as seen
	 */
	public void setSeen(boolean s) { seen = s; }
	
	/**
	 * Check if the word has been seen.
	 * @return true if it was seen
	 */
	public boolean wasSeen() { return seen; }
	
	/**
	 * Check if the word is fading.
	 * @return true if it's fading
	 */
	public boolean isFading() { return fadeState != FadeState.STABLE; }
	
	/**
	 * Check if the word is fading in.
	 * @return true if the word is fading in, false if not.
	 */
	public boolean isFadingIn() { return fadeState == FadeState.FADE_IN; }
	
	/**
	 * Check if the word is fading out.
	 * @return true if the word is fading out, false if not.
	 */
	public boolean isFadingOut() { return fadeState == FadeState.FADE_OUT; }
	
	/**
	 * Fade in.
	 * @param opacity opacity to fade to
	 */
	public void fadeIn(float opacity, float speed) { fadeState = FadeState.FADE_IN; fadeInTo = opacity; fadeInSpeed = speed; }

	/**
	 * Fade in.
	 * @param opacity opacity to fade to
	 */
	public void fadeIn(float opacity, float inspeed, float outspeed) { fadeIn(opacity, inspeed); fadeOutSpeed = outspeed; }

	/**
	 * Fade out completely.
	 */
	public void fadeOut(float speed) { fadeState = FadeState.FADE_OUT; fadeOutSpeed = speed; }

	/**
	 * Fade out completely.
	 */
	public void fadeOut() { fadeState = FadeState.FADE_OUT; }
	
	/**
	 * Get the center position of the word.
	 * @return center position
	 */
	public PVector center() {
		return new PVector((float)bounds.getCenterX(), (float)bounds.getCenterY(), position.z);
	}
	
	/**
	 * Draw.
	 */
	public void draw(ArrayList<Ripple> ripples) {
		final float rotation = -PApplet.QUARTER_PI/4 - PApplet.QUARTER_PI;
		
		//if fully transparent, nothing to do
		if (opacity == 0) return;

        if (!ripples.isEmpty())
        	Word.ripples = ripples;
        else
        	Word.ripples = null;
        
		//save the fill color
		int savedFill = p.g.fillColor;

		
		p.pushMatrix();
			p.translate(position.x, position.y);
			
			updateTessellation();
			
			//draw the word's shadow
			p.fill(0, 20*opacity);
			p.rotateX(PApplet.QUARTER_PI);
			drawTessellation();
			
			//draw the word at the right opacity
			p.fill(savedFill, p.alpha(savedFill)*opacity);
			p.rotateX(rotation);
			drawTessellation();
			p.fill(savedFill);

		p.popMatrix();
	}
	
	public void updateTessellation() {
		float[] vertex = new float[3]; 
		
		for(int i = 0; i < tessOrig.length; i+=3) {
		  vertex[0] = tessOrig[i];
		  vertex[1] = tessOrig[i+1];
		  vertex[2] = tessOrig[i+2];
            
		  if (contractFac != 0)
           	contractVertex(vertex);

          //add ripples
          if (ripples != null)
          	rippleVertex(vertex);
          
          tess[i] = vertex[0];
          tess[i+1] = vertex[1];
          tess[i+2] = vertex[2];
		}
	}
	
	public void drawTessellation() {
		p.g.beginShape(PApplet.TRIANGLES); 
		for(int i = 0; i < tess.length; i+=3)
          p.g.vertex(tess[i], tess[i+1], tess[i+2]);
		p.g.endShape();
	}
	
	/**
	 * Tessellate the text.
	 */
	public void tessellate() {
		//six element array received from the Java2D path iterator
        float textPoints[] = new float[6];

        //get the path iterator
        PathIterator iter = outline.getPathIterator(null);

        tessCount = 0;
        GLU.gluTessBeginPolygon(tessellator, this);
        // second param to gluTessVertex is for a user defined object that contains
        // additional info about this point, but that's not needed for anything

        float lastX = 0;
        float lastY = 0;

        // unfortunately the tessellator won't work properly unless a
        // new array of doubles is allocated for each point. that bites ass,
        // but also just reaffirms that in order to make things fast,
        // display lists will be the way to go.
        double vertex[];

        while (!iter.isDone()) {
            int type = iter.currentSegment(textPoints);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    GLU.gluTessBeginContour(tessellator);

                    vertex = new double[] {textPoints[0], textPoints[1], 0}; 

                    //apply contract
                    //if (contractFac != 0)
                    //	contractVertex(vertex);
                    
                    //apply ripples
                    //if (ripples != null)
                    //	rippleVertex(vertex);
                    
                    GLU.gluTessVertex(tessellator, vertex, 0, this);
                    
                    lastX = textPoints[0];
                    lastY = textPoints[1];
                    
                    break;

                /*case PathIterator.SEG_QUADTO:   // 2 points
                        
                        for (int i = 1; i <= TESSELLATOR_DETAIL; i++) {
                            float t = (float)(i/TESSELLATOR_DETAIL);
                            vertex = new double[] {
                                    p.g.bezierPoint(
                                            lastX, 
                                            lastX + ((textPoints[0]-lastX)*2/3), 
                                            textPoints[2] + ((textPoints[0]-textPoints[2])*2/3), 
                                            textPoints[2], 
                                            t
                                    ),
                                    p.g.bezierPoint(
                                            lastY, 
                                            lastY + ((textPoints[1]-lastY)*2/3),
                                            textPoints[3] + ((textPoints[1]-textPoints[3])*2/3), 
                                            textPoints[3], 
                                            t
                                    ), 
                                    0
                            };
                            
                            //apply contract
                            if (contractFac != 0)
                            	contractVertex(vertex);

                            //add ripples
                            if (ripples != null)
                            	rippleVertex(vertex);
                            
                            GLU.gluTessVertex(tessellator, vertex, 0, vertex);
                        }
                    
                    lastX = textPoints[2];
                    lastY = textPoints[3];
                    
                    break;*/
                    
                case PathIterator.SEG_CUBICTO:
                	
                	for (int i = 1; i <= TESSELLATOR_DETAIL; i++) {
                        float t = (float)(i/TESSELLATOR_DETAIL);       
                        vertex = new double[] {
                        		p.g.bezierPoint(lastX, textPoints[0], textPoints[2], textPoints[4], t),
                        		p.g.bezierPoint(lastY, textPoints[1], textPoints[3], textPoints[5], t),
                        		0
                        }; 
                        
                        //apply contract
                        //if (contractFac != 0)
                        //	contractVertex(vertex);

                        //add ripples
                        //if (ripples != null)
                        //	rippleVertex(vertex);
                        
                        GLU.gluTessVertex(tessellator, vertex, 0, vertex);
                    }
                
	                lastX = textPoints[4];
	                lastY = textPoints[5];
	                
                	break;

                case PathIterator.SEG_CLOSE:
                    GLU.gluTessEndContour(tessellator);
                    
                    break;
            }
            
            iter.next();
        }
        
        
        GLU.gluTessEndPolygon(tessellator);
	}
	
	/**
	 * Apply ripple deformation to a vertex.
	 * @param vertex the vertex to deform
	 */
	public void rippleVertex(double[] vertex) {
		//get the vertex absolute position
		double vx = position.x + vertex[0];
		double vy = position.y + p.textAscent() + vertex[1];

		//for each ripple
		for(Ripple r : ripples) {
			//get the distance between the vertex and the edge of the ripple
			double distSq = ((vx-r.center.x)*(vx-r.center.x) + (vy-r.center.y)*(vy-r.center.y))/* - (r.radiusSq)*/;
			distSq = Math.sqrt(distSq);
			distSq -= r.radius;
			if (distSq < 0) distSq *= -1;
			//if the distance square is less than the threshold, apply ripple force
			if (distSq < RIPPLE_LENGTH)
				vertex[2] += Math.sin((distSq/RIPPLE_LENGTH)*RIPPLE_CYCLES*2*PApplet.PI) * RIPPLE_AMPLITUDE * (1-distSq/RIPPLE_LENGTH);
		}
	}

	/**
	 * Apply ripple deformation to a vertex.
	 * @param vertex the vertex to deform
	 */
	public void rippleVertex(float[] vertex) {
		//get the vertex absolute position
		float vx = position.x + vertex[0];
		float vy = position.y + p.textAscent() + vertex[1];

		//for each ripple
		for(Ripple r : ripples) {
			//get the distance between the vertex and the edge of the ripple
			double distSq = ((vx-r.center.x)*(vx-r.center.x) + (vy-r.center.y)*(vy-r.center.y))/* - (r.radiusSq)*/;
			distSq = Math.sqrt(distSq);
			distSq -= r.radius;
			if (distSq < 0) distSq *= -1;
			//if the distance square is less than the threshold, apply ripple force
			if (distSq < RIPPLE_LENGTH)
				vertex[2] += Math.sin((distSq/RIPPLE_LENGTH)*RIPPLE_CYCLES*2*PApplet.PI) * RIPPLE_AMPLITUDE * (1-distSq/RIPPLE_LENGTH);
		}
	}
	
	/**
	 * Apply contract deformation to a vertex
	 * @param vertex the vertex to deform
	 */
	public void contractVertex(double[] vertex) {
		double vy = position.y + p.textAscent() + vertex[1];
		double dy = vy - contractFrom.y;
		if (dy < 0) dy *= -1;
		
		double vx = position.x + vertex[0];
		double dx = vx - contractFrom.x;
		if (dx < 0) dx *= -1;
		
		//deform y position of the vertex
		vertex[1] *= 1 - (dx>contractPeriod?0:(Math.cos(dx/contractPeriod*PConstants.PI)+1)/2) * (contractFac>1?1:contractFac);
	}

	/**
	 * Apply contract deformation to a vertex
	 * @param vertex the vertex to deform
	 */
	public void contractVertex(float[] vertex) {
		float vy = position.y + p.textAscent() + vertex[1];
		float dy = vy - contractFrom.y;
		if (dy < 0) dy *= -1;
		
		float vx = position.x + vertex[0];
		float dx = vx - contractFrom.x;
		if (dx < 0) dx *= -1;
		
		//deform y position of the vertex
		vertex[1] *= 1 - (dx>contractPeriod?0:(Math.cos(dx/contractPeriod*PConstants.PI)+1)/2) * (contractFac>1?1:contractFac);
	}
	
    /**
     * This method uses the Java AWT Font methods to create a vector outline of 
     * the letters based on the current positions.
     * @param str string to generate control points from
     */	
	public void initControlPoints(String str) {
       	//make space for the control points
    	vertices = new ArrayList<PVector>();
            
    	// create a list to store the contours
    	contours = new ArrayList<int[]>();
            
    	// vertex array index (used to associate more than one contour point
    	// with the same vertex)
    	int vertexIndex = 0;
    
    	// a temporary list to store vertex indices for each contour (once 
    	// the contour is closed, this list will be converted to an array
    	// and stored into the Contour list.
    	ArrayList<Integer> tmpContour = new ArrayList<Integer>();
                            
    	// used to receive the list of points from PathIterator.currentSegment()
    	float points[] = new float[6];  
    	
    	// used to receive the segment type from PathIterator.currentSegment()
    	// segmentType can be SEG_MOVETO, SEG_LINETO, SEG_QUADTO, SEG_CLOSE
    	int segmentType = 0; 
    	
    	// used to remember the previously calculated Anchor and ControlPoint.
    	// for a more detailed description of what an anchor and control point are,
    	// see the architecture document.
    	PVector lastAnchor = new PVector();
            
    	// get the Shape for this glyph
    	GlyphVector gv = p.g.textFont.getFont().createGlyphVector( frc, str );
    	Shape outline = gv.getOutline();
            
    	// store the glyph's logical bounds information
    	//Rectangle2D logicalBounds = gv.getLogicalBounds();
            
    	// no flattening done at the moment, just iterate through all the 
    	// segments of the outline.  For more details see Javadoc for
    	// java.awt.geom.PathIterator
    	PathIterator pit = outline.getPathIterator(null);
    
    	while ( !pit.isDone() ) {         
    		segmentType = pit.currentSegment( points ); 
                            
    		switch( segmentType ) {
    			case PathIterator.SEG_MOVETO:
					// start a new tmpContour vector
					tmpContour = new ArrayList<Integer>();
					// get the starting point for this contour      
					PVector startingPoint = new PVector( (float)points[0], (float)points[1] );
					// store the point in the list of vertices
					vertices.add( new PVector( startingPoint.x, startingPoint.y ) );
					// store this point in the current tmpContour and increment
					// the vertices index
					tmpContour.add( vertexIndex );
					vertexIndex++;
					// update temporary variables used for backtracking
					lastAnchor = startingPoint;
					break;
                                    
    			case PathIterator.SEG_LINETO:
					// then, we must convert the line to a curve
					// in order to allow smooth deformations
					PVector endPoint = new PVector( (float)points[0], (float)points[1] );
					PVector midPoint = new PVector( (lastAnchor.x + endPoint.x)/2, 
					                                (lastAnchor.y + endPoint.y)/2  );
					//vertices.add( new PVector( midPoint.x, midPoint.y ) );
					
					//the first control point is 2/3 the distance between
					//the last anchor and the mid point
					vertices.add( new PVector( lastAnchor.x + (midPoint.x-lastAnchor.x)*2/3,
							  				   lastAnchor.y + (midPoint.y-lastAnchor.y)*2/3) );
					tmpContour.add( vertexIndex );
					vertexIndex++;

					//the secnd control point is 1/3 the distance between
					//the the mid point and the end point
					vertices.add( new PVector( midPoint.x + (endPoint.x-midPoint.x)/3,
							  				   midPoint.y + (endPoint.y-midPoint.y)/3) );
					tmpContour.add( vertexIndex );
					vertexIndex++;
					
					// finally, we must add the endPoint twice to the contour
					// to preserve sharp corners
					vertices.add( endPoint );
					tmpContour.add( vertexIndex );
					vertexIndex++;
					
					// update variables used for backtracking
					lastAnchor = endPoint;
					break;
                                    
    			case PathIterator.SEG_QUADTO:
					PVector controlPoint = new PVector( (float)points[0], (float)points[1] );
					PVector anchorPoint = new PVector( (float)points[2], (float)points[3] );
					
					// Store control points
					//vertices.add( new PVector( controlPoint.x, controlPoint.y ) );
					vertices.add( new PVector( lastAnchor.x + (controlPoint.x-lastAnchor.x)*2/3,
	  						  				   lastAnchor.y + (controlPoint.y-lastAnchor.y)*2/3) );					
					tmpContour.add( vertexIndex );
					vertexIndex++;

					vertices.add( new PVector( controlPoint.x + (anchorPoint.x-controlPoint.x)/3,
											   controlPoint.y + (anchorPoint.y-controlPoint.y)/3) );					
					tmpContour.add( vertexIndex );
					vertexIndex++;
	
					// Store anchor point.
					vertices.add( new PVector( anchorPoint.x, anchorPoint.y ) );
					tmpContour.add( vertexIndex );
					vertexIndex++;
					
					// update temporary variables used for backtracking                                     
					lastAnchor = anchorPoint;
					break;  
				
    			case PathIterator.SEG_CLOSE:
					// A SEG_CLOSE signifies the end of a contour, therefore
					// convert tmpContour into a new array of correct size
					int contour[] = new int[tmpContour.size()];
					Iterator<Integer> it = tmpContour.iterator();
					int i = 0;
					while( it.hasNext() ) {
					        contour[i] = it.next();
					        i++;    
					}
					
					// add the newly created contour array to the contour list
					contours.add(contour);
					break;
                                    
    			case PathIterator.SEG_CUBICTO:        
					break;
    		} // end switch 
    
    		pit.next();
    	} // end while		
	}
	
    /**
     * Initialize the outline from the previous generated control points.
     */
    public void initOutline() {            	
    	// create a new GeneralPath to hold the vector outline
        GeneralPath gp = new GeneralPath();
        // get an iterator for the list of contours
        Iterator<int[]> it = contours.iterator();

        // process each contour
        while (it.hasNext()) {

            // get the list of vertices for this contour
            int contour[] = it.next();

            PVector firstPoint = vertices.get(contour[0]);
            // move the pen to the beginning of the contour
            gp.moveTo((float) firstPoint.x, (float) firstPoint.y);
            
            // generate all the quads forming the line
            //for (int i = 1; i < contour.length-1; i+=2) {
            //    PVector controlPoint = vertices.get(contour[i]);
            //    PVector anchorPoint = vertices.get(contour[i + 1]);

            //    gp.quadTo((float) controlPoint.x, (float) controlPoint.y,
            //              (float) anchorPoint.x, (float) anchorPoint.y);                   
            //}
            
            // generate all the beziers forming the outline
            for (int i = 1; i < contour.length-1; i+=3) {
                PVector controlPoint1 = vertices.get(contour[i]);
                PVector controlPoint2 = vertices.get(contour[i+1]);
                PVector anchorPoint = vertices.get(contour[i+2]);

                gp.curveTo((float) controlPoint1.x, (float) controlPoint1.y,
                		   (float) controlPoint2.x, (float) controlPoint2.y,
                           (float) anchorPoint.x, (float) anchorPoint.y);                   
            }
            
            // close the path
            gp.closePath();

        } // end while 

        // cache it
        outline = gp;	
    }	
	
    /**
     * Initialize the tessellator.
     */
    public void initTessellator() {
    	tessCount = 0;
    	tessInit = new ArrayList<Float>();
    	
    	//use init tessellator to build the first pass
    	tessCallback = new TessCallback();
        //GLU.gluTessCallback(tessellator, GLU.GLU_TESS_BEGIN_DATA, tessCallback); 
        //GLU.gluTessCallback(tessellator, GLU.GLU_TESS_END_DATA, tessCallback); 
        GLU.gluTessCallback(tessellator, GLU.GLU_TESS_VERTEX_DATA, tessCallback); 
        GLU.gluTessCallback(tessellator, GLU.GLU_TESS_COMBINE, tessCallback); 
        GLU.gluTessCallback(tessellator, GLU.GLU_TESS_ERROR_DATA, tessCallback);
        GLU.gluTessCallback(tessellator, GLU.GLU_TESS_EDGE_FLAG_DATA, tessCallback);
        
        //tessellate
        tessellate();

        //copy tessellation to buffers
        tessOrig = new float[tessInit.size()];
        tess = new float[tessInit.size()];
        for(int i = 0; i < tessInit.size(); i++) {
        	tessOrig[i] = tessInit.get(i);
            tess[i] = tessInit.get(i);
        }

        //tessBuffer = ByteBuffer.allocateDirect(4 * tess.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
        //tessBuffer.put(tess);
        //tessBuffer.rewind();
        
        //setup update tessellator for future calls
    	//tessCallback = new TessUpdateCallback();
        //GLU.gluTessCallback(tessellator, GLU.GLU_TESS_BEGIN_DATA, tessCallback); 
        //GLU.gluTessCallback(tessellator, GLU.GLU_TESS_END_DATA, tessCallback); 
        //GLU.gluTessCallback(tessellator, GLU.GLU_TESS_VERTEX_DATA, tessCallback); 
        //GLU.gluTessCallback(tessellator, GLU.GLU_TESS_COMBINE, tessCallback); 
        //GLU.gluTessCallback(tessellator, GLU.GLU_TESS_ERROR_DATA, tessCallback);
        //GLU.gluTessCallback(tessellator, GLU.GLU_TESS_EDGE_FLAG_DATA, tessCallback);
        
        //clean up
        tessInit.clear();
        tessInit = null;
    }

    /**
     * This tessellator callback uses native Processing drawing functions to 
     * initialize the tessellation data.
     */
    public class TessCallback extends GLUtessellatorCallbackAdapter {
    	
    	public void beginData(int type, Object polygonData) {}
        public void endData(Object polygonData) {}
        
        public void vertexData(Object vertexData, Object polygonData) {
            if (vertexData instanceof double[]) {
                double[] d = (double[]) vertexData;
                if (d.length != 3) {
                    throw new RuntimeException("TessCallback vertex() data " +
                    "isn't length 3");
                }

                Word w = (Word)polygonData;
                w.tessInit.add((float)d[0]);
                w.tessInit.add((float)d[1]);
                w.tessInit.add((float)d[2]);
                w.tessCount+=3;
                
            } else {
                throw new RuntimeException("TessCallback vertex() data not understood");
            }
        }

        public void errorData(int errnum, Object polygonData) {
            String estring = glu.gluErrorString(errnum);
            throw new RuntimeException("Tessellation Error: " + estring);
        }

        /**
         * Implementation of the GLU_TESS_COMBINE callback.
         * @param coords is the 3-vector of the new vertex
         * @param data is the vertex data to be combined, up to four elements.
         * This is useful when mixing colors together or any other
         * user data that was passed in to gluTessVertex.
         * @param weight is an array of weights, one for each element of "data"
         * that should be linearly combined for new values.
         * @param outData is the set of new values of "data" after being
         * put back together based on the weights. it's passed back as a
         * single element Object[] array because that's the closest
         * that Java gets to a pointer.
         */
        public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
            double[] vertex = new double[coords.length];
            vertex[0] = coords[0];
            vertex[1] = coords[1];
            vertex[2] = coords[2];
            
            outData[0] = vertex;
        }
        
        public void edgeFlagData (boolean flag, Object data) {}
    }
    
    /**
     * Get the word's string representation.
     */
    public String toString() { return value; }
}
