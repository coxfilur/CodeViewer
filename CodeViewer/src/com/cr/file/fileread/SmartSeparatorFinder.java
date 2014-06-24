/*
 * Copyright (C) 2010 Institute of Digital Publishing, Founder R&D Center
 *
 * smart separator finder.
 * it will CR,LF first, if not find, it will search comma, then letters and numbers.
 * 
 * 2011-04-06
 * 
 * Li Chunyu (lichunyu@founder.com)
 * 
 */
package com.cr.file.fileread;

import java.io.UnsupportedEncodingException;

import com.cr.file.charset.EncodingDefs;

public class SmartSeparatorFinder extends BlockDivider {

	private SeparatorFinder mFinder = null;
	
	private String[] mSeparators	= {
			"\r",
			"\n",
			",",
			" ",
			"	",
			"a",
			"e",
			"i",
			"o",
			"u",
			"A",
			"E",
			"I",
			"O",
			"U",
			"0",
			"1",
			"2",
			"3",
			"4",
			"5",
			"6",
			"7",
			"8",
			"9"
	};
	
	// index of separator just just,
	// updated in findDividerFrom and findDividerReversely.
	private int mIndexOfSepJustUsed = -1;
	
	public SmartSeparatorFinder() {
		mFinder = new SeparatorFinder();
	}
	
	public void setCharset(int charset) {
		super.setCharset(charset);
		mFinder.setCharset(charset);
	}
	
	@Override
	public int findDividerFrom(int from) {
		if (!mFinder.setBuffer(this.mBuff, false)) {
			return -1;
		}
		mFinder.setSearchingRange(0, this.mEnd);
		mIndexOfSepJustUsed = 0;
		while (true) {
			if (!mFinder.setSeparator(mSeparators[mIndexOfSepJustUsed])) {
				return -1;
			}
			int rc	= mFinder.findDividerFrom(from);
			if (rc != -1) {
				return rc;
			} 
			if (mIndexOfSepJustUsed == mSeparators.length - 1) {
				return -1;
			}
			++ mIndexOfSepJustUsed;
		}
	}

	/**
	 * ensure setSeparator is called successfully.
	 * -1 for failed.
	 */
	@Override
	public int getDividerLength() {
		if (mIndexOfSepJustUsed == -1) {
			return -1;
		}
		try {
			return this.mSeparators[mIndexOfSepJustUsed].getBytes(EncodingDefs.getInstance().getEncodingStr(mCharset)).length;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public int findDividerReversely(byte[] buff, int searchFrom) {
		if (buff == null) {
			return -1;
		}
		if (!mFinder.setBuffer(buff, false)) {
			return -1;
		}
		mIndexOfSepJustUsed = 0;
		while (true) {
			if (!mFinder.setSeparator(mSeparators[mIndexOfSepJustUsed])) {
				return -1;
			}
			int rc	= mFinder.findDividerReversely(buff, searchFrom);
			if (rc != -1) {
				return rc;
			} 
			if (mIndexOfSepJustUsed == mSeparators.length - 1) {
				return -1;
			}
			++ mIndexOfSepJustUsed;
		}
	}

}
