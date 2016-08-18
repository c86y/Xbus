package com.bilibili.xbus.utils;

import android.util.Log;

import com.bilibili.xbus.BuildConfig;

/**
 * @author chengyuan
 */
public class XBusLog {

    public static final boolean ENABLE = BuildConfig.DEBUG;

    private static final String TAG = "XBus";

    public static void d(String msg) {
        if (!ENABLE) return;
        if (msg == null) return;

        Log.w(TAG, msg);
    }

    public static void printStackTrace(Throwable tr) {
        if (!ENABLE) return;
        if (tr == null) return;

        Log.e(TAG, Log.getStackTraceString(tr));
    }
}
