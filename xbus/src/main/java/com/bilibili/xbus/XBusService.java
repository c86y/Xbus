/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * XBusService
 *
 * @author chengyuan
 */
public class XBusService extends Service {

    public static void startService(Context context) {
        Intent intent = new Intent(context, XBusService.class);
        context.startService(intent);
    }

    public static void stopService(Context context) {
        Intent intent = new Intent(context, XBusService.class);
        context.stopService(intent);
    }

    private XBusHost mXBusHost;

    @Override
    public void onCreate() {
        super.onCreate();

        mXBusHost = new XBusHost(this);
        mXBusHost.start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new RuntimeException("XBusService do not support bindService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mXBusHost != null) {
            mXBusHost.stopRunning();
            mXBusHost = null;
        }
    }
}
