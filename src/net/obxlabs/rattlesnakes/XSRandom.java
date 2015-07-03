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
