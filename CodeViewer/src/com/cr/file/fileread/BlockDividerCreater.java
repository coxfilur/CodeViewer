/*
 * Copyright (C) 2010 Institute of Digital Publishing, Founder R&D Center
 *
 * block divider creator.
 * 
 * 2011-04-06
 * 
 * Li Chunyu (lichunyu@founder.com)
 * 
 */
package com.cr.file.fileread;

import com.cr.file.charset.EncodingDefs;

public final class BlockDividerCreater {
	public final static int LineFeedFinder			= 0;
	public final static int CarraigereturnFinder	= 1;
	public final static int CommaFinder				= 2;
	public final static int SmartFinder				= 3;
	
	
	/**
	 * 
	 * @param id - divider finder id,see the final ints in this.
	 * @param charset - a supported charset,defined by the final ints of Encoding.
	 * 					 see isSupportedCharset for whether it is supported.
	 * @return
	 */
	public static BlockDivider create(int id,int charset) {
		if (isLineTerminatorDivider(id)) {
			return createLineTerminatorDivider(id, charset);
		}
		if (id == CommaFinder) {
			SeparatorFinder ret = new SeparatorFinder();
			ret.setCharset(charset);
			ret.setSeparator("£¬");
			return ret;
		}
		SmartSeparatorFinder ret = new SmartSeparatorFinder();
		ret.setCharset(charset);
		return ret;
	}
	
	static boolean isLineTerminatorDivider(int id) {
		return id == LineFeedFinder || id == CarraigereturnFinder;
	}
	
	/**
	 * 
	 * @param id - divider finder id,see the final ints in this.
	 * @param charset - a supported charset,defined by the final ints of Encoding.
	 * 					 see isSupportedCharset for whether it is supported.
	 * @return
	 */
	public static BlockDivider createLineTerminatorDivider(int id,int charset) {
		SeparatorFinder ret = new SeparatorFinder();
		ret.setCharset(charset);
		ret.setSeparator(LineFeedFinder == id ? "\n" : "\r");
		return ret;
	}
	
	public static boolean isSupportedCharset(int charset) {
		return charset != EncodingDefs.OTHER;
	}
}
