package com.bilibili.xbus.utils;

import android.text.TextUtils;
import android.util.Log;

import com.bilibili.xbus.BuildConfig;

/**
 * @author chengyuan
 */
public class XBusLog {

    public static final boolean ENABLE = BuildConfig.DEBUG;

    private static final String TAG = "XBus";

    public static void v(String msg) {
        if (!ENABLE) return;
        if (TextUtils.isEmpty(msg)) return;
        Log.v(TAG, msg);
    }

    public static void i(String msg) {
        if (!ENABLE) return;
        if (TextUtils.isEmpty(msg)) return;
        Log.i(TAG, msg);
    }

    public static void w(String msg) {
        if (!ENABLE) return;
        if (TextUtils.isEmpty(msg)) return;
        Log.w(TAG, msg);
    }

    public static void printStackTrace(Throwable tr) {
        if (!ENABLE) return;
        if (tr == null) return;

        Log.e(TAG, Log.getStackTraceString(tr));
    }
}
