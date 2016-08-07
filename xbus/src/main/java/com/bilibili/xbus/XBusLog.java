package com.bilibili.xbus;

import android.util.Log;

/**
 * Created by c86y on 2016/8/7.
 */
class XBusLog {

    static final boolean DEBUG = true;

    private static final String TAG = "XBuss";

    static void d(String msg) {
        if (!DEBUG) return;
        if (msg == null) return;

        Log.d(TAG, msg);
    }
}
