package com.cr.file.charset;

public class EncodingDefs {
	// Supported Encoding Types
	public static final int GB2312        = 0;
	public static final int GBK           = 1;
	public static final int GB18030       = 2;
	public static final int HZ            = 3;
	public static final int BIG5          = 4;
	public static final int CNS11643      = 5;
	public static final int UTF8 			= 6; // no BOM
	public static final int UNICODEBE16   = 7;
	public static final int UNICODELE16   = 8;
	public static final int ISO2022CN     = 9;
	public static final int ISO2022CN_CNS = 10;
	public static final int ISO2022CN_GB  = 11;
	public static final int EUC_KR        = 12;
	public static final int CP949         = 13;
	public static final int ISO2022KR     = 14;
	public static final int JOHAB         = 15;
	public static final int SJIS          = 16;
	public static final int EUC_JP        = 17;
	public static final int ISO2022JP     = 18;
	public static final int ASCII         = 19;
	public static final int OTHER         = 20;

	protected static final int TOTALTYPES    = 21;
	
	// Names of the encodings as understood by Java
	private static String[] javaname = null;
	private final static String mUnknown = "Unknown";

	private static EncodingDefs mInstance = null;
	

	public static EncodingDefs getInstance() {
		if (mInstance == null) {
			mInstance = new EncodingDefs();
		}
		return mInstance;
	}



	// Constructor
	public EncodingDefs() {
		javaname = new String[TOTALTYPES];

		// Assign encoding names
		javaname[GB2312] = "GB2312";
		javaname[GBK] = "GBK";
		javaname[GB18030] = "GB18030";
		javaname[HZ] = "ASCII";  // What to put here?  Sun doesn't support HZ
		javaname[ISO2022CN_GB] = "ISO2022CN_GB";
		javaname[BIG5] = "BIG5";
		javaname[CNS11643] = "EUC-TW";

		javaname[UTF8] 			= "UTF-8";
		javaname[UNICODEBE16]   = "UTF-16BE";
		javaname[UNICODELE16]   = "UTF-16LE";

		javaname[ISO2022CN_CNS] = "ISO2022CN_CNS";
		javaname[ISO2022CN] = "ISO2022CN";
		javaname[EUC_KR] = "EUC_KR";
		javaname[CP949] = "MS949";
		javaname[ISO2022KR] = "ISO2022KR";
		javaname[JOHAB] = "Johab";
		javaname[SJIS] = "SJIS";
		javaname[EUC_JP] = "EUC_JP";
		javaname[ISO2022JP] = "ISO2022JP";
		javaname[ASCII] = "ASCII";
		javaname[OTHER] = "ISO8859_1";

	}

	/**
	 * 
	 * @param index the charset index defined by this class.
	 * @return mUnknown if index is out of bound.
	 */
	public String getEncodingStr(int index) {
		if (index < 0 || index >= javaname.length) {
			return mUnknown;
		}
		return javaname[index];
	}

	/**
	 * 
	 * @param mCharsetName - a java charset name
	 * @return -1 for failed,the index of charset defined by this class.
	 */
	public int getEncodingId(String charsetName) {
		for (int i = 0; i < TOTALTYPES; ++i) {
			if (isEncodingOf(charsetName,i)) {
				return i;
			}			
		}
		return -1;
	}

	private boolean isEncodingOf(String charsetName,int id) {
		return (charsetName.equals(javaname[id]));
	}

}
