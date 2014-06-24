/*
 * Copyright (C) 2010 Institute of Digital Publishing, Founder R&D Center
 *
 * divider finder interface , this is part of the text file reading facilities.
 * aims to find an intact block of text. 
 * 
 * a divider can be several bytes.
 * 
 * 2010-12-16
 * 
 * Li Chunyu (lichunyu@founder.com)
 * 
 */
package com.cr.file.fileread;

import com.cr.file.charset.EncodingDefs;

public abstract class BlockDivider {

	protected static final String tag = "DividerFinder";

	// refer to outside memory.
	protected byte mBuff[] = null;

	private int mStart; // to find from, index of mBuff.
	protected int mEnd;   // to find to , exclusive.

	// charset of mBuff
	protected int mCharset;

	protected BlockDivider() {
		mBuff = null;
		mStart = -1;
		mEnd = -1;
	}

	public boolean isIndexesInRange() {
		return mBuff != null && isValidIndex(mStart) && isValidIndex(mEnd-1);
	}
	
	/**
	 * 
	 * @return whether finding is ready.
	 */
	public boolean isReady() {
		return mBuff != null && isValidIndex(mStart) && isValidIndex(mEnd-1); 
	}

	public void setSearchingRange(int start, int end) {
		mStart	= start;
		mEnd	= end;
	}

	/**
	 * @precondition buffer set.
	 * @param start - to search from
	 * @return true for set successfully , false for failed to set.
	 */
	public void setSearchingStart(int start) {
		mStart = start;
	}

	private boolean isValidIndex(int index) {
		return mBuff != null && index >= 0 && index < mBuff.length;
	}

	/**
	 * @precondition make sure the input is a legal index defined by Encode
	 * @param charset - charset of buff , see param of setBuffer.
	 * we will use this for getting the length of the divider.
	 */
	public void setCharset(int charset) {
		this.mCharset = charset;
	}

	/**
	 * 
	 * @param buff , the range of buff will be regarded as the search range.
	 * @param useOldIndex
	 *           update the index to the max size when this is false.
	 *           otherwise use the old index.
	 * @return true for set successfully, false otherwise.          
	 */
	public boolean setBuffer(byte[] buff,boolean useOldIndex) {
		this.mBuff = buff;
		if (!useOldIndex) {
			this.mStart = 0;
			this.mEnd = buff.length;
		}
		return isIndexesInRange();
	}

	/**
	 * 
	 * @param buff
	 * @param start - to search from
	 * @param end  - to search to , exclusive , must not be over length of buff.
	 */
	public boolean setBuffer(byte[] buff,int start,int end) {
		this.mBuff = buff;
		if (start >= end || !this.isValidIndex(start) || !this.isValidIndex(end-1)) {
			return false;
		}
		this.mStart = start;
		this.mEnd = end;
		return isIndexesInRange();
	}

	/**
	 * 
	 * @return true if it is Unicode 16 , big endian or little endian , false otherwise.
	 */
	protected boolean isWideCode() {
		return (this.mCharset == EncodingDefs.UNICODEBE16 || this.mCharset== EncodingDefs.UNICODELE16);
	}

	/**
	 * @return true if it is 16 bits unicode little endian,false otherwise. 
	 */
	protected boolean isUnicodeLittleEndian() {
		return this.mCharset == EncodingDefs.UNICODELE16;
	}

	/**
	 * 
	 * @return true if it is 16 bits unicode big endian,false otherwise.
	 */
	protected boolean isUnicodeBigEndian() {
		return this.mCharset== EncodingDefs.UNICODEBE16;
	}

	/**
	 * search from the start position set.
	 * @precondition buffer and range set.
	 * @return -1 for failed , otherwise the tail of the divider.
	 *
	 */
	public int findDivider() {
		return findDividerFrom(this.mStart);
	}

	/**
	 * @precondition buffer and range set.
	 * @return tail position of the divider.
	 */
	public abstract int findDividerFrom(int from);

	/**
	 * 
	 * @return -1 for failed,others for the divider length.
	 */
	abstract int getDividerLength();

	/**
	 * find divider reversely.
	 * @param buff				- the bytes to search.
	 * @param searchFrom		- to search from, must be in valid range of buff.
	 * @return -1 for not found, otherwise the tail of the divider.
	 */
	public abstract int findDividerReversely(byte[] buff, int searchFrom);
}
