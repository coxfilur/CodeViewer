package com.cr.file;


public interface CommonTXTFileReader {
	boolean openFile(String fullPath);

	void closeFile();
	
	int getFileStartPos();

	/**
	 *  
	 * @return file size in bytes.
	 */
	int getFileSize();

	int getCurrentOffset();

	void setCurrentOffset(long currentOffset);

	String readPrevBlockData();

	String readNextBlockData();

	boolean isFileEndReached();

	/**
	 * 
	 * @param contentText : in utf16-le, i.e. the output format of String returned by all functions who returns a String.
	 * @return  compute the bytes length of contentText when converted to original charset format.
	 */
	int computeLenOfString(String contentText);

    /**
     *
     * @param bytes
     * @param from , the position in param bytes
     * @param to , the end position in param bytes exclusive
     * @return the length of chars of param bytes from param from to param to.
     */
    int computeLenOfBytes(byte[] bytes, int from, int to);
	
	boolean isNewParagraph(long pos);

	String readNextTinyBlockData();

	/**
	 * @precondition the bytes range , from and to must both be valid position , that is , they must point to the head of char head.
	 * @param from , from this file offset we read.
	 * @param count count of read bytes.
	 * @return -1 for failed,the bytes read.
	 * @postcondition the offset won't be changed if successful.
	 * @note currently only TXTFileReader implements this.
	 */
	int getBytes(byte[] out, long from, int count);
	
}
