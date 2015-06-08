/*
 * Copyright (c) 2012 All Right Reserved, Jason E. Lewis [http://obxlabs.net]
 */

package net.obxlabs.rattlesnakes;

import java.util.Random;

/**
 * A subclass of java.util.random that implements the Xorshift random number generator
 * 
 * $LastChangedRevision$
 * $LastChangedDate$
 * $LastChangedBy$
 */
public class XSRandom extends Random {
	private static final long serialVersionUID = 1L;

	//the randomization seed
	private long seed;

	/**
	 * Constructor.
	 * @param seed
	 */
	public XSRandom(long seed) {
		this.seed = seed;
	}

	/**
	 * Get the next integer.
	 */
	protected int next(int nbits) {
		long x = seed;
		x ^= (x << 21);
		x ^= (x >>> 35);
		x ^= (x << 4);
		seed = x;
		x &= ((1L << nbits) - 1);
		return (int) x;
	}
}
