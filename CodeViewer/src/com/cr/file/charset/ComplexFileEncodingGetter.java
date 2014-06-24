/*
 * Copyright (C) 2010 Institute of Digital Publishing, Founder R&D Center
 *
 * file encoding(char set) getter
 * use BOM(Byte Order Mark) if there is,
 * otherwise call the SinoDetect.
 * 
 * 2010-10-19
 * 
 * LiChunyu(LiChunyu@founder.com)
 * 
 */
package com.cr.file.charset;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class ComplexFileEncodingGetter implements FileEncodingGetter {

	private static final String TEXTENCODING_ASCII = "GBK";
	private static final String TEXTENCODING_UNI16_LE = "UTF-16LE";
	private static final String TEXTENCODING_UNI16_BE = "UTF-16BE";
	private static final String TEXTENCODING_UTF8 = "UTF-8";

	private RandomAccessFile mRandomAccessFile = null;
	private int mHeadLength = 0;
	private CharsetDetect mSuperDetecter = null;

	private int mReadLenForCheck = 4096;
	private byte[] mData = null;
	private byte[] mInput = null;

	private String mCharset = null;

	/**
	 * @precondition : the file exists.
	 */
	@Override
	public String getFilecharset(File sourceFile) throws IOException {
		mRandomAccessFile = new RandomAccessFile(sourceFile,"r");
		String charset = getFilecharsetByBOM();
		if (charset.length() != 0) {
			mRandomAccessFile.close();
			mCharset = charset;
			return charset;
		}
		// since there is no BOM , so set the head length to 0.
		mHeadLength = 0;
		charset = doComplexCheck();		
		mRandomAccessFile.close();
		mCharset = charset;
		return charset;
	}

	@Override
	public int getHeadLength() {
		return this.mHeadLength;
	}

	/*
	 * return empty string for no BOM.
	 */
	private String getFilecharsetByBOM() throws IOException {
		byte [] fileHead = new byte[2];
		int uFileHeadRead = 0;

		int read = mRandomAccessFile.read(fileHead, 0, 2); 
		uFileHeadRead = read;

		if (read == 0) {
			mHeadLength = 0;
			return TEXTENCODING_ASCII;
		}

		if (uFileHeadRead != 2) {
			// there are less than 2 bytes in the file.
			return "";
		}

		if (fileHead[0] == (byte) 0xFF && fileHead[1] == (byte) 0xFE) {
			mHeadLength = 2;
			return TEXTENCODING_UNI16_LE; // Unicode
		}
		else if (fileHead[0] == (byte) 0xFE && fileHead[1] == (byte) 0xFF) {
			mHeadLength = 2;
			return TEXTENCODING_UNI16_BE; // Unicode Big Endian
		}
		else if (fileHead[0] == (byte) 0xEF && fileHead[1] == (byte) 0xBB) {
			// UTF-8
			byte [] thirdByte = new byte [1];
			int uThirdByteRead = 0;
			uThirdByteRead = mRandomAccessFile.read(thirdByte, 0, 1);
			if (uThirdByteRead != 1) { // there are only two bytes in the file.
				return "";
			}
			if (thirdByte[0] == (byte) 0xBF) {
				mHeadLength = 3;
				return TEXTENCODING_UTF8;
			}
			else {
				return "";
			}
		}
		else {
			return "";
		}

	}

	private String doComplexCheck() {
		if (mSuperDetecter == null) {
			mSuperDetecter = new CharsetDetect();
		}
		if (mData == null) {
			mData = new byte [mReadLenForCheck];
		}
		try {
			int len = mRandomAccessFile.read(mData);
			if (len <= 0) {
				return TEXTENCODING_ASCII;
			}
			if (len < mData.length) {
				mInput = new byte[len];
				System.arraycopy(mData, 0, mInput, 0, len);
			} else {
				mInput = mData;
			}
			String charset = this.mSuperDetecter.getEncodingStr(this.mSuperDetecter.detectEncoding(mInput));
			return charset;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public boolean isUnicodeBigEndian() {
		return TEXTENCODING_UNI16_BE.equals(mCharset);
	}

	@Override
	public boolean isUnicodeSmallEndian() {
		return TEXTENCODING_UNI16_LE.equals(mCharset);
	}

	@Override
	/**
	 * utf-32 not considered.
	 */
	public int getMaxBytesPerChar() {
		return TEXTENCODING_UTF8.equals(mCharset) ? 5 : 2;
	}

}
