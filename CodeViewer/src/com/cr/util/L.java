package com.cr.util;

import android.util.Log;

public class L {

    private static final String TAG = "CodeViewer";
    
	public static void e(String format, Object ... args) {
       Log.e(TAG, String.format(format, args));
	}

    public static void i(String format, Object ... args) {
        Log.i(TAG, String.format(format, args));
    }

	public static void d(String format, Object ... args) {
        Log.d(TAG, String.format(format, args));
    }

}
