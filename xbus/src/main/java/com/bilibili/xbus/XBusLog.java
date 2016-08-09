package com.bilibili.xbus;

import android.util.Log;

/**
 * Created by c86y on 2016/8/7.
 */
public class XBusLog {

    public static final boolean ENABLE = true;

    private static final String TAG = "XBus";

    static void d(String msg) {
        if (!ENABLE) return;
        if (msg == null) return;

        Log.w(TAG, msg);
    }

    static void printStackTrace(Throwable tr) {
        if (!ENABLE) return;
        if (tr == null) return;

        Log.e(TAG, Log.getStackTraceString(tr));
    }
}
