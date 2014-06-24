package com.cr.layout;

public class Atom {
	public int type;
	
	public int from;
	
	/**
	 * Exclusive.
	 */
	public int to;
	
	public boolean contains(int pos) {
		return from <= pos && pos < to;
	}
}
