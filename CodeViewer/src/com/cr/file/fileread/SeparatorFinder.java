/*
 * Copyright (C) 2010 Institute of Digital Publishing, Founder R&D Center
 *
 * separator finder.
 * caller has to set a separator.
 * 
 * 2011-04-06
 * 
 * Li Chunyu (lichunyu@founder.com)
 * 
 */
package com.cr.file.fileread;

import java.io.UnsupportedEncodingException;

import com.cr.file.charset.EncodingDefs;

public class SeparatorFinder extends BlockDivider {

	private byte[] mSeparator = null;
	
	/**
	 * ensure charset is set before calling this.
	 * @param separator
	 */
	public boolean setSeparator(String separator) {
		try {
			mSeparator	= separator.getBytes(EncodingDefs.getInstance().getEncodingStr(mCharset));
			return true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public boolean isReady() {
		return super.isReady() && mSeparator != null;
	}
	
	@Override
	public int findDividerFrom(int from) {
		final int separatorLen = mSeparator.length;
		for (int i = from, n = this.mEnd - separatorLen + 1; i < n; ++i) {
			boolean match = true;
			for (int j = 0; j < separatorLen; ++j) {
				if (this.mBuff[i + j] != mSeparator[j]) {
					match = false;
					break;
				}
			}
			if (match) {
				return i + separatorLen - 1;
			}
		}
		return -1;
	}

	/**
	 * ensure setSeparator is called successfully.
	 */
	@Override
	public int getDividerLength() {
		return this.mSeparator.length;
	}

	@Override
	public int findDividerReversely(byte[] buff, int searchFrom) {
		if (buff == null || buff.length < this.mSeparator.length) {
			return -1;
		}
		searchFrom -= (mSeparator.length - 1);
		final int separatorLen = mSeparator.length;
		for (int i = searchFrom; i >= 0; --i) {
			boolean match = true;
			for (int j = 0; j < separatorLen; ++j) {
				if (this.mBuff[i + j] != mSeparator[j]) {
					match = false;
					break;
				}
			}
			if (match) {
				return i + separatorLen - 1;
			}
		}
		return -1;
	}

}
