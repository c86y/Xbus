/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * XBusService
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class XBusService extends Service{

    private XBusDaemon mXBusDaemon;

    @Override
    public void onCreate() {
        super.onCreate();
        mXBusDaemon = new XBusDaemon(this);
        mXBusDaemon.start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mXBusDaemon != null) {
            mXBusDaemon.stopRunning();
            mXBusDaemon = null;
        }
    }
}
