/*
 * Copyright (C) 2010 Institute of Digital Publishing, Founder R&D Center
 *
 * file encoding(char set) getter interface.
 * 
 * 2010-10-19
 * 
 * LiChunyu(LiChunyu@founder.com)
 * 
 */
package com.cr.file.charset;
import java.io.File;
import java.io.IOException;


public interface FileEncodingGetter {
	
	/**
	 * 
	 * @param sourceFile
	 * @return the charset string needed by java file reader.
	 * @throws IOException
	 */
	String getFilecharset(File sourceFile) throws IOException;
	
	/**
	 * @precondition getFilecharset called.
	 * @return num of bytes should be ignored when showing.
	 */
	int getHeadLength();

	/**
	 * @precondition getFilecharset called.
	 * @return
	 */
	boolean isUnicodeBigEndian();

	/**
	 * @precondition getFilecharset called.
	 * @return
	 */
	boolean isUnicodeSmallEndian();

	/**
	 * @precondition getFilecharset called.
	 * @return max bytes count per character
	 */
	int getMaxBytesPerChar();
	
}
