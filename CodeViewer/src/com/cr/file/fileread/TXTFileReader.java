package com.cr.file.fileread;

/* 
 * all output string are in chars of unicode.
 * all sizes and positions are in bytes.
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

import com.cr.file.CommonTXTFileReader;
import com.cr.file.charset.ComplexFileEncodingGetter;
import com.cr.file.charset.EncodingDefs;
import com.cr.file.charset.FileEncodingGetter;
import com.cr.util.L;


public class TXTFileReader implements CommonTXTFileReader {

	private RandomAccessFile mFileReader = null;

	private FileEncodingGetter mCharsetGetter = new ComplexFileEncodingGetter();
	private String mCharsetName = null;// the input file's charset.
	private int mHeadLen = 0; // these several bytes should not be read by caller.
	private long mFileLen = 0;

	private int mBuffInitSize = 4096; 
	private byte[] mBuff = null;

	private int mReadSize4Block = 3072; // must not be over mBuff.length.
	private int mBlockLowBoundSize = 2048;// block min-size. find divider from.

	private final static int mLineTypicalSize = 80;

	private int mBuffValidLen = 0; // the valid bytes length in mBuff.

	// true for unicode and unicode big endian,false otherwise. 
	private boolean mIsWideCode;
	private boolean mIsUnicodeBigEn;
	private boolean mIsUnicode;

	// return codes for readPrevLineData,must be negative, for non-negative means positions.
	private final static int RC_READPREVBLOCK_NODIVIDERFOUND 	= -1;
	private final static int RC_READPREVBLOCK_NOPREV 			= -2;
	private final static int RC_READPREVBLOCK_OTHERERROR 		= -3;

	// return codes for readPrevLineData,must be negative, for non-negative means positions.
	private final static int RC_READPREVLINE_NOCARRIAGEFOUND 	= -1;
	private final static int RC_READPREVLINE_NOPREVLINE 		= -2;
	private final static int RC_READPREVLINE_OTHERERROR 		= -3;
	
	// for the only CR occasion , we replace CR with LF, for kernel regards only LF as delimiter.
	// this data is for performance - avoiding processing the same data repeatedly.
	private CarriageReturnProcessedInfo mProcessedBytesInfo = new CarriageReturnProcessedInfo(); 

	private static final String tag = "TXTFileReader";
	
	private BlockDivider mBlockDivider = null;

	/**
	 * initialization is done in this.
	 */
	@Override
	public boolean openFile(String path) {
		File f = new File(path);
		if (!f.isFile()) {
			return false;
		}

		try {
			mFileLen = f.length();
			mCharsetName = mCharsetGetter.getFilecharset(f);
			init();
			mHeadLen = mCharsetGetter.getHeadLength();

			// open the file in read-only mode.
			mFileReader = new RandomAccessFile(f,"r");
			if (!seekTo(mHeadLen)) {
				return false;
			}
			mBuff = new byte[mBuffInitSize];
			mBuffValidLen = 0;

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		mBlockDivider = BlockDividerCreater.create(BlockDividerCreater.SmartFinder, EncodingDefs.getInstance().getEncodingId(mCharsetName));
		return true;
	}
	
	/**
	 * the buffer for block data is reallocated if necessary.
	 * call this only after openFile called and true returned.
	 * @param newBlockChars		block size of chars.
	 * @param doNotUpdateIfAlreadyEnough		if this is true, 
	 * the buffer for block won't be updated if the buffer is no smaller than input. 
	 * @return true if updated successfully or already big enough, false otherwise.
	 * Note buffered data is not changed if buffer was not allocated. 
	 */
	public boolean updateBlockSize(int newBlockChars, boolean reserveOldData, boolean doNotUpdateIfAlreadyEnough) {
		if (mBuff == null || mCharsetGetter == null || newBlockChars <= 0) {
			return false;
		}
		final int newBlockBytes = this.mCharsetGetter.getMaxBytesPerChar() * newBlockChars;
		if (mBuff.length >= newBlockBytes && doNotUpdateIfAlreadyEnough) {
			return true;
		}
		if (!reserveOldData || mBuffValidLen <= 0) {
			setReadBlockSize(newBlockBytes, doNotUpdateIfAlreadyEnough);
			if (!allocateBuff()) {
				return false;
			}
			mBuffValidLen = 0;
			return true;
		}
		
		// correction.
		if (mBuffValidLen > mBuff.length) {
			mBuffValidLen	= mBuff.length;
		}
		
		if (!setReadBlockSize(newBlockChars, doNotUpdateIfAlreadyEnough)) {
			return false;
		}
		
		int copySize = (mBuffValidLen >= newBlockBytes ? newBlockBytes : mBuffValidLen);
		byte[] oldBuff = mBuff;
		if (!allocateBuff()) {
			L.e(tag, "program error.");
			return false;
		}
		System.arraycopy(oldBuff, 0, mBuff , 0, copySize);
		
		return true;
	}

	public String readPrevLineData() {
		long curPos = getCurrentOffset();
		if (curPos == -1 || curPos <= mHeadLen) {
			return null;
		}

		long bkCurPos = curPos;

		int maxTryLen = (int) (curPos - mHeadLen - 1); // do not read current byte when read prev.
		if (maxTryLen >= mBuff.length) {
			maxTryLen = mBuff.length;
		}
		
		int firstTryLen = mLineTypicalSize <= maxTryLen ? mLineTypicalSize : maxTryLen; 

		int posOfCRTail = RC_READPREVLINE_OTHERERROR;
		for (int tryLen = firstTryLen ; tryLen <= maxTryLen ; tryLen += mLineTypicalSize) {
			posOfCRTail= readPrevLineData(tryLen);
			if (posOfCRTail >= 0) {
				break;
			}
			if (posOfCRTail == RC_READPREVLINE_NOPREVLINE) {
				return null;
			} else if (posOfCRTail == RC_READPREVLINE_NOCARRIAGEFOUND) {
				if (!seekTo(bkCurPos)) {
					System.out.println("fatal error! in readPrevLineData's seekTo invoking.");
					return null;
				}	
			} else {
				System.out.println("fatal error : RC_READPREV_OTHERERROR!");
				return null;
			}
		}

		if (posOfCRTail < 0) {
			System.out.println("failed to get previous line, for no carriage found in limited-capacity buffer!");
			return null;
		}

		int len = (int) (bkCurPos-posOfCRTail); 
		return convert2Unicode(mBuff,posOfCRTail+1,len);
	}

	/**
	 * read until to next position where a valid char can be recognized.
	 */
	@Override
	public String readNextTinyBlockData() {
		return this.readNextBlockData(mLineTypicalSize);
	}

	@Override
	public boolean isFileEndReached() {
		try {
			return mFileReader.getFilePointer() >= mFileLen;
		} catch (IOException e) {
			e.printStackTrace();
			return true;
		}
	}
	/**
	 * @return true : byte at pos is a '\r' or '\n' , or before start position,false for otherwise. 
	 */
	@Override
	public boolean isNewParagraph(long pos) {
		if (pos < this.mHeadLen) {
			return true;
		}
		if (pos >= getFileSize()) {
			return false;
		}
		if (!seekTo(pos)) {
			System.out.println("program error! in isNewParagraph");
			return false;
		}
		try {
			// for unicode , carriage return is \r ,\0 or \0,\r , and line feed is \n,\0 or \0,\n.
			// the two -1 are not arbitrary , they ensure the answer is right even only 1 byte is read.
			byte bytes[] = {-1, -1};
			mFileReader.read(bytes, 0, 2);
			return isCarriageReturn(bytes, 0) || isLinefeed(bytes, 0);
		} catch (IOException e) {
			e.printStackTrace();
			return false;

		}
	}

	@Override
	public void closeFile() {
		if (mFileReader == null) {
			return;
		}
		try {
			mFileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * get length in bytes of given string.
	 * @param contentText - in unicode.
	 * @return size in byte of the result string, -1 for failed, non-negative for success.
	 */
	@Override
	public int computeLenOfString(String contentText) {
		try {
			return contentText.getBytes(mCharsetName).length;
			// unicodeData.getBytes().length
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	/**
	 * get length in chars of given bytes.
	 * @param bytes
	 * @param from , the position in param bytes
	 * @param to , the end position in param bytes exclusive
	 * @return the length of chars of param bytes from param from to param to.
	 */
	@Override
	public int computeLenOfBytes(byte[] bytes, int from, int to) {
		if (from < 0 || to > bytes.length || from >= to) {
			L.e(tag, "input error");
			return -1;
		}
		String newStr;
		try {
			newStr = new String(bytes, from,to - from, this.mCharsetName);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return -1;
		}
		return newStr.length();
	}

	/**
	 * ensure the bytes range , from and to must both be valid position , that is , they must point to the head of char head.
	 * @param from , from this file offset we read.
	 * @param count count of read bytes.
	 * @return -1 for failed,the bytes read.
	 * @postcondition the offset won't be changed if successful.
	 */
	@Override
	public int getBytes(byte[] out,long from,int count) {
		long bkPos = this.getCurrentOffset();
		if (!seekTo(from)) {
			System.out.println("Error , seek error");
		}
		int read = -1;
		try {
			read = this.mFileReader.read(out, 0, count);
		} catch (IOException e) {
			e.printStackTrace();
		}
		seekTo(bkPos);
		return read;
	}

	/**
	 * @return -1 for failed.
	 */
	@Override
	public int getCurrentOffset() {
		try {
			return (int) this.mFileReader.getFilePointer();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * get an offset pointing to delimiter's next after pos,
	 * if pos does not point to a delimiter or the next byte of a delimiter in file,
	 * here delimiter mean the tail of '\r\n' , or '\r' if there is no '\n' next to '\r'.
	 * ensure true returned by openFile.
	 * @param pos - an offset in bytes.
	 * @return offset corrected.
	 */
	public long getCorrectedOffset(long pos) {
		
		// there may be a delimiter before given position.
		pos -= 4;
		
		if (pos % 2 == 1 && this.mIsWideCode) {
			pos += 1;
		}
		if (pos <= mHeadLen) {
			return mHeadLen;
		}
		if (pos >= this.mFileLen) {
			return mFileLen;
		}

		seekTo(pos);
		try {
			mBuffValidLen = mFileReader.read(mBuff,0,mReadSize4Block);
			long posDelimiter = this.findBlockDividerFrom(0);
			if (posDelimiter == -1) {// block divider not found in buffer,simply return head position.
				return mHeadLen;
			} else {
				long ret = posDelimiter + pos + 1;
				if (this.mIsWideCode) {
					if (ret % 2 == 1) {
						System.out.println("Error , calculation error , bytes splitted wrongly.");
						ret += 1;
					}
				}
				if (ret > mFileLen) {
					return mFileLen;
				} else {
					return ret;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mHeadLen; // this is a bad occasion.
	}

	/**
	 * get an offset pointing to delimiter 's next after percent pointing to, 
	 * if the input percent does not point to a delimiter or the next byte of a delimiter in file,
	 * here delimiter mean the tail of '\r\n' , or '\r' if there is no '\n' next to '\r'.
	 * ensure true returned by openFile.
	 * @param percent - an offset by percent.
	 * @return offset corrected.
	 */
	public long getCorrectOffsetByPercent(float percent) {
		long offset = (long) (this.mFileLen * percent + 0.5);
		return getCorrectedOffset(offset);
	}

	/**
	 * ensure openFile called and true returned by openFile.
	 */
	@Override
	public int getFileSize() {
		return (int) mFileLen;
	}

	/**
	 * ensure openFile called and true returned by openFile.
	 */
	@Override
	public int getFileStartPos() {
		return mHeadLen;
	}

	/**
	 * read block with given minimum size.
	 * @param blockMinSize	block minimum char(s).
	 * @return not null string if successful, null for failed.
	 * Note empty string will be returned if end reached.
	 */
	public String readNextBlockData(int blockMinSize) {
		if (blockMinSize <= 0) {
			L.d(tag, "warning: block size must be positive, corrected...");
			if (mBlockLowBoundSize > 0 && mReadSize4Block > mBlockLowBoundSize) {
				return readNextBlockData();
			}
		}
		int blockMinBytes = this.mCharsetGetter.getMaxBytesPerChar() * blockMinSize;
		if (blockMinBytes <= this.mBlockLowBoundSize) {
			return readNextBlockData();
		}
		// update sizes and then read.
		this.mBlockLowBoundSize	= blockMinBytes;
		this.mBuffValidLen		= 0;
		this.mReadSize4Block	= blockMinBytes + mLineTypicalSize;
		try {
			this.mBuff	= new byte[mReadSize4Block];
		} catch (OutOfMemoryError e) {
			mBlockLowBoundSize = 0;
			mReadSize4Block = 0;
			return null;
		}
		return readNextBlockData();
	}

	@Override
	public String readNextBlockData() {
		if (isFileEndReached()) {
			return "";
		}

		long bkCurPos = getCurrentOffset();
		if (this.mIsWideCode && ((bkCurPos % 2) == 1)) {
			L.e(tag, "divide error for wide charset");
		}

		try {
			mBuffValidLen = mFileReader.read(mBuff, 0, mReadSize4Block);
			if (mBuffValidLen < mReadSize4Block) { // file end reached.
				return convert2Unicode(mBuff, 0, mBuffValidLen);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		if (isFileEndReached()) {
			return convert2Unicode(mBuff, 0, mBuffValidLen);
		}

		int posCRTail = findBlockDividerFrom(mBlockLowBoundSize);

		while (posCRTail < 0) {
			// no divider found from the block, we will reallocate buffer and read more and find.
			int lastFoundLen = mBuffValidLen;

			if (mBuffValidLen >= mBuff.length) {
				byte [] newBytes = new byte [2 * mBuff.length];
				// ReaderLog.i(tag, Integer.toString(newBytes.length));
				System.arraycopy(mBuff, 0, newBytes, 0, mBuffValidLen);
				mBuff = newBytes;	
			}
		
			try {
				int more = mBuff.length-mBuffValidLen;
				int read = mFileReader.read(mBuff, mBuffValidLen, more);
				mBuffValidLen += read;
				if (read < more) { // file end reached.
					return convert2Unicode(mBuff, 0, mBuffValidLen);
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			posCRTail = findBlockDividerFrom(lastFoundLen);
		}
		seekTo(bkCurPos + posCRTail + 1);
		return convert2Unicode(mBuff, 0, posCRTail);
	}

	@Override
	public String readPrevBlockData() {
		int bkCurPos = (int) getCurrentOffset();
		int ret = 0;
		int tryLen = mReadSize4Block;
		while(true) {
			ret = readPrevBlockData(tryLen);
			if (ret >= 0) {
				break;
			}
			if (ret == RC_READPREVBLOCK_NODIVIDERFOUND) {
				if (!seekTo(bkCurPos)) {
					System.out.println("must be a silly mistake.");
					return null;
				}
				tryLen += mReadSize4Block;
				continue;
			} else if (ret == RC_READPREVBLOCK_NOPREV){
				return null;
			}else { // (ret == RC_READPREVBLOCK_OTHERERROR)
				System.out.println("fatal error : RC_READPREVBLOCK_OTHERERROR!");
				return null;
			} 
		}
		seekTo(bkCurPos);
		if (ret == 0) {
			return convert2Unicode(mBuff, 0 , mBuffValidLen);
		} else {
			return convert2Unicode(mBuff, ret + 1, mBuffValidLen - ret - 1);
		}
	}

	/**
	 * if currentOffset is less than head length , offset will be set to start position.
	 * if currentOffset is bigger than file length, offset will be set to tail position - file length - 1.
	 */
	@Override
	public void setCurrentOffset(long currentOffset) {
		
		// for unicode or unicode big endian, offset must not be odd.
		if (this.mIsWideCode && currentOffset % 2 == 1) {
			++ currentOffset;
		}
		if (currentOffset < this.mHeadLen) {
			seekToStart();
			return;
		}
		try {
			mFileReader.seek(currentOffset);
		} catch (IOException e) {
			// if it goes here , I guess the user wants to goto tail.
			seekToEnd();
		}
	}

	protected boolean setReadBlockSize(int blockSizeInBytes, boolean doNotUpdateIfEnough) {
		if (blockSizeInBytes <= 0) {
			return false;
		}
		if (doNotUpdateIfEnough && mBlockLowBoundSize >= blockSizeInBytes) {
			return true;
		}
		mBlockLowBoundSize = blockSizeInBytes;
		mReadSize4Block = blockSizeInBytes << 1; // must not be over mBuff.length.
		mBuffInitSize = mReadSize4Block + mLineTypicalSize << 1; 
		return true;
	}

	protected boolean allocateBuff() {
		if (mBuffInitSize <= 0) {
			return false;
		}
		mBuff = new byte [mBuffInitSize];
		return true;
	}
	
	private void seekToStart() {
		seekTo(mHeadLen);
	}

	private void seekToEnd() {
		seekTo(mFileLen-1);
	}

	/**
	 * 
	 * @param data
	 * @param offset - position to convert from.
	 * @param length - length to do conversion.
	 * @return
	 */
	private String convert2Unicode(byte[] data,int offset,int length) {
		try {
			if (!this.mProcessedBytesInfo.isProcessed(data, offset, length)) {
				replaceCarriageReturnIfNecesary(data);	
				mProcessedBytesInfo.setProcessed(data, offset, length);
			}
			return new String(data,offset,length,mCharsetName);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static class CarriageReturnProcessedInfo {
		private byte[] data = null;
		private int from = 0;
		private int length = 0;
		
		public void setProcessed(byte [] data, int from, int length) {
			this.data = data;
			this.from = from;
			this.length = length;
		}
		
		/**
		 * check whether given data range is processed for carriage-return problem.
		 * @param data
		 * @param from
		 * @param length
		 * @return
		 */
		public boolean isProcessed(byte [] data, int from, int length) {
			if (data != this.data) {
				return false;
			}
			return from >= this.from && length <= this.length;
		}
	}
	
	/**
	 * for the occasion of only \r - that is \r without \n followed,replace \r with \n.
	 * do nothing for other occasions.
	 * @param data
	 */
	private void replaceCarriageReturnIfNecesary(byte[] data) {
		int pos = findCRFrom(data, 0, data.length);
		if (pos == -1) {
			return;
		}
		
		if (haveLFNearPos(data, pos)) {
			return;
		}
		
		L.d(tag, "need replace CR with LF...................");
		
		if (data[pos] == '\r') {
			data[pos] = '\n';
			L.d(tag, "replaced 1");
		} else if (pos - 1 >= 0 && data[pos - 1] == '\r') {
			L.d(tag, "replaced 2");
			data[pos - 1] = '\n';
		}
	}
	
	private boolean haveLFNearPos(byte[] data, int pos) {
		if (pos < 0 || pos >= data.length) {
			return false;
		}
		int beg = pos - 2;
		int end = pos + 3;
		if (beg < 0) {
			beg = 0;
		}
		if (end > data.length) {
			end = data.length;
		}
		for (int i = beg; i < end; ++i) {
			if (data[i] == '\n') {
				return true;
			}
		}
		return false;
	}

	private boolean seekTo(long pos) {
		try {
			mFileReader.seek(pos);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	private int findBlockDividerFrom(int from) {
		mBlockDivider.setBuffer(mBuff, false);
		return mBlockDivider.findDividerFrom(from);
	}
	
	/**
	 * @from - the offset in bytes, to find from.
	 * @to   - the offset in bytes, to find to, exclusive.
	 * @return tail position of the carriage return , -1 for not found.
	 * @attention the first byte may not be '\r' (maybe \0).
	 */
	private int findCRFrom(byte [] bytes, int from, int to) {
		if (this.mIsWideCode) {
			if (this.mIsUnicode) {
				for (int pos = from; pos < to; pos += 2) {
					if (bytes[pos] == '\r' && bytes[pos + 1] == '\0') {
						return pos + 1;
					}
				}
			} else {
				for (int pos = from; pos < to; pos += 2) {
					if (bytes[pos] == '\0' && bytes[pos + 1] == '\r') {
						return pos + 1;
					}
				}
			}
		} else {
			for (int pos = from; pos < to; ++ pos) {
				if (bytes[pos] == '\r') {
					return pos;
				}
			}
		}
		return -1;
	}

	private boolean isCarriageReturn(byte [] buff, int pos) {
		if (pos >= buff.length) {
			return false;
		}
		
		if (!mIsWideCode) {
			return buff[pos] == '\r';
		}

		// for unicode (little endian), CR is \r \0
		// for unicode big endian,      CR is \0 \r
		if (pos >= buff.length-1) {
			return false;
		}

		return (this.mIsUnicode ? 
				(buff[pos] == '\r' && buff[pos + 1] == '\0') : (buff[pos] == '\0' && buff[pos + 1] == '\r'));
	}
	
	private boolean isLinefeed(byte [] buff, int pos) {
		if (pos >= buff.length) {
			return false;
		}
		
		if (!mIsWideCode) {
			return buff[pos] == '\n';
		}

		// for unicode (little endian), CR is \n \0
		// for unicode big endian,      CR is \0 \n
		if (pos >= buff.length-1) {
			return false;
		}

		return (this.mIsUnicode ? 
				(buff[pos] == '\n' && buff[pos + 1] == '\0') : (buff[pos] == '\0' && buff[pos + 1] == '\n'));
	}

	/**
	 * @param tryLen - size to read backwardly.
	 * @return RC_READPREV_NOCARRIAGEFOUND for failed to get CR and can try next length,
	 * 		   RC_READPREV_NOPREVLINE for no previous line,
	 *         RC_READPREV_OTHERERROR for other fatal errors ,
	 *         non-negative for the tail position of CR in the mBuff or the start position if start position reached. 
	 */
	private int readPrevLineData(int tryLen) {
		// read tryLen bytes backward , not including byte of current position.
		int prePos = (int) (getCurrentOffset() - tryLen - 1);
		boolean startReached = false;
		if (prePos <= mHeadLen) {
			prePos = mHeadLen;
			startReached = true;
		}

		tryLen = (int) (getCurrentOffset() - prePos - 1);

		if (tryLen <= 0) {
			return RC_READPREVLINE_NOPREVLINE;
		}

		if (!seekTo(prePos)) {
			System.out.println("fatal error! in readPrevLineData's seekTo invoking.");
			return RC_READPREVLINE_OTHERERROR;
		}

		if (mBuff.length < tryLen) {
			mBuff = new byte [tryLen];
		}

		try {
			mBuffValidLen = mFileReader.read(mBuff, 0, tryLen);
		} catch (IOException e) {
			e.printStackTrace();
			return RC_READPREVLINE_OTHERERROR;
		}		

		if (mBuffValidLen <= 0) {
			L.e(tag, "0 byte got backwardly. why ?");
			return RC_READPREVLINE_OTHERERROR;
		}

		// the start position is reached.
		if (startReached) {
			return 0;
		}

		int rc = mBlockDivider.findDividerReversely(mBuff, mBuffValidLen);
		return rc == -1 ? RC_READPREVLINE_NOCARRIAGEFOUND : rc;
	}

	/**
	 * @param tryLen - size to read backwardly.
	 * @return RC_READPREVBLOCK_NODIVIDERFOUND for failed to get divider and can try next length,
	 * 		   RC_READPREVBLOCK_NOPREV for no previous bytes,
	 *         RC_READPREVBLOCK_OTHERERROR for other fatal errors,
	 *         others for the tail position of CR in the mBuff or the start position if start position reached. 
	 */
	private int readPrevBlockData(int tryLen) {
		// read block size bytes backward , not including byte of current position.
		int prePos = (int) (getCurrentOffset() - tryLen);
		boolean startReached = false;
		if (prePos < mHeadLen) {
			prePos = mHeadLen;
			startReached = true;
		}

		tryLen = (int) (getCurrentOffset() - prePos);

		if (tryLen <= 0) {
			return RC_READPREVBLOCK_NOPREV;
		}

		if ((prePos & 0x1) == 1 && this.mIsWideCode) {
			L.e(tag, "wrongly divivided for wide code");
		}
		
		if (!seekTo(prePos)) {
			System.out.println("fatal error! in readPrevLineData's seekTo invoking.");
			return RC_READPREVBLOCK_OTHERERROR;
		}

		if (mBuff.length < tryLen) {
			mBuff = new byte[tryLen];
		}
		
		try {
			mBuffValidLen = mFileReader.read(mBuff, 0, tryLen);
		} catch (IOException e) {
			e.printStackTrace();
			return RC_READPREVBLOCK_OTHERERROR;
		}

		if (mBuffValidLen <= 0) {
			L.e("error , 0 bytes got backwardly. from why ?");
			return RC_READPREVBLOCK_OTHERERROR;
		}

		// the start position is reached.
		if (startReached) {
			return 0;
		}

		int rc	= this.mBlockDivider.findDividerReversely(mBuff, mBuffValidLen-mBlockLowBoundSize);
		return rc == -1 ? RC_READPREVBLOCK_NODIVIDERFOUND : rc;
	}
	
	private void init() {
		mIsUnicodeBigEn = mCharsetGetter.isUnicodeBigEndian();
		mIsUnicode		= mCharsetGetter.isUnicodeSmallEndian();
		mIsWideCode = (mIsUnicodeBigEn || mIsUnicode);
		
	}

}
